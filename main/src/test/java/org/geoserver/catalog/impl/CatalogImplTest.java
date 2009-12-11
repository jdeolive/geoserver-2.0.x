package org.geoserver.catalog.impl;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;

import junit.framework.TestCase;

public class CatalogImplTest extends TestCase {

    CatalogImpl catalog;
    WorkspaceInfo ws;
    NamespaceInfo ns;
    DataStoreInfo ds;
    CoverageStoreInfo cs;
    FeatureTypeInfo ft;
    CoverageInfo cv;
    LayerInfo l;
    StyleInfo s;
    
    protected void setUp() throws Exception {
        catalog = new CatalogImpl();
        CatalogFactory factory = catalog.getFactory();
        
        ns = factory.createNamespace();
        ns.setPrefix( "nsPrefix" );
        ns.setURI( "nsURI" );
        
        ws = factory.createWorkspace();
        ws.setName( "wsName");
        
        ds = factory.createDataStore();
        ds.setEnabled(true);
        ds.setName( "dsName");
        ds.setDescription("dsDescription");
        ds.setWorkspace( ws );
        
        ft = factory.createFeatureType();
        ft.setEnabled(true);
        ft.setName( "ftName" );
        ft.setAbstract( "ftAbstract" );
        ft.setDescription( "ftDescription" );
        ft.setStore( ds );
        ft.setNamespace( ns );

        cs = factory.createCoverageStore();
        cs.setName("csName");
        cs.setType("fakeCoverageType");
        cs.setURL("file://fake");
        
        cv = factory.createCoverage();
        cv.setName("cvName");
        cv.setStore(cs);
        
        s = factory.createStyle();
        s.setName( "styleName" );
        s.setFilename( "styleFilename" );
        
        l = factory.createLayer();
        l.setEnabled(true);
        l.setResource( ft );
        l.setDefaultStyle( s );
    }

    public void testAddNamespace() {
        assertTrue( catalog.getNamespaces().isEmpty() );
        catalog.add( ns );
        assertEquals( 1, catalog.getNamespaces().size() );
        
        NamespaceInfo ns2 = catalog.getFactory().createNamespace();
        
        try {
            catalog.add( ns2 );
            fail( "adding without a prefix should throw exception");
        }
        catch( Exception e ) {
        }
        
        ns2.setPrefix( "ns2Prefix");
        try {
            catalog.add( ns2 );
            fail( "adding without a uri should throw exception");
        }
        catch( Exception e ) {
        }
        
        ns2.setURI( "bad uri");
        try {
            catalog.add( ns2 );
            fail( "adding an invalid uri should throw exception");
        }
        catch( Exception e ) {
        }
        
        ns2.setURI( "ns2URI");
        
        try {
            catalog.getNamespaces().add( ns2 );
            fail( "adding directly should throw an exception" );
        }
        catch( Exception e ) {
        }
        
        catalog.add( ns2 );
    }
    
    public void testRemoveNamespace() {
        catalog.add( ns );
        assertEquals( 1, catalog.getNamespaces().size() );
        
        try {
            assertFalse( catalog.getNamespaces().remove( ns ) );
            fail( "removing directly should throw an exception" );
        }
        catch( Exception e ) {
        }
        
        catalog.remove( ns );
        assertTrue( catalog.getNamespaces().isEmpty() );
    }

    public void testGetNamespaceById() {
        catalog.add( ns );
        NamespaceInfo ns2 = catalog.getNamespace(ns.getId());
        
        assertNotNull(ns2);
        assertFalse( ns == ns2 );
        assertEquals( ns, ns2 );
    }
    
    public void testGetNamespaceByPrefix() {
        catalog.add( ns );
        NamespaceInfo ns2 = catalog.getNamespaceByPrefix(ns.getPrefix());
        
        assertNotNull(ns2);
        assertFalse( ns == ns2 );
        assertEquals( ns, ns2 );
    }
    
    public void testGetNamespaceByURI() {
        catalog.add( ns );
        NamespaceInfo ns2 = catalog.getNamespaceByURI(ns.getURI());
        
        assertNotNull(ns2);
        assertFalse( ns == ns2 );
        assertEquals( ns, ns2 );
    }
    
