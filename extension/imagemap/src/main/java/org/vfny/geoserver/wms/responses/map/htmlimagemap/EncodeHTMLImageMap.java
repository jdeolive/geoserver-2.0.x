/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.vfny.geoserver.wms.responses.map.htmlimagemap;

import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultQuery;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.crs.ReprojectFeatureResults;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureTypes;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.MapLayer;
import org.geotools.referencing.CRS;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.spatial.BBOX;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.vfny.geoserver.wms.WMSMapContext;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Encodes a set of MapLayers in HTMLImageMap format.
 *
 * @author Mauro Bartolomeoli 
 */
public class EncodeHTMLImageMap {
    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.vfny.geoserver.responses.wms.map");
    
    private WMSMapContext mapContext;

    /** Filter factory for creating filters */
	private final static FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
    
    /** 
     * Current writer. 
     * The writer is able to encode a single feature.
     */
    private HTMLImageMapWriter writer;

    
    private boolean abortProcess;

    /**
     * Creates a new EncodeHTMLImageMap object.
     *
     * @param mapContext current wms context
     */
    public EncodeHTMLImageMap(WMSMapContext mapContext) {
        this.mapContext = mapContext;
    }

    /**
     * Aborts encoding.
     */
    public void abort() {
        abortProcess = true;
    }

    /**
     * Encodes the current set of layers.
     *
     * @param out stream to write the produced map to.
     *
     * @throws IOException if an error occurs in encoding map
     */
    public void encode(final OutputStream out) throws IOException {
        // initializes the writer
        this.writer = new HTMLImageMapWriter(out, mapContext);
        
        abortProcess = false;

        long t = System.currentTimeMillis();

        try {
        	// encodes the different layers
            writeLayers();

            this.writer.flush();
            t = System.currentTimeMillis() - t;
            LOGGER.info("HTML ImageMap generated in " + t + " ms");
        } catch (IOException ioe) {
            if (abortProcess) {
                LOGGER.fine("HTML ImageMap encoding aborted");

                return;
            } else {
                throw ioe;
            }
        } catch (AbortedException ex) {
            return;
        }
    }

    
    /**
	 * Applies Filters from style rules to the given query, to optimize
	 * DataStore queries.
	 * Similar to the method in StreamingRenderer.
	 * 
	 * @param styles
	 * @param q
	 */
    private Filter processRuleForQuery(FeatureTypeStyle[] styles) {
		try {

			// first we check to see if there are >
			// "getMaxFiltersToSendToDatastore" rules
			// if so, then we dont do anything since no matter what there's too
			// many to send down.
			// next we check for any else rules. If we find any --> dont send
			// anything to Datastore
			// next we check for rules w/o filters. If we find any --> dont send
			// anything to Datastore
			//
			// otherwise, we're gold and can "or" together all the fiters then
			// AND it with the original filter.
			// ie. SELECT * FROM ... WHERE (the_geom && BBOX) AND (filter1 OR
			// filter2 OR filter3);

			
			final List<Filter> filtersToDS = new ArrayList<Filter>();
			
			final int stylesLength = styles.length;
			
			int styleRulesLength;
			FeatureTypeStyle style;
			int u = 0;
			Rule r;
			
			for (int t = 0; t < stylesLength; t++) // look at each
			// featuretypestyle
			{
				style = styles[t];
				
				Rule[] rules=style.getRules();
				styleRulesLength = rules.length;
				
				for (u = 0; u < styleRulesLength; u++) // look at each
														// rule in the
														// featuretypestyle
				{
					r = rules[u];
					if (r.getFilter() == null)
						return null; // uh-oh has no filter (want all rows)
					if(r.hasElseFilter())
						return null;  // uh-oh has elseRule
					filtersToDS.add(r.getFilter());
				}
			}
			

			Filter ruleFiltersCombined;
			Filter newFilter;
			// We're GOLD -- OR together all the Rule's Filters
			if (filtersToDS.size() == 1) // special case of 1 filter
			{
				ruleFiltersCombined = filtersToDS.get(0);
			} else {
				// build it up
				ruleFiltersCombined = filtersToDS.get(0);
				final int size = filtersToDS.size();
				for (int t = 1; t < size; t++) // NOTE: dont
				// redo 1st one
				{
					newFilter = filtersToDS.get(t);
					ruleFiltersCombined = filterFactory.or(
							ruleFiltersCombined, newFilter);
				}
			}
			return ruleFiltersCombined;
			/*
			// combine with the geometry filter (preexisting)
			ruleFiltersCombined = filterFactory.or(
					q.getFilter(), ruleFiltersCombined);

			// set the actual filter
			q.setFilter(ruleFiltersCombined);
			*/
		} catch (Exception e) {
			return null;
		}
	}

