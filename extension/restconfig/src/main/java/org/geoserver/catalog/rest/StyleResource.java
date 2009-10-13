/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.MediaTypes;
import org.geotools.styling.Style;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

public class StyleResource extends AbstractCatalogResource {

    /**
     * media type for SLD
     */
    public static final MediaType MEDIATYPE_SLD = new MediaType( "application/vnd.ogc.sld+xml" );
    static {
        MediaTypes.registerExtension( "sld", MEDIATYPE_SLD );
    }
    
    public StyleResource(Context context, Request request, Response response, Catalog catalog) {
        super(context, request, response, StyleInfo.class, catalog);
        
    }
    
    @Override
    protected List<DataFormat> createSupportedFormats(Request request,Response response) {
        List<DataFormat> formats =  super.createSupportedFormats(request,response);
        formats.add( new SLDFormat() );
        return formats;
    }
    
    @Override
    protected Object handleObjectGet() {
        String style = getAttribute("style");
        
        LOGGER.fine( "GET style " + style );
        StyleInfo sinfo = catalog.getStyleByName( style );
        
        //check the format, if specified as sld, return the sld itself
        DataFormat format = getFormatGet();
        if ( format instanceof SLDFormat ) {
            try {
                return sinfo.getStyle();
            } 
            catch (IOException e) {
                throw new RestletException( "", Status.SERVER_ERROR_INTERNAL, e );
            }
        }
        
        return sinfo;
    }

    @Override
    public boolean allowPost() {
        return getAttribute("style") == null;
    }
    
    @Override
    protected String handleObjectPost(Object object) throws Exception {
        String layer = getAttribute( "layer" );
        
        if ( object instanceof StyleInfo ) {
            StyleInfo style = (StyleInfo) object;
            
            if ( layer != null ) {
                StyleInfo existing = catalog.getStyleByName( style.getName() );
                if ( existing == null ) {
                    //TODO: add a new style to catalog
                    throw new RestletException( "No such style: " + style.getName(), Status.CLIENT_ERROR_NOT_FOUND );
                }
                
                LayerInfo l = catalog.getLayerByName( layer );
                l.getStyles().add( existing );
                
                //check for default
                String def = getRequest().getResourceRef().getQueryAsForm().getFirstValue("default");
                if ( "true".equals( def ) ) {
                    l.setDefaultStyle( existing );
                }
                catalog.save(l);
                LOGGER.info( "POST style " + style.getName() + " to layer " + layer);
            }
            else {
                catalog.add( style  );
                LOGGER.info( "POST style " + style.getName() );
            }

            return style.getName();
        }
        else if ( object instanceof Style ) {
            Style style = (Style) object;
            
            //figure out the name of the new style, first check if specified directly
            String name = getRequest().getResourceRef().getQueryAsForm().getFirstValue( "name");
            
            if ( name == null ) {
                //infer name from sld
                name = style.getName();
            }
            
            if ( name == null ) {
                throw new RestletException( "Style must have a name.", Status.CLIENT_ERROR_BAD_REQUEST );
            }
            
            //ensure that the style does not already exist
            if ( catalog.getStyleByName( name ) != null ) {
                throw new RestletException( "Style " + name + " already exists.", Status.CLIENT_ERROR_FORBIDDEN  );
            }
            
            //serialize the style out into the data directory
            GeoServerResourceLoader loader = catalog.getResourceLoader();
            File f;
            try {
                f = loader.find( "styles/" +  name + ".sld" );
            } 
            catch (IOException e) {
                throw new RestletException( "Error looking up file", Status.SERVER_ERROR_INTERNAL, e );
            }
            
            if ( f != null ) {
                String msg = "SLD file " + name + ".sld already exists."; 
                throw new RestletException( msg, Status.CLIENT_ERROR_FORBIDDEN);
            }
            
            //TODO: have the writing out of the style delegate to ResourcePool.writeStyle()
            try {
                f = loader.createFile( "styles/" + name + ".sld") ;
                
                //serialize the file to the styles directory
                BufferedOutputStream out = new BufferedOutputStream( new FileOutputStream ( f ) );
                
                SLDFormat format = new SLDFormat(true);
                format.toRepresentation(style).write(out);
                
                out.flush();
                out.close();
            } 
            catch (IOException e) {
                throw new RestletException( "Error creating file", Status.SERVER_ERROR_INTERNAL, e );
            }
            
            //create a style info object
            StyleInfo sinfo = catalog.getFactory().createStyle();
            sinfo.setName( name );
            sinfo.setFilename( f.getName() );
            catalog.add( sinfo );
            
            LOGGER.info( "POST SLD " + name);
            return name;
        }
        
        return null;
    }

    @Override
    public boolean allowPut() {
        return getAttribute("style") != null;
    }
    
    @Override
    protected void handleObjectPut(Object object) throws Exception {
        String style = getAttribute("style");
        
        if ( object instanceof StyleInfo ) {
            StyleInfo s = (StyleInfo) object;
            StyleInfo original = catalog.getStyleByName( style );
     
            new CatalogBuilder( catalog ).updateStyle( original, s );
            catalog.save( original );
        }
        else if ( object instanceof Style ) {
            StyleInfo s = catalog.getStyleByName( style );
            catalog.getResourcePool().writeStyle( s, (Style) object, true );
        }
        
        LOGGER.info( "PUT style " + style);
    }

    @Override
    public boolean allowDelete() {
        return getAttribute( "style" ) != null;
    }
    
    @Override
    protected void handleObjectDelete() throws Exception {
        String style = getAttribute("style");
        StyleInfo s = catalog.getStyleByName(style);
        
        //ensure that no layers reference the style
        List<LayerInfo> layers = catalog.getLayers(s);
        if ( !layers.isEmpty() ) {
            throw new RestletException( "Can't delete style referenced by existing layers.", Status.CLIENT_ERROR_FORBIDDEN );
        }
        
        catalog.remove( s );
        
        LOGGER.info( "DELETE style " + style);
       
    }
}
