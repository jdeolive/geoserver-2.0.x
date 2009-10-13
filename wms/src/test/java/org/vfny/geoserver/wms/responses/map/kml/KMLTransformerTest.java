/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.vfny.geoserver.wms.responses.map.kml;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.Test;

import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.data.test.MockData;
import org.geoserver.data.util.IOUtils;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.data.FeatureSource;
import org.geotools.map.MapLayer;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.vfny.geoserver.wms.WMSMapContext;
import org.vfny.geoserver.wms.requests.GetMapRequest;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.vividsolutions.jts.geom.Envelope;


public class KMLTransformerTest extends WMSTestSupport {
    WMSMapContext mapContext;
    MapLayer mapLayer;
    
    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new KMLTransformerTest());
    }
    
    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();

        mapLayer = createMapLayer( MockData.BASIC_POLYGONS );
        
        mapContext = new WMSMapContext(createGetMapRequest(MockData.BASIC_POLYGONS));
        mapContext.addLayer(mapLayer);
    }
    
    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);
        dataDirectory.addStyle("allsymbolizers", getClass().getResource("allsymbolizers.sld"));
        dataDirectory.addStyle("SingleFeature", getClass().getResource("singlefeature.sld"));
        dataDirectory.addStyle("Bridge", getClass().getResource("bridge.sld"));
        dataDirectory.addStyle("dynamicsymbolizer", getClass().getResource("dynamicsymbolizer.sld"));
        dataDirectory.copyTo(getClass().getResourceAsStream("bridge.png"), "styles/bridge.png");
    }

    @SuppressWarnings("unchecked")
    public void testVectorTransformer() throws Exception {
        KMLVectorTransformer transformer = new KMLVectorTransformer(mapContext, mapLayer);
        transformer.setIndentation(2);

        FeatureSource <SimpleFeatureType, SimpleFeature> featureSource;
        featureSource = (FeatureSource<SimpleFeatureType, SimpleFeature>) mapLayer.getFeatureSource();
        int nfeatures = featureSource.getFeatures().size();

        Document document = WMSTestSupport.transform(featureSource.getFeatures(), transformer);

        Element element = document.getDocumentElement();
        assertEquals("kml", element.getNodeName());
        assertEquals(nfeatures, element.getElementsByTagName("Style").getLength());
        assertEquals(nfeatures, element.getElementsByTagName("Placemark").getLength());
    }
    
    @SuppressWarnings("unchecked")
    public void testExternalGraphicBackround() throws Exception {
        // see http://jira.codehaus.org/browse/GEOS-1947
        MapLayer mapLayer = createMapLayer( MockData.POINTS,  "Bridge");
        WMSMapContext mapContext = new WMSMapContext(createGetMapRequest(MockData.POINTS));
        mapContext.addLayer(mapLayer);
        KMLVectorTransformer transformer = new KMLVectorTransformer(mapContext, mapLayer);
        transformer.setIndentation(2);

        FeatureSource <SimpleFeatureType, SimpleFeature> featureSource;
        featureSource = (FeatureSource<SimpleFeatureType, SimpleFeature>) mapLayer.getFeatureSource();
        int nfeatures = featureSource.getFeatures().size();

        Document document = WMSTestSupport.transform(featureSource.getFeatures(), transformer);

        // make sure we are generating icon styles, but that we're not sticking a color onto them
        XMLAssert.assertXpathEvaluatesTo("" + nfeatures, "count(//Style/IconStyle/Icon/href)", document);
        XMLAssert.assertXpathEvaluatesTo("0", "count(//Style/IconStyle/Icon/color)", document);
    }
    
    @SuppressWarnings("unchecked")
    public void testFilteredData() throws Exception {
        MapLayer mapLayer = createMapLayer( MockData.BASIC_POLYGONS,  "SingleFeature");
        
        WMSMapContext mapContext = new WMSMapContext(createGetMapRequest(MockData.BASIC_POLYGONS));
        mapContext.addLayer(mapLayer);
        
        KMLVectorTransformer transformer = new KMLVectorTransformer(mapContext, mapLayer);
        transformer.setIndentation(2);

        FeatureSource <SimpleFeatureType, SimpleFeature> featureSource;
        featureSource = (FeatureSource<SimpleFeatureType, SimpleFeature>) mapLayer.getFeatureSource();

        Document document = WMSTestSupport.transform(featureSource.getFeatures(), transformer);

        Element element = document.getDocumentElement();
        assertEquals("kml", element.getNodeName());
        assertEquals(1, element.getElementsByTagName("Placemark").getLength());
        assertEquals(1, element.getElementsByTagName("Style").getLength());
    }
    