    public void testModifyNamespace() {
        catalog.add( ns );
        
        NamespaceInfo ns2 = catalog.getNamespaceByPrefix(ns.getPrefix());
        ns2.setPrefix( null );
        ns2.setURI( null );
        
        try {
            catalog.save(ns2);
            fail( "setting prefix to null should throw exception");
        }
        catch( Exception e ) {
        }
        
        ns2.setPrefix( "ns2Prefix" );
        try {
            catalog.save(ns2);
            fail( "setting uri to null should throw exception");
        }
        catch( Exception e ) {
        }
        
        ns2.setURI( "ns2URI");
        
        NamespaceInfo ns3 = catalog.getNamespaceByPrefix(ns.getPrefix());
        assertEquals( "nsPrefix", ns3.getPrefix() );
        assertEquals( "nsURI", ns3.getURI() );
        
        catalog.save( ns2 );
        ns3 = catalog.getNamespaceByPrefix(ns.getPrefix());
        assertEquals(ns2, ns3);
        assertEquals( "ns2Prefix", ns3.getPrefix() );
        assertEquals( "ns2URI", ns3.getURI() );
    }
    
    public void testNamespaceEvents() {
        TestListener l = new TestListener();
        catalog.addListener( l );
        
        NamespaceInfo ns = catalog.getFactory().createNamespace();
        ns.setPrefix( "ns2Prefix" );
        ns.setURI( "ns2URI");
        
        assertTrue( l.added.isEmpty() );
        assertTrue( l.modified.isEmpty() );
        catalog.add( ns );
        assertEquals( 1, l.added.size() );
        assertEquals( ns, l.added.get(0).getSource());
        assertEquals( 1, l.modified.size() );
        assertEquals( catalog, l.modified.get(0).getSource());
        assertEquals( "defaultNamespace", l.modified.get(0).getPropertyNames().get(0));
        
        ns = catalog.getNamespaceByPrefix( "ns2Prefix" );
        ns.setURI( "changed");
        catalog.save( ns );
        
        assertEquals( 2, l.modified.size() );
        assertTrue(l.modified.get(1).getPropertyNames().contains( "uRI" ));
        assertTrue(l.modified.get(1).getOldValues().contains( "ns2URI" ));
        assertTrue(l.modified.get(1).getNewValues().contains( "changed" ));
        
        assertTrue( l.removed.isEmpty() );
        catalog.remove( ns );
        assertEquals( 1, l.removed.size() );
        assertEquals( ns, l.removed.get(0).getSource() );
    }
    
    public void testAddWorkspace() {
        assertTrue( catalog.getWorkspaces().isEmpty() );
        catalog.add( ws );
        assertEquals( 1, catalog.getWorkspaces().size() );
        
        WorkspaceInfo ws2 = catalog.getFactory().createWorkspace();
        
        try {
            catalog.getNamespaces().add( ws2 );
            fail( "adding directly should throw an exception" );
        }
        catch( Exception e ) {
        }
        
        try {
            catalog.add( ws2 );
            fail( "addign without a name should throw an exception");
        }
        catch( Exception e ) {
        }
        
        ws2.setName( "ws2" );
        catalog.add( ws2 );
    }
    
    public void testRemoveWorkspace() {
        catalog.add( ws );
        assertEquals( 1, catalog.getWorkspaces().size() );
        
        try {
            assertFalse( catalog.getWorkspaces().remove( ws ) );
            fail( "removing directly should throw an exception" );
        }
        catch( Exception e ) {
        }
        
        catalog.remove( ws );
        assertTrue( catalog.getWorkspaces().isEmpty() );
    }

    public void testAutoSetDefaultWorkspace() {
        catalog.add( ws );
        assertEquals( 1, catalog.getWorkspaces().size() );
        assertEquals(ws, catalog.getDefaultWorkspace());
    }

    public void testRemoveDefaultWorkspace() {
        catalog.add( ws );        
        catalog.remove( ws );
        assertNull(catalog.getDefaultWorkspace());
    }

    public void testAutoSetDefaultNamespace() {
        catalog.add( ns );
        assertEquals( 1, catalog.getNamespaces().size() );
        assertEquals(ns, catalog.getDefaultNamespace());
    }

    public void testRemoveDefaultNamespace() {
        catalog.add( ns );        
        catalog.remove( ns );
        assertNull(catalog.getDefaultNamespace());
    }

    public void testGetWorkspaceById() {
        catalog.add( ws );
        WorkspaceInfo ws2 = catalog.getWorkspace(ws.getId());
        
        assertNotNull(ws2);
        assertFalse( ws == ws2 );
        assertEquals( ws, ws2 );
    }
    
