/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.vfny.geoserver.global;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.opengis.feature.simple.SimpleFeatureType;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.NoSuchElementException;
import java.util.logging.Logger;
import javax.servlet.ServletContext;


/**
 * This class allows for abstracting the location of the Geoserver Data directory. Some people call this "GEOSERVER_HOME".
 *
 * Inside this directory should be two more directories: a. "WEB-INF/" Inside this is a catalog.xml b. "data/" Inside this is a set of other
 * directories.
 *
 * For the exact content of these directories, see any existing geoserver install's server/geoserver directory.
 *
 * In order to find the geoserver data directory the following steps take place:
 *
 * 1. search for the "GEOSERVER_DATA_DIR" system property. this will most likely have come from "java -DGEOSERVER_DATA_DIR=..." or from you
 * web container 2. search for a "GEOSERVER_DATA_DIR" in the web.xml document <context-param> <param-name>GEOSERVER_DATA_DIR</param-name>
 * <param-value>...</param-value> </context-param> 3. It defaults to the old behavior - ie. the application root - usually
 * "server/geoserver" in your .WAR.
 *
 *
 * NOTE: a set method is currently undefined because you should either modify you web.xml or set the environment variable and re-start
 * geoserver.
 *
 * @author dblasby
 *
 */
public class GeoserverDataDirectory {
    // caches the dataDir
    public static GeoServerResourceLoader loader;
    private static Catalog catalog;
    private static ApplicationContext appContext;
    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.vfny.geoserver.global");

    /**
     * See the class documentation for more details. 1. search for the "GEOSERVER_DATA_DIR" system property. 2. search for a
     * "GEOSERVER_DATA_DIR" in the web.xml document 3. It defaults to the old behavior - ie. the application root - usually
     * "server/geoserver" in your .WAR.
     *
     * @return location of the geoserver data dir
     */
    static public File getGeoserverDataDirectory() {
        if (loader != null) {
            return loader.getBaseDirectory();
        } else {
            return null;
        }
    }
    
    /**
     * Locate feature type directory name using the FeatureType as a key into the catalog 
     * @see Data#getFeatureTypeInfo(String) 
     * @param name
     *            String The FeatureTypeInfo Name

     * @return the feature type dir name, or null if not found (either the feature type or the directory)
     *
     * @throws NoSuchElementException
     */
    static public String findFeatureTypeDirName(SimpleFeatureType featureType) {
        String name = featureType.getTypeName();
        String namespace = featureType.getName().getNamespaceURI();
        FeatureTypeInfo ftInfo = null;
        Catalog data = getCatalog();
        if(namespace != null) {
            NamespaceInfo nsInfo = data.getNamespaceByURI(namespace);
            if(nsInfo != null)
                ftInfo = data.getFeatureTypeByName( nsInfo.getPrefix(), name);
        }
        if(ftInfo == null) 
            ftInfo = data.getFeatureTypeByName(name);
        if(ftInfo == null)
            return null;
        String dirName = ftInfo.getMetadata().get("dirName",String.class);
        if ( dirName == null ) {
            dirName = ftInfo.getNamespace().getPrefix() + "_" + ftInfo.getName();
        }
        
        return dirName;
    }

    /**
     * Locate coverage type directory name using the coverage name as a key into the catalog 
     * @see Data#getCoverageInfo(String)
     * @param coverageName
     *            String The FeatureTypeInfo Name

     * @return the feature type dir name, or null if not found (either the feature type or the directory)
     *
     * @throws NoSuchElementException
     */
    public static String findCoverageDirName(String coverageName) {
        Catalog data = getCatalog();
        CoverageInfo coverageInfo = data.getCoverageByName(coverageName);
        return coverageInfo.getMetadata().get( "dirName", String.class );
    }

