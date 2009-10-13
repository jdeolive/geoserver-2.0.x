/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.IOException;

import org.geotools.styling.Style;

/**
 * A style for a geospatial resource.
 * 
 * @author Justin Deoliveira, The Open Planning project
 */
public interface StyleInfo extends CatalogInfo {

    /**
     * Name of the default point style.
     */
    public static String DEFAULT_POINT = "point";
    /**
     * Name of the default line style.
     */
    public static String DEFAULT_LINE = "line";
    /**
     * Name of the default polygon style.
     */
    public static String DEFAULT_POLYGON = "polygon";
    /**
     * Name of the default raster style. 
     */
    public static String DEFAULT_RASTER = "raster";

    
    /**
     * Name of the style.
     * <p>
     * This value is unique among all styles and can be used to identify the
     * style.
     * </p>
     * 
     * @uml.property name="name"
     */
    String getName();

    /**
     * Sets the name of the style.
     * 
     * @uml.property name="name"
     */
    void setName(String name);

    /**
     * The name of the file the style originates from.
     */
    String getFilename();

    /**
     * Sets the name of the file the style originated from.
     */
    void setFilename( String fileName );
    
    /**
     * The style object.
     */
    Style getStyle() throws IOException;
    
}