    public void testGetWorkspaceByName() {
        catalog.add( ws );
        WorkspaceInfo ws2 = catalog.getWorkspaceByName(ws.getName());
        
        assertNotNull(ws2);
        assertFalse( ws == ws2 );
        assertEquals( ws, ws2 );
    }
    
    public void testModifyWorkspace() {
        catalog.add( ws );
        
        WorkspaceInfo ws2 = catalog.getWorkspaceByName(ws.getName());
        ws2.setName( null );
        try {
            catalog.save( ws2 );
            fail( "setting name to null should throw exception");
        }
        catch( Exception e) {
        }
        
        ws2.setName( "ws2");
        
        WorkspaceInfo ws3 = catalog.getWorkspaceByName(ws.getName());
        assertEquals( "wsName", ws3.getName() );
        
        catalog.save( ws2 );
        ws3 = catalog.getWorkspaceByName(ws2.getName());
        assertEquals(ws2, ws3);
        assertEquals( "ws2", ws3.getName() );
    }
    
    public void testWorkspaceEvents() {
        TestListener l = new TestListener();
        catalog.addListener( l );
        
        WorkspaceInfo ws = catalog.getFactory().createWorkspace();
        ws.setName( "ws2");
        
        assertTrue( l.added.isEmpty() );
        assertTrue( l.modified.isEmpty() );
        catalog.add( ws );
        assertEquals( 1, l.added.size() );
        assertEquals( ws, l.added.get(0).getSource());
        assertEquals( catalog, l.modified.get(0).getSource());
        assertEquals( "defaultWorkspace", l.modified.get(0).getPropertyNames().get(0));
        
        ws = catalog.getWorkspaceByName( "ws2" );
        ws.setName( "changed");
        
        catalog.save( ws );
        assertEquals( 2, l.modified.size() );
        assertTrue(l.modified.get(1).getPropertyNames().contains( "name" ));
        assertTrue(l.modified.get(1).getOldValues().contains( "ws2" ));
        assertTrue(l.modified.get(1).getNewValues().contains( "changed" ));
        
        assertTrue( l.removed.isEmpty() );
        catalog.remove( ws );
        assertEquals( 1, l.removed.size() );
        assertEquals( ws, l.removed.get(0).getSource() );
    }
    
    public void testAddDataStore() {
        assertTrue( catalog.getDataStores().isEmpty() );
        catalog.add( ds );
        assertEquals( 1, catalog.getDataStores().size() );
        
        DataStoreInfo retrieved = catalog.getDataStore(ds.getId());
        
        DataStoreInfo ds2 = catalog.getFactory().createDataStore();
        try {
            catalog.add( ds2 );
            fail( "adding without a name should throw exception" );
        }
        catch(Exception e ) {
        }
        
        ds2.setName( "ds2Name" );
        try {
            catalog.getDataStores().add( ds2 );
            fail( "adding directly should throw an exception" );
        }
        catch( Exception e ) {
        }
        
        try {
            catalog.add( ds2 );
            fail( "adding with no workspace should throw exception" );
        }
        catch( Exception e ) {
        }
        ds2.setWorkspace( ws );
        
        catalog.add( ds2 );
        assertEquals( 2, catalog.getDataStores().size() );
    }
    
    public void testAddDataStoreDefaultWorkspace() {
        catalog.setDefaultWorkspace( ws );
        
        DataStoreInfo ds2 = catalog.getFactory().createDataStore();
        ds2.setName( "ds2Name");
        catalog.add( ds2 );
        
        assertEquals( ws, ds2.getWorkspace() );
    }
    
    public void testRemoveDataStore() {
        catalog.add( ds );
        assertEquals( 1, catalog.getDataStores().size() );
        
        try {
            assertFalse( catalog.getDataStores().remove( ds ) );
            fail( "removing directly should throw an exception" );
        }
        catch( Exception e ) {
        }
        
        catalog.remove( ds );
        assertTrue( catalog.getDataStores().isEmpty() );
    }
    
    public void testGetDataStoreById() {
        catalog.add( ds );
        
        DataStoreInfo ds2 = catalog.getDataStore(ds.getId());
        assertNotNull(ds2);
        assertFalse( ds == ds2 );
        assertEquals( ds, ds2 );
    }
    
