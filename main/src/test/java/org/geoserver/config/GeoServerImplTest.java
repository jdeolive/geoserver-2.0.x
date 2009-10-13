package org.geoserver.config;

import java.util.ArrayList;
import java.util.List;

import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.config.impl.ServiceInfoImpl;

import junit.framework.TestCase;

public class GeoServerImplTest extends TestCase {

    GeoServerImpl geoServer;
    
    protected void setUp() throws Exception {
        super.setUp();
        
        geoServer = new GeoServerImpl();
    }
    
    public void testGlobal() throws Exception {
        GeoServerInfo global = geoServer.getFactory().createGlobal();
        geoServer.setGlobal( global );
       
        assertEquals( global, geoServer.getGlobal() );
    }
    
    public void testModifyGlobal() throws Exception {
        GeoServerInfo global = geoServer.getFactory().createGlobal();
        geoServer.setGlobal( global );

        GeoServerInfo g1 = geoServer.getGlobal();
        g1.setAdminPassword( "newAdminPassword" );
        
        GeoServerInfo g2 = geoServer.getGlobal();
        assertNull( g2.getAdminPassword() );
        
        geoServer.save( g1 );
        g2 = geoServer.getGlobal();
        assertEquals( "newAdminPassword", g2.getAdminPassword() );
    }
    
    public void testAddService() throws Exception {
        ServiceInfo service = geoServer.getFactory().createService();
        service.setName( "foo" );
        
        try {
            geoServer.add( service );
            fail( "adding without id should throw exception" );
        }
        catch( Exception e ) {};
        
        ((ServiceInfoImpl)service).setId( "id" );
        geoServer.add( service );
        
        ServiceInfo s2 = geoServer.getFactory().createService();
        ((ServiceInfoImpl)s2).setId( "id" );
        
        try {
            geoServer.add( s2 );
            fail( "adding service with duplicate id should throw exception" );
        }
        catch( Exception e ) {}
        
        ServiceInfo s = geoServer.getServiceByName( "foo", ServiceInfo.class );
        assertTrue( s != service );
        assertEquals( service, s );
    }
    
    public void testModifyService() throws Exception {
        ServiceInfo service = geoServer.getFactory().createService();
        ((ServiceInfoImpl)service).setId( "id" );
        service.setName( "foo" );
        service.setTitle( "bar" );
        
        geoServer.add( service );
        
        ServiceInfo s1 = geoServer.getServiceByName( "foo", ServiceInfo.class );
        s1.setTitle( "changed" );
        
        ServiceInfo s2 = geoServer.getServiceByName( "foo", ServiceInfo.class );
        assertEquals( "bar", s2.getTitle() );
        
        geoServer.save( s1 );
        assertEquals( "changed", s2.getTitle() );
    }
    
    public void testGlobalEvents() throws Exception {
        
        TestListener tl = new TestListener();
        geoServer.addListener( tl );
        
        GeoServerInfo global = geoServer.getFactory().createGlobal();
        geoServer.setGlobal( global );
       
        global = geoServer.getGlobal();
        global.setAdminPassword( "foo" );
        global.setOnlineResource( "bar" );
        
        assertEquals( 0, tl.gPropertyNames.size() );
        geoServer.save( global );
        
        assertEquals( 2, tl.gPropertyNames.size() );
        assertTrue( tl.gPropertyNames.contains( "adminPassword" ) );
        assertTrue( tl.gPropertyNames.contains( "onlineResource" ) );
    }
    
    static class TestListener extends ConfigurationListenerAdapter {

        List<String> gPropertyNames = new ArrayList();
        List<Object> gOldValues = new ArrayList();
        List<Object> gNewValues = new ArrayList();
        
        List<String> sPropertyNames = new ArrayList();
        List<Object> sOldValues = new ArrayList();
        List<Object> sNewValues = new ArrayList();
        
        public void handleGlobalChange(GeoServerInfo global,
                List<String> propertyNames, List<Object> oldValues,
                List<Object> newValues) {
            gPropertyNames.addAll( propertyNames );
            gOldValues.addAll( oldValues );
            gNewValues.addAll( newValues );
        }

        public void handleServiceChange(ServiceInfo service,
                List<String> propertyNames, List<Object> oldValues,
                List<Object> newValues) {
            
            sPropertyNames.addAll( propertyNames );
            sOldValues.addAll( oldValues );
            sNewValues.addAll( newValues );
        }
    }
}