    /**
     * Filters the feature type styles of <code>style</code> returning only
     * those that apply to <code>featureType</code>
     * <p>
     * This methods returns feature types for which
     * <code>featureTypeStyle.getFeatureTypeName()</code> matches the name
     * of the feature type of <code>featureType</code>, or matches the name of
     * any parent type of the feature type of <code>featureType</code>. This
     * method returns an empty array in the case of which no rules match.
     * </p>
     * @param style The style containing the feature type styles.
     * @param featureType The feature type being filtered against.
     *
     */
    protected FeatureTypeStyle[] filterFeatureTypeStyles(Style style, SimpleFeatureType featureType) {
        FeatureTypeStyle[] featureTypeStyles = style.getFeatureTypeStyles();

        if ((featureTypeStyles == null) || (featureTypeStyles.length == 0)) {
            return new FeatureTypeStyle[0];
        }

        List<FeatureTypeStyle> filtered = new ArrayList<FeatureTypeStyle>(featureTypeStyles.length);

        for (int i = 0; i < featureTypeStyles.length; i++) {
            FeatureTypeStyle featureTypeStyle = featureTypeStyles[i];
            String featureTypeName = featureTypeStyle.getFeatureTypeName();

            //does this style have any rules
            if (featureTypeStyle.getRules() == null || featureTypeStyle.getRules().length == 0 ) {
            	continue;
            }
            
            //does this style apply to the feature collection
            if (featureType.getTypeName().equalsIgnoreCase(featureTypeName)
                    || FeatureTypes.isDecendedFrom(featureType,null,featureTypeName)) {
                filtered.add(featureTypeStyle);
            }
        }

        return filtered.toArray(new FeatureTypeStyle[filtered.size()]);
    }
    
    /**
     * Encodes the current set of layers.
     *
     * @throws IOException if an error occurs during encoding
     * @throws AbortedException if the encoding is aborted
     *
     * @task TODO: respect layer filtering given by their Styles
     */
    @SuppressWarnings("unchecked")
	private void writeLayers() throws IOException, AbortedException {
        MapLayer[] layers = mapContext.getLayers();
        int nLayers = layers.length;       

        for (int i = 0; i < nLayers; i++) {
            MapLayer layer = layers[i];
            FeatureSource<SimpleFeatureType, SimpleFeature> fSource;
            fSource = (FeatureSource<SimpleFeatureType, SimpleFeature>) layer.getFeatureSource();
            SimpleFeatureType schema = fSource.getSchema();
            /*FeatureSource fSource = layer.getFeatureSource();
            FeatureType schema = fSource.getSchema();*/

            try {
            	ReferencedEnvelope aoi = mapContext.getAreaOfInterest();
                
                CoordinateReferenceSystem sourceCrs = schema.getGeometryDescriptor().getCoordinateReferenceSystem();

                boolean reproject = (sourceCrs != null)
                && !CRS.equalsIgnoreMetadata(aoi.getCoordinateReferenceSystem(), sourceCrs); 
                if (reproject) {
                    aoi = aoi.transform(sourceCrs, true);
                }
            	// apply filters.
                // 1) bbox filter
                BBOX bboxFilter = filterFactory.bbox(schema.getGeometryDescriptor().getLocalName(), 
                        aoi.getMinX() , aoi.getMinY(), aoi.getMaxX(), aoi.getMaxY(), null);
                DefaultQuery q = new DefaultQuery(schema.getTypeName(), bboxFilter);
                
                String mapId = null;               

                mapId = schema.getTypeName();

                writer.write("<map name=\"" + mapId + "\">\n");

            	// 2) definition query filter
                Query definitionQuery = layer.getQuery();
                LOGGER.info("Definition Query: "+definitionQuery.toString());
                if (!definitionQuery.equals(Query.ALL)) {
                    if (q.equals(Query.ALL)) {
                        q = (DefaultQuery) definitionQuery;
                    } else {
                        q = (DefaultQuery) DataUtilities.mixQueries(definitionQuery, q, "HTMLImageMapEncoder");
                    }
                }
                
                FeatureTypeStyle[] ftsList=filterFeatureTypeStyles(layer.getStyle(), fSource.getSchema());
            	// 3) rule filters               
                Filter ruleFilter=processRuleForQuery(ftsList);
				if(ruleFilter!=null) {
					// combine with the geometry filter (preexisting)
					ruleFilter = filterFactory.and(
						q.getFilter(), ruleFilter);

					// set the actual filter
					q.setFilter(ruleFilter);
                	//q = (DefaultQuery) DataUtilities.mixQueries(new DefaultQuery(schema.getTypeName(),ruleFilter), q, "HTMLImageMapEncoder");
				}
                //ensure reprojection occurs, do not trust query, use the wrapper  
                FeatureCollection<SimpleFeatureType, SimpleFeature> fColl = null;//fSource.getFeatures(q);
                //FeatureCollection fColl=null;
                if ( reproject ) {
                	fColl=new ReprojectFeatureResults( fSource.getFeatures(q),mapContext.getCoordinateReferenceSystem() );
                } else
                	fColl=fSource.getFeatures(q);
                
                // encodes the current layer, using the defined style
                writer.writeFeatures(fColl, layer.getStyle(),ftsList);
                writer.write("</map>\n");
                
            } catch (IOException ex) {
                throw ex;
            } catch (AbortedException ae) {
                LOGGER.info("process aborted: " + ae.getMessage());
                throw ae;
            } catch (Throwable t) {
                LOGGER.warning("UNCAUGHT exception: " + t.getMessage());

                IOException ioe = new IOException("UNCAUGHT exception: " + t.getMessage());
                ioe.setStackTrace(t.getStackTrace());
                throw ioe;
            }
        }
    }
}