    public void testGetDataStoreByName() {
        catalog.add( ds );
        
        DataStoreInfo ds2 = catalog.getDataStoreByName(ds.getName());
        assertNotNull(ds2);
        assertFalse( ds == ds2 );
        assertEquals( ds, ds2 );
    }
    
    public void testModifyDataStore() {
        catalog.add( ds );
        
        DataStoreInfo ds2 = catalog.getDataStoreByName(ds.getName());
        ds2.setName( "dsName2" );
        ds2.setDescription( "dsDescription2" );
        
        DataStoreInfo ds3 = catalog.getDataStoreByName(ds.getName());
        assertEquals( "dsName", ds3.getName() );
        assertEquals( "dsDescription", ds3.getDescription() );
        
        catalog.save( ds2 );
        ds3 = catalog.getDataStoreByName(ds.getName());
        assertEquals(ds2, ds3);
        assertEquals( "dsName2", ds3.getName() );
        assertEquals( "dsDescription2", ds3.getDescription() );
    }
    
    public void testChangeDataStoreWorkspace() throws Exception {
        catalog.add( ds );
        
        WorkspaceInfo ws2 = catalog.getFactory().createWorkspace();
        ws2.setName( "newWorkspace");
        catalog.add( ws2 );
        ws2 = catalog.getWorkspaceByName( ws2.getName() );
        
        DataStoreInfo ds2 = catalog.getDataStoreByName(ds.getName());
        ds2.setWorkspace( ws2 );
        catalog.save( ds2 );
        
        assertNull( catalog.getDataStoreByName( ws, ds2.getName() ) );
        assertNotNull( catalog.getDataStoreByName( ws2, ds2.getName() ) );
        
    }
    
    public void testDataStoreEvents() {
        TestListener l = new TestListener();
        catalog.addListener( l );
        
        assertEquals( 0, l.added.size() );
        catalog.add( ds );
        assertEquals( 1, l.added.size() );
        assertEquals( ds, l.added.get(0).getSource() );
        
        DataStoreInfo ds2 = catalog.getDataStoreByName( ds.getName() );
        ds2.setDescription( "changed" );
        
        assertEquals( 0, l.modified.size() );
        catalog.save( ds2 );
        assertEquals( 1, l.modified.size() );
        
        CatalogModifyEvent me = l.modified.get(0);
        assertEquals( ds2, me.getSource() );
        assertEquals( 1, me.getPropertyNames().size() );
        assertEquals( "description", me.getPropertyNames().get(0));
        
        assertEquals( 1, me.getOldValues().size() );
        assertEquals( 1, me.getNewValues().size() );
        
        assertEquals( "dsDescription", me.getOldValues().get(0));
        assertEquals( "changed", me.getNewValues().get(0));
        
        assertEquals( 0, l.removed.size() );
        catalog.remove( ds );
        
        assertEquals( 1, l.removed.size() );
        assertEquals( ds, l.removed.get( 0 ).getSource() );
    }
    
    public void testAddFeatureType() {
        //set a default namespace
        catalog.add( ns );
        
        assertTrue( catalog.getFeatureTypes().isEmpty() );
        
        catalog.add( ft );
        assertEquals( 1, catalog.getFeatureTypes().size() );
        
        FeatureTypeInfo ft2 = catalog.getFactory().createFeatureType();
        try {
            catalog.add(ft2);
            fail( "adding with no name should throw exception");
        }
        catch( Exception e ) {}
        
        ft2.setName("ft2Name");
        try {
            catalog.add(ft2);
            fail( "adding with no store should throw exception");
        }
        catch( Exception e ) {}
        
        ft2.setStore( ds );
        catalog.add( ft2 );
        
        FeatureTypeInfo ft3 = catalog.getFactory().createFeatureType();
        ft3.setName( "ft3Name");
        try {
            catalog.getFeatureTypes().add( ft3 );
            fail( "adding directly should throw an exception");
        }
        catch( Exception e ) {}
    }
    
