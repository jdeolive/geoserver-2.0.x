/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.util.HashMap;

import org.geoserver.catalog.Catalog;
import org.geoserver.data.util.CoverageStoreUtils;
import org.geoserver.rest.RestletException;
import org.geotools.data.DataStoreFactorySpi;
import org.opengis.coverage.grid.Format;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

public class StoreFileFinder extends AbstractCatalogFinder {

    protected static HashMap<String,String> formatToDataStoreFactory = new HashMap();
    static {
        formatToDataStoreFactory.put( "shp", "org.geotools.data.shapefile.ShapefileDataStoreFactory");
        formatToDataStoreFactory.put( "properties", "org.geotools.data.property.PropertyDataStoreFactory");
    }
    
    protected static HashMap<String,String> formatToCoverageStoreFormat = new HashMap();
    static {
        for (Format format : CoverageStoreUtils.formats) {
            formatToCoverageStoreFormat.put(format.getName().toLowerCase(), format.getName());
        }
    }
    
    public StoreFileFinder(Catalog catalog) {
        super(catalog);
    }

    @Override
    public Resource findTarget(Request request, Response response) {
        
        //figure out what kind of store this maps to
        String format = (String) request.getAttributes().get( "format" );
        String datastore = (String) request.getAttributes().get( "datastore" );
        String coveragestore = (String) request.getAttributes().get( "coveragestore" );
        
        if ( datastore != null ) {
            String factoryClassName = formatToDataStoreFactory.get( format );
            
            if ( factoryClassName == null ) {
                throw new RestletException( "Unsupported format: " + format, Status.CLIENT_ERROR_BAD_REQUEST );
            }
            
            DataStoreFactorySpi factory;
            try {
                Class factoryClass = Class.forName( factoryClassName );
                factory = (DataStoreFactorySpi) factoryClass.newInstance();
            }
            catch ( Exception e ) {
                throw new RestletException( "Datastore format unavailable: " + factoryClassName, Status.SERVER_ERROR_INTERNAL );
            }
            
            return new DataStoreFileResource(request,response,factory,catalog);
        }
        else {
            String coverageFormatName = formatToCoverageStoreFormat.get( format );
            
            if ( coverageFormatName == null ) {
                throw new RestletException( "Unsupported format: " + format, Status.CLIENT_ERROR_BAD_REQUEST );
            }
            
            Format coverageFormat = null;
            try {
                coverageFormat = CoverageStoreUtils.acquireFormat( coverageFormatName );
            }
            catch( Exception e ) {
                throw new RestletException( "Coveragestore format unavailable: " + coverageFormatName, Status.SERVER_ERROR_INTERNAL );
            }
            
            return new CoverageStoreFileResource(request,response,coverageFormat,catalog);
        }
        
    }
}
