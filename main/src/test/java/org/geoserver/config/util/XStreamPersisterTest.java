package org.geoserver.config.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import junit.framework.TestCase;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServerFactory;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.config.impl.ServiceInfoImpl;
import org.geotools.referencing.CRS;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XStreamPersisterTest extends TestCase {

    GeoServerFactory factory;
    CatalogFactory cfactory;
    XStreamPersister persister;
    
    protected void setUp() throws Exception {
        super.setUp();
        
        factory = new GeoServerImpl().getFactory();
        persister = new XStreamPersister.XML();
    }
    
    public void testGlobal() throws Exception {
        GeoServerInfo g1 = factory.createGlobal();
        g1.setAdminPassword( "foo" );
        g1.setAdminUsername( "bar" );
        g1.setCharset( "ISO-8859-1" );
        
        ContactInfo contact = factory.createContact();
        g1.setContact( contact );
        contact.setAddress( "123" );
        contact.setAddressCity( "Victoria" );
        contact.setAddressCountry( "Canada" );
        contact.setAddressPostalCode( "V1T3T8");
        contact.setAddressState( "BC" );
        contact.setAddressType( "house" );
        contact.setContactEmail( "bob@acme.org" );
        contact.setContactFacsimile("+1 250 123 4567" );
        contact.setContactOrganization( "Acme" );
        contact.setContactPerson( "Bob" );
        contact.setContactPosition( "hacker" );
        contact.setContactVoice( "+1 250 765 4321" );
        
        g1.setNumDecimals( 2 );
        g1.setOnlineResource( "http://acme.org" );
        g1.setProxyBaseUrl( "http://proxy.acme.org" );
        g1.setSchemaBaseUrl( "http://schemas.acme.org");
        
        g1.setTitle( "Acme's GeoServer" );
        g1.setUpdateSequence( 123 );
        g1.setVerbose( true );
        g1.setVerboseExceptions( true );
        
        g1.getMetadata().put( "one", new Integer(1) );
        g1.getMetadata().put( "two", new Double(2.2) );
        
        ByteArrayOutputStream out = out();
        
        persister.save( g1, out );
        
        GeoServerInfo g2 = persister.load(in(out),GeoServerInfo.class);
        assertEquals( g1, g2 );
        
        Document dom = dom( in( out ) );
        assertEquals( "global", dom.getDocumentElement().getNodeName() );
    }
    
    public void testLogging() throws Exception {
        LoggingInfo logging = factory.createLogging();
        
        logging.setLevel( "CRAZY_LOGGING" );
        logging.setLocation( "some/place/geoserver.log" );
        logging.setStdOutLogging( true );
        
        ByteArrayOutputStream out = out();
        persister.save( logging, out );
        
        LoggingInfo logging2 = persister.load(in(out),LoggingInfo.class);
        assertEquals( logging, logging2 );
        
        Document dom = dom( in( out ) );
        assertEquals( "logging", dom.getDocumentElement().getNodeName() );
        
    }
    public void testGobalContactDefault() throws Exception {
        GeoServerInfo g1 = factory.createGlobal();
        ContactInfo contact = factory.createContact();
        g1.setContact( contact );
        
        ByteArrayOutputStream out = out();
        persister.save(g1, out);
        
        ByteArrayInputStream in = in( out );
        Document dom = dom( in );
        
        Element e = (Element) dom.getElementsByTagName( "contact" ).item(0);
        e.removeAttribute( "class" );
        in = in( dom );
        
        GeoServerInfo g2 = persister.load( in, GeoServerInfo.class );
        assertEquals( g1, g2 );
    }
   
    static class MyServiceInfo extends ServiceInfoImpl {
        
        String foo;
        
        String getFoo() {
            return foo;
        }
        
        void setFoo( String foo ) {
            this.foo = foo;
        }
        
        public boolean equals(Object obj) {
            if ( !( obj instanceof MyServiceInfo ) ) {
                return false;
            }
            
            MyServiceInfo other = (MyServiceInfo) obj;
            if ( foo == null ) {
                if ( other.foo != null ) {
                    return false;
                }
            }
            else {
                if ( !foo.equals( other.foo ) ) {
                    return false;
                }
            }
            
            return super.equals(other); 
        }
    }

    public void testService() throws Exception {
        MyServiceInfo s1 = new MyServiceInfo();
        s1.setAbstract( "my service abstract" );
        s1.setAccessConstraints( "no constraints" );
        s1.setCiteCompliant( true );
        s1.setEnabled( true );
        s1.setFees( "no fees" );
        s1.setFoo( "bar" );
        s1.setId( "id" );
        s1.setMaintainer( "Bob" );
        s1.setMetadataLink( factory.createMetadataLink() );
        s1.setName( "MS" );
        s1.setOnlineResource( "http://acme.org?service=myservice" );
        s1.setOutputStrategy("FAST");
        s1.setSchemaBaseURL( "http://schemas.acme.org/");
        s1.setTitle( "My Service" );
        s1.setVerbose(true);
        
        ByteArrayOutputStream out = out();
        persister.save( s1, out );
        
        MyServiceInfo s2 = persister.load( in( out ), MyServiceInfo.class );
        assertEquals( s1.getAbstract(), s2.getAbstract() );
        assertEquals( s1.getAccessConstraints(), s2.getAccessConstraints() );
        assertEquals( s1.isCiteCompliant(), s2.isCiteCompliant() );
        assertEquals( s1.isEnabled(), s2.isEnabled() );
        assertEquals( s1.getFees(), s2.getFees() );
        assertEquals( s1.getFoo(), s2.getFoo() );
        assertEquals( s1.getId(), s2.getId() );
        assertEquals( s1.getMaintainer(), s2.getMaintainer() );
        assertEquals( s1.getMetadataLink(), s2.getMetadataLink() );
        assertEquals( s1.getName(), s2.getName() );
        assertEquals( s1.getOnlineResource( ), s2.getOnlineResource() );
        assertEquals( s1.getOutputStrategy(), s2.getOutputStrategy() );
        assertEquals( s1.getSchemaBaseURL(), s2.getSchemaBaseURL() );
        assertEquals( s1.getTitle(), s2.getTitle() );
        assertEquals( s1.isVerbose(), s2.isVerbose() );
    } 
    
    public void testServiceOmitGlobal() throws Exception {
        MyServiceInfo s1 = new MyServiceInfo();
        s1.setGeoServer( new GeoServerImpl() );
        
        ByteArrayOutputStream out = out();
        persister.save( s1, out );
        
        MyServiceInfo s2 = persister.load( in( out ), MyServiceInfo.class );
        
        assertNull( s2.getGeoServer() );
    }
    
    public void testServiceCustomAlias() throws Exception {
        XStreamPersister p = new XStreamPersister.XML();
        p.getXStream().alias( "ms", MyServiceInfo.class );
        
        MyServiceInfo s1 = new MyServiceInfo();
        
        ByteArrayOutputStream out = out();
        p.save( s1, out );
        
        Document dom = dom( in( out ) ) ;
        assertEquals( "ms", dom.getDocumentElement().getNodeName() );
    }
    
    public void testDataStore() throws Exception {
        Catalog catalog = new CatalogImpl();
        CatalogFactory cFactory = catalog.getFactory();
        
        WorkspaceInfo ws = cFactory.createWorkspace();
        ws.setName( "foo" );
        
        DataStoreInfo ds1 = cFactory.createDataStore();
        ds1.setName( "bar" );
        ds1.setWorkspace( ws );
        
        ByteArrayOutputStream out = out();
        persister.save( ds1 , out );
        
        DataStoreInfo ds2 = persister.load( in( out ), DataStoreInfo.class );
        assertEquals( "bar", ds2.getName() );
        
        //TODO: reenable when resolving proxy commited
        //assertNotNull( ds2.getWorkspace() );
        //assertEquals( "foo", ds2.getWorkspace().getId() );
        
        Document dom = dom( in( out ) );
        assertEquals( "dataStore", dom.getDocumentElement().getNodeName() );
    }
    
    public void testCoverageStore() throws Exception {
        Catalog catalog = new CatalogImpl();
        CatalogFactory cFactory = catalog.getFactory();
        
        WorkspaceInfo ws = cFactory.createWorkspace();
        ws.setName( "foo" );
        
        CoverageStoreInfo cs1 = cFactory.createCoverageStore();
        cs1.setName( "bar" );
        cs1.setWorkspace( ws );
        
        ByteArrayOutputStream out = out();
        persister.save( cs1 , out );
        
        CoverageStoreInfo ds2 = persister.load( in( out ), CoverageStoreInfo.class );
        assertEquals( "bar", ds2.getName() );
        
        //TODO: reenable when resolving proxy commited
        //assertNotNull( ds2.getWorkspace() );
        //assertEquals( "foo", ds2.getWorkspace().getId() );
        
        Document dom = dom( in( out ) );
        assertEquals( "coverageStore", dom.getDocumentElement().getNodeName() );
    }
    
    public void testStyle() throws Exception {
        Catalog catalog = new CatalogImpl();
        CatalogFactory cFactory = catalog.getFactory();
        
        StyleInfo s1 = cFactory.createStyle();
        s1.setName( "foo" );
        s1.setFilename( "foo.sld" );
        
        ByteArrayOutputStream out = out();
        persister.save( s1, out );
        
        ByteArrayInputStream in = in( out );
        
        StyleInfo s2 = persister.load(in,StyleInfo.class);
        assertEquals( s1, s2 );
        
        Document dom = dom( in( out ) );
        assertEquals( "style", dom.getDocumentElement().getNodeName() );
    }
    
    public void testCatalog() throws Exception {
        Catalog catalog = new CatalogImpl();
        CatalogFactory cFactory = catalog.getFactory();
        
        WorkspaceInfo ws = cFactory.createWorkspace();
        ws.setName( "foo" );
        catalog.add( ws );
        
        NamespaceInfo ns = cFactory.createNamespace();
        ns.setPrefix( "acme" );
        ns.setURI( "http://acme.org" );
        catalog.add( ns );
        
        DataStoreInfo ds = cFactory.createDataStore();
        ds.setWorkspace( ws );
        ds.setName( "foo" );
        catalog.add( ds );
        
        CoverageStoreInfo cs = cFactory.createCoverageStore();
        cs.setWorkspace( ws );
        cs.setName( "bar" );
        catalog.add( cs );
        
        StyleInfo s = cFactory.createStyle();
        s.setName( "style" );
        s.setFilename( "style.sld" );
        catalog.add(s);
     
        ByteArrayOutputStream out = out();
        persister.save( catalog, out );
        
        catalog = persister.load( in(out), Catalog.class );
        assertNotNull(catalog);
        
        assertEquals( 1, catalog.getWorkspaces().size() );
        assertNotNull( catalog.getDefaultWorkspace() );
        ws = catalog.getDefaultWorkspace();
        assertEquals( "foo", ws.getName() );
        
        assertEquals( 1, catalog.getNamespaces().size() );
        assertNotNull( catalog.getDefaultNamespace() );
        ns = catalog.getDefaultNamespace();
        assertEquals( "acme", ns.getPrefix() );
        assertEquals( "http://acme.org", ns.getURI() );
        
        assertEquals( 1, catalog.getDataStores().size() );
        ds = catalog.getDataStores().get( 0 );
        assertEquals( "foo", ds.getName() );
        assertNotNull( ds.getWorkspace() );
        assertEquals( ws, ds.getWorkspace() );
        
        assertEquals( 1, catalog.getCoverageStores().size() );
        cs = catalog.getCoverageStores().get( 0 );
        assertEquals( "bar", cs.getName() );
        assertEquals( ws, cs.getWorkspace() );
        
        assertEquals( 1, catalog.getStyles().size() );
        s = catalog.getStyles().get(0);
        assertEquals( "style", s.getName() );
        assertEquals( "style.sld", s.getFilename() );
    }
    
    public void testFeatureType() throws Exception {
        Catalog catalog = new CatalogImpl();
        CatalogFactory cFactory = catalog.getFactory();
        
        WorkspaceInfo ws = cFactory.createWorkspace();
        ws.setName( "foo" );
        catalog.add( ws );
        
        NamespaceInfo ns = cFactory.createNamespace();
        ns.setPrefix( "acme" );
        ns.setURI( "http://acme.org" );
        catalog.add( ns );
        
        DataStoreInfo ds = cFactory.createDataStore();
        ds.setWorkspace( ws );
        ds.setName( "foo" );
        catalog.add( ds );
        
        FeatureTypeInfo ft = cFactory.createFeatureType();
        ft.setStore( ds );
        ft.setNamespace( ns );
        ft.setName( "ft" );
        ft.setAbstract( "abstract");
        ft.setSRS( "EPSG:4326");
        ft.setNativeCRS( CRS.decode( "EPSG:4326") );
        
        ByteArrayOutputStream out = out();
        persister.save( ft, out );
        
        persister.setCatalog( catalog );
        ft = persister.load( in( out ), FeatureTypeInfo.class );
        assertNotNull( ft );
        
        assertEquals( "ft", ft.getName() );
        assertEquals( ds, ft.getStore() );
        assertEquals( ns, ft.getNamespace() );
        assertEquals( "EPSG:4326", ft.getSRS() );
        assertTrue( CRS.equalsIgnoreMetadata( CRS.decode( "EPSG:4326"), ft.getNativeCRS() ) ); 
    }
    
    public void testLayer() throws Exception {
        Catalog catalog = new CatalogImpl();
        CatalogFactory cFactory = catalog.getFactory();
        
        WorkspaceInfo ws = cFactory.createWorkspace();
        ws.setName( "foo" );
        catalog.add( ws );
        
        NamespaceInfo ns = cFactory.createNamespace();
        ns.setPrefix( "acme" );
        ns.setURI( "http://acme.org" );
        catalog.add( ns );
        
        DataStoreInfo ds = cFactory.createDataStore();
        ds.setWorkspace( ws );
        ds.setName( "foo" );
        catalog.add( ds );
        
        FeatureTypeInfo ft = cFactory.createFeatureType();
        ft.setStore( ds );
        ft.setNamespace( ns );
        ft.setName( "ft" );
        ft.setAbstract( "abstract");
        ft.setSRS( "EPSG:4326");
        ft.setNativeCRS( CRS.decode( "EPSG:4326") );
        catalog.add( ft );
        
        StyleInfo s = cFactory.createStyle();
        s.setName( "style" );
        s.setFilename( "style.sld" );
        catalog.add( s );
        
        LayerInfo l = cFactory.createLayer();
        // TODO: reinstate when layer/publish slipt is actually in place
        // l.setName( "layer" );
        l.setResource( ft );
        l.setDefaultStyle( s );
        catalog.add( l );
        
        ByteArrayOutputStream out = out();
        persister.save( l, out );
        
        persister.setCatalog( catalog );
        l = persister.load( in( out ) , LayerInfo.class );
        
        assertEquals( l.getResource().getName(), l.getName() );
        assertEquals( ft, l.getResource() );
        assertEquals( s, l.getDefaultStyle() );
        //assertNotNull( l.getStyles() );
        
    }
    
    ByteArrayOutputStream out() {
        return new ByteArrayOutputStream();
    }
    
    ByteArrayInputStream in( ByteArrayOutputStream in ) {
        return new ByteArrayInputStream( in.toByteArray() );
    }
    
    ByteArrayInputStream in( Document dom ) throws Exception {
        Transformer tx = TransformerFactory.newInstance().newTransformer();
        tx.setOutputProperty( OutputKeys.INDENT, "yes" );
        
        ByteArrayOutputStream out = out();
        tx.transform( new DOMSource( dom ), new StreamResult( out ) );
        
        return in( out );
    }
    
    Document dom( InputStream in ) throws Exception {
        return 
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse( in );
    }
    
    void print( InputStream in ) throws Exception {
        Transformer tx = TransformerFactory.newInstance().newTransformer();
        tx.setOutputProperty( OutputKeys.INDENT, "yes" );
        
        tx.transform( new StreamSource( in ), new StreamResult( System.out ) );
    }
}
