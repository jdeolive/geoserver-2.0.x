/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class CoverageResource extends AbstractCatalogResource {

    public CoverageResource(Context context, Request request,
            Response response, Catalog catalog) {
        super(context, request, response, CoverageInfo.class, catalog);
        
    }

    @Override
    protected DataFormat createHTMLFormat(Request request, Response response) {
        return new ResourceHTMLFormat(CoverageInfo.class, request, response, this);
    }
    
    @Override
    protected Object handleObjectGet() throws Exception {
        String workspace = getAttribute( "workspace");
        String coveragestore = getAttribute( "coveragestore");
        String coverage = getAttribute( "coverage" );

        if ( coveragestore == null ) {
            LOGGER.fine( "GET coverage " + workspace + "," + coverage );
            //grab the corresponding namespace for this workspace
            NamespaceInfo ns = catalog.getNamespaceByPrefix( workspace );
            if ( ns != null ) {
                return catalog.getCoverageByName(ns,coverage);
            }

            throw new RestletException( "", Status.CLIENT_ERROR_NOT_FOUND );
        }

        LOGGER.fine( "GET coverage " + coveragestore + "," + coverage );
        CoverageStoreInfo cs = catalog.getCoverageStoreByName(workspace, coveragestore);
        return catalog.getCoverageByCoverageStore( cs, coverage );
    }

    @Override
    public boolean allowPost() {
        return getAttribute("coverage") == null;
    }
    
    @Override
    protected String handleObjectPost(Object object) throws Exception {
        String workspace = getAttribute( "workspace");
        String coveragestore = getAttribute( "coveragestore");

        CoverageInfo coverage = (CoverageInfo) object;
        if ( coverage.getStore() == null ) {
            //get from requests
            CoverageStoreInfo ds = catalog.getCoverageStoreByName( workspace, coveragestore );
            coverage.setStore( ds );
        }
        
        NamespaceInfo ns = coverage.getNamespace();
        if ( ns != null && !ns.getPrefix().equals( workspace ) ) {
            //TODO: change this once the two can be different and we untie namespace
            // from workspace
            LOGGER.warning( "Namespace: " + ns.getPrefix() + " does not match workspace: " + workspace + ", overriding." );
            ns = null;
        }
        
        if ( ns == null){
            //infer from workspace
            ns = catalog.getNamespaceByPrefix( workspace );
            coverage.setNamespace( ns );
        }
        
        coverage.setEnabled(true);
        catalog.add( coverage );
        
        //create a layer for the coverage
        catalog.add(new CatalogBuilder(catalog).buildLayer(coverage));
        
        LOGGER.info( "POST coverage " + coveragestore + "," + coverage.getName() );
        return coverage.getName();
    }

    @Override
    public boolean allowPut() {
        return getAttribute("coverage") != null;
    }
    
    @Override
    protected void handleObjectPut(Object object) throws Exception {
        CoverageInfo c = (CoverageInfo) object;
        
        String workspace = getAttribute("workspace");
        String coveragestore = getAttribute("coveragestore");
        String coverage = getAttribute("coverage");
        
        CoverageStoreInfo cs = catalog.getCoverageStoreByName(workspace, coveragestore);
        CoverageInfo original = catalog.getCoverageByCoverageStore( cs,  coverage );
        new CatalogBuilder(catalog).updateCoverage(original,c);
        catalog.save( original );
        
        LOGGER.info( "PUT coverage " + coveragestore + "," + coverage );
    }
    
    @Override
    public boolean allowDelete() {
        return getAttribute("coverage") != null;
    }
    
    @Override
    protected void handleObjectDelete() throws Exception {
        String workspace = getAttribute("workspace");
        String coveragestore = getAttribute("coveragestore");
        String coverage = getAttribute("coverage");
        
        CoverageStoreInfo ds = catalog.getCoverageStoreByName(workspace, coveragestore);
        CoverageInfo c = catalog.getCoverageByCoverageStore( ds,  coverage );
        catalog.remove( c );

        LOGGER.info( "DELETE coverage " + coveragestore + "," + coverage );
    }
    
    @Override
    protected void configurePersister(XStreamPersister persister, DataFormat format) {
        persister.setCallback( new XStreamPersister.Callback() {
            @Override
            protected void postEncodeReference(Object obj, String ref,
                    HierarchicalStreamWriter writer, MarshallingContext context) {
                if ( obj instanceof NamespaceInfo ) {
                    NamespaceInfo ns = (NamespaceInfo) obj;
                    encodeLink( "/namespaces/" + ns.getPrefix(), writer);
                }
                if ( obj instanceof CoverageStoreInfo ) {
                    CoverageStoreInfo cs = (CoverageStoreInfo) obj;
                    encodeLink( "/workspaces/" + cs.getWorkspace().getName() + "/coveragestores/" + 
                            cs.getName(), writer );
                    
                }
            }
        });
    }

}
