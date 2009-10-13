/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.vfny.geoserver.wms.responses.map.kml;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import java.io.File;
import java.util.Map;

import junit.framework.Test;

import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.TestData;
import org.geoserver.test.GeoServerAbstractTestSupport;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMSMockData;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.map.DefaultMapLayer;
import org.geotools.map.MapLayer;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.vfny.geoserver.wms.WMSMapContext;
import org.vfny.geoserver.wms.requests.GetMapRequest;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletRequest;
import com.vividsolutions.jts.geom.Point;

/**
 * Unit test suite for {@link KMLLegendTransformer}
 * 
 * @author Gabriel Roldan (OpenGeo)
 * @version $Id$
 * @todo this test does not need to extend GeoServerAbstractTestSupport but just TestCase. For the
 *       time being, its a workaround for the build to keep going until we find out why these tests
 *       produce other ones to fail
 */
public class KMLLegendTransformerTest extends GeoServerAbstractTestSupport {

    private WMSMockData mockData;

    /**
     * The map context for the transformer constructor. It shouldn't be needed, see the comment at
     * {@link KMLLegendTransformer#KMLLegendTransformer(WMSMapContext)}
     */
    private WMSMapContext mapContext;

    /**
     * The layer to encode the legend url for
     */
    private MapLayer mapLayer;

    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new KMLLegendTransformerTest());
    }

    @Override
    protected TestData buildTestData() throws Exception {
        return new TestData() {
            public File getDataDirectoryRoot() {
                return null;
            }

            public boolean isTestDataAvailable() {
                return false;
            }

            public void setUp() throws Exception {
            }

            public void tearDown() throws Exception {
            }
        };
    }

    /**
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUpInternal() throws Exception {
        mockData = new WMSMockData();
        mockData.setUp();

        // Map<String, String> namespaces = new HashMap<String, String>();
        // namespaces.put("atom", "http://purl.org/atom/ns#");
        // XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));

        MapLayerInfo layer = mockData.addFeatureTypeLayer("TestPoints", Point.class);
        mapContext = new WMSMapContext();
        GetMapRequest request = mockData.createRequest();
        request.setLayers(new MapLayerInfo[] { layer });

        FeatureSource<SimpleFeatureType, SimpleFeature> featureSource;
        featureSource = (FeatureSource<SimpleFeatureType, SimpleFeature>) ((FeatureTypeInfo)layer.getFeature()).getFeatureSource(null, null);
        
        mapLayer = new DefaultMapLayer(featureSource, mockData.getDefaultStyle().getStyle());

        MockHttpServletRequest httpreq = (MockHttpServletRequest) request.getHttpServletRequest();
        httpreq.setScheme("http");
        httpreq.setServerName("geoserver.org");
        httpreq.setServerPort(8181);
        httpreq.setContextPath("/geoserver");
        mapContext.setRequest(request);
    }

    /**
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDownInternal() throws Exception {
    }

    /**
     * Test method for
     * {@link KMLLegendTransformer#KMLLegendTransformer(org.vfny.geoserver.wms.WMSMapContext)}.
     * 
     * @throws Exception
     */
    public void testKMLLegendTransformer() throws Exception {
        FeatureCollection<SimpleFeatureType, SimpleFeature> features = FeatureCollections
                .newCollection();
        XpathEngine xpath = XMLUnit.newXpathEngine();

        KMLLegendTransformer transformer = new KMLLegendTransformer(mapContext);
        transformer.setIndentation(2);
        Document dom = WMSTestSupport.transform(mapLayer, transformer);
        assertXpathEvaluatesTo("Legend", "//kml/ScreenOverlay/name", dom);
        assertXpathEvaluatesTo("0", "//kml/ScreenOverlay/overlayXY/@x", dom);
        assertXpathEvaluatesTo("0", "//kml/ScreenOverlay/overlayXY/@y", dom);
        assertXpathEvaluatesTo("pixels", "//kml/ScreenOverlay/overlayXY/@xunits", dom);
        assertXpathEvaluatesTo("pixels", "//kml/ScreenOverlay/overlayXY/@yunits", dom);

        Map<String, String> expectedKVP = KMLReflectorTest
                .toKvp("http://geoserver.org:8181/geoserver/wms?service=wms&width=20&height=20&style=Default+Style&request=GetLegendGraphic&layer=&format=image%2Fpng&version=1.1.1");
        Map<String, String> resultantKVP = KMLReflectorTest.toKvp(xpath.evaluate(
                "//kml/ScreenOverlay/Icon/href", dom));

        KMLReflectorTest.assertMapsEqual(expectedKVP, resultantKVP);
    }
}
