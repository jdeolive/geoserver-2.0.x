/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.vfny.geoserver.wms.responses.map.kml;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wms.util.WMSRequests;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultQuery;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.crs.ReprojectFeatureResults;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureTypes;
import org.geotools.filter.IllegalFilterException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.MapLayer;
import org.geotools.referencing.CRS;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.vfny.geoserver.wms.WMSMapContext;
import org.vfny.geoserver.wms.WmsException;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

import com.vividsolutions.jts.geom.Envelope;


/**
 * Some convenience methods used by the kml transformers.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 * @deprecated use {@link WMSRequests}.
 */
public class KMLUtils {
    /**
     * logger
     */
    static Logger LOGGER = org.geotools.util.logging.Logging
            .getLogger("org.geoserver.kml");

    /**
     * Tolerance used to compare doubles for equality
     */
    static final double TOLERANCE = 1e-6;

    private static final CoordinateReferenceSystem WGS84;
    private static final int RULES = 0;
    private static final int ELSE_RULES = 1;

    static {
        try {
            WGS84 = CRS.decode("EPSG:4326");
        } catch (Exception e) {
            throw new RuntimeException(
                    "Cannot decode EPSG:4326, the CRS subsystem must be badly broken...");
        }
    }

    public final static Envelope WORLD_BOUNDS_WGS84 = new Envelope(-180,180,-90,90);    
    
    private final static double[] TILE_RESOLUTIONS = new double[100];

    static {
        for (int i = 0; i < TILE_RESOLUTIONS.length; i++) {
            TILE_RESOLUTIONS[i] = WORLD_BOUNDS_WGS84.getWidth() / ((0x01 << i) * 256);
        }
    }

    /**
     * Factory used to create filter objects
     */
    private static FilterFactory filterFactory = (FilterFactory) CommonFactoryFinder
            .getFilterFactory(null);

    /**
     * Encodes the url of a GetMap request from a map context + map layer.
     * <p>
     * If the <tt>mapLayer</tt> argument is <code>null</code>, the request is
     * made including all layers in the <tt>mapContexT</tt>.
     * </p>
     * <p>
     * If the <tt>bbox</tt> argument is <code>null</code>. {@link WMSMapContext#getAreaOfInterest()}
     * is used for the bbox parameter.
     * </p>
     *
     * @param mapContext The map context.
     * @param mapLayer The Map layer, may be <code>null</code>.
     * @param layerIndex The index of the layer in the request.
     * @param bbox The bounding box of the request, may be <code>null</code>.
     * @param kvp Additional or overiding kvp parameters, may be <code>null</code>
     * @param tile Flag controlling whether the request should be made against tile cache
     *
     * @return The full url for a getMap request.
     * @deprecated use {@link WMSRequests#getGetMapUrl(WMSMapContext, MapLayer, Envelope, String[])}
     */
    public static String getMapUrl(
            WMSMapContext mapContext,
            MapLayer mapLayer,
            int layerIndex,
            Envelope bbox,
            String[] kvp,
            boolean tile) {
       
        if ( tile ) {
            return WMSRequests.getTiledGetMapUrl( mapContext.getRequest(), mapLayer, layerIndex, bbox, kvp );
        }
        
        return WMSRequests.getGetMapUrl( 
                    mapContext.getRequest(),
                    mapLayer,
                    layerIndex,
                    bbox,
                    kvp 
                ); 
    }

    /**
     * Encodes the url of a GetMap request from a map context + map layer.
     * <p>
     * If the <tt>mapLayer</tt> argument is <code>null</code>, the request is
     * made including all layers in the <tt>mapContexT</tt>.
     * </p>
     * @param mapContext The map context.
     * @param mapLayer The Map layer, may be <code>null</code>
     * @param layerIndex The index of the layer in the request.
     * @param kvp Additional or overidding kvp parameters, may be <code>null</code>
     * @param tile Flag controlling wether the request should be made against tile cache
     *
     * @return The full url for a getMap request.
     * @deprecated use {@link WMSRequests#getGetMapUrl(WMSMapContext, MapLayer, int, Envelope, String[])}
     */
    public static String getMapUrl(WMSMapContext mapContext, MapLayer mapLayer, int layerIndex, boolean tile) {
        return getMapUrl(mapContext, mapLayer, layerIndex, mapContext.getAreaOfInterest(), null, tile);
    }

