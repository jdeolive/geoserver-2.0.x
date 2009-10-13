package org.vfny.geoserver.wms.responses.map.kml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.Test;

import org.geoserver.data.test.MockData;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.map.MapLayer;
import org.vfny.geoserver.wms.WMSMapContext;
import org.w3c.dom.Document;

import com.vividsolutions.jts.geom.Envelope;


public class KMLSuperOverlayTransformerTest extends WMSTestSupport {

    public static QName DISPERSED_FEATURES = new QName(MockData.SF_URI, "Dispersed", MockData.SF_PREFIX);
    WMSMapContext mapContext;
    MapLayer mapLayer;

    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new KMLSuperOverlayTransformerTest());
    }

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();

        mapLayer = createMapLayer(DISPERSED_FEATURES);
        
        mapContext = new WMSMapContext(createGetMapRequest(MockData.BASIC_POLYGONS));
        mapContext.addLayer(mapLayer);
    }
    
    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);
        dataDirectory.addStyle("allsymbolizers", getClass().getResource("allsymbolizers.sld"));
        dataDirectory.addStyle("SingleFeature", getClass().getResource("singlefeature.sld"));
        dataDirectory.addStyle("Bridge", getClass().getResource("bridge.sld"));

        dataDirectory.addPropertiesType(
                DISPERSED_FEATURES,
                getClass().getResource("Dispersed.properties"),
                Collections.EMPTY_MAP
                );

        dataDirectory.copyTo(getClass().getResourceAsStream("bridge.png"), "styles/bridge.png");
    }
 
    /**
     * Verify that two overlay tiles are produced for a request that encompasses the world.
     */
    public void testWorldBoundsSuperOverlay() throws Exception {
        KMLSuperOverlayTransformer transformer = new KMLSuperOverlayTransformer(mapContext);
        transformer.setIndentation(2);

        mapContext.setAreaOfInterest(new Envelope(-180, 180, -90, 90));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        transformer.transform(mapLayer, output);
        
        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = docBuilder.parse(new ByteArrayInputStream(output.toByteArray()));

        assertEquals("kml", document.getDocumentElement().getNodeName());
        assertEquals(5, document.getElementsByTagName("Region").getLength());
        assertEquals(4, document.getElementsByTagName("NetworkLink").getLength());
        assertEquals(0, document.getElementsByTagName("GroundOverlay").getLength());
    }

    /**
     * Verify that when a tile smaller than one hemisphere is requested, four subtiles are included in the result.
     */
    public void testSubtileSuperOverlay() throws Exception {
        KMLSuperOverlayTransformer transformer = new KMLSuperOverlayTransformer(mapContext);
        transformer.setIndentation(2);

        mapContext.setAreaOfInterest(new Envelope(0, 180, -90, 90));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        transformer.transform(mapLayer, output);
        
        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = docBuilder.parse(new ByteArrayInputStream(output.toByteArray()));

        assertEquals("kml", document.getDocumentElement().getNodeName());
        assertEquals(6, document.getElementsByTagName("Region").getLength());
        assertEquals(5, document.getElementsByTagName("NetworkLink").getLength());
        assertEquals(0, document.getElementsByTagName("GroundOverlay").getLength());
    }
}
