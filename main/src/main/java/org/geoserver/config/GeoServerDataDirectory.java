/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.platform.GeoServerResourceLoader;

/**
 * Abstracts access to the geoserver data directory.
 * <p>
 * Example usage:
 * <pre>
 *   GeoServerDataDirectory dd = new GeoServerDataDirectory(resourceLoader);
 * 
 *   //find some data
 *   File shp = dd.findDataFile( "shapefiles/somedata.shp" );
 *   
 *   //create a directory for some data
 *   File shapefiles = dd.findOrCreateDataDirectory("shapefiles");
 *   
 *   //find a template file for a feature type
 *   FeatureTypeInfo ftinfo = ...;
 *   File template = dd.findSuppResourceFile(ftinfo,"title.ftl");
 *   
 * </pre>
 * </p>
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class GeoServerDataDirectory {

    /**
     * resource loader
     */
    GeoServerResourceLoader resourceLoader;
    
    /**
     * Creates the data directory specifying the resource loader.
     */
    public GeoServerDataDirectory( GeoServerResourceLoader resourceLoader ) {
        this.resourceLoader = resourceLoader;
    }
    
    /**
     * Creates the data directory specifying the base directory.
     */
    public GeoServerDataDirectory( File baseDirectory ) {
        this( new GeoServerResourceLoader( baseDirectory ) );
    }
    
    /**
     * The root of the data directory.
     */
    public File root() {
        return resourceLoader.getBaseDirectory();
    }
    
    /**
     * Returns the root of the directory which contains spatial data files, if the directory does
     * exist, null is returned.
     * <p>
     * This directory is called 'data', and is located directly under {@link #root()}
     * </p>
     */
    public File findDataRoot() throws IOException {
        return dataRoot(false);
    }
    
    /**
     * Returns the root of the directory which contains spatial data
     * files, if the directory does not exist it will be created.
     * <p>
     * This directory is called 'data', and is located directly under {@link #root()}
     * </p>
     */
    public File findOrCreateDataRoot() throws IOException {
        return dataRoot(true);
    }
    
    File dataRoot(boolean create) throws IOException {
        return create ? resourceLoader.findOrCreateDirectory( "data" ) 
            : resourceLoader.find( "data");
    }
    
    /**
     * Returns a directory under the {@link #dataRoot()} directory, if the directory does not exist
     * null will be returned.
     */
    public File findDataDir( String... location ) throws IOException {
        return dataDir( false, location );
    }
    
    /**
     * Returns a directory under the {@link #dataRoot()} directory, if the directory does not exist 
     * it will be created.
     */
    public File findOrCreateDataDir( String... location ) throws IOException {
        return dataDir(true, location);
    }
    
    protected File dataDir( boolean create, String... location ) throws IOException {
        return create ? resourceLoader.findOrCreateDirectory(dataRoot(create), location) 
            : resourceLoader.find( dataRoot(create), location );
    }
    
    /**
     * Returns a file under the {@link #dataRoot()} directory, if the file does not exist null is 
     * returned.
     */
    public File findDataFile( String... location ) throws IOException {
        return dataFile(false,location);
    }
    
    /**
     * Returns a file under the {@link #dataRoot()} directory, if the file does not exist it a file
     * object will still be returned.
     */
    public File findOrResolveDataFile( String... location ) throws IOException {
        return dataFile(true,location);
    }
    
    File dataFile( boolean create, String... location ) throws IOException {
        return create ? resourceLoader.createFile(dataRoot(create), location) 
            : resourceLoader.find( dataRoot(create), location );
    }
    
    /**
     * Returns the root of the directory which contains security configuration files, if the 
     * directory does exist, null is returned.
     * <p>
     * This directory is called 'security', and is located directly under {@link #root()}
     * </p>
     */
    public File findSecurityRoot() throws IOException {
        return securityRoot(false);
    }
    
    /**
     * Returns the root of the directory which contains security configuration files, if the 
     * directory does exist it is created.
     * <p>
     * This directory is called 'security', and is located directly under {@link #root()}
     * </p>
     */
    public File findOrCreateSecurityRoot() throws IOException {
        return securityRoot(true);
    }
    
    File securityRoot(boolean create) throws IOException {
        return create ? resourceLoader.findOrCreateDirectory( "security" ) 
                : resourceLoader.find( "security");
    }
    
    /**
     * Copies a file into a security configuration directory.
     * <p>
     * If the security configuration directory does exist it will be created.
     * </p>
     */
    public void copyToSecurityDir( File f ) throws IOException {
        FileUtils.copyFileToDirectory( f, securityRoot( true ) );
    }

    /**
     * Copies data into a security configuration directory.
     * <p>
     * If the security configuration directory does exist it will be created
     * </p>
     */
    public void copyToSecurityDir( InputStream data, String filename ) 
        throws IOException {
        copy( data, securityRoot( true ), filename );
    }
    
    /**
     * Returns the directory for the specified workspace, if the directory does not exist null is
     * returned.
     */
    public File findWorkspaceDir( WorkspaceInfo ws ) throws IOException {
        return workspaceDir(false,ws); 
    }
    
    /**
     * Returns the directory for the specified workspace, if the directory does not exist it will be
     * created.
     * 
     * @param create If set to true the directory will be created when it does not exist.
     */
    public File findOrCreateWorkspaceDir( WorkspaceInfo ws ) throws IOException {
        return workspaceDir(true,ws); 
    }
    
    File workspaceDir( boolean create, WorkspaceInfo ws ) throws IOException {
        File workspaces = create ? resourceLoader.findOrCreateDirectory( "workspaces" ) 
           : resourceLoader.find( "workspaces" );
        if ( workspaces != null ) {
            return dir(new File( workspaces, ws.getName() ), create);
        }
        return null;
    }
    
    /**
     * Returns the configuration file for the specified workspace, if the file does not exist null 
     * is returned.
     */
    public File findWorkspaceFile( WorkspaceInfo ws ) throws IOException {
        return workspaceFile(false,ws);
    }
    
    /**
     * Returns the configuration file for the specified workspace, if the file does nost exist a 
     * file object will still be returned.
     * 
     */
    public File findOrResolveWorkspaceFile( WorkspaceInfo ws ) throws IOException {
        return workspaceFile(true,ws);
    }
    
    File workspaceFile( boolean create, WorkspaceInfo ws ) throws IOException {
        File wsdir = workspaceDir(create, ws);
        return wsdir != null ? file(new File( wsdir, "workspace.xml" ), create) : null;
    }
    
    /**
     * Returns a supplementary configuration file for a workspace, if the file does not exist null
     * is returned.
     */
    public File findSuppWorkspaceFile( WorkspaceInfo ws, String filename ) throws IOException {
        File wsdir = findWorkspaceDir( ws );
        return wsdir != null ? file(new File( wsdir, filename ), false) : null;
    }
    
    /**
     * Copies a file into a workspace configuration directory.
     * <p>
     * If the workspace configuration directory does exist it will be created.
     * </p>
     */
    public void copyToWorkspaceDir( WorkspaceInfo ws, File f ) throws IOException {
        FileUtils.copyFileToDirectory( f, workspaceDir( true, ws ) );
    }

    /**
     * Copies data into a workspace configuration directory.
     * <p>
     * If the workspace configuration directory does exist it will be created
     * </p>
     */
    public void copyToWorkspaceDir( WorkspaceInfo ws, InputStream data, String filename ) 
        throws IOException {
        copy( data, workspaceDir( true, ws ), filename );
    }
    
    /**
     * Returns the directory in which a stores configuration is persisted, if the directory does 
     * not exists null is returned.
     */
    public File findStoreDir( StoreInfo s ) throws IOException {
        return storeDir( false, s );
    }
    
    /**
     * Returns the directory in which a stores configuration is persisted, if the directory does 
     * not exist it is created.
     */
    public File findOrCreateStoreDir( StoreInfo s ) throws IOException {
        return storeDir( true, s );
    }
    
    File storeDir( boolean create, StoreInfo s ) throws IOException {
        File wsdir = workspaceDir(create,s.getWorkspace());
        return wsdir != null ? dir(new File( wsdir, s.getName() ),create) : null ;   
    }
    
    /**
     * Returns the configuration file for the specified store, if the file does not exist null is 
     * returned.
     */
    public File findStoreFile( StoreInfo s ) throws IOException {
        return storeFile(false,s);
    }
    
    /**
     * Returns the configuration file for the specified store, if the file does not exist a file 
     * object is still returned.
     */
    public File findOrResolveStoreFile( StoreInfo s ) throws IOException {
        return storeFile(true,s);
    }
    
    File storeFile( boolean create, StoreInfo s ) throws IOException {
        File sdir = storeDir(create, s);
        if ( sdir == null ) {
            return null;
        }
        
        if ( s instanceof DataStoreInfo ) {
            return file(new File( sdir, "datastore.xml"), create);
        }
        else if ( s instanceof CoverageStoreInfo ) {
            return file(new File( sdir, "coveragestore.xml"), create);
        }
        return null;
    }
    
    /**
     * Returns a supplementary configuration file for a store, if the file does not exist null is 
     * returned.
     */
    public File findSuppStoreFile( StoreInfo ws, String filename ) throws IOException {
        File sdir = findStoreDir( ws );
        return sdir != null ? file(new File( sdir, filename ), false) : null;
    }
    
    /**
     * Copies a file into a store configuration directory.
     * <p>
     * If the store configuration directory does exist it will be created
     * </p>
     */
    public void copyToStoreDir( StoreInfo s, File f ) throws IOException {
        FileUtils.copyFileToDirectory( f, storeDir( true, s ) );
    }

    /**
     * Copies data into a store configuration directory.
     * <p>
     * If the store configuration directory does exist it will be created
     * </p>
     */
    public void copyToStoreDir( StoreInfo s, InputStream data, String filename ) 
        throws IOException {
        copy( data, storeDir( true, s ), filename );
    }
    

    /**
     * Returns the directory in which a resources configuration is persisted, if the directory does
     * not exist null is returned.
     */
    public File findResourceDir( ResourceInfo r ) throws IOException {
        return resourceDir(false,r);
    }
    
    /**
     * Returns the directory in which a resources configuration is persisted, if the directory does
     * not exist it will be created.
     */
    public File findOrCreateResourceDir( ResourceInfo r ) throws IOException {
        return resourceDir(true,r);
    }
    
    File resourceDir( boolean create, ResourceInfo r ) throws IOException {
        File sdir = storeDir(create, r.getStore());
        return sdir != null ? dir(new File( sdir, r.getName() ), create) : null;
    }
    
    /**
     * Returns the configuration file for the specified resource, if the file does not exist null is
     * returned.
     */
    public File findResourceFile( ResourceInfo r ) throws IOException {
        return resourceFile(false,r);
    }
    
    /**
     * Returns the configuration file for the specified resource, if the file does not exist a file
     * object is still returned.
     * 
     */
    public File findOrResolveResourceFile( ResourceInfo r ) throws IOException {
        return resourceFile(true,r);
    }
    
    File resourceFile( boolean create, ResourceInfo r ) throws IOException {
        File rdir = resourceDir( create, r );
        if ( rdir == null ) {
            return null;
        }
        
        if ( r instanceof FeatureTypeInfo ) {
            return file(new File( rdir, "featuretype.xml"), create);
        }
        else if ( r instanceof CoverageInfo ) {
            return file(new File( rdir, "coverage"), create);
        }
        
        return null;
    }
   
    /**
     * Returns a supplementary configuration file for a resource, if the file does not exist null
     * is returned.
     */
    public File findSuppResourceFile( ResourceInfo r, String filename ) throws IOException {
        File rdir = findResourceDir( r );
        return rdir != null ? file(new File( rdir, filename ), false) : null;
    }
    
    /**
     * Copies a file into a feature type configuration directory.
     * <p>
     * If the resource directory does exist it will be created
     * </p>
     */
    public void copyToResourceDir( ResourceInfo r, File f ) throws IOException {
        FileUtils.copyFileToDirectory( f, resourceDir( true, r ) );
    }

    /**
     * Copies data into a feature type configuration directory.
     * <p>
     * If the resource directory does exist it will be created
     * </p>
     */
    public void copyToResourceDir( ResourceInfo r, InputStream data, String filename ) 
        throws IOException {
        copy( data, resourceDir( true, r ), filename );
    }
    
    /**
     * Returns the configuration file for the specified namespace, if the file does not exist null
     * is returned.
     */
    public File findNamespaceFile( WorkspaceInfo ws ) throws IOException {
        return workspaceDir(false,ws);
    }
    
    /**
     * Returns the configuration file for the specified namespace, if the file does not exist a file
     * object is still returned.
     */
    public File findOrResolveNamespaceFile( WorkspaceInfo ws ) throws IOException {
        return workspaceDir(true,ws);
    }
    
    File namespaceFile( boolean create, WorkspaceInfo ws ) throws IOException {
        File wsdir = workspaceDir(create, ws);
        return wsdir != null ? file(new File( wsdir, "namespace.xml"), create) : null;
    }
    
    /**
     * Returns the configuration file for the specified layer, if the file does not exist null is 
     * returned.
     */
    public File findLayerFile( LayerInfo l ) throws IOException {
        return layerFile(false,l);
    }
    
    /**
     * Returns the configuration file for the specified layer, if the file does not exist a file
     * object is still returned.
     * 
     */
    public File findOrResolveLayerFile( LayerInfo l ) throws IOException {
        return layerFile(true,l);
    }
    
    File layerFile( boolean create, LayerInfo l ) throws IOException {
        File rdir = resourceDir(create, l.getResource());
        return rdir != null ? file(new File( rdir, "layer.xml"), create) : null;
    }
    
    /**
     * Returns the directory in which styles are persisted, if the directory does not exist null
     * is returned.
     */
    public File findStyleDir() throws IOException {
        return styleDir(false);
    }
    
    
    /**
     * Returns the directory in which styles are persisted, if the directory does not exist it will
     * be created.
     */
    public File findOrCreateStyleDir() throws IOException {
        return styleDir(true);
    }
    
    File styleDir(boolean create) throws IOException {
        return create ? resourceLoader.findOrCreateDirectory( "styles") : 
            resourceLoader.find( "styles" );
    }
    
    /**
     * Returns the configuration file for the specified style, if the file does not exist null is
     * returned.
     */
    public File findStyleFile( StyleInfo s ) throws IOException {
        return styleFile(false,s);
    }
    
    /**
     * Returns the configuration file for the specified style, if the file does not exist a file 
     * object is still returned.
     */
    public File findOrCreateStyleFile( StyleInfo s ) throws IOException {
        return styleFile(true,s);
    }
    
    File styleFile( boolean create, StyleInfo s ) throws IOException {
        File sdir = styleDir(create);
        return sdir != null ? file(new File( sdir, s.getName()+".xml" ),create) : null;
    }
    
    /**
     * Copies a file into the style configuration directory.
     * <p>
     * If the resource directory does exist it will be created
     * </p>
     */
    public void copyToStyleDir( File f ) throws IOException {
        FileUtils.copyFileToDirectory( f, styleDir( true ) );
    }
    
    /**
     * Copies data into the style directory.
     * <p>
     * If the style directory does exist it will be created
     * </p>
     */
    public void copyToStyleDir( InputStream data, String filename ) throws IOException {
        copy( data, styleDir(true), filename );
    }
    
    //
    // Helper methods
    //
    void copy( InputStream data, File targetDir, String filename ) throws IOException {
        BufferedOutputStream out = 
            new BufferedOutputStream( new FileOutputStream( new File( targetDir, filename ) ) );
        IOUtils.copy( data, out );
        out.flush();
        out.close();
    }
    
    File file( File f ) {
        return file(f,true);
    }
    
    File file( File f, boolean create) {
        if ( create ) {
            return f;
        }
        
        return f.exists() ? f : null;
    }
    
    File dir( File d, boolean create ) {
        if ( create ) {
            d.mkdirs();
            return d;
        }
        
        return d.exists() ? d : null;
    }

}