    /**
     * Encodes the url for a GetLegendGraphic request from a map context + map layer.
     *
     * @param mapContext The map context.
     * @param mapLayer The map layer.
     * @param kvp Additional or overidding kvp parameters, may be <code>null</code>
     *
     * @return A map containing all the key value pairs for a GetLegendGraphic request.
     * @deprecated use {@link WMSRequests#getGetLegendGraphicUrl(WMSMapContext, MapLayer, String[])
     */
    public static String getLegendGraphicUrl(WMSMapContext mapContext, MapLayer mapLayer,
        String[] kvp) {
        return WMSRequests.getGetLegendGraphicUrl(mapContext.getRequest(), mapLayer, kvp);
    }

    /**
     * Creates sax attributes from an array of key value pairs.
     *
     * @param nameValuePairs Alternating key value pair array.
     *
     */
    public static Attributes attributes(String[] nameValuePairs) {
        AttributesImpl attributes = new AttributesImpl();

        for (int i = 0; i < nameValuePairs.length; i += 2) {
            String name = nameValuePairs[i];
            String value = nameValuePairs[i + 1];

            attributes.addAttribute("", name, name, "", value);
        }

        return attributes;
    }
    
    /**
     * Filters the rules of <code>featureTypeStyle</code> returnting only
     * those that apply to <code>feature</code>.
     * <p>
     * This method returns rules for which:
     * <ol>
     *  <li><code>rule.getFilter()</code> matches <code>feature</code>, or:
     *  <li>the rule defines an "ElseFilter", and the feature matches no
     *  other rules.
     * </ol>
     * This method returns an empty array in the case of which no rules
     * match.
     * </p>
     * @param featureTypeStyle The feature type style containing the rules.
     * @param feature The feature being filtered against.
     *
     */
    public static Rule[] filterRules(FeatureTypeStyle featureTypeStyle,
            SimpleFeature feature, double scaleDenominator) {
        Rule[] rules = featureTypeStyle.getRules();

        if ((rules == null) || (rules.length == 0)) {
            return new Rule[0];
        }

        ArrayList filtered = new ArrayList(rules.length);

        //process the rules, keep track of the need to apply an else filters
        boolean match = false;
        boolean hasElseFilter = false;

        for (int i = 0; i < rules.length; i++) {
            Rule rule = rules[i];
            LOGGER.finer(new StringBuffer("Applying rule: ").append(
                    rule.toString()).toString());

            //does this rule have an else filter
            if (rule.hasElseFilter()) {
                hasElseFilter = true;

                continue;
            }

            //is this rule within scale?
            if (!isWithInScale(rule, scaleDenominator)) {
                continue;
            }

            //does this rule have a filter which applies to the feature
            Filter filter = rule.getFilter();

            if ((filter == null) || filter.evaluate(feature)) {
                match = true;

                filtered.add(rule);
            }
        }

        //if no rules mached the feautre, re-run through the rules applying
        // any else filters
        if (!match && hasElseFilter) {
            //loop through again and apply all the else rules
            for (int i = 0; i < rules.length; i++) {
                Rule rule = rules[i];

                //is this rule within scale?
                if (!isWithInScale(rule, scaleDenominator)) {
                    continue;
                }

                if (rule.hasElseFilter()) {
                    filtered.add(rule);
                }
            }
        }

        return (Rule[]) filtered.toArray(new Rule[filtered.size()]);
    }

    /**
     * Checks if a rule can be triggered at the current scale level
     * 
     * @param r
     *            The rule
     * @return true if the scale is compatible with the rule settings
     */
    public static boolean isWithInScale(Rule r, double scaleDenominator) {
        return ((r.getMinScaleDenominator() - TOLERANCE) <= scaleDenominator)
                && ((r.getMaxScaleDenominator() + TOLERANCE) > scaleDenominator);
    }
    
