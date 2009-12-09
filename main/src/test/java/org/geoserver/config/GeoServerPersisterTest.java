package org.geoserver.config;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;

import java.io.File;

import javax.xml.parsers.DocumentBuilderFactory;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.test.GeoServerTestSupport;
import org.w3c.dom.Document;

public class GeoServerPersisterTest extends GeoServerTestSupport {

    Catalog catalog;
    
    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        
        catalog = getCatalog();
        GeoServerPersister p = 
            new GeoServerPersister( getResourceLoader(), new XStreamPersisterFactory().createXMLPersister() );
        catalog.addListener( p );
    }
    
    public void testAddWorkspace() throws Exception {
        File ws = new File( testData.getDataDirectoryRoot(), "workspaces/acme" );
        assertFalse( ws.exists() );
        
        WorkspaceInfo acme = catalog.getFactory().createWorkspace();
        acme.setName( "acme" );
        catalog.add( acme );
        
        assertTrue( ws.exists() );
    }
    
    public void testRemoveWorkspace() throws Exception {
        testAddWorkspace();
        
        File ws = new File( testData.getDataDirectoryRoot(), "workspaces/acme" );
        assertTrue( ws.exists() );
        
        WorkspaceInfo acme = catalog.getWorkspaceByName( "acme" );
        catalog.remove( acme );
        assertFalse( ws.exists() );
    }
    
    public void testDefaultWorkspace() throws Exception {
        testAddWorkspace();
        WorkspaceInfo ws = catalog.getWorkspaceByName("acme");
        catalog.setDefaultWorkspace(ws);
        
        File dws = new File( testData.getDataDirectoryRoot(), "workspaces/default.xml" );
        assertTrue( dws.exists() );
        
        Document dom = dom(dws);
        assertXpathEvaluatesTo("acme", "/workspace/name", dom );
    }
    
    public void testAddDataStore() throws Exception {
        testAddWorkspace();
        
        File dir = new File( testData.getDataDirectoryRoot(), "workspaces/acme/foostore");
        assertFalse( dir.exists() );
        
        DataStoreInfo ds = catalog.getFactory().createDataStore();
        ds.setName( "foostore" );
        ds.setWorkspace( catalog.getWorkspaceByName( "acme" ) );
        catalog.add( ds );
        
        assertTrue( dir.exists() );
        assertTrue( new File( dir, "datastore.xml").exists() );
    }
    
    public void testModifyDataStore() throws Exception {
        testAddDataStore();
        
        DataStoreInfo ds = catalog.getDataStoreByName( "acme", "foostore" );
        assertTrue( ds.getConnectionParameters().isEmpty() );
        
        ds.getConnectionParameters().put( "foo", "bar" );
        catalog.save( ds );
        
        File f = 
            new File( testData.getDataDirectoryRoot(), "workspaces/acme/foostore/datastore.xml");
        Document dom = dom( f );
        assertXpathExists( "/dataStore/connectionParameters/entry[@key='foo']", dom );
    }
    
    public void testChangeDataStoreWorkspace() throws Exception {
        testAddDataStore();
        File f1 = 
            new File( testData.getDataDirectoryRoot(), "workspaces/acme/foostore/datastore.xml");
        assertTrue( f1.exists() );
        
        WorkspaceInfo nws = catalog.getFactory().createWorkspace();
        nws.setName( "topp");
        catalog.add( nws );
        
        DataStoreInfo ds = catalog.getDataStoreByName( "acme", "foostore" );
        ds.setWorkspace( nws );
        catalog.save( ds );
        
        assertFalse( f1.exists() );
        File f2 = new File( testData.getDataDirectoryRoot(), "workspaces/topp/foostore/datastore.xml");
        assertTrue( f2.exists() );
    }
    
    public void testRemoveDataStore() throws Exception {
        testAddDataStore();
        
        File f = new File( testData.getDataDirectoryRoot(), "workspaces/acme/foostore");
        assertTrue( f.exists() );
        
        DataStoreInfo ds = catalog.getDataStoreByName( "acme", "foostore");
        catalog.remove( ds );
        assertFalse( f.exists() );
    }
    
    public void testAddFeatureType() throws Exception {
        testAddDataStore();
        
        File d = new File( testData.getDataDirectoryRoot(), 
            "workspaces/acme/foostore/foo");
        assertFalse( d.exists() );
        
        NamespaceInfo ns = catalog.getFactory().createNamespace();
        ns.setPrefix( "bar" );
        ns.setURI( "http://bar" );
        catalog.add( ns );
        
        FeatureTypeInfo ft = catalog.getFactory().createFeatureType();
        ft.setName( "foo" );
        ft.setNamespace( ns );
        ft.setStore( catalog.getDataStoreByName( "acme", "foostore"));
        catalog.add( ft );
        
        assertTrue( d.exists() );
    }
    
    public void testModifyFeatureType() throws Exception {
        testAddFeatureType();
        
        FeatureTypeInfo ft = catalog.getFeatureTypeByName( "bar", "foo" );
        ft.setTitle( "fooTitle" );
        catalog.save( ft );
        
        File f = new File( testData.getDataDirectoryRoot(), 
            "workspaces/acme/foostore/foo/featuretype.xml");
        Document dom = dom( f );
        
        assertXpathEvaluatesTo( "fooTitle", "/featureType/title", dom );
    }
    
    public void testRemoveFeatureType() throws Exception {
        testAddFeatureType();
        
        File d = new File( testData.getDataDirectoryRoot(), 
        "workspaces/acme/foostore/foo");
        assertTrue( d.exists() );
        
        FeatureTypeInfo ft = catalog.getFeatureTypeByName( "bar", "foo" );
        catalog.remove( ft );
        
        assertFalse( d.exists() );
    }
    
    public void testAddCoverageStore() throws Exception {
        testAddWorkspace();
        
        File dir = new File( testData.getDataDirectoryRoot(), "workspaces/acme/foostore");
        assertFalse( dir.exists() );
        
        CoverageStoreInfo cs = catalog.getFactory().createCoverageStore();
        cs.setName( "foostore" );
        cs.setWorkspace( catalog.getWorkspaceByName( "acme" ) );
        catalog.add( cs );
        
        assertTrue( dir.exists() );
        assertTrue( new File( dir, "coveragestore.xml").exists() );
    }
    
    public void testModifyCoverageStore() throws Exception {
        testAddCoverageStore();
        
        CoverageStoreInfo cs = catalog.getCoverageStoreByName( "acme", "foostore" );
        assertNull( cs.getURL() );
        
        cs.setURL( "file:data/foo.tiff" );
        catalog.save( cs );
        
        File f = 
            new File( testData.getDataDirectoryRoot(), "workspaces/acme/foostore/coveragestore.xml");
        Document dom = dom( f );
        assertXpathEvaluatesTo( "file:data/foo.tiff","/coverageStore/url/text()", dom );
    }
    
    public void testRemoveCoverageStore() throws Exception {
        testAddCoverageStore();
        
        File f = new File( testData.getDataDirectoryRoot(), "workspaces/acme/foostore");
        assertTrue( f.exists() );
        
        CoverageStoreInfo cs = catalog.getCoverageStoreByName( "acme", "foostore");
        catalog.remove( cs );
        assertFalse( f.exists() );
    }
    
    public void testAddCoverage() throws Exception {
        testAddCoverageStore();
        
        File d = new File( testData.getDataDirectoryRoot(), 
            "workspaces/acme/foostore/foo");
        assertFalse( d.exists() );
        
        NamespaceInfo ns = catalog.getFactory().createNamespace();
        ns.setPrefix( "bar" );
        ns.setURI( "http://bar" );
        catalog.add( ns );
        
        CoverageInfo ft = catalog.getFactory().createCoverage();
        ft.setName( "foo" );
        ft.setNamespace( ns );
        ft.setStore( catalog.getCoverageStoreByName( "acme", "foostore"));
        catalog.add( ft );
        
        assertTrue( d.exists() );
    }
    
    public void testModifyCoverage() throws Exception {
        testAddCoverage();
        
        CoverageInfo ft = catalog.getCoverageByName( "bar", "foo" );
        ft.setTitle( "fooTitle" );
        catalog.save( ft );
        
        File f = new File( testData.getDataDirectoryRoot(), 
            "workspaces/acme/foostore/foo/coverage.xml");
        Document dom = dom( f );
        
        assertXpathEvaluatesTo( "fooTitle", "/coverage/title", dom );
    }
    
    public void testRemoveCoverage() throws Exception {
        testAddCoverage();
        
        File d = new File( testData.getDataDirectoryRoot(), 
        "workspaces/acme/foostore/foo");
        assertTrue( d.exists() );
        
        CoverageInfo ft = catalog.getCoverageByName( "bar", "foo" );
        catalog.remove( ft );
        
        assertFalse( d.exists() );
    }
    
    public void testAddLayer() throws Exception {
        testAddFeatureType();
        testAddStyle();
        
        File f = new File( testData.getDataDirectoryRoot(), 
        "workspaces/acme/foostore/foo/layer.xml");
        assertFalse( f.exists() );
        
        LayerInfo l = catalog.getFactory().createLayer();
        // l.setName("foo");
        l.setResource( catalog.getFeatureTypeByName( "bar", "foo") );
        
        StyleInfo s = catalog.getStyleByName( "foostyle");
        l.setDefaultStyle(s);
        catalog.add( l );
        
        assertTrue( f.exists() );
    }
    
    public void testModifyLayer() throws Exception {
        testAddLayer();
        
        LayerInfo l = catalog.getLayerByName( "foo" );
        l.setPath( "/foo/bar" );
        catalog.save( l );
        
        File f = new File( testData.getDataDirectoryRoot(), 
            "workspaces/acme/foostore/foo/layer.xml");
        Document dom = dom( f );
        
        assertXpathEvaluatesTo( "/foo/bar", "/layer/path", dom );
    }
    
    public void testRemoveLayer() throws Exception {
        testAddLayer();
        
        File f = new File( testData.getDataDirectoryRoot(), 
            "workspaces/acme/foostore/foo/layer.xml");
        assertTrue( f.exists() );
        
        LayerInfo l = catalog.getLayerByName( "foo" );
        catalog.remove( l );
        
        assertFalse( f.exists() );
    }
    
    public void testAddStyle() throws Exception {
        
        File f = new File( testData.getDataDirectoryRoot(), 
            "styles/foostyle.xml");
        assertFalse( f.exists() );
        
        StyleInfo s = catalog.getFactory().createStyle();
        s.setName("foostyle");
        s.setFilename( "foostyle.sld");
        catalog.add( s );
        
        assertTrue( f.exists() );
    }
    
    public void testModifyStyle() throws Exception {
        testAddStyle();
        
        StyleInfo s = catalog.getStyleByName( "foostyle" );
        s.setFilename( "foostyle2.sld");
        catalog.save( s );
        
        File f = new File( testData.getDataDirectoryRoot(), 
            "styles/foostyle.xml");
        Document dom = dom( f );
        
        assertXpathEvaluatesTo( "foostyle2.sld", "/style/filename", dom );
    }
    
    public void testRemoveStyle() throws Exception {
        testAddStyle();
        
        File f = new File( testData.getDataDirectoryRoot(), 
            "styles/foostyle.xml");
        assertTrue( f.exists() );
        
        StyleInfo s = catalog.getStyleByName( "foostyle" );
        catalog.remove( s );
        
        assertFalse( f.exists() );
    }

    public void testAddLayerGroup() throws Exception {
        testAddLayer();
        //testAddStyle();
        
        File f = new File( testData.getDataDirectoryRoot(), 
            "layergroups/lg.xml");
        assertFalse( f.exists() );
        
        LayerGroupInfo lg = catalog.getFactory().createLayerGroup();
        lg.setName("lg");
        lg.getLayers().add( catalog.getLayerByName( "foo") );
        lg.getStyles().add( catalog.getStyleByName( "foostyle") );
        lg.getLayers().add( catalog.getLayerByName( "foo") );
        lg.getStyles().add( /* default style */ null);
        lg.getLayers().add( catalog.getLayerByName( "foo") );
        lg.getStyles().add( catalog.getStyleByName( "foostyle"));

        catalog.add( lg );
        
        assertTrue( f.exists() );
    }
    
    public void testModifyLayerGroup() throws Exception {
        testAddLayerGroup();
        
        LayerGroupInfo lg = catalog.getLayerGroupByName( "lg" );
        
        StyleInfo s = catalog.getFactory().createStyle();
        s.setName( "foostyle2" );
        s.setFilename( "foostyle2.sld");
        catalog.add( s );
        
        lg.getStyles().set( 0, s );
        catalog.save( lg );
        
        File f = new File( testData.getDataDirectoryRoot(), 
            "layergroups/lg.xml");
        Document dom = dom( f );
        assertXpathEvaluatesTo( s.getId(), "/layerGroup/styles/style/id", dom );
    }
    
    public void testRemoveLayerGroup() throws Exception {
        testAddLayerGroup();
        
        File f = new File( testData.getDataDirectoryRoot(), 
            "layergroups/lg.xml");
        assertTrue( f.exists() );
        
        LayerGroupInfo lg = catalog.getLayerGroupByName( "lg" );
        catalog.remove( lg );
        
        assertFalse( f.exists() );
    }
    
    public void testModifyGlobal() throws Exception {
        GeoServerInfo global = getGeoServer().getGlobal();
        global.setTitle( "ACME");
        getGeoServer().save( global );
        
        File f = new File( testData.getDataDirectoryRoot(), "global.xml" ); 
        Document dom = dom( f );
        assertXpathEvaluatesTo( "ACME", "/global/title", dom );
    }
    
    Document dom( File f ) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse( f );
    }
}
