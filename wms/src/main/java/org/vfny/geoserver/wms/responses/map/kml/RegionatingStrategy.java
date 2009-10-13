package org.vfny.geoserver.wms.responses.map.kml;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geotools.map.MapLayer;
import org.opengis.filter.Filter;
import org.vfny.geoserver.wms.WMSMapContext;

/**
 * Common interface for classes defining a mechanism for regionating KML placemarks.  
 * @author David Winslow
 * @author Andrea Aime
 */
public interface RegionatingStrategy {
    /**
     * Given the KML request context, asks the strategy to return a filter matching only
     * the features that have to be included in the output. 
     * An SLD based strategy will use the current scale, a tiling based one the area occupied
     * by the requested tile and some criteria to fit in features, and so on. 
     * @param context
     * @param layer
     */
    public Filter getFilter(WMSMapContext context, MapLayer layer);

    /**
     * Clear any cached work (indexing, etc.) for a particular feature type's default regionating 
     * options.
     */
    public void clearCache(FeatureTypeInfo cfg);
}