    public static int findZoomLevel(Envelope extent){
        double resolution = Math.max(extent.getWidth()/256d, extent.getHeight() / 256d);
        
        int i;
        
        for (i = 1; i < TILE_RESOLUTIONS.length; i++){
            if (resolution > TILE_RESOLUTIONS[i]) {
                i--;
                break;
            }
        }
        
        return i;
    }

    public static Envelope expandToTile(Envelope extent){
        double resolution = Math.max(extent.getWidth() / 256d, extent.getHeight() / 256d);
        
        int i = findZoomLevel(extent);
         
        while (i > 0) {
            resolution = TILE_RESOLUTIONS[i];

            double tilelon = resolution * 256;
            double tilelat = resolution * 256;

            double lon0 = extent.getMinX() - WORLD_BOUNDS_WGS84.getMinX();
            double lon1 = extent.getMaxX() - WORLD_BOUNDS_WGS84.getMinX();

            int col0 = (int) Math.floor(lon0 / tilelon);
            int col1 = (int) Math.floor((lon1 / tilelon) - 1E-9);

            double lat0 = extent.getMinY() - WORLD_BOUNDS_WGS84.getMinY();
            double lat1 = extent.getMaxY() - WORLD_BOUNDS_WGS84.getMinY();

            int row0 = (int) Math.floor(lat0 / tilelat);
            int row1 = (int) Math.floor((lat1 / tilelat) - 1E-9);

            if ((col0 == col1) && (row0 == row1)) {
                double tileoffsetlon = WORLD_BOUNDS_WGS84.getMinX() + (col0 * tilelon);
                double tileoffsetlat = WORLD_BOUNDS_WGS84.getMinY() + (row0 * tilelat);

                return new Envelope(tileoffsetlon, tileoffsetlon + tilelon, tileoffsetlat,
                        tileoffsetlat + tilelat);
            } else {
                i--;
            }
        }
        
        return WORLD_BOUNDS_WGS84;
    }
    
    /**
     * Utility method to convert an int into hex, padded to two characters.
     * handy for generating colour strings.
     *
     * @param i Int to convert
     * @return String a two character hex representation of i
     * NOTE: this is a utility method and should be put somewhere more useful.
     */
    public static String intToHex(int i) {
        String prelim = Integer.toHexString(i);

        if (prelim.length() < 2) {
            prelim = "0" + prelim;
        }

        return prelim;
    }

