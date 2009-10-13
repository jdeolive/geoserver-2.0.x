/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.vfny.geoserver.wms.responses.map.svg;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;

import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultQuery;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.feature.FeatureIterator;
import org.geotools.filter.Expression;
import org.geotools.filter.FilterFactory;
import org.geotools.filter.FilterFactoryFinder;
import org.geotools.filter.FilterType;
import org.geotools.filter.GeometryFilter;
import org.geotools.map.MapLayer;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.vfny.geoserver.wms.WMSMapContext;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;


/**
 * DOCUMENT ME!
 *
 * @author Gabriel Rold?n
 * @version $Id$
 */
public class EncodeSVG {
    /** DOCUMENT ME! */
    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.vfny.geoserver.responses.wms.map");
    private static final String DOCTYPE = "<!DOCTYPE svg \n\tPUBLIC \"-//W3C//DTD SVG 20001102//EN\" \n\t\"http://www.w3.org/TR/2000/CR-SVG-20001102/DTD/svg-20001102.dtd\">\n";

    /** the XML and SVG header */
    private static final String SVG_HEADER = "<?xml version=\"1.0\" standalone=\"no\"?>\n\t"
        + "<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" \n\tstroke=\"green\" \n\tfill=\"none\" \n\tstroke-width=\"0.1%\"\n\tstroke-linecap=\"round\"\n\tstroke-linejoin=\"round\"\n\twidth=\"_width_\" \n\theight=\"_height_\" \n\tviewBox=\"_viewBox_\" \n\tpreserveAspectRatio=\"xMidYMid meet\">\n";

    /** the SVG closing element */
    private static final String SVG_FOOTER = "</svg>\n";

    /** DOCUMENT ME! */
    private WMSMapContext mapContext;

    /** DOCUMENT ME! */
    private SVGWriter writer;

    /** DOCUMENT ME! */
    private boolean abortProcess;

    /**
     * Creates a new EncodeSVG object.
     *
     * @param mapContext DOCUMENT ME!
     */
    public EncodeSVG(WMSMapContext mapContext) {
        this.mapContext = mapContext;
    }

    /**
     * DOCUMENT ME!
     */
    public void abort() {
        abortProcess = true;
    }

    /**
     * DOCUMENT ME!
     *
     * @param out DOCUMENT ME!
     *
     * @throws IOException DOCUMENT ME!
     */
    public void encode(final OutputStream out) throws IOException {
        Envelope env = this.mapContext.getAreaOfInterest();
        this.writer = new SVGWriter(out, mapContext);
        writer.setMinCoordDistance(env.getWidth() / 1000);

        abortProcess = false;

        long t = System.currentTimeMillis();

        try {
            writeHeader();

            writeLayers();

            writer.write(SVG_FOOTER);

            this.writer.flush();
            t = System.currentTimeMillis() - t;
            LOGGER.info("SVG generated in " + t + " ms");
        } catch (IOException ioe) {
            if (abortProcess) {
                LOGGER.fine("SVG encoding aborted");

                return;
            } else {
                throw ioe;
            }
        } catch (AbortedException ex) {
            return;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public String createViewBox() {
        Envelope referenceSpace = mapContext.getAreaOfInterest();
        String viewBox = writer.getX(referenceSpace.getMinX()) + " "
            + (writer.getY(referenceSpace.getMinY()) - referenceSpace.getHeight()) + " "
            + referenceSpace.getWidth() + " " + referenceSpace.getHeight();

        return viewBox;
    }

    /**
     * DOCUMENT ME!
     *
     * @throws IOException DOCUMENT ME!
     */
    private void writeHeader() throws IOException {
        //TODO: this does not write out the doctype definition, there should be 
        // a configuration option wether to include it or not.
        String viewBox = createViewBox();
        String header = SVG_HEADER.replaceAll("_viewBox_", viewBox);
        header = header.replaceAll("_width_", String.valueOf(mapContext.getMapWidth()));
        header = header.replaceAll("_height_", String.valueOf(mapContext.getMapHeight()));
        writer.write(header);
    }

    /**
     * DOCUMENT ME!
     *
     * @param layer DOCUMENT ME!
     *
     * @throws IOException DOCUMENT ME!
     */
    private void writeDefs(SimpleFeatureType layer) throws IOException {
        GeometryDescriptor gtype = layer.getGeometryDescriptor();
        Class geometryClass = gtype.getType().getBinding();

        if ((geometryClass == MultiPoint.class) || (geometryClass == Point.class)) {
            writePointDefs();
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @throws IOException DOCUMENT ME!
     */
    private void writePointDefs() throws IOException {
        writer.write(
            "<defs>\n\t<circle id='point' cx='0' cy='0' r='0.25%' fill='blue'/>\n</defs>\n");
    }

    /**
     * DOCUMENT ME!
     *
     * @throws IOException DOCUMENT ME!
     * @throws AbortedException DOCUMENT ME!
     *
     * @task TODO: respect layer filtering given by their Styles
     */
    @SuppressWarnings("unchecked")
    private void writeLayers() throws IOException, AbortedException {
        MapLayer[] layers = mapContext.getLayers();
        int nLayers = layers.length;

        // FeatureTypeInfo layerInfo = null;
        int defMaxDecimals = writer.getMaximunFractionDigits();

        FilterFactory fFac = FilterFactoryFinder.createFilterFactory();

        for (int i = 0; i < nLayers; i++) {
            MapLayer layer = layers[i];
            FeatureIterator<SimpleFeature> featureReader = null;
            FeatureSource<SimpleFeatureType, SimpleFeature> fSource;
            fSource = (FeatureSource<SimpleFeatureType, SimpleFeature>) layer.getFeatureSource();
            SimpleFeatureType schema = fSource.getSchema();

            try {
                Expression bboxExpression = fFac.createBBoxExpression(mapContext.getAreaOfInterest());
                GeometryFilter bboxFilter = fFac.createGeometryFilter(FilterType.GEOMETRY_INTERSECTS);
                bboxFilter.addLeftGeometry(fFac.createAttributeExpression(schema,
                        schema.getGeometryDescriptor().getName().getLocalPart()));
                bboxFilter.addRightGeometry(bboxExpression);

                Query bboxQuery = new DefaultQuery(schema.getTypeName(), bboxFilter);
                Query definitionQuery = layer.getQuery();
                DefaultQuery finalQuery = new DefaultQuery(DataUtilities.mixQueries(definitionQuery, bboxQuery, "svgEncoder"));
                finalQuery.setHints(definitionQuery.getHints());
                finalQuery.setSortBy(definitionQuery.getSortBy());
                finalQuery.setStartIndex(definitionQuery.getStartIndex());

                LOGGER.fine("obtaining FeatureReader for " + schema.getTypeName());
                featureReader = fSource.getFeatures(finalQuery).features();
                LOGGER.fine("got FeatureReader, now writing");

                String groupId = null;
                String styleName = null;

                groupId = schema.getTypeName();

                styleName = layer.getStyle().getName();

                writer.write("<g id=\"" + groupId + "\"");

                if (!styleName.startsWith("#")) {
                    writer.write(" class=\"" + styleName + "\"");
                }

                writer.write(">\n");

                writeDefs(schema);

                writer.writeFeatures(fSource.getSchema(), featureReader, styleName);
                writer.write("</g>\n");
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
            } finally {
                if (featureReader != null) {
                    featureReader.close();
                }
            }
        }
    }
}
