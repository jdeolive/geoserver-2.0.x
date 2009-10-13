/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.sf.json.JSON;
import net.sf.json.JSONObject;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class FeatureTypeTest extends CatalogRESTTestSupport {

    public void testGetAllByWorkspace() throws Exception {
        Document dom = getAsDOM( "/rest/workspaces/sf/featuretypes.xml");
        assertEquals( 
            catalog.getFeatureTypesByNamespace( catalog.getNamespaceByPrefix( "sf") ).size(), 
            dom.getElementsByTagName( "featureType").getLength() );
    }
    
    void addPropertyDataStore(boolean configureFeatureType) throws Exception {
        ByteArrayOutputStream zbytes = new ByteArrayOutputStream();
        ZipOutputStream zout = new ZipOutputStream( zbytes );
        
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        BufferedWriter writer = new BufferedWriter( new OutputStreamWriter( bytes ) );
        writer.write( "_=name:String,pointProperty:Point\n" );
        writer.write( "pdsa.0='zero'|POINT(0 0)\n");
        writer.write( "pdsa.1='one'|POINT(1 1)\n");
        writer.flush();
        
        zout.putNextEntry( new ZipEntry( "pdsa.properties") );
        zout.write( bytes.toByteArray() );
        bytes.reset();
        
        writer.write( "_=name:String,pointProperty:Point\n" );
        writer.write( "pdsb.0='two'|POINT(2 2)\n");
        writer.write( "pdsb.1='trhee'|POINT(3 3)\n");
        writer.flush();
        zout.putNextEntry( new ZipEntry( "pdsb.properties" ) );
        zout.write( bytes.toByteArray() );
        
        zout.flush();
        zout.close();
        
        String q = "configure=" + (configureFeatureType ? "all" : "none"); 
        put( "/rest/workspaces/gs/datastores/pds/file.properties?" + q, zbytes.toByteArray(), "application/zip");
    }
    
    void addGeomlessPropertyDataStore(boolean configureFeatureType) throws Exception {
        ByteArrayOutputStream zbytes = new ByteArrayOutputStream();
        ZipOutputStream zout = new ZipOutputStream( zbytes );
        
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        BufferedWriter writer = new BufferedWriter( new OutputStreamWriter( bytes ) );
        writer.write( "_=name:String,intProperty:Integer\n" );
        writer.write( "ngpdsa.0='zero'|0\n");
        writer.write( "ngpdsa.1='one'|1\n");
        writer.flush();
        
        zout.putNextEntry( new ZipEntry( "ngpdsa.properties") );
        zout.write( bytes.toByteArray() );
        bytes.reset();
        
        writer.write( "_=name:String,intProperty:Integer\n" );
        writer.write( "ngpdsb.0='two'|2\n");
        writer.write( "ngpdsb.1='trhee'|3\n");
        writer.flush();
        zout.putNextEntry( new ZipEntry( "ngpdsb.properties" ) );
        zout.write( bytes.toByteArray() );
        
        zout.flush();
        zout.close();
        
        String q = "configure=" + (configureFeatureType ? "all" : "none"); 
        put( "/rest/workspaces/gs/datastores/ngpds/file.properties?" + q, zbytes.toByteArray(), "application/zip");
    }
    
    public void testGetAllByDataStore() throws Exception {
      
        addPropertyDataStore(true);
        
        Document dom = getAsDOM( "/rest/workspaces/gs/datastores/pds/featuretypes.xml");
        
        assertEquals( 2, dom.getElementsByTagName( "featureType").getLength() );
        assertXpathEvaluatesTo( "1", "count(//featureType/name[text()='pdsa'])", dom );
        assertXpathEvaluatesTo( "1", "count(//featureType/name[text()='pdsb'])", dom );
    }
    
    public void testGetAllAvailable() throws Exception {
        addPropertyDataStore(false);
        
        Document dom = getAsDOM( "/rest/workspaces/gs/datastores/pds/featuretypes.xml?list=available");
        assertXpathEvaluatesTo("1", "count(//featureTypeName[text()='pdsa'])", dom);
        assertXpathEvaluatesTo("1", "count(//featureTypeName[text()='pdsb'])", dom);
    }
    
    public void testPutAllUnauthorized() throws Exception {
        assertEquals( 405, putAsServletResponse("/rest/workspaces/sf/datastores/sf/featuretypes").getStatusCode() );
    }
    
    public void testDeleteAllUnauthorized() throws Exception {
        assertEquals( 405, deleteAsServletResponse("/rest/workspaces/sf/datastores/sf/featuretypes").getStatusCode() );
    }
    
    public void testPostAsXML() throws Exception {
        Document dom = getAsDOM( "wfs?request=getfeature&typename=sf:pdsa");
        assertEquals( "ows:ExceptionReport", dom.getDocumentElement().getNodeName());
        
        addPropertyDataStore(false);
        String xml = 
          "<featureType>"+
            "<name>pdsa</name>"+
            "<nativeName>pdsa</nativeName>"+
            "<srs>EPSG:4326</srs>" + 
            "<nativeCRS>EPSG:4326</nativeCRS>" + 
            "<nativeBoundingBox>"+
              "<minx>0.0</minx>"+
              "<maxx>1.0</maxx>"+
              "<miny>0.0</miny>"+
              "<maxy>1.0</maxy>"+
              "<crs>EPSG:4326</crs>" + 
            "</nativeBoundingBox>"+
            "<store>pds</store>" + 
          "</featureType>";
        MockHttpServletResponse response = 
            postAsServletResponse( "/rest/workspaces/gs/datastores/pds/featuretypes/", xml, "text/xml");
        
        assertEquals( 201, response.getStatusCode() );
        assertNotNull( response.getHeader( "Location") );
        assertTrue( response.getHeader("Location").endsWith( "/workspaces/gs/datastores/pds/featuretypes/pdsa" ) );
        
        dom = getAsDOM( "wfs?request=getfeature&typename=gs:pdsa");
        assertEquals( "wfs:FeatureCollection", dom.getDocumentElement().getNodeName());
        assertEquals( 2, dom.getElementsByTagName( "gs:pdsa").getLength());
    }
    
    public void testPostAsJSON() throws Exception {
        Document dom = getAsDOM( "wfs?request=getfeature&typename=sf:pdsa");
        assertEquals( "ows:ExceptionReport", dom.getDocumentElement().getNodeName());
        
        addPropertyDataStore(false);
        String json = 
          "{" + 
           "'featureType':{" + 
              "'name':'pdsa'," +
              "'nativeName':'pdsa'," +
              "'srs':'EPSG:4326'," +
              "'nativeBoundingBox':{" +
                 "'minx':0.0," +
                 "'maxx':1.0," +
                 "'miny':0.0," +
                 "'maxy':1.0," +
                 "'crs':'EPSG:4326'" +
              "}," +
              "'nativeCRS':'EPSG:4326'," +
              "'store':'pds'" +
             "}" +
          "}";
        MockHttpServletResponse response =  
            postAsServletResponse( "/rest/workspaces/gs/datastores/pds/featuretypes/", json, "text/json");
        
        assertEquals( 201, response.getStatusCode() );
        assertNotNull( response.getHeader( "Location") );
        assertTrue( response.getHeader("Location").endsWith( "/workspaces/gs/datastores/pds/featuretypes/pdsa" ) );
        
        dom = getAsDOM( "wfs?request=getfeature&typename=gs:pdsa");
        assertEquals( "wfs:FeatureCollection", dom.getDocumentElement().getNodeName());
        assertEquals( 2, dom.getElementsByTagName( "gs:pdsa").getLength());
    }
    
    public void testPostToResource() throws Exception {
        addPropertyDataStore(true);
        String xml = 
            "<featureType>"+
              "<name>pdsa</name>"+
            "</featureType>";
        
        MockHttpServletResponse response = 
            postAsServletResponse( "/rest/workspaces/gs/datastores/pds/featuretypes/pdsa", xml, "text/xml");
        assertEquals( 405, response.getStatusCode() );
    }
    
    public void testGetAsXML() throws Exception {
        Document dom = getAsDOM( "/rest/workspaces/sf/featuretypes/PrimitiveGeoFeature.xml");
        
        assertEquals( "featureType", dom.getDocumentElement().getNodeName() );
        assertXpathEvaluatesTo("PrimitiveGeoFeature", "/featureType/name", dom);
        assertXpathEvaluatesTo( "EPSG:4326", "/featureType/srs", dom);
        assertEquals( CRS.decode( "EPSG:4326" ).toWKT(), xp.evaluate( "/featureType/nativeCRS", dom ) );
        
        FeatureTypeInfo ft = catalog.getFeatureTypeByName( "sf", "PrimitiveGeoFeature" );
        
        /*
        ReferencedEnvelope re = ft.getNativeBoundingBox();
        assertXpathEvaluatesTo(  re.getMinX()+"" , "/featureType/nativeBoundingBox/minx", dom );
        assertXpathEvaluatesTo(  re.getMaxX()+"" , "/featureType/nativeBoundingBox/maxx", dom );
        assertXpathEvaluatesTo(  re.getMinY()+"" , "/featureType/nativeBoundingBox/miny", dom );
        assertXpathEvaluatesTo(  re.getMaxY()+"" , "/featureType/nativeBoundingBox/maxy", dom );
        */
        ReferencedEnvelope re = ft.getLatLonBoundingBox();
        assertXpathEvaluatesTo(  re.getMinX()+"" , "/featureType/latLonBoundingBox/minx", dom );
        assertXpathEvaluatesTo(  re.getMaxX()+"" , "/featureType/latLonBoundingBox/maxx", dom );
        assertXpathEvaluatesTo(  re.getMinY()+"" , "/featureType/latLonBoundingBox/miny", dom );
        assertXpathEvaluatesTo(  re.getMaxY()+"" , "/featureType/latLonBoundingBox/maxy", dom );
    }
    
    public void testGetAsJSON() throws Exception {
        JSON json = getAsJSON( "/rest/workspaces/sf/featuretypes/PrimitiveGeoFeature.json");
        JSONObject featureType = ((JSONObject)json).getJSONObject("featureType");
        assertNotNull(featureType);
        
        assertEquals( "PrimitiveGeoFeature", featureType.get("name") );
        assertEquals( CRS.decode("EPSG:4326").toWKT(), featureType.get( "nativeCRS") );
        assertEquals( "EPSG:4326", featureType.get( "srs") );
    }
    
    public void testGetAsHTML() throws Exception {
        Document dom = getAsDOM( "/rest/workspaces/sf/datastores/sf/featuretypes/PrimitiveGeoFeature.html");
    }
    
    public void testPut() throws Exception {
        String xml = 
          "<featureType>" + 
            "<title>new title</title>" +  
          "</featureType>";
        MockHttpServletResponse response = 
            putAsServletResponse("/rest/workspaces/sf/datastores/sf/featuretypes/PrimitiveGeoFeature", xml, "text/xml");
        assertEquals( 200, response.getStatusCode() );
        
        Document dom = getAsDOM("/rest/workspaces/sf/datastores/sf/featuretypes/PrimitiveGeoFeature.xml");
        assertXpathEvaluatesTo("new title", "/featureType/title", dom );
        
        FeatureTypeInfo ft = catalog.getFeatureTypeByName( "sf", "PrimitiveGeoFeature");
        assertEquals( "new title", ft.getTitle() );
    }
    
    public void testPutNonExistant() throws Exception {
        String xml = 
            "<featureType>" + 
              "<title>new title</title>" +  
            "</featureType>";
          MockHttpServletResponse response = 
              putAsServletResponse("/rest/workspaces/sf/datastores/sf/featuretypes/NonExistant", xml, "text/xml");
          assertEquals( 404, response.getStatusCode() );
    }
   
    public void testDelete() throws Exception {
        assertNotNull( catalog.getFeatureTypeByName("sf", "PrimitiveGeoFeature"));
        for (LayerInfo l : catalog.getLayers( catalog.getFeatureTypeByName("sf", "PrimitiveGeoFeature") ) ) {
            catalog.remove(l);
        }
        assertEquals( 200,  
            deleteAsServletResponse( "/rest/workspaces/sf/datastores/sf/featuretypes/PrimitiveGeoFeature").getStatusCode());
        assertNull( catalog.getFeatureTypeByName("sf", "PrimitiveGeoFeature"));
    }
    
    public void testDeleteNonExistant() throws Exception {
        assertEquals( 404,  
            deleteAsServletResponse( "/rest/workspaces/sf/datastores/sf/featuretypes/NonExistant").getStatusCode());
    }
    
    public void testPostGeometrylessFeatureType() throws Exception {
        addGeomlessPropertyDataStore(false);
        
        String xml = 
            "<featureType>" + 
              "<name>ngpdsa</name>" +
            "</featureType>";
        
      MockHttpServletResponse response = 
          postAsServletResponse("/rest/workspaces/gs/datastores/ngpds/featuretypes", xml, "text/xml");
      assertEquals( 201, response.getStatusCode() );
      assertNotNull( response.getHeader( "Location") );
      assertTrue( response.getHeader("Location").endsWith( "/workspaces/gs/datastores/ngpds/featuretypes/ngpdsa" ) );
    }
}
