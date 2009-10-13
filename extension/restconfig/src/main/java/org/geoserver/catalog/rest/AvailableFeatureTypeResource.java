/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.ReflectiveXMLFormat;
import org.geotools.data.DataStore;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

import com.thoughtworks.xstream.XStream;

public class AvailableFeatureTypeResource extends AbstractCatalogResource {

    public AvailableFeatureTypeResource(Context context, Request request, Response response, Catalog catalog) {
        super(context, request, response, FeatureTypeInfo.class, catalog);
    }

    @Override
    protected Object handleObjectGet() {
        String workspace = (String) getRequest().getAttributes().get( "workspace" );
        String datastore = (String) getRequest().getAttributes().get( "datastore" );
        
        DataStoreInfo info = catalog.getDataStoreByName( workspace, datastore );
        if ( info == null ) {
            throw new RestletException( "No such datastore: " + datastore, Status.CLIENT_ERROR_NOT_FOUND );
        }
        
        //list of available feature types
        List<String> available = new ArrayList<String>();
        try {
            DataStore ds = (DataStore) info.getDataStore(null);
            
            String[] featureTypeNames = ds.getTypeNames(); 
            for ( String featureTypeName : featureTypeNames ) {
                FeatureTypeInfo ftinfo = catalog.getFeatureTypeByDataStore(info, featureTypeName);
                if (ftinfo == null ) {
                    //not in catalog, add it
                    available.add( featureTypeName );
                }
            }
        } 
        catch (IOException e) {
            throw new RestletException( "Could not load datastore: " + datastore, Status.SERVER_ERROR_INTERNAL, e );
        }
        
        return available;
    }
    
    
    @Override
    protected ReflectiveXMLFormat createXMLFormat(Request request, Response response) {
        return new ReflectiveXMLFormat() {
          
            @Override
            protected void write(Object data, OutputStream output)
                    throws IOException {
                XStream xstream = new XStream();
                xstream.alias( "featureTypeName", String.class);
                xstream.toXML( data, output );
            }
        };
    }
    
    @Override
    public boolean allowPost() {
        return false;
    }
    
    @Override
    protected String handleObjectPost(Object object) {
        return null;
    }

    @Override
    protected void handleObjectPut(Object object) {
        //do nothing, we do not allow post
    }
}
