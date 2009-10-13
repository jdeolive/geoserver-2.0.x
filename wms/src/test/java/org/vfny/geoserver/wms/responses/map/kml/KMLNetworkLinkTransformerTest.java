/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.vfny.geoserver.wms.responses.map.kml;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;

import java.io.File;
import java.util.Collections;
import java.util.Map;

import junit.framework.Test;

import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.data.test.TestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerAbstractTestSupport;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMSMockData;
import org.geoserver.wms.WMSTestSupport;
import org.vfny.geoserver.wms.WMSMapContext;
import org.vfny.geoserver.wms.requests.GetMapRequest;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletRequest;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Point;

/**
 * Unit test suite for {@link KMLNetworkLinkTransformer}
 * 
 * @author Gabriel Roldan
 * @todo this test does not need to extend GeoServerAbstractTestSupport but just TestCase. For the
 *       time being, its a workaround for the build to keep going until we find out why these tests
 *       produce other ones to fail
 */
public class KMLNetworkLinkTransformerTest extends GeoServerAbstractTestSupport {

    private WMSMockData mockData;

    /**
     * The request to encode
     */
    private GetMapRequest request;

    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new KMLNetworkLinkTransformerTest());
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
        WMSMapContext mapContext = new WMSMapContext();
        request = mockData.createRequest();
        request.setLayers(new MapLayerInfo[] { layer });

        request.setFormatOptions(Collections.singletonMap("relLinks", "true"));
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
        new GeoServerExtensions().setApplicationContext(null);
    }

    /**
     * Assert the encoding of the request as a Region inside the NetworkLink
     * 
     * @see KMLNetworkLinkTransformer#setEncodeAsRegion(boolean)
     */
    public void testEncodeAsRegion() throws Exception {
        XpathEngine xpath = XMLUnit.newXpathEngine();

        KMLNetworkLinkTransformer transformer = new KMLNetworkLinkTransformer();
        transformer.setEncodeAsRegion(true);
        transformer.setIndentation(2);

        request.setBbox(new Envelope(-1, 1, -10, 10));

        Document dom = WMSTestSupport.transform(request, transformer);
        assertXpathEvaluatesTo("1", "count(//kml/Folder)", dom);
        assertXpathEvaluatesTo("1", "count(//kml/Folder/NetworkLink)", dom);
        assertXpathEvaluatesTo("1", "count(//kml/Folder/LookAt)", dom);

        assertXpathEvaluatesTo("geos:TestPoints", "//kml/Folder/NetworkLink/name", dom);
        assertXpathEvaluatesTo("1", "//kml/Folder/NetworkLink/open", dom);
        assertXpathEvaluatesTo("1", "//kml/Folder/NetworkLink/visibility", dom);
        // should match the request BBOX
        assertXpathEvaluatesTo("10.0", "//kml/Folder/NetworkLink/Region/LatLonAltBox/north", dom);
        assertXpathEvaluatesTo("-10.0", "//kml/Folder/NetworkLink/Region/LatLonAltBox/south", dom);
        assertXpathEvaluatesTo("1.0", "//kml/Folder/NetworkLink/Region/LatLonAltBox/east", dom);
        assertXpathEvaluatesTo("-1.0", "//kml/Folder/NetworkLink/Region/LatLonAltBox/west", dom);

        assertXpathEvaluatesTo("256", "//kml/Folder/NetworkLink/Region/Lod/minLodPixels", dom);
        assertXpathEvaluatesTo("-1", "//kml/Folder/NetworkLink/Region/Lod/maxLodPixels", dom);

        final Map<String, String> expectedKvp = KMLReflectorTest.toKvp("http://geoserver.org:8181/geoserver/wms?format_options=relLinks%3Atrue%3B&service=wms&srs=EPSG%3A4326&width=512&styles=Default+Style&height=256&transparent=false&bbox=-1.0%2C-10.0%2C1.0%2C10.0&request=GetMap&layers=geos%3ATestPoints&format=image%2Fdummy&version=1.1.1");
        final Map<String, String> actualKvp = KMLReflectorTest.toKvp(xpath.evaluate(
        "//kml/Folder/NetworkLink/Link/href", dom));
        KMLReflectorTest.assertMapsEqual(expectedKvp, actualKvp);
        assertXpathEvaluatesTo("onRegion", "//kml/Folder/NetworkLink/Link/viewRefreshMode", dom);

        // feature type bounds?
        assertXpathEvaluatesTo("180.0", "//kml/Folder/LookAt/longitude", dom);
        assertXpathEvaluatesTo("0.0", "//kml/Folder/LookAt/latitude", dom);
        assertXpathEvaluatesTo("0", "//kml/Folder/LookAt/altitude", dom);
        assertXpathEvaluatesTo("0", "//kml/Folder/LookAt/tilt", dom);
        assertXpathEvaluatesTo("0", "//kml/Folder/LookAt/heading", dom);
        assertXpathEvaluatesTo("clampToGround", "//kml/Folder/LookAt/altitudeMode", dom);
        // this one is hard to compute, maybe someone understanding what's going on could add an
        // expected value?
        assertXpathExists("//kml/Folder/LookAt/range", dom);
    }

    /**
     * If {@code KMLNetworkLinkTransformer.setEncodeAsRegion(false)} (default behaviour), the
     * request is encoded as an overlay instead than as a Region
     * 
     * @throws Exception
     */
    public void testEncodeAsOverlay() throws Exception {
        XpathEngine xpath = XMLUnit.newXpathEngine();
        KMLNetworkLinkTransformer transformer = new KMLNetworkLinkTransformer();
        transformer.setEncodeAsRegion(false);
        transformer.setIndentation(2);

        request.setBbox(new Envelope(-1, 1, -10, 10));

        Document dom = WMSTestSupport.transform(request, transformer);
        assertXpathEvaluatesTo("1", "count(//kml/Folder)", dom);
        assertXpathEvaluatesTo("1", "count(//kml/Folder/NetworkLink)", dom);
        assertXpathEvaluatesTo("1", "count(//kml/Folder/LookAt)", dom);

        assertXpathEvaluatesTo("geos:TestPoints", "//kml/Folder/NetworkLink/name", dom);
        assertXpathEvaluatesTo("1", "//kml/Folder/NetworkLink/open", dom);
        assertXpathEvaluatesTo("1", "//kml/Folder/NetworkLink/visibility", dom);

        final Map<String, String> expectedKvp = 
            KMLReflectorTest.toKvp("http://geoserver.org:8181/geoserver/wms?format_options=relLinks%3Atrue%3B&service=wms&srs=EPSG%3A4326&width=512&styles=Default+Style&height=256&transparent=false&request=GetMap&layers=geos%3ATestPoints&format=image%2Fdummy&version=1.1.1");
        final Map<String, String> actualKvp = 
            KMLReflectorTest.toKvp(xpath.evaluate("//kml/Folder/NetworkLink/Url/href", dom));
        KMLReflectorTest.assertMapsEqual(expectedKvp, actualKvp);
        assertXpathEvaluatesTo("onStop", "//kml/Folder/NetworkLink/Url/viewRefreshMode", dom);
        assertXpathEvaluatesTo("1", "//kml/Folder/NetworkLink/Url/viewRefreshTime", dom);

        // feature type bounds?
        assertXpathEvaluatesTo("180.0", "//kml/Folder/LookAt/longitude", dom);
        assertXpathEvaluatesTo("0.0", "//kml/Folder/LookAt/latitude", dom);
        assertXpathEvaluatesTo("0", "//kml/Folder/LookAt/altitude", dom);
        assertXpathEvaluatesTo("0", "//kml/Folder/LookAt/tilt", dom);
        assertXpathEvaluatesTo("0", "//kml/Folder/LookAt/heading", dom);
        assertXpathEvaluatesTo("clampToGround", "//kml/Folder/LookAt/altitudeMode", dom);
        // this one is hard to compute, maybe someone understanding what's going on could add an
        // expected value?
        assertXpathExists("//kml/Folder/LookAt/range", dom);
    }
}
