/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.io.IOException;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogVisitor;
import org.geoserver.catalog.StyleInfo;
import org.geotools.styling.Style;

public class StyleInfoImpl implements StyleInfo {

    protected String id;

    protected String name;

    protected String filename;
    
    protected transient Catalog catalog;

    protected StyleInfoImpl() {
    }
    
    public StyleInfoImpl( Catalog catalog ) {
        this.catalog = catalog;
    }
    
    public void setCatalog(Catalog catalog) {
        this.catalog = catalog;
    }
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFilename() {
        return filename;
    }
    
    public void setFilename(String filename) {
        this.filename = filename;
    }
    
    public Style getStyle() throws IOException {
        return catalog.getResourcePool().getStyle( this );
    }

    public void accept(CatalogVisitor visitor) {
        visitor.visit( this );
    }
    
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((filename == null) ? 0 : filename.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof StyleInfo))
            return false;
        final StyleInfo other = (StyleInfo) obj;
        if (filename == null) {
            if (other.getFilename() != null)
                return false;
        } else if (!filename.equals(other.getFilename()))
            return false;
        if (id == null) {
            if (other.getId() != null)
                return false;
        } else if (!id.equals(other.getId()))
            return false;
        if (name == null) {
            if (other.getName() != null)
                return false;
        } else if (!name.equals(other.getName()))
            return false;
        
        return true;
    }

    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName()).append('[').append(name).append(']')
                .toString();
    }
    
}