//    public void testReprojection() throws Exception {
//        KMLTransformer transformer = new KMLTransformer();
//        transformer.setIndentation(2);
//           
//        ByteArrayOutputStream output = new ByteArrayOutputStream();
//        transformer.transform(mapContext, output);
//        transformer.transform(mapContext,System.out);
//        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
//        Document doc1 = docBuilder.parse(new ByteArrayInputStream(output.toByteArray()));
//
//        mapContext.setCoordinateReferenceSystem(CRS.decode("EPSG:3005"));
//        output = new ByteArrayOutputStream();
//        transformer.transform(mapContext, output);
//        transformer.transform(mapContext,System.out);
//        Document doc2 = docBuilder.parse(new ByteArrayInputStream(output.toByteArray()));
//        
//        NodeList docs1 = doc1.getDocumentElement().getElementsByTagName("Document");
//        NodeList docs2 = doc2.getDocumentElement().getElementsByTagName("Document");
//        
//        assertEquals( docs1.getLength(), docs2.getLength() );
//        for ( int i = 0; i < docs1.getLength(); i++ ) {
//            Element e1 = (Element) docs1.item(i);
//            Element e2 = (Element) docs2.item(i);
//            
//            String name1 = ReaderUtils.getChildText( e1, "name" );
//            String name2 = ReaderUtils.getChildText( e2, "name" );
//            
//            assertEquals( name1, name2 );
//            
//            Element p1 = (Element) e1.getElementsByTagName("Placemark").item(0);
//            Element p2 = (Element) e2.getElementsByTagName("Placemark").item(0);
//            
//            Element poly1 = (Element) p1.getElementsByTagName("Polygon").item(0);
//            Element poly2 = (Element) p2.getElementsByTagName("Polygon").item(0);
//            
//            Element c1 = (Element) poly1.getElementsByTagName("coordinates").item(0);
//            Element c2 = (Element) poly2.getElementsByTagName("coordinates").item(0);
//            
//            assertFalse(c1.getFirstChild().getNodeValue().equals( c2.getFirstChild().getNodeValue()));
//        }
//        
//    }

    public void testRasterTransformerInline() throws Exception {
        KMLRasterTransformer transformer = new KMLRasterTransformer(mapContext);
        transformer.setInline(true);

        Document document = WMSTestSupport.transform(mapLayer, transformer);

        assertEquals("kml", document.getDocumentElement().getNodeName());

        assertEquals(mapContext.getLayerCount(), document.getElementsByTagName("Folder").getLength());
        assertEquals(mapContext.getLayerCount(),
            document.getElementsByTagName("GroundOverlay").getLength());

        assertEquals(mapContext.getLayerCount(), document.getElementsByTagName("href").getLength());

        Element href = (Element) document.getElementsByTagName("href").item(0);
        assertEquals("layer_0.png", href.getFirstChild().getNodeValue());
    }

    public void testRasterTransformerNotInline() throws Exception {
        KMLRasterTransformer transformer = new KMLRasterTransformer(mapContext);
        transformer.setInline(false);

        Document document = WMSTestSupport.transform(mapLayer, transformer);

        assertEquals("kml", document.getDocumentElement().getNodeName());

        assertEquals(mapContext.getLayerCount(), document.getElementsByTagName("Folder").getLength());
        assertEquals(mapContext.getLayerCount(),
            document.getElementsByTagName("GroundOverlay").getLength());

        assertEquals(mapContext.getLayerCount(), document.getElementsByTagName("href").getLength());

        Element href = (Element) document.getElementsByTagName("href").item(0);
        assertTrue(href.getFirstChild().getNodeValue().startsWith("http://localhost"));
    }
    
    public void testRasterPlacemark() throws Exception {
        doTestRasterPlacemark(true);
        doTestRasterPlacemark(false);
    }

    protected void doTestRasterPlacemark(boolean doPlacemarks) throws Exception {
        GetMapRequest getMapRequest = createGetMapRequest(MockData.BASIC_POLYGONS);
        HashMap formatOptions = new HashMap();
        formatOptions.put("kmplacemark", new Boolean(doPlacemarks));
        formatOptions.put("kmscore", new Integer(0));
        getMapRequest.setFormatOptions(formatOptions);

        WMSMapContext mapContext = new WMSMapContext(getMapRequest);
        mapContext.addLayer(mapLayer);
        mapContext.setMapHeight(1024);
        mapContext.setMapWidth(1024);

        // create the map producer
        KMZMapProducer mapProducer = new KMZMapProducer(getWMS());
        mapProducer.setMapContext(mapContext);
        mapProducer.produceMap();

        // create the kmz
        File tempDir = IOUtils.createRandomDirectory("./target", "kmplacemark",
                "test");
        tempDir.deleteOnExit();

        File zip = new File(tempDir, "kmz.zip");
        zip.deleteOnExit();

        FileOutputStream output = new FileOutputStream(zip);
        mapProducer.writeTo(output);

        output.flush();
        output.close();

        assertTrue(zip.exists());

        // unzip and test it
        ZipFile zipFile = new ZipFile(zip);

        ZipEntry entry = zipFile.getEntry("wms.kml");
        assertNotNull(entry);
        assertNotNull(zipFile.getEntry("layer_0.png"));

        // unzip the wms.kml to file
        byte[] buffer = new byte[1024];
        int len;

        InputStream inStream = zipFile.getInputStream(entry);
        File temp = File.createTempFile("test_out", "kmz", tempDir);
        temp.deleteOnExit();
        BufferedOutputStream outStream = new BufferedOutputStream(
                new FileOutputStream(temp));

        while ((len = inStream.read(buffer)) >= 0)
            outStream.write(buffer, 0, len);
        inStream.close();
        outStream.close();

        // read in the wms.kml and check its contents
        Document document = dom(new BufferedInputStream(new FileInputStream(
                temp)));

        assertEquals("kml", document.getDocumentElement().getNodeName());
        if (doPlacemarks) {
            assertEquals(getFeatureSource(MockData.BASIC_POLYGONS)
                    .getFeatures().size(), document.getElementsByTagName(
                    "Placemark").getLength());
        } else {
            assertEquals(0, document.getElementsByTagName("Placemark")
                    .getLength());
        }

        zipFile.close();
    }

    public void testSuperOverlayTransformer() throws Exception {
        KMLSuperOverlayTransformer transformer = new KMLSuperOverlayTransformer(mapContext);
        transformer.setIndentation(2);

        mapContext.setAreaOfInterest(new Envelope(-180.0, 180.0, -90.0, 90.0));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        transformer.transform(mapLayer, output);
        
        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = docBuilder.parse(new ByteArrayInputStream(output.toByteArray()));

        assertEquals("kml", document.getDocumentElement().getNodeName());
        assertEquals(5, document.getElementsByTagName("Region").getLength());
        assertEquals(4, document.getElementsByTagName("NetworkLink").getLength());
    }

    public void testStyleConverter() throws Exception {
        KMLTransformer transformer = new KMLTransformer();
        mapContext.removeLayer(mapContext.getLayer(0));
        mapContext.addLayer(createMapLayer(MockData.BASIC_POLYGONS, "allsymbolizers"));
        mapContext.setAreaOfInterest(new Envelope(-180,0,-90,90));
        mapContext.setMapHeight(256);
        mapContext.setMapWidth(256);

        Document document = WMSTestSupport.transform(mapContext, transformer, false);
        // print(document);

        assertEquals("kml", document.getDocumentElement().getNodeName());
        assertEquals(3, document.getElementsByTagName("Style").getLength());
        XMLAssert.assertXpathEvaluatesTo("0", "count(//Style[1]/IconStyle/Icon/color)", document);
        XMLAssert.assertXpathEvaluatesTo("http://maps.google.com/mapfiles/kml/pal4/icon25.png", "//Style[1]/IconStyle/Icon/href", document);
        XMLAssert.assertXpathEvaluatesTo("b24d4dff", "//Style[1]/PolyStyle/color", document);
        XMLAssert.assertXpathEvaluatesTo("1", "//Style[1]/PolyStyle/outline", document);
        XMLAssert.assertXpathEvaluatesTo("ffba3e00", "//Style[1]/LineStyle/color", document);
    }

    /**
     * See http://jira.codehaus.org/browse/GEOS-2670
     */
    public void testDynamicSymbolizer() throws Exception {
        KMLTransformer transformer = new KMLTransformer();
        mapContext.removeLayer(mapContext.getLayer(0));
        mapContext.addLayer(createMapLayer(MockData.STREAMS, "dynamicsymbolizer"));
        mapContext.setAreaOfInterest(new Envelope(-180,0,-90,90));
        mapContext.setMapHeight(256);
        mapContext.setMapWidth(256);

        Document document = WMSTestSupport.transform(mapContext, transformer, false);

        assertEquals("kml", document.getDocumentElement().getNodeName());
        assertEquals(1, document.getElementsByTagName("Style").getLength());
        XMLAssert.assertXpathEvaluatesTo("http://example.com/Cam Stream", "//Style[1]/IconStyle/Icon/href", document);
    }


    public void testTransformer() throws Exception {
        KMLTransformer transformer = new KMLTransformer();

        Document document = WMSTestSupport.transform(mapContext, transformer);

        assertEquals("kml", document.getDocumentElement().getNodeName());
    }
}
