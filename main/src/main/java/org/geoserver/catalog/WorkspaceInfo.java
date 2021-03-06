/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.Serializable;
import java.util.Map;

/**
 * A container of grouping for store objects.
 * 
 * @author Justin Deoliveira, The Open Planning Project
 *
 */
public interface WorkspaceInfo extends CatalogInfo {

    /**
     * The unique name of the workspace.
     */
    String getName();
    
    /**
     * Sets the name of the workspace.
     */
    void setName( String name );
    
    /**
     * A persistent map of metadata.
     * <p>
     * Data in this map is intended to be persisted. Common case of use is to
     * have services associate various bits of data with a particular workspace.
     * </p>
     * 
     */
    MetadataMap getMetadata();
}
