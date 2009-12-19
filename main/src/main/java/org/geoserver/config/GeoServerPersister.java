/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.util.logging.Logging;

public class GeoServerPersister implements CatalogListener, ConfigurationListener {

    /**
     * logging instance
     */
    static Logger LOGGER = Logging.getLogger( "org.geoserver.config");
     
    GeoServerResourceLoader rl;
    XStreamPersister xp;
    
    public GeoServerPersister(GeoServerResourceLoader rl, XStreamPersister xp) {
        this.rl = rl;
        this.xp = xp;
    }
    
    public void handleAddEvent(CatalogAddEvent event) {
        Object source = event.getSource();
        try {
            if ( source instanceof WorkspaceInfo ) {
                addWorkspace( (WorkspaceInfo) source );
            }
            else if ( source instanceof NamespaceInfo ) {
                addNamespace( (NamespaceInfo) source );
            }
            else if ( source instanceof DataStoreInfo ) {
                addDataStore( (DataStoreInfo) source );
            }
            else if ( source instanceof FeatureTypeInfo ) {
                addFeatureType( (FeatureTypeInfo) source );
            }
            else if ( source instanceof CoverageStoreInfo ) {
                addCoverageStore( (CoverageStoreInfo) source );
            }
            else if ( source instanceof CoverageInfo ) {
                addCoverage( (CoverageInfo) source );
            }
            else if ( source instanceof LayerInfo ) {
                addLayer( (LayerInfo) source );
            }
            else if ( source instanceof StyleInfo ) {
                addStyle( (StyleInfo) source );
            }
            else if ( source instanceof LayerGroupInfo ) {
                addLayerGroup( (LayerGroupInfo) source );
            }
        }
        catch( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    public void handleModifyEvent(CatalogModifyEvent event) {
        Object source = event.getSource();
        
        try {
            //here we handle name changes
            int i = event.getPropertyNames().indexOf( "name" );
            if ( i > -1 ) {
                String newName = (String) event.getNewValues().get( i );
                
                if ( source instanceof WorkspaceInfo ) {
                    renameWorkspace( (WorkspaceInfo) source, newName );
                }
                else if ( source instanceof StoreInfo ) {
                    renameStore( (StoreInfo) source, newName );
                }
                else if ( source instanceof ResourceInfo ) {
                    renameResource( (ResourceInfo) source, newName );
                }
                else if ( source instanceof StyleInfo ) {
                    renameStyle( (StyleInfo) source, newName );
                }
                else if ( source instanceof LayerGroupInfo ) {
                    renameLayerGroup( (LayerGroupInfo) source, newName );
                }
            }
            
            //handle the case of a store changing workspace
            if ( source instanceof StoreInfo ) {
                i = event.getPropertyNames().indexOf( "workspace");
                if ( i > -1 ) {
                    WorkspaceInfo newWorkspace = (WorkspaceInfo) event.getNewValues().get( i );
                    File oldDir = dir( (StoreInfo) source );
                    oldDir.renameTo( new File( dir( newWorkspace ), oldDir.getName() ) );
                }
            }
            
            //handle default workspace
            if ( source instanceof Catalog ) {
                i = event.getPropertyNames().indexOf("defaultWorkspace");
                if ( i > -1 ) {
                    WorkspaceInfo defWorkspace = (WorkspaceInfo) event.getNewValues().get( i );
                    File d = rl.createDirectory( "workspaces");
                    persist(defWorkspace, new File(d, "default.xml"));
                }
            }
            
        } 
        catch (IOException e) {
            throw new RuntimeException( e );
        }
    }
    
    public void handlePostModifyEvent(CatalogPostModifyEvent event) {
        Object source = event.getSource();
        try {
            if ( source instanceof WorkspaceInfo ) {
                modifyWorkspace( (WorkspaceInfo) source);
            }
            else if ( source instanceof DataStoreInfo ) {
                modifyDataStore( (DataStoreInfo) source );
            }
            else if ( source instanceof NamespaceInfo ) {
                modifyNamespace( (NamespaceInfo) source );
            }
            else if ( source instanceof FeatureTypeInfo ) {
                modifyFeatureType( (FeatureTypeInfo) source );
            }
            else if ( source instanceof CoverageStoreInfo ) {
                modifyCoverageStore( (CoverageStoreInfo) source );
            }
            else if ( source instanceof CoverageInfo ) {
                modifyCoverage( (CoverageInfo) source );
            }
            else if ( source instanceof LayerInfo ) {
                modifyLayer( (LayerInfo) source );
            }
            else if ( source instanceof StyleInfo ) {
                modifyStyle( (StyleInfo) source );
            }
            else if ( source instanceof LayerGroupInfo ) {
                modifyLayerGroup( (LayerGroupInfo) source );
            }
        }
        catch( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    public void handleRemoveEvent(CatalogRemoveEvent event) {
        Object source = event.getSource();
        try {
            if ( source instanceof WorkspaceInfo ) {
                removeWorkspace( (WorkspaceInfo) source );
            }
            else if ( source instanceof NamespaceInfo ) {
                removeNamespace( (NamespaceInfo) source );
            }
            else if ( source instanceof DataStoreInfo ) {
                removeDataStore( (DataStoreInfo) source );
            }
            else if ( source instanceof FeatureTypeInfo ) {
                removeFeatureType( (FeatureTypeInfo) source );
            }
            else if ( source instanceof CoverageStoreInfo ) {
                removeCoverageStore( (CoverageStoreInfo) source );
            }
            else if ( source instanceof CoverageInfo ) {
                removeCoverage( (CoverageInfo) source );
            }
            else if ( source instanceof LayerInfo ) {
                removeLayer( (LayerInfo) source );
            }
            else if ( source instanceof StyleInfo ) {
                removeStyle( (StyleInfo) source );
            }
            else if ( source instanceof LayerGroupInfo ) {
                removeLayerGroup( (LayerGroupInfo) source );
            }
        }
        catch( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    public void handleGlobalChange(GeoServerInfo global, List<String> propertyNames,
            List<Object> oldValues, List<Object> newValues) {
    }
    
    public void handlePostGlobalChange(GeoServerInfo global) {
        try {
            persist( global, new File( rl.getBaseDirectory(), "global.xml") );
        } 
        catch (IOException e) {
            throw new RuntimeException( e );
        }
    }
    
    public void handleLoggingChange(LoggingInfo logging, List<String> propertyNames,
            List<Object> oldValues, List<Object> newValues) {
    }
    
    public void handlePostLoggingChange(LoggingInfo logging) {
        try {
            persist( logging, new File( rl.getBaseDirectory(), "logging.xml") );
        } 
        catch (IOException e) {
            throw new RuntimeException( e );
        }
    }
    
    public void handleServiceAdded(ServiceInfo service) {
    }
    
    public void handleServiceChange(ServiceInfo service, List<String> propertyNames,
            List<Object> oldValues, List<Object> newValues) {
    }
    
    public void handlePostServiceChange(ServiceInfo service) {
    }
    
    public void reloaded() {
    }
    
    //workspaces
    void addWorkspace( WorkspaceInfo ws ) throws IOException {
        LOGGER.fine( "Persisting workspace " + ws.getName() );
        File dir = dir( ws, true );
        dir.mkdirs();
        
        persist( ws, file( ws ) );
    }
    
    void renameWorkspace( WorkspaceInfo ws, String newName ) throws IOException {
        LOGGER.fine( "Renaming workspace " + ws.getName() + "to " + newName );
        rename( dir( ws ), newName );
    }
    
    void modifyWorkspace( WorkspaceInfo ws ) throws IOException {
        LOGGER.fine( "Persisting workspace " + ws.getName() );
        persist( ws, file( ws ) );
    }
    
    void removeWorkspace( WorkspaceInfo ws ) throws IOException {
        LOGGER.fine( "Removing workspace " + ws.getName() );
        File dir = dir( ws );
        FileUtils.deleteDirectory( dir );
    }
    
    File dir( WorkspaceInfo ws ) throws IOException {
        return dir( ws, false );
    }
    
    File dir( WorkspaceInfo ws, boolean create ) throws IOException {
        File d = rl.find( "workspaces", ws.getName() );
        if ( d == null && create ) {
            d = rl.createDirectory( "workspaces", ws.getName() );
        }
        return d;
    }
    
    File file( WorkspaceInfo ws ) throws IOException {
        return new File( dir( ws ), "workspace.xml" );
    }
    
    //namespaces
    void addNamespace( NamespaceInfo ns ) throws IOException {
        LOGGER.fine( "Persisting namespace " + ns.getPrefix() );
        File dir = dir( ns, true );
        dir.mkdirs();
        persist( ns, file(ns) );
    }
    
    void modifyNamespace( NamespaceInfo ns) throws IOException {
        LOGGER.fine( "Persisting namespace " + ns.getPrefix() );
        persist( ns, file(ns) );
    }
    
    void removeNamespace( NamespaceInfo ns ) throws IOException {
        LOGGER.fine( "Removing namespace " + ns.getPrefix() );
        file( ns ).delete();
    }
    
    File dir( NamespaceInfo ns ) throws IOException {
        return dir( ns, false );
    }
    
    File dir( NamespaceInfo ns, boolean create ) throws IOException {
        File d = rl.find( "workspaces", ns.getPrefix() );
        if ( d == null && create ) {
            d = rl.createDirectory( "workspaces", ns.getPrefix() );
        }
        return d;
    }
    
    File file( NamespaceInfo ns ) throws IOException {
        return new File( dir( ns ), "namespace.xml");
    }
    
    //datastores
    void addDataStore( DataStoreInfo ds ) throws IOException {
        LOGGER.fine( "Persisting datastore " + ds.getName() );
        File dir = dir( ds );
        dir.mkdir();
        
        persist( ds, file( ds ) );
    }
    
    void renameStore( StoreInfo s, String newName ) throws IOException {
        LOGGER.fine( "Renaming store " + s.getName() + "to " + newName );
        rename( dir( s ), newName );
    }
    
    void modifyDataStore( DataStoreInfo ds ) throws IOException {
        LOGGER.fine( "Persisting datastore " + ds.getName() );
        persist( ds, file( ds ) );
    }
    
    void removeDataStore( DataStoreInfo ds ) throws IOException {
        LOGGER.fine( "Removing datastore " + ds.getName() );
        File dir = dir( ds );
        FileUtils.deleteDirectory( dir );
    }
    
    File dir( StoreInfo s ) throws IOException {
        return new File( dir( s.getWorkspace() ), s.getName() );
    }
    
    File file( DataStoreInfo ds ) throws IOException {
        return new File( dir( ds ), "datastore.xml" );
    }
    
    //feature types
    void addFeatureType( FeatureTypeInfo ft ) throws IOException {
        LOGGER.fine( "Persisting feature type " + ft.getName() );
        File dir = dir( ft );
        dir.mkdir();
        persist( ft, file( ft ) );
    }
    
    void renameResource( ResourceInfo r, String newName ) throws IOException {
        LOGGER.fine( "Renaming resource " + r.getName() + " to " + newName );
        rename( dir( r ), newName );
    }
    
    void modifyFeatureType( FeatureTypeInfo ft ) throws IOException {
        LOGGER.fine( "Persisting feature type " + ft.getName() );
        persist( ft, file( ft ) );
    }
    
    void removeFeatureType( FeatureTypeInfo ft ) throws IOException {
        LOGGER.fine( "Removing feature type " + ft.getName() );
        File dir = dir( ft );
        FileUtils.deleteDirectory( dir );
    }
    
    File dir( ResourceInfo r ) throws IOException {
        return new File( dir( r.getStore() ), r.getName() );
    }
    
    File file( FeatureTypeInfo ft ) throws IOException {
        return new File( dir( ft ), "featuretype.xml");
    }
    
    //coverage stores
    void addCoverageStore( CoverageStoreInfo cs ) throws IOException {
        LOGGER.fine( "Persisting coverage store " + cs.getName() );
        File dir = dir( cs );
        dir.mkdir();
        
        persist( cs, file( cs ) );
    }
    
    void modifyCoverageStore( CoverageStoreInfo cs ) throws IOException {
        LOGGER.fine( "Persisting coverage store " + cs.getName() );
        persist( cs, file( cs ) );
    }
    
    void removeCoverageStore( CoverageStoreInfo cs ) throws IOException {
        LOGGER.fine( "Removing coverage store " + cs.getName() );
        File dir = dir( cs );
        FileUtils.deleteDirectory( dir );
    }
    
    File file( CoverageStoreInfo cs ) throws IOException {
        return new File( dir( cs ), "coveragestore.xml");
    }
    
    //coverages
    void addCoverage( CoverageInfo c ) throws IOException {
        LOGGER.fine( "Persisting coverage " + c.getName() );
        File dir = dir( c );
        dir.mkdir();
        persist( c, dir, "coverage.xml" );
    }
    
    void modifyCoverage( CoverageInfo c ) throws IOException {
        LOGGER.fine( "Persisting coverage " + c.getName() );
        File dir = dir( c );
        persist( c, dir, "coverage.xml");
    }
    
    void removeCoverage( CoverageInfo c ) throws IOException {
        LOGGER.fine( "Removing coverage " + c.getName() );
        File dir = dir( c );
        FileUtils.deleteDirectory( dir );
    }
    
    //layers
    void addLayer( LayerInfo l ) throws IOException {
        LOGGER.fine( "Persisting layer " + l.getName() );
        File dir = dir( l );
        dir.mkdir();
        persist( l, file( l ) );
    }
    
    void modifyLayer( LayerInfo l ) throws IOException {
        LOGGER.fine( "Persisting layer " + l.getName() );
        persist( l, file( l ) );
    }
    
    void removeLayer( LayerInfo l ) throws IOException {
        LOGGER.fine( "Removing layer " + l.getName() );
        File dir = dir( l );
        FileUtils.deleteDirectory( dir );
    }
    
    File dir( LayerInfo l ) throws IOException {
        if ( l.getResource() instanceof FeatureTypeInfo) {
            return dir( (FeatureTypeInfo) l.getResource() );
        }
        else if ( l.getResource() instanceof CoverageInfo ) {
            return dir( (CoverageInfo) l.getResource() );
        }
        return null;
    }
    
    File file( LayerInfo l ) throws IOException {
        return new File( dir( l ), "layer.xml" );
    }
    
    //styles
    void addStyle( StyleInfo s ) throws IOException {
        LOGGER.fine( "Persisting style " + s.getName() );
        dir( s, true );
        persist( s, file( s ) );
    }
    
    void renameStyle( StyleInfo s, String newName ) throws IOException {
        LOGGER.fine( "Renameing style " + s.getName() + " to " + newName );
        rename( file( s ), newName+".xml" );
    }
    
    void modifyStyle( StyleInfo s ) throws IOException {
        LOGGER.fine( "Persisting style " + s.getName() );
        persist( s, file( s ) );
        /*
        //save out sld
        File f = file(s);
        BufferedOutputStream out = new BufferedOutputStream( new FileOutputStream( f ) );
        SLDTransformer tx = new SLDTransformer();
        try {
            tx.transform( s.getSLD(),out );
            out.flush();
        } 
        catch (TransformerException e) {
            throw (IOException) new IOException().initCause( e );
        }
        finally {
            out.close();
        }
        */
    }
    
    void removeStyle( StyleInfo s ) throws IOException {
        LOGGER.fine( "Removing style " + s.getName() );
        file( s ).delete();
    }
    
    File dir( StyleInfo s ) throws IOException {
        return dir( s, false );
    }
    
    File dir( StyleInfo s, boolean create ) throws IOException {
        File d = rl.find( "styles" );
        if ( d == null && create ) {
            d = rl.createDirectory( "styles" );
        }
        return d;
    }
    
    File file( StyleInfo s ) throws IOException {
        //special case for styles, if the file name (minus the suffix) matches the id of the style
        // and the suffix is xml (rather than sld) we need to avoid overwritting the actual 
        // style file
        if (s.getFilename() != null && s.getFilename().endsWith(".xml") 
            && s.getFilename().startsWith(s.getName()+".")) {
            //append a second .xml suffix
            return new File( dir( s ), s.getName() + ".xml.xml");
        }
        else {
            return new File( dir( s ), s.getName() + ".xml");
        }
    }
    
    //layer groups
    void addLayerGroup( LayerGroupInfo lg ) throws IOException {
        LOGGER.fine( "Persisting layer group " + lg.getName() );
        
        dir( lg, true );
        persist( lg, file( lg ) );
    }
    
    void renameLayerGroup( LayerGroupInfo lg, String newName ) throws IOException {
        LOGGER.fine( "Renaming layer group " + lg.getName() + " to " + newName );
        rename( file( lg ), newName+".xml" );
    }

    void modifyLayerGroup( LayerGroupInfo lg ) throws IOException {
        LOGGER.fine( "Persisting layer group " + lg.getName() );
        persist( lg, file( lg ) );
    }
    
    void removeLayerGroup( LayerGroupInfo lg ) throws IOException {
        LOGGER.fine( "Removing layer group " + lg.getName() );
        file( lg ).delete();
    }
    
    File dir( LayerGroupInfo lg ) throws IOException {
        return dir( lg, false );
    }
    
    File dir( LayerGroupInfo lg, boolean create ) throws IOException {
        File d = rl.find( "layergroups" );
        if ( d == null && create ) {
            d = rl.createDirectory( "layergroups"); 
        }
        return d;
    }
    
    File file( LayerGroupInfo lg ) throws IOException {
        return new File( dir( lg ), lg.getName() + ".xml" );
    }
    
    //helpers
    void backupDirectory(File dir) throws IOException {
        File bak = new File( dir.getCanonicalPath() + ".bak");
        if ( bak.exists() ) {
            FileUtils.deleteDirectory( bak );
        }
        dir.renameTo( bak );
    }
    
    void rename(File f, String newName) throws IOException {
        rename( f, new File( f.getParentFile(), newName ) );
    }
    
    void rename( File source, File dest ) throws IOException {
        boolean win = System.getProperty("os.name").startsWith("Windows");
        if ( win && dest.exists() ) {
            //windows does not do atomic renames, and can not rename a file if the dest file
            // exists
            if (!dest.delete()) {
                throw new IOException("Could not delete: " + dest.getCanonicalPath());
            }
            source.renameTo(dest);
        }
        else {
            source.renameTo(dest);
        }
    }
    
    void persist( Object o, File dir, String filename ) throws IOException {
        persist( o, new File( dir, filename ) );
    }

    void persist( Object o, File f ) throws IOException {
        try {
            synchronized ( xp ) {
                //first save to a temp file
                File temp = new File(f.getParentFile(),f.getName()+".tmp");
                if ( temp.exists() ) {
                    temp.delete();
                }
                
                BufferedOutputStream out = 
                    new BufferedOutputStream( new FileOutputStream( temp ) );
                xp.save( o, out );
                out.flush();
                out.close();
                
                //no errors, overwrite the original file
                rename(temp,f);
            }
            LOGGER.fine("Persisted " + o.getClass().getName() + " to " + f.getAbsolutePath() );
        }
        catch( Exception e ) {
            //catch any exceptions and send them back as CatalogExeptions
            String msg = "Error persisting " + o + " to " + f.getCanonicalPath();
            throw new CatalogException(msg, e);
        }
    }

}
