package org.geoserver.h2;

import org.geoserver.data.DataStoreFactoryInitializer;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.data.h2.H2DataStoreFactory;

/**
 * Initializes an H2 data store factory setting its location to the geoserver
 *  data directory.
 *
 * @author Justin Deoliveira, The Open Planning Project
 *
 */
public class H2DataStoreFactoryInitializer extends 
    DataStoreFactoryInitializer<H2DataStoreFactory> {

    GeoServerResourceLoader resourceLoader;
    
    public H2DataStoreFactoryInitializer() {
        super( H2DataStoreFactory.class );
    }
    
    public void setResourceLoader(GeoServerResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
    
    public void initialize(H2DataStoreFactory factory) {
        factory.setBaseDirectory( resourceLoader.getBaseDirectory() );
    }
}
