package org.vfny.geoserver.wms.responses.featureinfo;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import javax.xml.namespace.QName;

import junit.framework.Test;

import org.geoserver.data.test.MockData;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSTestSupport;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class GetFeatureInfoTest extends WMSTestSupport {
    
    public static String WCS_PREFIX = "wcs";
    public static String WCS_URI = "http://www.opengis.net/wcs/1.1.1";
    public static QName TASMANIA_BM = new QName(WCS_URI, "BlueMarble", WCS_PREFIX);
    public static QName SQUARES = new QName(MockData.CITE_URI, "squares", MockData.CITE_PREFIX);

    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new GetFeatureInfoTest());
    }
    
    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        WMSInfo wmsInfo = getGeoServer().getService(WMSInfo.class);
        wmsInfo.setMaxBuffer(50);
        getGeoServer().save(wmsInfo);
    }

    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);
        dataDirectory.addStyle("thickStroke", GetFeatureInfoTest.class.getResource("thickStroke.sld"));
        dataDirectory.addStyle("raster", GetFeatureInfoTest.class.getResource("raster.sld"));
        dataDirectory.addStyle("rasterScales", GetFeatureInfoTest.class.getResource("rasterScales.sld"));
        dataDirectory.addCoverage(TASMANIA_BM, GetFeatureInfoTest.class.getResource("tazbm.tiff"),
                "tiff", "raster");
        dataDirectory.addStyle("squares", GetFeatureInfoTest.class.getResource("squares.sld"));
        dataDirectory.addPropertiesType(SQUARES, GetFeatureInfoTest.class.getResource("squares.properties"),
                null);
    }
    
    /**
     * Tests a simple GetFeatureInfo works, and that the result contains the
     * expected polygon
     * 
     * @throws Exception
     */
    public void testSimple() throws Exception {
        String layer = getLayerId(MockData.FORESTS);
        String request = "wms?bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg&info_format=text/plain&request=GetFeatureInfo&layers="
                + layer + "&query_layers=" + layer + "&width=20&height=20&x=10&y=10";
        String result = getAsString(request);
        System.out.println(result);
        assertNotNull(result);
        assertTrue(result.indexOf("Green Forest") > 0);
    }
    
    /**
     * Tests a simple GetFeatureInfo works, and that the result contains the
     * expected polygon
     * 
     * @throws Exception
     */
    public void testSimpleHtml() throws Exception {
        String layer = getLayerId(MockData.FORESTS);
        String request = "wms?bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg&info_format=text/html&request=GetFeatureInfo&layers="
                + layer + "&query_layers=" + layer + "&width=20&height=20&x=10&y=10";
        Document dom = getAsDOM(request);
        // count lines that do contain a forest reference
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/td[starts-with(.,'Forests.')])", dom);
    }
    
    /**
     * Tests GetFeatureInfo with a buffer specified works, and that the result contains the
     * expected polygon
     * 
     * @throws Exception
     */
    public void testBuffer() throws Exception {
        // to setup the request and the buffer I rendered BASIC_POLYGONS using GeoServer, then played
        // against the image coordinates
        String layer = getLayerId(MockData.BASIC_POLYGONS);
        String base = "wms?bbox=-4.5,-2.,4.5,7&styles=&format=jpeg&info_format=text/html&request=GetFeatureInfo&layers="
                + layer + "&query_layers=" + layer + "&width=300&height=300";
        Document dom = getAsDOM(base + "&x=85&y=230");
        // make sure the document is empty, as we chose an area with no features inside
        assertXpathEvaluatesTo("0", "count(/html/body/table/tr)", dom);

        // another request that will catch one feature due to the extended buffer, make sure it's in
        dom = getAsDOM(base + "&x=85&y=230&buffer=40");
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/td[starts-with(.,'BasicPolygons.')])", dom);
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/td[. = 'BasicPolygons.1107531493630'])", dom);
        
        // this one would end up catching everything (3 features) if it wasn't that we say the max buffer at 50
        // in the WMS configuration
        dom = getAsDOM(base + "&x=85&y=230&buffer=300");
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/td[starts-with(.,'BasicPolygons.')])", dom);
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/td[. = 'BasicPolygons.1107531493630'])", dom);
    }
    
    /**
     * Tests GetFeatureInfo with a buffer specified works, and that the result contains the
     * expected polygon
     * 
     * @throws Exception
     */
    public void testAutoBuffer() throws Exception {
        String layer = getLayerId(MockData.BASIC_POLYGONS);
        String base = "wms?bbox=-4.5,-2.,4.5,7&format=jpeg&info_format=text/html&request=GetFeatureInfo&layers="
                + layer + "&query_layers=" + layer + "&width=300&height=300&x=114&y=229";
        Document dom = getAsDOM(base + "&styles=");
        // make sure the document is empty, the style we chose has thin lines
        assertXpathEvaluatesTo("0", "count(/html/body/table/tr)", dom);

        // another request that will catch one feature due to the style with a thick stroke, make sure it's in
        dom = getAsDOM(base + "&styles=thickStroke");
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/td[starts-with(.,'BasicPolygons.')])", dom);
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/td[. = 'BasicPolygons.1107531493630'])", dom);
    }
    
    /**
     * Tests GetFeatureInfo with a buffer specified works, and that the result contains the
     * expected polygon
     * 
     * @throws Exception
     */
    public void testBufferScales() throws Exception {
        String layer = getLayerId(SQUARES);
        String base = "wms?&format=png&info_format=text/html&request=GetFeatureInfo&layers="
                + layer + "&query_layers=" + layer + "&styles=squares&bbox=0,0,10000,10000&feature_count=10";
        
        // first request, should provide no result, scale is 1:100
        int w = (int) (100.0 / 0.28 * 1000); // dpi compensation
        Document dom = getAsDOM(base + "&width=" + w + "&height=" + w + "&x=20&y=" + (w - 20));
        // print(dom);
        // make sure the document is empty, the style we chose has thin lines
        assertXpathEvaluatesTo("0", "count(/html/body/table/tr)", dom);
        
        // second request, should provide oe result, scale is 1:50
        w = (int) (200.0 / 0.28 * 1000); // dpi compensation
        dom = getAsDOM(base + "&width=" + w + "&height=" + w + "&x=20&y=" + (w - 20));
        // print(dom);
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/td[starts-with(.,'squares.')])", dom);
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/td[. = 'squares.1'])", dom);

        // third request, should provide two result, scale is 1:10
        w = (int) (1000.0 / 0.28 * 1000); // dpi compensation
        dom = getAsDOM(base + "&width=" + w + "&height=" + w + "&x=20&y=" + (w - 20));
        // print(dom);
        assertXpathEvaluatesTo("2", "count(/html/body/table/tr/td[starts-with(.,'squares.')])", dom);
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/td[. = 'squares.1'])", dom);
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/td[. = 'squares.2'])", dom);
        
    }
    
    /**
     * Tests a GetFeatureInfo againworks, and that the result contains the
     * expected polygon
     * 
     * @throws Exception
     */
    public void testTwoLayers() throws Exception {
        String layer = getLayerId(MockData.FORESTS) + "," + getLayerId(MockData.LAKES);
        String request = "wms?bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg&info_format=text/html&request=GetFeatureInfo&layers="
                + layer + "&query_layers=" + layer + "&width=20&height=20&x=10&y=10&info";
        String result = getAsString(request);
        assertNotNull(result);
        assertTrue(result.indexOf("Green Forest") > 0);
        // GEOS-2603 GetFeatureInfo returns html tables without css style if more than one layer is selected
        assertTrue(result.indexOf("<style type=\"text/css\">") > 0);

    }

    /**
     * Check GetFeatureInfo returns an error if the format is not known, instead
     * of returning the text format as in
     * http://jira.codehaus.org/browse/GEOS-1924
     * 
     * @throws Exception
     */
    public void testUknownFormat() throws Exception {
        String layer = MockData.FORESTS.getPrefix() + ":" + MockData.FORESTS.getLocalPart();
        String request = "wms?bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg&info_format=unknown/format&request=GetFeatureInfo&layers="
                + layer + "&query_layers=" + layer + "&width=20&height=20&x=10&y=10";
        Document doc = dom(get(request), true);
        // print(doc);
        assertXpathEvaluatesTo("1", "count(//ServiceExceptionReport/ServiceException)", doc);
        assertXpathEvaluatesTo("InvalidParameterValue", "/ServiceExceptionReport/ServiceException/@code", doc);
        assertXpathEvaluatesTo("info_format", "/ServiceExceptionReport/ServiceException/@locator", doc);
    }
    
    public void testCoverage() throws Exception {
        // http://jira.codehaus.org/browse/GEOS-2574
        String layer = getLayerId(TASMANIA_BM);
        String request = "wms?service=wms&request=GetFeatureInfo&version=1.1.1" +
        		"&layers=" + layer + "&styles=&bbox=146.5,-44.5,148,-43&width=600&height=600" + 
        		"&info_format=text/html&query_layers=" + layer + "&x=300&y=300&srs=EPSG:4326";
        Document dom = getAsDOM(request);
        // we also have the charset which may be platf. dep.
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/th[. = 'RED_BAND'])", dom);
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/th[. = 'GREEN_BAND'])", dom);
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/th[. = 'BLUE_BAND'])", dom);
    }
    
    public void testCoverageScales() throws Exception {
        String layer = getLayerId(TASMANIA_BM);
        String request = "wms?service=wms&request=GetFeatureInfo&version=1.1.1" +
                "&layers=" + layer + "&styles=rasterScales&bbox=146.5,-44.5,148,-43" + 
                "&info_format=text/html&query_layers=" + layer + "&x=300&y=300&srs=EPSG:4326";
        
        // this one should be blank
        Document dom = getAsDOM(request + "&width=300&height=300");
        assertXpathEvaluatesTo("0", "count(/html/body/table/tr/th)", dom);
        
        // this one should draw the coverage
        dom = getAsDOM(request + "&width=600&height=600");
        // we also have the charset which may be platf. dep.
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/th[. = 'RED_BAND'])", dom);
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/th[. = 'GREEN_BAND'])", dom);
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/th[. = 'BLUE_BAND'])", dom);
    }
    
    public void testOutsideCoverage() throws Exception {
        // a request which is way large on the west side, lots of blank space
        String layer = getLayerId(TASMANIA_BM);
        String request = "wms?service=wms&request=GetFeatureInfo&version=1.1.1" +
                "&layers=" + layer + "&styles=raster&bbox=0,-90,148,-43" + 
                "&info_format=text/html&query_layers=" + layer + "&width=300&height=300&x=10&y=150&srs=EPSG:4326";
        
        // this one should be blank, but not be a service exception
        Document dom = getAsDOM(request + "");
        assertXpathEvaluatesTo("1", "count(/html)", dom);
        assertXpathEvaluatesTo("0", "count(/html/body/table/tr/th)", dom);
    }
    
    /**
     * Check we report back an exception when query_layer contains layers not part of LAYERS
     * @throws Exception
     */
    public void testUnkonwnQueryLayer() throws Exception {
        String layers1 = getLayerId(MockData.FORESTS) + "," + getLayerId(MockData.LAKES);
        String layers2 = getLayerId(MockData.FORESTS) + "," + getLayerId(MockData.BRIDGES);
        String request = "wms?bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg&info_format=text/html&request=GetFeatureInfo&layers="
                + layers1 + "&query_layers=" + layers2 + "&width=20&height=20&x=10&y=10&info";
        
        Document dom = getAsDOM(request + "");
        assertXpathEvaluatesTo("1", "count(/ServiceExceptionReport)", dom);
    }
    
    
}
