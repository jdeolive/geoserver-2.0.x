/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.vfny.geoserver.wms.responses.map.kml;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import org.apache.batik.dom.util.HashTable;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.map.MapLayer;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Symbolizer;
import org.geotools.xml.transform.Translator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.vfny.geoserver.wms.WMSMapContext;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;


/**
 * Transforms a feature collection to a kml document consisting of nested
 * "Style" and "Placemark" elements for each feature in the collection.
 * A new transfomer must be instantianted for each feature collection, 
 * the feature collection provided to the translator is supposed to be
 * the one coming out of the MapLayer 
 * <p>
 * Usage:
 * </p>
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 *
 */
public class KMLVectorTransformer extends KMLMapTransformer {
    public KMLVectorTransformer(WMSMapContext mapContext, MapLayer mapLayer) {
        super(mapContext, mapLayer);

        setNamespaceDeclarationEnabled(false);
    }

   /**
     * Sets the scale denominator.
     */
    public void setScaleDenominator(double scaleDenominator) {
        this.scaleDenominator = scaleDenominator;
    }

    public Translator createTranslator(ContentHandler handler) {
        return new KMLTranslator(handler);
    }

    protected class KMLTranslator extends KMLMapTranslatorSupport {
        /**
         * Store the regionating strategy being applied
         */
        private RegionatingStrategy myStrategy;

        public KMLTranslator(ContentHandler contentHandler) {
            super(contentHandler);

            KMLGeometryTransformer geometryTransformer = new KMLGeometryTransformer();
            //geometryTransformer.setUseDummyZ( true );
            geometryTransformer.setOmitXMLDeclaration(true);
            geometryTransformer.setNamespaceDeclarationEnabled(true);

            GeoServer config = mapContext.getRequest().getWMS().getGeoServer();
            geometryTransformer.setNumDecimals(config.getGlobal().getNumDecimals());

            geometryTranslator = 
                (KMLGeometryTransformer.KMLGeometryTranslator)
                geometryTransformer.createTranslator(contentHandler, mapContext);
        }

        public void setRegionatingStrategy(RegionatingStrategy rs){
            myStrategy = rs;
        }

        public void encode(Object o) throws IllegalArgumentException {
            FeatureCollection<SimpleFeatureType, SimpleFeature> features = (FeatureCollection) o;
            SimpleFeatureType featureType = features.getSchema();
            Catalog catalog = mapContext.getRequest().getWMS().getGeoServer().getCatalog();

            if (isStandAlone()) {
                start( "kml" );
            }

            //start the root document, name it the name of the layer
            start("Document", KMLUtils.attributes(
                    new String[] {"xmlns:atom", "http://purl.org/atom/ns#" }));
            element("name", mapLayer.getTitle());

            String relLinks = (String)mapContext.getRequest().getFormatOptions().get("relLinks");
            // Add prev/next links if requested
            if (mapContext.getRequest().getMaxFeatures() != null &&
                relLinks != null && relLinks.equalsIgnoreCase("true") ){

                String linkbase = "";
                try {
                    linkbase = getFeatureTypeURL();
                    linkbase += ".kml";
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }

                int maxFeatures = mapContext.getRequest().getMaxFeatures();
                int startIndex =
                    (mapContext.getRequest().getStartIndex() == null)
                    ? 0 
                    : mapContext.getRequest().getStartIndex().intValue();
                int prevStart = startIndex - maxFeatures;
                int nextStart = startIndex + maxFeatures;

                // Previous page, if any
                if (prevStart >= 0) {
                    String prevLink = linkbase + "?startindex=" 
                        + prevStart + "&maxfeatures=" + maxFeatures;
                    element("atom:link", null, KMLUtils.attributes(new String[] {
                                "rel", "prev", "href", prevLink }));
                    encodeSequentialNetworkLink(linkbase, prevStart,
                            maxFeatures, "prev", "Previous page");
                }

                // Next page, if any
                if (features.size() >= maxFeatures) {
                    String nextLink = linkbase + "?startindex=" + nextStart
                        + "&maxfeatures=" + maxFeatures;
                    element("atom:link", null, KMLUtils.attributes(new String[] {
                                "rel", "next", "href", nextLink }));
                    encodeSequentialNetworkLink(linkbase, nextStart,
                            maxFeatures, "next", "Next page");
                }
            }

            //get the styles for the layer
            FeatureTypeStyle[] featureTypeStyles = KMLUtils.filterFeatureTypeStyles(mapLayer.getStyle(),
                    featureType);

            // encode the schemas (kml 2.2)
            encodeSchemas(features);

            // encode the layers
            encode(features, featureTypeStyles);
            
            //encode the legend
            //encodeLegendScreenOverlay();
            end("Document");
            
            if ( isStandAlone() ) {
                end( "kml" );
            }
        }

        /**
         * 
         * Encodes a networklink for previous or next document in a sequence
         * 
         * Note that in KML 2.2 atom:link is supported and may be better.
         *
         * @param linkbase the base fore creating URLs
         * @param prevStart previous start value
         * @param maxFeatures maximum number of features to return
         * @param id attribute to use for this NetworkLink
         * @param readableName goes into linkName
         */
        private void encodeSequentialNetworkLink(String linkbase, int prevStart,
                int maxFeatures, String id, String readableName) {
            String link = linkbase + "?startindex=" + prevStart
                    + "&maxfeatures=" + maxFeatures;
            start("NetworkLink", KMLUtils.attributes(new String[] {"id", id}));
            element("description",readableName);
            start("Link");
            element("href",link);
            end("Link");
            end("NetworkLink");
        }

        /**
         * Encodes the <Schema> element in kml 2.2
         * @param featureTypeStyles
         */
        protected void encodeSchemas(FeatureCollection<SimpleFeatureType, SimpleFeature> featureTypeStyles) {
            // the code is at the moment in KML3VectorTransformer
        }

        protected void encode(FeatureCollection<SimpleFeatureType, SimpleFeature> features,
                FeatureTypeStyle[] styles) {
           //grab a reader and process
            FeatureIterator<SimpleFeature> reader = null;

            try {
                //grab a reader and process
                reader = features.features();
                
                // Write Styles
                while (reader.hasNext()) {
                    SimpleFeature feature = (SimpleFeature) reader.next();
                    try {
                        List<Symbolizer> symbolizers = filterSymbolizers(feature, styles);
                        if (symbolizers.size() > 0) {
                            encodePlacemark(feature, symbolizers);    
                        }
                    } catch (RuntimeException t) {
                        // if the stream has been closed by the client don't keep on going forward,
                        // this is not a feature local issue
                        //
                        if(t.getCause() instanceof SAXException)
                            throw t;
                        else
                            LOGGER.log(
                                    Level.WARNING,
                                    "Failure tranforming feature to KML:" + feature.getID(),
                                    t
                                );
                    } 
                }
            } finally {
                //make sure we always close
                features.close(reader);
            }
        }
    }

    
}
