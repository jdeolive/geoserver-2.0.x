package org.geoserver.proxy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

import org.vfny.geoserver.global.ConfigurationException;
import org.vfny.geoserver.global.GeoserverDataDirectory;

import com.thoughtworks.xstream.XStream;

/**
 * This class holds the the configuration for the Proxy server module during runtime. It is also
 * serialized as XML to persistently store settings.
 * 
 * @author Alan Gerber <agerber@openplans.org>
 */
//vvv everybody else is doing this
@SuppressWarnings("serial")
public class ProxyConfig implements java.io.Serializable{
    //This rwlock controls access to the file in which proxy configuration is stored.
    private static ReadWriteLock configFileLock = new ReentrantReadWriteLock();
    private static Lock configReadLock = configFileLock.readLock();
    private static Lock configWriteLock = configFileLock.writeLock();    
    
    /*
     * Sets the mode of the proxy server: -HOSTNAMEORMIMETYPE means a request must match have
     * matches on the hostname AND MIMEType whitelists -HOSTNAMEANDMIMETYPE means a request must
     * match have matches on the hostname OR MIMEType whitelists -HOSTNAME means a request must
     * match have a match on the hostname whitelist alone -MIMETYPE means a request must match have
     * a match on the MIMEType whitelist alone
     */
    public enum Mode {
        HOSTNAMEORMIMETYPE ("Hostname OR MIMEType"),
        HOSTNAMEANDMIMETYPE ("Hostname AND MIMEType"),
        HOSTNAME ("Hostname only"),
        MIMETYPE ("MIMEType only");
        
        public final String modeName;
        Mode(String modeName){
            this.modeName = modeName;
        }
        
        public static List<String> modeNames() {
            List<String> modeNames = new ArrayList<String>();
            for (Mode mode : Mode.values())
            {
                modeNames.add(mode.modeName);
            }
            return modeNames;
        }
    };

    public Mode mode;

    /* A list of regular expressions describing hostnames the proxy is permitted to forward to */
    public LinkedHashSet<String> hostnameWhitelist;

    /* A list of regular expressions describing MIMETypes the proxy is permitted to forward */
    public LinkedHashSet<String> mimetypeWhitelist;

    /*The name of the directory where configuration is stored*/
    private static final String confDirName = "proxy";
    /*The name of the file the configuration is stored in*/
    private static final String confFileName = "proxy.xml";
    
    /* Attempts to grab the proxy's config file from the data dir.
     * @return  the proxy's configuration's File
     * @throws  ConfigurationException if the config system is busted
     */
    public static File getConfigFile() throws ConfigurationException
    {
        File dir = GeoserverDataDirectory.findCreateConfigDir(confDirName);
        File proxyConfFile = new File(dir, confFileName);
        return proxyConfFile;
    }
    
    /*The default proxy configuration allows all requests to localhost and all OGC mimetypes*/
    private static final ProxyConfig DEFAULT;
    static {
        DEFAULT = new ProxyConfig();
        DEFAULT.mode = Mode.HOSTNAMEORMIMETYPE;
        DEFAULT.hostnameWhitelist = new LinkedHashSet<String>();
        DEFAULT.mimetypeWhitelist = new LinkedHashSet<String>(Arrays.asList(
                "application/xml", "text/xml",
                "application/vnd.ogc.se_xml",           // OGC Service Exception 
                "application/vnd.ogc.se+xml",           // OGC Service Exception
                "application/vnd.ogc.success+xml",      // OGC Success (SLD Put)
                "application/vnd.ogc.wms_xml",          // WMS Capabilities
                "application/vnd.ogc.context+xml",      // WMC
                "application/vnd.ogc.gml",              // GML
                "application/vnd.ogc.sld+xml",          // SLD
                "application/vnd.google-earth.kml+xml"  // KML;
        ));
    }

    private static final Logger LOG = org.geotools.util.logging.Logging
            .getLogger("org.geoserver.proxy");

    /* this is pretty unappealingly hackish */
    public static ProxyConfig loadConfFromDisk() {
        ProxyConfig retval;

        try {
            File proxyConfFile = getConfigFile();
            InputStream proxyConfStream = new FileInputStream(proxyConfFile);
            XStream xs = new XStream();
            //Take the read lock, then read the file
            configReadLock.lock();
            retval = (ProxyConfig) (xs.fromXML(proxyConfStream));
            configReadLock.unlock();
        } catch (Exception e) {
            LOG.warning("Failed to open configuration for Proxy module. Using default. Exception:"
                    + e.toString());
            //writeConfigToDisk(DEFAULT);
            retval = DEFAULT;
        }
        return retval;
    }

    public static boolean writeConfigToDisk(ProxyConfig pc) {
        try {
            File proxyConfFile = getConfigFile();
            XStream xs = new XStream();
            String xml = xs.toXML(pc);
            FileWriter fw = new FileWriter(proxyConfFile, false); // false means overwrite old file
            //Take the write lock on the file & lock it
            configWriteLock.lock();
            fw.write(xml);
            fw.close();
            configWriteLock.unlock();
            return true;
        } catch (Exception e) {
            LOG.warning("Failed to save configuration for Proxy module. Exception:"
                    + e.toString());
            return false;
        }
    }
    
    /*Output a textual representation of the config
     *@return a String representation of the config
     */
    @Override
    public String toString(){
        StringBuilder stringForm = new StringBuilder(256);
        stringForm.append("Mode: " + this.mode.modeName + "\n");
        stringForm.append("Hostname regex whitelist: \n");
        for (String hostname : this.hostnameWhitelist)
            stringForm.append(hostname + "\n");
        stringForm.append("MIMEType regex whitelist: \n");
        for (String mimetype : this.mimetypeWhitelist)
            stringForm.append(mimetype + "\n");
        stringForm.append(this.mode.modeName + "\n");        
        return stringForm.toString();
    }
}