    /**
     * Utility method to find the approriate sub-data dir config. This is a helper for the fact that we're transitioning away from the
     * WEB-INF type of hacky storage, but during the transition things can be in both places. So this method takes the root file, the
     * dataDir, and a name of a directory that is stored in the data dir, and checks for it in the data/ dir (the old way), and directly in
     * the dir (the new way)
     *
     * @param root
     *            Generally the Data Directory, the directory to try to find the config file in.
     * @param dirName
     *            The name of the directory to find in the data Dir.
     * @return The proper config directory.
     * @throws ConfigurationException
     *             if the directory could not be found at all.
     */
    public static File findConfigDir(File root, String dirName)
        throws ConfigurationException {
        File configDir;

        try {
            configDir = loader.find(dirName);
        } catch (IOException e) {
            throw new ConfigurationException(e);
        }

        return configDir;
    }

    /**
     * Same as {@link #findConfigDir(File, String), but it will create the configuration directory
     * if missing (as a top level directory inside the Geoserver data directory)
     * @param dirName
     * @return
     * @throws ConfigurationException
     */
    public static File findCreateConfigDir(String dirName)
        throws ConfigurationException {
        File configDir = findConfigDir(getGeoserverDataDirectory(), dirName);

        if ((configDir == null) || !configDir.exists()) {
            configDir = new File(getGeoserverDataDirectory(), dirName);
            configDir.mkdir();

            if (configDir.exists()) {
                return configDir;
            }
        }

        return configDir;
    }

    /**
     * Given a url, tries to interpret it as a file into the data directory, or as an absolute
     * location, and returns the actual absolute location of the File
     * @param path
     * @return
     */
    public static File findDataFile(URL url) {
        return findDataFile(url.getFile());
    }

    /**
     * Looks up a file under the "styles" directory.
     * 
     * @param fileName The name of the file.
     * 
     * @return The style file, or null if it does not exist.
     */
    public static File findStyleFile(String fileName) {
        return findStyleFile( fileName, false );
    }
    
    /**
     * Looks up a file under the "styles" directory.
     * 
     * @param fileName The name of the file.
     * @param resolve If set to true a non-null file handle will be returned even 
     * when the file does not exist.
     * 
     * @return The style file, or null if it does not exist and resolve == false.
     */
    public static File findStyleFile(String fileName, boolean resolve) {
        File baseDir = GeoserverDataDirectory.getGeoserverDataDirectory();
        File styleFile = new File( new File( baseDir, "styles" ), fileName );
        
        if (resolve || styleFile.exists() ) {
            return styleFile;
        }
        
        return null;
    }
    
    /**
     * Given a path, tries to interpret it as a file into the data directory, or as an absolute
     * location, and returns the actual absolute location of the File
     * @param path
     * @return
     */
    public static File findDataFile(String path) {
        File baseDir = GeoserverDataDirectory.getGeoserverDataDirectory();

        // do we ever have something that is not a file system reference?
        if (path.startsWith("file:")) {
            path = path.substring(5); // remove 'file:' prefix

            File f = new File(path);

            // if it's an absolute path, use it as such, 
            // otherwise try to map it inside the data dir
            if (f.isAbsolute() || f.exists()) {
                return f;
            } else {
                return new File(baseDir, path);
            }
        } else {
            return new File(path);
        }
    }

    /**
     * Utility method fofinding a config file under the data directory.
     *
     * @param file
     *            Path to file, absolute or relative to data dir.
     *
     * @return The file handle, or null.
     */
    public static File findConfigFile(String file) throws ConfigurationException {
        try {
            return loader.find(file);
        } catch (IOException e) {
            throw new ConfigurationException(e);
        }
    }