    /**
     * Utility method to convert a Color and opacity (0,1.0) into a KML
     * color ref.
     *
     * @param c The color to convert.
     * @param opacity Opacity / alpha, double from 0 to 1.0.
     *
     * @return A String of the form "AABBGGRR".
     */
    public static String colorToHex(Color c, double opacity) {
        return new StringBuffer().append(
                intToHex(new Float(255 * opacity).intValue())).append(
                intToHex(c.getBlue())).append(intToHex(c.getGreen())).append(
                intToHex(c.getRed())).toString();
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
    public static FeatureTypeStyle[] filterFeatureTypeStyles(Style style,
            SimpleFeatureType featureType) {
        FeatureTypeStyle[] featureTypeStyles = style.getFeatureTypeStyles();

        if ((featureTypeStyles == null) || (featureTypeStyles.length == 0)) {
            return new FeatureTypeStyle[0];
        }

        ArrayList filtered = new ArrayList(featureTypeStyles.length);

        for (int i = 0; i < featureTypeStyles.length; i++) {
            FeatureTypeStyle featureTypeStyle = featureTypeStyles[i];
            String featureTypeName = featureTypeStyle.getFeatureTypeName();

            //does this style have any rules
            if (featureTypeStyle.getRules() == null
                    || featureTypeStyle.getRules().length == 0) {
                continue;
            }

            //does this style apply to the feature collection
            if (featureType.getTypeName().equalsIgnoreCase(featureTypeName)
                    || FeatureTypes.isDecendedFrom(featureType, null,
                            featureTypeName)) {
                filtered.add(featureTypeStyle);
            }
        }

        return (FeatureTypeStyle[]) filtered
                .toArray(new FeatureTypeStyle[filtered.size()]);
    }

    public static FeatureCollection<SimpleFeatureType, SimpleFeature> loadFeatureCollection(
            FeatureSource<SimpleFeatureType, SimpleFeature> featureSource,
            MapLayer layer, WMSMapContext mapContext) throws Exception {
        SimpleFeatureType schema = featureSource.getSchema();

        Envelope envelope = mapContext.getAreaOfInterest();
        ReferencedEnvelope aoi = new ReferencedEnvelope(envelope, mapContext
                .getCoordinateReferenceSystem());
        CoordinateReferenceSystem sourceCrs = schema
                .getCoordinateReferenceSystem();

        boolean reprojectBBox = (sourceCrs != null)
                && !CRS.equalsIgnoreMetadata(
                        aoi.getCoordinateReferenceSystem(), sourceCrs);
        if (reprojectBBox) {
            aoi = aoi.transform(sourceCrs, true);
        }

        Filter filter = createBBoxFilter(schema, aoi);

        // now build the query using only the attributes and the bounding
        // box needed
        DefaultQuery q = new DefaultQuery(schema.getTypeName());
        q.setFilter(filter);

        // now, if a definition query has been established for this layer,
        // be sure to respect it by combining it with the bounding box one.
        Query definitionQuery = layer.getQuery();

        if (definitionQuery != Query.ALL) {
            if (q == Query.ALL) {
                q = (DefaultQuery) definitionQuery;
            } else {
                q = (DefaultQuery) DataUtilities.mixQueries(definitionQuery, q,
                        "KMLEncoder");
            }
        }

        // handle startIndex requested by client query
        q.setStartIndex(definitionQuery.getStartIndex());

        // check the regionating strategy
        RegionatingStrategy regionatingStrategy = null;
        String stratname = (String) mapContext.getRequest().getFormatOptions()
                .get("regionateBy");
        if (("auto").equals(stratname)) {
            Catalog catalog = mapContext.getRequest().getWMS().getGeoServer().getCatalog();
            Name name = layer.getFeatureSource().getName();
            stratname = catalog.getFeatureTypeByName(name).getMetadata().get( "kml.regionateStrategy",String.class );
            if (stratname == null || "".equals( stratname ) ){
                stratname = "best_guess";
                LOGGER.log(
                        Level.FINE,
                        "No default regionating strategy has been configured in " + name
                        + "; using automatic best-guess strategy."
                    );
            }
        }

        if (stratname != null) {
            regionatingStrategy = findStrategyByName(stratname);
            
            // if a strategy was specified but we did not find it, let the user
            // know
            if (regionatingStrategy == null)
                throw new WmsException("Unknown regionating strategy " + stratname);
        }

        // try to load less features by leveraging regionating strategy and the
        // SLD
        Filter regionatingFilter = Filter.INCLUDE;

        if (regionatingStrategy != null)
            regionatingFilter = regionatingStrategy.getFilter(mapContext, layer);

        Filter ruleFilter = summarizeRuleFilters(getLayerRules(featureSource
                .getSchema(), layer.getStyle()));
        Filter finalFilter = joinFilters(q.getFilter(), joinFilters(ruleFilter,
                regionatingFilter));
        q.setFilter(finalFilter);

        // make sure we output in 4326 since that's what KML mandates
        if (sourceCrs != null && !CRS.equalsIgnoreMetadata(WGS84, sourceCrs)) {
            return new ReprojectFeatureResults(featureSource.getFeatures(q),
                    WGS84);
        }

        return featureSource.getFeatures(q);
    }

    public static RegionatingStrategy findStrategyByName(String name) {
        List<RegionatingStrategyFactory> factories = GeoServerExtensions
            .extensions(RegionatingStrategyFactory.class);
        Iterator<RegionatingStrategyFactory> it = factories.iterator();
        while (it.hasNext()) {
            RegionatingStrategyFactory factory = it.next();
            if (factory.canHandle(name)) {
                return factory.createStrategy();
            }
        }

        return null;
    }

    /**
     * Creates the bounding box filters (one for each geometric attribute)
     * needed to query a <code>MapLayer</code>'s feature source to return
     * just the features for the target rendering extent
     * 
     * @param schema
     *            the layer's feature source schema
     * @param bbox
     *            the expression holding the target rendering bounding box
     * @return an or'ed list of bbox filters, one for each geometric attribute
     *         in <code>attributes</code>. If there are just one geometric
     *         attribute, just returns its corresponding
     *         <code>GeometryFilter</code>.
     * @throws IllegalFilterException
     *             if something goes wrong creating the filter
     */
    private static Filter createBBoxFilter(SimpleFeatureType schema,
            Envelope bbox) throws IllegalFilterException {
        List filters = new ArrayList();
        for (int j = 0; j < schema.getAttributeCount(); j++) {
            AttributeDescriptor attType = schema.getDescriptor(j);

            if (attType instanceof GeometryDescriptor) {
                Filter gfilter = filterFactory.bbox(attType.getLocalName(),
                        bbox.getMinX(), bbox.getMinY(), bbox.getMaxX(), bbox
                                .getMaxY(), null);
                filters.add(gfilter);
            }
        }

        if (filters.size() == 0)
            return Filter.INCLUDE;
        else if (filters.size() == 1)
            return (Filter) filters.get(0);
        else
            return filterFactory.or(filters);
    }

    private static List[] getLayerRules(SimpleFeatureType ftype, Style style) {
        List[] result = new List[] { new ArrayList(), new ArrayList() };

        final String typeName = ftype.getTypeName();
        FeatureTypeStyle[] featureStyles = style.getFeatureTypeStyles();
        final int length = featureStyles.length;
        for (int i = 0; i < length; i++) {
            // getting feature styles
            FeatureTypeStyle fts = featureStyles[i];

            // check if this FTS is compatible with this FT.
            if ((typeName != null)
                    && FeatureTypes.isDecendedFrom(ftype, null, fts
                            .getFeatureTypeName())) {

                // get applicable rules at the current scale
                Rule[] ftsRules = fts.getRules();
                for (int j = 0; j < ftsRules.length; j++) {
                    // getting rule
                    Rule r = ftsRules[j];

                    if (KMLUtils.isWithInScale(r, 1)) {
                        if (r.hasElseFilter()) {
                            result[ELSE_RULES].add(r);
                        } else {
                            result[RULES].add(r);
                        }
                    }
                }
            }
        }

        return result;
    }

    private static Filter joinFilters(Filter first, Filter second) {
        if (Filter.EXCLUDE.equals(first) || Filter.EXCLUDE.equals(second))
            return Filter.EXCLUDE;

        if (first == null || Filter.INCLUDE.equals(first))
            return second;

        if (second == null || Filter.INCLUDE.equals(second))
            return first;

        FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
        return ff.and(first, second);
    }

    /**
     * Summarizes, when possible, the rule filters into one.
     * 
     * @param rules
     * @param originalFiter
     * @return
     */
    private static Filter summarizeRuleFilters(List[] rules) {
        if (rules[RULES].size() == 0 || rules[ELSE_RULES].size() > 0)
            return Filter.INCLUDE;

        List filters = new ArrayList();
        for (Iterator it = rules[RULES].iterator(); it.hasNext();) {
            Rule rule = (Rule) it.next();
            // if there is a single rule asking for all filters, we have to
            // return everything that the original filter returned already
            if (rule.getFilter() == null
                    || Filter.INCLUDE.equals(rule.getFilter()))
                return Filter.INCLUDE;
            else
                filters.add(rule.getFilter());
        }

        FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
        return ff.or(filters);
    }
}
