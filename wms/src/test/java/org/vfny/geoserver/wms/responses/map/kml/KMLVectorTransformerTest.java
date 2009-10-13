/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.vfny.geoserver.wms.responses.map.kml;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;

import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.TestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerAbstractTestSupport;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMSMockData;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.map.DefaultMapLayer;
import org.geotools.map.MapLayer;
import org.geotools.styling.Style;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.vfny.geoserver.wms.WMSMapContext;
import org.vfny.geoserver.wms.requests.GetMapRequest;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletRequest;
import com.vividsolutions.jts.geom.Point;

/**
 * Unit test suite for {@link KMLVectorTransformer}
 * 
 * @author Gabriel Roldan
 * @todo this test does not need to extend GeoServerAbstractTestSupport but just TestCase. For the
 *       time being, its a workaround for the build to keep going until we find out why these tests
 *       produce other ones to fail
 */
public class KMLVectorTransformerTest extends GeoServerAbstractTestSupport {

    private WMSMockData mockData;

    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new KMLVectorTransformerTest());
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

        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("atom", "http://purl.org/atom/ns#");
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
    }

    /**
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDownInternal() throws Exception {
        new GeoServerExtensions().setApplicationContext(null);
    }

    /**
     * If {@link KMLVectorTransformer#isStandAlone()} then the root element is Document, otherwise
     * its kml
     * 
     * @throws Exception
     */
    public void testSetStandAlone() throws Exception {
        FeatureCollection<SimpleFeatureType, SimpleFeature> features = FeatureCollections
                .newCollection();
        Style style = mockData.getDefaultStyle().getStyle();
        MapLayer mapLayer = new DefaultMapLayer(features, style);

        WMSMapContext mapContext = new WMSMapContext();
        GetMapRequest request = mockData.createRequest();
        mapContext.setRequest(request);

        KMLVectorTransformer transformer = new KMLVectorTransformer(mapContext, mapLayer);

        Document document;

        transformer.setStandAlone(true);
        document = WMSTestSupport.transform(features, transformer);
        assertEquals("kml", document.getDocumentElement().getNodeName());

        transformer.setStandAlone(false);
        document = WMSTestSupport.transform(features, transformer);
        assertEquals("Document", document.getDocumentElement().getNodeName());
    }

    /**
     * Paging is only enabled if the request has the {@code maxfeatures} parameter set and the
     * {@code relLinks} parameter set to {@code true}.
     * 
     * @throws IOException
     * @see GetMapRequest#getMaxFeatures()
     * @see GetMapRequest#getStartIndex()
     */
    public void testEncodeWithPaging() throws Exception {
        MapLayerInfo layer = mockData.addFeatureTypeLayer("TestPoints", Point.class);
        FeatureTypeInfo typeInfo = layer.getFeature();
        SimpleFeatureType featureType = (SimpleFeatureType) typeInfo.getFeatureType();
        mockData.addFeature(featureType, new Object[] { "name1", "POINT(1 1)" });
        mockData.addFeature(featureType, new Object[] { "name2", "POINT(2 2)" });
        mockData.addFeature(featureType, new Object[] { "name3", "POINT(3 3)" });
        mockData.addFeature(featureType, new Object[] { "name4", "POINT(4 4)" });

        FeatureSource<SimpleFeatureType, SimpleFeature> fs = 
            (FeatureSource<SimpleFeatureType, SimpleFeature>) typeInfo.getFeatureSource(null, null);
        FeatureCollection<SimpleFeatureType, SimpleFeature> features = fs.getFeatures();

        Style style = mockData.getDefaultStyle().getStyle();
        MapLayer mapLayer = new DefaultMapLayer(features, style);
        mapLayer.setTitle("TestPointsTitle");

        WMSMapContext mapContext = new WMSMapContext();
        GetMapRequest request = mockData.createRequest();
        request.setLayers(new MapLayerInfo[] { layer });

        request.setMaxFeatures(2);
        request.setStartIndex(2);
        request.setFormatOptions(Collections.singletonMap("relLinks", "true"));
        MockHttpServletRequest httpreq = (MockHttpServletRequest) request.getHttpServletRequest();
        httpreq.setRequestURL("baseurl");
        mapContext.setRequest(request);

        KMLVectorTransformer transformer = new KMLVectorTransformer(mapContext, mapLayer);
        transformer.setStandAlone(false);
        transformer.setIndentation(2);

        Document dom = WMSTestSupport.transform(features, transformer);
        assertXpathExists("//Document/name", dom);
        assertXpathEvaluatesTo("TestPointsTitle", "//Document/name", dom);
        assertXpathExists("//Document/atom:link", dom);
        assertXpathEvaluatesTo("prev", "//Document/atom:link[1]/@rel", dom);
        assertXpathEvaluatesTo("next", "//Document/atom:link[2]/@rel", dom);

        // we're at startIndex=2 and maxFeatures=2, so expect previous link to be 0, and next to be
        // 4
        String expectedLink;
        expectedLink = "baseurl/rest/geosearch/geos/TestPoints.kml?startindex=0&maxfeatures=2";
        assertXpathEvaluatesTo(expectedLink, "//Document/atom:link[1]/@href", dom);
        expectedLink = "baseurl/rest/geosearch/geos/TestPoints.kml?startindex=4&maxfeatures=2";
        assertXpathEvaluatesTo(expectedLink, "//Document/atom:link[2]/@href", dom);

        assertXpathEvaluatesTo("prev", "//Document/NetworkLink[1]/@id", dom);
        assertXpathEvaluatesTo("next", "//Document/NetworkLink[2]/@id", dom);

        expectedLink = "baseurl/rest/geosearch/geos/TestPoints.kml?startindex=0&maxfeatures=2";
        assertXpathEvaluatesTo(expectedLink, "//Document/NetworkLink[1]/Link/href", dom);

        expectedLink = "baseurl/rest/geosearch/geos/TestPoints.kml?startindex=4&maxfeatures=2";
        assertXpathEvaluatesTo("next", "//Document/NetworkLink[2]/@id", dom);
    }
}
