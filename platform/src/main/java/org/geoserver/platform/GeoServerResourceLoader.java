/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.platform;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Manages resources in GeoServer.
 * <p>
 * The loader maintains a search path in which it will use to look up resources.
 * The {@link #baseDirectory} is a member of this path.
 * </p>
 * <p>
 * Files and directories created by the resource loader are made relative to
 * {@link #baseDirectory}.
 * </p>
 * <p>
 * <pre>
 *         <code>
 * File dataDirectory = ...
 * GeoServerResourceLoader loader = new GeoServerResourceLoader( dataDirectory );
 * loader.addSearchLocation( new File( "/WEB-INF/" ) );
 * loader.addSearchLocation( new File( "/data" ) );
 * ...
 * File catalog = loader.find( "catalog.xml" );
 *         </code>
 * </pre>
 * </p>
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 *
 */
public class GeoServerResourceLoader extends DefaultResourceLoader {
    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.vfny.geoserver.global");

    /** "path" for resource lookups */
    Set searchLocations;

    /**
     * Base directory
     */
    File baseDirectory;

    /**
     * Creates a new resource loader with no base directory.
     * <p>
     * Such a constructed resource loader is not capable of creating resources
     * from relative paths.
     * </p>
     */
    public GeoServerResourceLoader() {
        searchLocations = new TreeSet();
    }

    /**
     * Creates a new resource loader.
     *
     * @param baseDirectory The directory in which
     */
    public GeoServerResourceLoader(File baseDirectory) {
        this();
        this.baseDirectory = baseDirectory;
        setSearchLocations(Collections.EMPTY_SET);
    }

    /**
     * Adds a location to the path used for resource lookups.
     *
     * @param A directory containing resources.
     */
    public void addSearchLocation(File searchLocation) {
        searchLocations.add(searchLocation);
    }

    /**
     * Sets the search locations used for resource lookups.
     *
     * @param searchLocations A set of {@link File}.
     */
    public void setSearchLocations(Set searchLocations) {
        this.searchLocations = new HashSet(searchLocations);

        //always add the base directory
        if (baseDirectory != null) {
            this.searchLocations.add(baseDirectory);
        }
    }

    /**
     * @return The base directory.
     */
    public File getBaseDirectory() {
        return baseDirectory;
    }

    /**
     * Sets the base directory.
     *
     * @param baseDirectory
     */
    public void setBaseDirectory(File baseDirectory) {
        this.baseDirectory = baseDirectory;

        searchLocations.add(baseDirectory);
    }

    /**
     * Performs a resource lookup.
     *
     * @param location The name of the resource to lookup, can be absolute or
     * relative.
     *
     * @return The file handle representing the resource, or null if the
     * resource could not be found.
     *
     * @throws IOException In the event of an I/O error.
     */
    public File find( String location ) throws IOException {
        return find( null, location );
    }
    
    /**
     * Performs a resource lookup, optionally specifying the containing directory.
     *
     * @param parent The containing directory, optionally null. 
     * @param location The name of the resource to lookup, can be absolute or
     * relative.
     *
     * @return The file handle representing the resource, or null if the
     * resource could not be found.
     *
     * @throws IOException In the event of an I/O error.
     */
    public File find(File parent, String location) throws IOException {
        //first to an existance check
        File file = parent != null ? new File(parent,location) : new File(location);

        if (file.isAbsolute()) {
            return file.exists() ? file : null;
        } else {
            //try a relative url if no parent specified
            if ( parent == null ) {
                for (Iterator f = searchLocations.iterator(); f.hasNext();) {
                    File base = (File) f.next();
                    file = new File(base, location);
    
                    try {
                        if (file.exists()) {
                            return file;
                        }
                    } catch (SecurityException e) {
                        LOGGER.warning("Failed attemp to check existance of " + file.getAbsolutePath());
                    }
                }
            }
        }

        //look for a generic resource if no parent specified
        if ( parent == null ) {
            Resource resource = getResource(location);
    
            if (resource.exists()) {
                return resource.getFile();
            }
        }

        return null;
    }
    
    /**
     * Performs a resource lookup.
     * <p>
     * <pre>
     * Example:
     *   File f = resourceLoader.find( "data", "shapefiles", "foo.shp" );
     * </pre> 
     * </p>
     * @param location The components of the path of the resource to lookup.
     * 
     * @return The file handle representing the resource, or null if the
     *  resource could not be found.
     *  
     * @throws IOException Any I/O errors that occur.
     */
    public File find( String... location ) throws IOException {
        return find( null, location );
    }

    /**
     * Performs a resource lookup, optionally specifying a containing directory.
     * <p>
     * <pre>
     * Example:
     *   File f = resourceLoader.find( "data", "shapefiles", "foo.shp" );
     * </pre> 
     * </p>
     * @param parent The parent directory, may be null.
     * @param location The components of the path of the resource to lookup.
     * 
     * @return The file handle representing the resource, or null if the
     *  resource could not be found.
     *  
     * @throws IOException Any I/O errors that occur.
     */
    public File find( File parent, String... location ) throws IOException {
        return find( parent, concat( location ) );
    }

    /**
     * Helper method to build up a file path from components.
     */
    String concat( String... location ) {
        StringBuffer loc = new StringBuffer();
        for ( int i = 0; i < location.length; i++ ) {
            loc.append( location[i] ).append( File.separator );
        }
        loc.setLength(loc.length()-1);
        return loc.toString();
    }
    
    /**
     * Performs a directory lookup, creating the file if it does not exist.
     * 
     * @param location The components of the path that make up the location of the directory to
     *  find or create.
     */
    public File findOrCreateDirectory( String... location ) throws IOException {
        return findOrCreateDirectory(null,location);
    }
    
    /**
     * Performs a directory lookup, creating the file if it does not exist.
     * 
     * @param parent The containing directory, possibly null.
     * @param location The components of the path that make up the location of the directory to
     *  find or create.
     */
    public File findOrCreateDirectory( File parent, String... location ) throws IOException {
        return findOrCreateDirectory(concat(location));
    }
    
    /**
     * Performs a directory lookup, creating the file if it does not exist.
     * 
     * @param location The location of the directory to find or create.
     * 
     * @return The file handle.
     * 
     * @throws IOException If any i/o errors occur.
     */
    public File findOrCreateDirectory( String location ) throws IOException {
        return findOrCreateDirectory(null,location);
    }
    
    /**
     * Performs a directory lookup, creating the file if it does not exist.
     * 
     * @param parent The containing directory, may be null.
     * @param location The location of the directory to find or create.
     * 
     * @return The file handle.
     * 
     * @throws IOException If any i/o errors occur.
     */
    public File findOrCreateDirectory( File parent, String location ) throws IOException {
        File dir = find( parent, location );
        if ( dir != null ) {
            if ( !dir.isDirectory() ) {
                //location exists, but is a file
                throw new IllegalArgumentException( "Location '" + location + "' specifies a file");
            }
            
            return dir;
        }
        
        //create it
        return createDirectory( location );
    }
    
    /**
     * Creates a new directory specifying components of the location.
     * <p>
     * Calls through to {@link #createDirectory(String)}
     * </p>
     */
    public File createDirectory(String... location) throws IOException {
        return createDirectory(null,location);
    }
    
    /**
     * Creates a new directory specifying components of the location, and the containing directory.
     * <p>
     * Calls through to {@link #createDirectory(String)}
     * </p>
     */
    public File createDirectory(File parent, String... location) throws IOException {
        return createDirectory(parent,concat(location));
    }
    
    /**
     * Creates a new directory.
     * <p>
     * Relative paths are created relative to {@link #baseDirectory}.
     * If {@link #baseDirectory} is not set, an IOException is thrown.
     * </p>
     * <p>
     * If <code>location</code> already exists as a file, an IOException is thrown.
     * </p>
     * @param location Location of directory to create, either absolute or
     * relative.
     *
     * @return The file handle of the created directory.
     *
     * @throws IOException
     */
    public File createDirectory(String location) throws IOException {
        return createDirectory(null,location);
    }
    
    /**
     * Creates a new directory, optionally specifying a containing directory.
     * <p>
     * Relative paths are created relative to {@link #baseDirectory}.
     * If {@link #baseDirectory} is not set, an IOException is thrown.
     * </p>
     * <p>
     * If <code>location</code> already exists as a file, an IOException is thrown.
     * </p>
     * @param parent The containing directory, may be null.
     * @param location Location of directory to create, either absolute or
     * relative.
     *
     * @return The file handle of the created directory.
     *
     * @throws IOException
     */
    public File createDirectory(File parent, String location) throws IOException {
        File file = find(parent,location);

        if (file != null) {
            if (!file.isDirectory()) {
                String msg = location + " already exists and is not directory";
                throw new IOException(msg);
            }
        }

        file = parent != null ? new File(parent,location) : new File(location);

        if (file.isAbsolute()) {
            file.mkdirs();

            return file;
        }

        if ( parent == null ) {
            //no base directory set, cannot create a relative path
            if (baseDirectory == null) {
                String msg = "No base location set, could not create directory: " + location;
                throw new IOException(msg);
            }
    
            file = new File(baseDirectory, location);
            file.mkdirs();
    
            return file;
        }
        return null;
    }

    /**
     * Creates a new file.
     * <p>
     * Calls through to {@link #createFile(String)}.
     * </p>
     * 
     * @param location The components of the location.
     *
     * @return The file handle of the created file.
     *
     * @throws IOException In the event of an I/O error.
     */
    public File createFile(String ...location) throws IOException {
        return createFile( concat(location) );
    }
    
    /**
     * Creates a new file.
     * <p>
     * Calls through to {@link #createFile(File, String)}
     * </p>
     * @param location Location of file to create, either absolute or relative.
     *
     * @return The file handle of the created file.
     *
     * @throws IOException In the event of an I/O error.
     */
    public File createFile(String location) throws IOException {
        return createFile(null,location);
    }
    
    /**
     * Creates a new file.
     * <p>
     * Calls through to {@link #createFile(File, String)}
     * </p>
     * @param location Location of file to create, either absolute or relative.
     * @param parent The containing directory for the file.
     * 
     * @return The file handle of the created file.
     *
     * @throws IOException In the event of an I/O error.
     */
    public File createFile(File parent, String... location) throws IOException{
        return createFile(parent,concat(location));
    }
    
    /**
     * Creates a new file.
     * <p>
     * Relative paths are created relative to {@link #baseDirectory}.
     * </p>
     * If {@link #baseDirectory} is not set, an IOException is thrown.
     * </p>
     * <p>
     * If <code>location</code> already exists as a directory, an IOException is thrown.
     * </p>
     * @param location Location of file to create, either absolute or relative.
     * @param parent The containing directory for the file.
     * 
     * @return The file handle of the created file.
     *
     * @throws IOException In the event of an I/O error.
     */
    public File createFile(File parent, String location) throws IOException{
        File file = find(parent,location);

        if (file != null) {
            if (file.isDirectory()) {
                String msg = location + " already exists and is a directory";
                throw new IOException(msg);
            }

            return file;
        }

        file = parent != null ? new File(parent,location) : new File(location);

        if (file.isAbsolute()) {
            file.createNewFile();

            return file;
        }

        if ( parent == null ) {
            //no base directory set, cannot create a relative path
            if (baseDirectory == null) {
                String msg = "No base location set, could not create file: " + location;
                throw new IOException(msg);
            }

            file = new File(baseDirectory, location);
            file.createNewFile();
        }
        
        return file;
    }
    
    /**
     * Copies a resource located on the classpath to a specified path.
     * <p>
     * The <tt>resource</tt> is obtained from teh context class loader of the 
     * current thread. When the <tt>to</tt> parameter is specified as a relative
     * path it is considered to be relative to {@link #getBaseDirectory()}.
      </p>
     * 
     * @param resource The resource to copy.
     * @param to The destination to copy to.
     */
    public void copyFromClassPath( String resource, String to ) throws IOException {
        File target = new File( to );
        if ( !target.isAbsolute() ) {
            target = new File( getBaseDirectory(), to );
        }
        
        copyFromClassPath(resource, target);
    }
    
    /**
     * Copies a resource from the classpath to a specified file.
     * 
     */
    public void copyFromClassPath( String resource, File target ) throws IOException {
        InputStream is = null; 
        OutputStream os = null;
        byte[] buffer = new byte[4096];
        int read;
        
        try {
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
            os = new FileOutputStream(target);
            while((read = is.read(buffer)) > 0)
                os.write(buffer, 0, read);
        } catch (FileNotFoundException targetException) {
            throw new IOException("Can't write to file " + target.getAbsolutePath() + 
                    ". Check write permissions on target folder for user " + System.getProperty("user.name"));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error trying to copy logging configuration file", e);
        } finally {
            try {
                if(is != null){
                    is.close();
                }
                if(os != null){
                    os.close();
                }
            } catch(IOException e) {
                // we tried...
            }
        }
    }
}
