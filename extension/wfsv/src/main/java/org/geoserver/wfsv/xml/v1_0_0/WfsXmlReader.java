/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfsv.xml.v1_0_0;

import org.geoserver.catalog.Catalog;
import org.geoserver.platform.ExtensionPriority;
import org.geotools.xml.Configuration;

/**
 * Readers for plain WFS queries that need to use versioning extended elements
 * @author Andrea Aime - TOPP
 *
 */
public class WfsXmlReader extends org.geoserver.wfs.xml.v1_0_0.WfsXmlReader implements ExtensionPriority {

    public WfsXmlReader(String element, Configuration configuration, Catalog catalog) {
        super(element, configuration, catalog, "wfsv");
    }

    /**
     * Pump up the priority of this reader so that it gets used in place of the WFS one,
     * as we have a configuration that contains the extra transaction elements
     */
    public int getPriority() {
        return ExtensionPriority.HIGHEST;
    }
}