    /**
     * Initializes the data directory lookup service.
     * 
     * @param servContext
     */
    public static void init(WebApplicationContext context) {
        ServletContext servContext = context.getServletContext();

        // Oh, this is really sad. We need a reference to Data in order to
        // resolve feature type dirs, but gathering it here triggers the loading
        // of Geoserver (on whose Catalog depends on), which depends on having
        // DataDirectory and Config initialized, but this is not possible
        // here...
        // So we keep a reference to context in order to resolve Data later
        appContext = context;

        // This was once in the GetGeoserverDataDirectory method, I've moved
        // here so that servlet
        // context is not needed as a parameter anymore.
        // caching this, so we're not looking up everytime, and more
        // importantly, so we can actually look up this stuff without
        // having to pass in a ServletContext. This should be fine, since we
        // don't allow a set method, as we recommend restarting GeoServer,
        // so it should always get a ServletContext in the startup routine.
        // If this assumption can't be made, then we can't allow data_dir
        // _and_ webapp options with relative data/ links -ch
        
        if (loader == null) {
            // get the loader from the context
            loader = (GeoServerResourceLoader) context
                    .getBean("resourceLoader");

            File dataDir = null;

            String dataDirStr = findGeoServerDataDir(servContext);

            dataDir = new File(dataDirStr);
            loader.setBaseDirectory(dataDir);
            loader.addSearchLocation(new File(dataDir, "data"));
            loader.addSearchLocation(new File(dataDir, "WEB-INF"));
            LOGGER
                    .severe("\n----------------------------------\n- GEOSERVER_DATA_DIR: "
                            + dataDir.getAbsolutePath()
                            + "\n----------------------------------");

                return;
        }
    }

    
    /**
     * Loops over a list of variables that can represent the path to the
     * GeoServer data directory and attempts to resolve the value by looking at
     * 1) Java environment variable
     * 2) Servlet context variable
     * 3) System variable 
     *
     * For each of these, the methods checks that
     * 1) The path exists
     * 2) Is a directory
     * 3) Is writable
     * 
     * @param servContext
     * @return String representation of path, null otherwise
     */
    public static String findGeoServerDataDir(ServletContext servContext) {
        final String[] typeStrs = { "Java environment variable ",
                "Servlet context parameter ", "System environment variable " };

        String dataDirStr = null;
        
        final String[] varStrs = { "GEOSERVER_DATA_DIR", "GEOSERVER_DATA_ROOT" };

        String msgPrefix = null;
        int iVar = 0;
        // Loop over variable names
        for (int i = 0; i < varStrs.length && dataDirStr == null; i++) {
            
            // Loop over variable access methods
            for (int j = 0; j < typeStrs.length && dataDirStr == null; j++) {
                String value = null;
                String varStr = new String(varStrs[i]);
                String typeStr = typeStrs[j];

                // Lookup section
                switch (j) {
                case 0:
                    value = System.getProperty(varStr);
                    break;
                case 1:
                    value = servContext.getInitParameter(varStr);
                    break;
                case 2:
                    value = System.getenv(varStr);
                    break;
                }

                if (value == null || value.equalsIgnoreCase("")) {
                    LOGGER.finer("Found " + typeStr + varStr + " to be unset");
                    continue;
                }

                
                // Verify section
                File fh = new File(value);

                // Being a bit pessimistic here
                msgPrefix = "Found " + typeStr + varStr + " set to " + value;

                if (!fh.exists()) {
                    LOGGER.fine(msgPrefix + " , but this path does not exist");
                    continue;
                }
                if (!fh.isDirectory()) {
                    LOGGER.fine(msgPrefix + " , which is not a directory");
                    continue;
                }
                if (!fh.canWrite()) {
                    LOGGER.fine(msgPrefix + " , which is not writeable");
                    continue;
                }

                // Sweet, we can work with this
                dataDirStr = value;
                iVar = i;
            }
        }
        
        // fall back to embedded data dir
        if(dataDirStr == null)
            dataDirStr = servContext.getRealPath("/data");
        
        return dataDirStr;
    }
    /**
     * Signals the data directory to throw away all global state.
     * <p>
     * This code should *not* be called by any non-test GeoServer code.
     * </p>
     */
    public static void destroy() {
        loader = null;
        catalog = null;
    }
    
    private static Catalog getCatalog() {
        if(catalog == null) {
            catalog = (Catalog) GeoServerExtensions.bean( "catalog");
        }
        return catalog;
    }
    
    /**
     * Helper method to help client code migrade from using this class to using
     * {@link org.geoserver.config.GeoserverDataDirectory}.
     */
    public static org.geoserver.config.GeoServerDataDirectory accessor() {
        return new org.geoserver.config.GeoServerDataDirectory(loader);
    }
}
