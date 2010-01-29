/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.impl;

import java.util.HashMap;
import java.util.Map;

import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.JAIInfo;

public class GeoServerInfoImpl implements GeoServerInfo {

    protected String id;

    protected ContactInfo contact = new ContactInfoImpl();

    protected JAIInfo jai = new JAIInfoImpl();
    
    // Charset charSet = Charset.forName("UTF-8");
    protected String charset = "UTF-8";

    protected String title;

    protected int numDecimals = 4;

    protected String onlineResource;

    protected String schemaBaseUrl;

    protected String proxyBaseUrl;

    protected boolean verbose = true;

    protected boolean verboseExceptions = false;

    protected MetadataMap metadata = new MetadataMap();

    protected Map<Object, Object> clientProperties = new HashMap<Object, Object>();

    protected int updateSequence;
    
    protected String adminUsername;
    protected String adminPassword;
    
    protected int featureTypeCacheSize;

    protected Boolean globalServices = true;
    
    protected GeoServer geoServer;

    public GeoServerInfoImpl(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    protected GeoServerInfoImpl() {
    }
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setGeoServer(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    public void setContact(ContactInfo contactInfo) {
        this.contact = contactInfo;
    }

    public ContactInfo getContact() {
        return contact;
    }
    
    public JAIInfo getJAI() {
        return jai;
    }
    
    public void setJAI(JAIInfo jai) {
        this.jai = jai;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public int getNumDecimals() {
        return numDecimals;
    }

    public void setNumDecimals(int numDecimals) {
        this.numDecimals = numDecimals;
    }

    public String getOnlineResource() {
        return onlineResource;
    }

    public void setOnlineResource(String onlineResource) {
        this.onlineResource = onlineResource;
    }

    public String getProxyBaseUrl() {
        return proxyBaseUrl;
    }

    public void setProxyBaseUrl(String proxyBaseUrl) {
        this.proxyBaseUrl = proxyBaseUrl;
    }

    public String getSchemaBaseUrl() {
        return schemaBaseUrl;
    }

    public void setSchemaBaseUrl(String schemaBaseUrl) {
        this.schemaBaseUrl = schemaBaseUrl;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public boolean isVerboseExceptions() {
        return verboseExceptions;
    }

    public void setVerboseExceptions(boolean verboseExceptions) {
        this.verboseExceptions = verboseExceptions;
    }

    public int getUpdateSequence() {
        return updateSequence;
    }
    
    public void setUpdateSequence( int updateSequence ) {
        this.updateSequence = updateSequence;
    }

    public String getAdminPassword() {
        return adminPassword;
    }
    
    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }
    
    public String getAdminUsername() {
        return adminUsername;
    }
    
    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }
    
    public int getFeatureTypeCacheSize() {
        return featureTypeCacheSize;
    }
    
    public void setFeatureTypeCacheSize(int featureTypeCacheSize) {
        this.featureTypeCacheSize = featureTypeCacheSize;
    }

    public Boolean isGlobalServices() {
        return globalServices;
    }
    
    public void setGlobalServices(Boolean forceVirtualServices) {
        this.globalServices = forceVirtualServices;
    }
    
    public MetadataMap getMetadata() {
        return metadata;
    }

    public void setMetadata(MetadataMap metadata) {
        this.metadata = metadata;
    }

    public Map<Object, Object> getClientProperties() {
        return clientProperties;
    }
    
    public void setClientProperties(Map<Object, Object> properties) {
        this.clientProperties = properties;
    }
    
    public void dispose() {
    }

    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((adminPassword == null) ? 0 : adminPassword.hashCode());
        result = prime * result
                + ((adminUsername == null) ? 0 : adminUsername.hashCode());
        result = prime * result + ((charset == null) ? 0 : charset.hashCode());
        result = prime
                * result
                + ((clientProperties == null) ? 0 : clientProperties.hashCode());
        result = prime * result
                + ((contact == null) ? 0 : contact.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result
                + ((metadata == null) ? 0 : metadata.hashCode());
        result = prime * result + numDecimals;
        result = prime * result
                + ((onlineResource == null) ? 0 : onlineResource.hashCode());
        result = prime * result
                + ((proxyBaseUrl == null) ? 0 : proxyBaseUrl.hashCode());
        result = prime * result
                + ((schemaBaseUrl == null) ? 0 : schemaBaseUrl.hashCode());
        result = prime * result + ((title == null) ? 0 : title.hashCode());
        result = prime * result + updateSequence;
        result = prime * result + (verbose ? 1231 : 1237);
        result = prime * result + (verboseExceptions ? 1231 : 1237);
        result = prime * result + (globalServices ? 1231 : 1237);
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!( obj instanceof GeoServerInfo ) ) {
            return false;
        }
        final GeoServerInfo other = (GeoServerInfo) obj;
        if (adminPassword == null) {
            if (other.getAdminPassword() != null)
                return false;
        } else if (!adminPassword.equals(other.getAdminPassword()))
            return false;
        if (adminUsername == null) {
            if (other.getAdminUsername() != null)
                return false;
        } else if (!adminUsername.equals(other.getAdminUsername()))
            return false;
        if (charset == null) {
            if (other.getCharset() != null)
                return false;
        } else if (!charset.equals(other.getCharset()))
            return false;
        if (contact == null) {
            if (other.getContact() != null)
                return false;
        } else if (!contact.equals(other.getContact()))
            return false;
        if (id == null) {
            if (other.getId() != null)
                return false;
        } else if (!id.equals(other.getId()))
            return false;
        if (numDecimals != other.getNumDecimals())
            return false;
        if (onlineResource == null) {
            if (other.getOnlineResource() != null)
                return false;
        } else if (!onlineResource.equals(other.getOnlineResource()))
            return false;
        if (proxyBaseUrl == null) {
            if (other.getProxyBaseUrl() != null)
                return false;
        } else if (!proxyBaseUrl.equals(other.getProxyBaseUrl()))
            return false;
        if (schemaBaseUrl == null) {
            if (other.getSchemaBaseUrl() != null)
                return false;
        } else if (!schemaBaseUrl.equals(other.getSchemaBaseUrl()))
            return false;
        if (title == null) {
            if (other.getTitle() != null)
                return false;
        } else if (!title.equals(other.getTitle()))
            return false;
        if (updateSequence != other.getUpdateSequence())
            return false;
        if (verbose != other.isVerbose())
            return false;
        if (verboseExceptions != other.isVerboseExceptions())
            return false;
        if (globalServices != other.isGlobalServices())
            return false;
        return true;
    }

    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName()).append('[').append(title).append(']')
                .toString();
    }
    
    
    /*
     * XStream specific method, needed to initialize members that are added over time.
     */
    public Object readResolve() {
        if (this.globalServices == null) {
            this.globalServices = true;
        }
        return this;
    }
    
}