    public void testAddCoverage() {
        //set a default namespace
        catalog.add( ns );
        
        assertNotNull(catalog.getCoverages());
        assertTrue( catalog.getCoverages().isEmpty() );
        
        catalog.add( cv );
        assertEquals( 1, catalog.getCoverages().size() );
        
        CoverageInfo cv2 = catalog.getFactory().createCoverage();
        try {
            catalog.add(cv2);
            fail( "adding with no name should throw exception");
        }
        catch( Exception e ) {}
        
        cv2.setName("cv2Name");
        try {
            catalog.add(cv2);
            fail( "adding with no store should throw exception");
        }
        catch( Exception e ) {}
        
        cv2.setStore( cs );
        catalog.add( cv2 );
        assertEquals( 2, catalog.getCoverages().size() );

        CoverageInfo fromCatalog = catalog.getCoverageByName("cv2Name");
        assertNotNull(fromCatalog);
        //ensure the collection properties are set to NullObjects and not to null
        assertNotNull(fromCatalog.getParameters());
        
        CoverageInfo cv3 = catalog.getFactory().createCoverage();
        cv3.setName( "cv3Name");
        try {
            catalog.getCoverages().add( cv3 );
            fail( "adding directly should throw an exception");
        }
        catch( Exception e ) {}
    }

    public void testRemoveFeatureType() {
        catalog.add( ft );
        assertFalse( catalog.getFeatureTypes().isEmpty() );
        
        try {
            catalog.getFeatureTypes().remove( ft );
            fail( "removing directly should cause exception");
        }
        catch( Exception e ) {}
        
        catalog.remove( ft );
        assertTrue( catalog.getFeatureTypes().isEmpty() );
    }
    
    public void testGetFeatureTypeById() {
        catalog.add( ft );
        FeatureTypeInfo  ft2 = catalog.getFeatureType(ft.getId());
        
        assertNotNull(ft2);
        assertFalse( ft == ft2 );
        assertEquals( ft, ft2 );
    }

    public void testGetFeatureTypeByName() {
        catalog.add( ft );
        FeatureTypeInfo  ft2 = catalog.getFeatureTypeByName(ft.getName());
        
        assertNotNull(ft2);
        assertFalse( ft == ft2 );
        assertEquals( ft, ft2 );
        
        NamespaceInfo ns2 = catalog.getFactory().createNamespace();
        ns2.setPrefix( "ns2Prefix" );
        ns2.setURI( "ns2URI" );
        catalog.add( ns2 );
        
        FeatureTypeInfo ft3 = catalog.getFactory().createFeatureType();
        ft3.setName( "ft3Name" );
        ft3.setStore( ds );
        ft3.setNamespace( ns2 );
        catalog.add( ft3 );
        
        FeatureTypeInfo ft4 = catalog.getFeatureTypeByName(ns2.getPrefix(), ft3.getName() );
        assertNotNull(ft4);
        assertFalse( ft4 == ft3 );
        assertEquals( ft3, ft4 );
        
        ft4 = catalog.getFeatureTypeByName(ns2.getURI(), ft3.getName() );
        assertNotNull(ft4);
        assertFalse( ft4 == ft3 );
        assertEquals( ft3, ft4 );
    }
    
    public void testGetFeatureTypesByStore() {
        catalog.add( ns );
        catalog.add( ws );
        
        catalog.setDefaultNamespace( ns );
        catalog.setDefaultWorkspace( ws );
        
        DataStoreInfo ds1 = catalog.getFactory().createDataStore();
        ds1.setName( "ds1" );
        catalog.add( ds );
        
        FeatureTypeInfo ft1 = catalog.getFactory().createFeatureType();
        ft1.setName( "ft1" );
        ft1.setStore(ds1);
        catalog.add( ft1 );
        
        FeatureTypeInfo ft2 = catalog.getFactory().createFeatureType();
        ft2.setName( "ft2" );
        ft2.setStore(ds1);
        catalog.add( ft2 );
        
        DataStoreInfo ds2 = catalog.getFactory().createDataStore();
        ds2.setName( "ds2" );
        catalog.add( ds2 );
        
        FeatureTypeInfo ft3 = catalog.getFactory().createFeatureType();
        ft3.setName( "ft3" );
        ft3.setStore( ds2 );
        catalog.add( ft3 );
        
        List<FeatureTypeInfo> ft = catalog.getFeatureTypesByStore( ds1 );
        assertEquals( 2, ft.size() );
        
        ft = catalog.getFeatureTypesByStore( ds2 );
        assertEquals( 1, ft.size() );
        
        List<ResourceInfo> r = catalog.getResourcesByStore(ds1,ResourceInfo.class);
        assertEquals( 2, r.size() );
        assertEquals( ft1, r.get(0) );
        assertEquals( ft2, r.get(1) );
    }
    
    public void testModifyFeatureType() {
        catalog.add( ft );
        
        FeatureTypeInfo ft2 = catalog.getFeatureTypeByName(ft.getName());
        ft2.setDescription( "ft2Description" );
        ft2.getKeywords().add( "ft2");
        
        FeatureTypeInfo ft3 = catalog.getFeatureTypeByName(ft.getName());
        assertEquals( "ftName", ft3.getName() );
        assertEquals( "ftDescription", ft3.getDescription() );
        assertTrue( ft3.getKeywords().isEmpty() );
        
        catalog.save( ft2 );
        ft3 = catalog.getFeatureTypeByName(ft.getName());
        assertEquals(ft2, ft3);
        assertEquals( "ft2Description", ft3.getDescription() );
        assertEquals( 1, ft3.getKeywords().size() );
    }
    
    
    public void testFeatureTypeEvents() {
        
        //set default namespace
        catalog.add( ns );
        
        TestListener l = new TestListener();
        catalog.addListener( l );
        
        FeatureTypeInfo ft = catalog.getFactory().createFeatureType();
        ft.setName( "ftName" );
        ft.setDescription( "ftDescription" );
        ft.setStore( ds );
        
        assertTrue( l.added.isEmpty() );
        catalog.add(ft);
        
        assertEquals( 1, l.added.size() );
        assertEquals( ft, l.added.get(0).getSource() );
        
        ft = catalog.getFeatureTypeByName("ftName");
        ft.setDescription( "changed" );
        assertTrue( l.modified.isEmpty() );
        catalog.save(ft);
        assertEquals( 1, l.modified.size() );
        assertEquals( ft, l.modified.get(0).getSource() );
        assertTrue( l.modified.get(0).getPropertyNames().contains( "description"));
        assertTrue( l.modified.get(0).getOldValues().contains( "ftDescription"));
        assertTrue( l.modified.get(0).getNewValues().contains( "changed"));
        
        assertTrue( l.removed.isEmpty() );
        catalog.remove( ft );
        assertEquals( 1, l.removed.size() );
        assertEquals( ft, l.removed.get(0).getSource() );
    }
    
    public void testAddLayer() {
        assertTrue( catalog.getLayers().isEmpty() );
        catalog.add( l );
        
        assertEquals( 1, catalog.getLayers().size() );
        
        LayerInfo l2 = catalog.getFactory().createLayer();
        try {
            catalog.add( l2 );
            fail( "adding with no name should throw exception");
        }
        catch( Exception e) {}
        
        // l2.setName( "l2" );
        try {
            catalog.add( l2 );
            fail( "adding with no resource should throw exception");
        }
        catch( Exception e) {}
        
        l2.setResource( ft );
        //try {
        //    catalog.add( l2 );
        //    fail( "adding with no default style should throw exception");
        //}
        //catch( Exception e) {}
        //
        l2.setDefaultStyle( s );
        
        catalog.add( l2 );
        assertEquals( 2, catalog.getLayers().size() );
    }
    
    public void testGetLayerById() {
        catalog.add( l );
            
        LayerInfo l2 = catalog.getLayer( l.getId() );
        assertNotNull(l2);
        assertNotSame(l,l2);
        assertEquals( l, l2 );
    }
    
    public void testGetLayerByName() {
        catalog.add( l );
            
        LayerInfo l2 = catalog.getLayerByName( l.getName() );
        assertNotNull(l2);
        assertNotSame(l,l2);
        assertEquals( l, l2 );
    }
    
    public void testGetLayerByResource() {
        catalog.add(l);
        
        List<LayerInfo> layers = catalog.getLayers(ft);
        assertEquals( 1, layers.size() );
        LayerInfo l2 = layers.get(0);
        
        assertNotSame( l, l2 );
        assertEquals( l, l2 );
    }
    
    public void testRemoveLayer() {
        catalog.add(l);
        assertEquals( 1, catalog.getLayers().size() );
        
        catalog.remove(l);
        assertTrue( catalog.getLayers().isEmpty() );
    }
    
    public void testModifyLayer() {
        catalog.add(l);
        
        LayerInfo l2 = catalog.getLayerByName( l.getName() );
//        l2.setName( null );
        l2.setResource( null );
        
        LayerInfo l3 = catalog.getLayerByName( l.getName() );
        assertEquals( l.getName(), l3.getName() );
        
//        try {
//            catalog.save(l2);
//            fail( "setting name to null should throw exception");
//        }
//        catch( Exception e ) {}
//        
//        l2.setName( "changed" );
        try {
            catalog.save(l2);
            fail( "setting resource to null should throw exception");
        }
        catch( Exception e ) {}
        
        l2.setResource(ft);
        catalog.save(l2);
        
        // TODO: reinstate with resource/publishing split done
        // l3 = catalog.getLayerByName( "changed" );
        l3 = catalog.getLayerByName( ft.getName() );
        assertNotNull(l3);
    }
    
    public void testEnableLayer() {
        catalog.add(l);
        
        LayerInfo l2 = catalog.getLayerByName(l.getName());
        assertTrue(l2.isEnabled());
        assertTrue(l2.enabled());
        assertTrue(l2.getResource().isEnabled());
        
        l2.setEnabled(false);
        catalog.save(l2);
        
        l2 = catalog.getLayerByName(l2.getName());
        assertFalse(l2.isEnabled());
        assertFalse(l2.enabled());
        assertFalse(l2.getResource().isEnabled());
    }
    
    public void testLayerEvents() {
        TestListener tl = new TestListener();
        catalog.addListener( tl );
        
        assertTrue( tl.added.isEmpty() );
        catalog.add( l );
        assertEquals( 1, tl.added.size() );
        assertEquals( l, tl.added.get(0).getSource() );
        
        LayerInfo l2 = catalog.getLayerByName( l.getName() );
        l2.setPath( "newPath" );
        
        assertTrue( tl.modified.isEmpty() );
        catalog.save( l2 );
        assertEquals( 1, tl.modified.size() );
        assertEquals( l2, tl.modified.get(0).getSource() );
        assertTrue( tl.modified.get(0).getPropertyNames().contains( "path") );
        assertTrue( tl.modified.get(0).getOldValues().contains( null ) );
        assertTrue( tl.modified.get(0).getNewValues().contains( "newPath") );
        
        assertTrue( tl.removed.isEmpty() );
        catalog.remove( l2 );
        assertEquals( 1, tl.removed.size() ); 
        assertEquals( l2, tl.removed.get(0).getSource() );
    }
    
    public void testAddStyle() {
        assertTrue( catalog.getStyles().isEmpty() );
        catalog.add( s );
        assertEquals( 1, catalog.getStyles().size() );
        
        StyleInfo s2 = catalog.getFactory().createStyle();
        try {
            catalog.add( s2 );
            fail( "adding without name should throw exception");
        }
        catch( Exception e ) {}
        
        s2.setName( "s2Name");
        try {
            catalog.add( s2 );
            fail( "adding without fileName should throw exception");
        }
        catch( Exception e ) {}
        
        s2.setFilename( "s2Filename");
        try {
            catalog.getStyles().add( s2 );
            fail( "adding directly should throw exception");
        }
        catch( Exception e ) {}
        
        catalog.add( s2 );
        assertEquals( 2, catalog.getStyles().size() );
    }
    
    public void testGetStyleById() {
        catalog.add( s );
        
        StyleInfo s2 = catalog.getStyle( s.getId() );
        assertNotNull( s2 );
        assertNotSame(s,s2);
        assertEquals(s,s2);
    }
    
    public void testGetStyleByName() {
        catalog.add( s );
        
        StyleInfo s2 = catalog.getStyleByName( s.getName() );
        assertNotNull( s2 );
        assertNotSame(s,s2);
        assertEquals(s,s2);
    }
    
    public void testModifyStyle() {
        catalog.add(s);
        
        StyleInfo s2 = catalog.getStyleByName( s.getName() );
        s2.setName( null );
        s2.setFilename( null );
        
        StyleInfo s3 = catalog.getStyleByName( s.getName() );
        assertEquals( s, s3 );
        
        try {
            catalog.save(s2);
            fail("setting name to null should fail");
        }
        catch( Exception e ) {}
        
        s2.setName( "s2Name");
        try {
            catalog.save(s2);
            fail("setting filename to null should fail");
        }
        catch( Exception e ) {}
        
        s2.setFilename( "s2Filename");
        catalog.save( s2 );
        
        s3 = catalog.getStyleByName( "styleName" );
        assertNull( s3 );
        
        s3 = catalog.getStyleByName( s2.getName() );
        assertEquals( s2, s3 );
    }
    
    public void testRemoveStyle() {
        catalog.add(s);
        assertEquals( 1, catalog.getStyles().size());
        
        catalog.remove(s);
        assertTrue( catalog.getStyles().isEmpty() );
    }
    
    public void testStyleEvents() {
        TestListener l = new TestListener();
        catalog.addListener( l );
        
        assertTrue( l.added.isEmpty() );
        catalog.add( s );
        assertEquals( 1, l.added.size() );
        assertEquals( s, l.added.get(0).getSource() );
        
        StyleInfo s2 = catalog.getStyleByName(s.getName());
        s2.setFilename( "changed");
        
        assertTrue( l.modified.isEmpty() );
        catalog.save( s2 );
        assertEquals( 1, l.modified.size() );
        assertEquals( s2, l.modified.get(0).getSource() );
        assertTrue( l.modified.get(0).getPropertyNames().contains( "filename") );
        assertTrue( l.modified.get(0).getOldValues().contains( "styleFilename") );
        assertTrue( l.modified.get(0).getNewValues().contains( "changed") );
        
        assertTrue( l.removed.isEmpty() );
        catalog.remove( s2 );
        assertEquals( 1, l.removed.size() );
        assertEquals( s2, l.removed.get(0).getSource());
    }
    
    public void testProxyBehaviour() throws Exception {
        testAddLayer();
        
        // l = catalog.getLayerByName( "layerName");
        LayerInfo l = catalog.getLayerByName(ft.getName());
        assertTrue( l instanceof Proxy );
        
        ResourceInfo r = l.getResource();
        assertTrue( r instanceof Proxy );
        
        r.setName( "changed");
        catalog.save( l );
        
        // l = catalog.getLayerByName( "layerName");
        l = catalog.getLayerByName(ft.getName());
        assertEquals( "changed", l.getResource().getName() );
    }
    
    public void testProxyListBehaviour() throws Exception {
        catalog.add( s );
        
        StyleInfo s2 = catalog.getFactory().createStyle();
        s2.setName( "a" + s.getName() );
        s2.setFilename( "a.sld");
        catalog.add( s2 );
        
        List<StyleInfo> styles = catalog.getStyles();
        assertEquals( 2 , styles.size() );
        
        assertEquals( s.getName(), styles.get( 0 ).getName() );
        assertEquals( "a"+s.getName(), styles.get( 1).getName() );
        
        //test sorting
        Collections.sort( styles, new Comparator<StyleInfo>() {

            public int compare(StyleInfo o1, StyleInfo o2) {
                return o1.getName().compareTo( o2.getName());
            }
        });
        
        assertEquals( "a"+s.getName(), styles.get( 0 ).getName() );
        assertEquals( s.getName(), styles.get( 1 ).getName() );

    }
    
    public void testExceptionThrowingListener() throws Exception {
        ExceptionThrowingListener l = new ExceptionThrowingListener();
        catalog.addListener(l);
        
        l.throwCatalogException = false;
        
        WorkspaceInfo ws = catalog.getFactory().createWorkspace();
        ws.setName("foo");
        
        //no exception thrown back
        catalog.add(ws);
        
        l.throwCatalogException = true;
        ws = catalog.getFactory().createWorkspace();
        ws.setName("bar");
        
        try {
            catalog.add(ws);
            fail();
        }
        catch( CatalogException ce ) {
            //good
        }
    }
    
    
    static class TestListener implements CatalogListener {

        public List<CatalogAddEvent> added = new ArrayList();
        public List<CatalogModifyEvent> modified = new ArrayList();
        public List<CatalogRemoveEvent> removed = new ArrayList();
        
        public void handleAddEvent(CatalogAddEvent event) {
            added.add( event );
        }

        public void handleModifyEvent(CatalogModifyEvent event) {
            modified.add( event );
        }

        public void handlePostModifyEvent(CatalogPostModifyEvent event) {
        }
        
        public void handleRemoveEvent(CatalogRemoveEvent event) {
            removed.add( event );
        }
        
        public void reloaded() {
        }
    }
    
    static class ExceptionThrowingListener implements CatalogListener {

        public boolean throwCatalogException;
        
        public void handleAddEvent(CatalogAddEvent event) throws CatalogException {
            if (throwCatalogException) {
                throw new CatalogException();
            }
            else {
                throw new RuntimeException();
            }
        }

        public void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {
        }

        public void handlePostModifyEvent(CatalogPostModifyEvent event) throws CatalogException {
        }

        public void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {
        }

        public void reloaded() {
        }
        
    }
}
