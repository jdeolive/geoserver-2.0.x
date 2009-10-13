/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.layer.Grid;
import org.geowebcache.layer.GridCalculator;
import org.geowebcache.layer.SRS;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.TileLayerDispatcher;
import org.geowebcache.layer.wms.WMSLayer;
import org.geowebcache.util.ApplicationContextProvider;
import org.geowebcache.util.Configuration;
import org.geowebcache.util.GeoServerConfiguration;
import org.geowebcache.util.wms.BBOX;


/**
 * This class acts as a source of TileLayer objects for GeoWebCache.
 * 
 * @author Arne Kepp / OpenGeo 2009
 */
public class GWCCatalogListener implements CatalogListener, Configuration {
    private static Logger log = Logging.getLogger("org.geoserver.gwc");
    
    protected Catalog cat = null;
    
    protected TileLayerDispatcher layerDispatcher = null;
    
    private List<String> mimeFormats = null;
    
    private int[] metaFactors = {4,4};
    
    private String wmsUrl = null;
    
    ArrayList<TileLayer> list;
   
    //TODO Maybe all this coverageinfo business is a waster of time?
    
    /**
     * Constructor for Spring
     * 
     * @param cat
     * @param layerDispatcher
     * @param ctxProv
     */
    public GWCCatalogListener(Catalog cat, TileLayerDispatcher layerDispatcher, ApplicationContextProvider ctxProv) {
        this.cat = cat;
        this.layerDispatcher = layerDispatcher;
        
        mimeFormats = new ArrayList<String>(5);
        mimeFormats.add("image/png");
        mimeFormats.add("image/gif");
        mimeFormats.add("image/png8");
        mimeFormats.add("image/jpeg"); 
        mimeFormats.add("application/vnd.google-earth.kml+xml");
        
        wmsUrl = ctxProv.getSystemVar(
                GeoServerConfiguration.GEOSERVER_WMS_URL, 
                "http://localhost:8080/geoserver/wms" );
        
        cat.addListener(this);
        
        log.fine("GWCCatalogListener registered with catalog");
    }
    
    /**
     * Handles when a layer is added to the catalog
     */
    public void handleAddEvent(CatalogAddEvent event) {
        Object obj = event.getSource();
        
        WMSLayer wmsLayer = null;
        
        if(obj instanceof CoverageInfo) {
            CoverageInfo covInfo = (CoverageInfo) obj;
            wmsLayer = getLayer(covInfo);
        }
        if(obj instanceof ResourceInfo) {
            ResourceInfo resInfo = (ResourceInfo) obj;
            wmsLayer = getLayer(resInfo);
        }
        
        if (wmsLayer != null && this.list != null) {
            addToList(wmsLayer);
            layerDispatcher.getLayers();
            layerDispatcher.add(wmsLayer);
            log.finer(wmsLayer.getName() + " added to TileLayerDispatcher");
        }
    }
    
    public void handleModifyEvent(CatalogModifyEvent event) {
        // We don't really care about this one, 
        // though we could clear the cache on style change
    }
    
    public void handleRemoveEvent(CatalogRemoveEvent event) { 
        Object obj = event.getSource();
        
        //String name = null; //getLayerName(obj);
        
        WMSLayer wmsLayer = null;
        
        if(obj instanceof CoverageInfo) {
            CoverageInfo covInfo = (CoverageInfo) obj;
            wmsLayer = getLayer(covInfo);
        }
        if(obj instanceof ResourceInfo) {
            ResourceInfo resInfo = (ResourceInfo) obj;
            wmsLayer = getLayer(resInfo);
        }
        
        if(wmsLayer != null && this.list != null) {
            removeFromList(wmsLayer);
            layerDispatcher.getLayers();
            layerDispatcher.remove(wmsLayer.getName());
            log.finer(wmsLayer.getName() + " removed from TileLayerDispatcher");
        }
    }

    public void handlePostModifyEvent(CatalogPostModifyEvent event) {
        Object obj = event.getSource();

        WMSLayer wmsLayer = null; //getLayer(obj);

        if(obj instanceof CoverageInfo) {
            CoverageInfo covInfo = (CoverageInfo) obj;
            wmsLayer = getLayer(covInfo);
        }
        if(obj instanceof ResourceInfo) {
            ResourceInfo resInfo = (ResourceInfo) obj;
            wmsLayer = getLayer(resInfo);
        }
        if(obj instanceof LayerGroupInfo) {
            LayerGroupInfo lgInfo = (LayerGroupInfo) obj;
            wmsLayer = getLayer(lgInfo);
        }
        
        if (wmsLayer != null && this.list != null) {
            updateList(wmsLayer);
            layerDispatcher.getLayers();
            layerDispatcher.update(wmsLayer);
            log.finer(wmsLayer.getName() + " updated on TileLayerDispatcher");
        }
    }

    public void reloaded() {
        try {
            layerDispatcher.reInit();
        } catch (GeoWebCacheException gwce) {
            log.fine("Unable to reinit TileLayerDispatcher gwce.getMessage()");
        }
    }

    public String getIdentifier() throws GeoWebCacheException {
        return "GeoServer Catalog Listener";
    }

    public synchronized List<TileLayer> getTileLayers(boolean reload) 
    throws GeoWebCacheException {
        
        if(! reload && list != null) {
            return list;
        }
        
        list = new ArrayList<TileLayer>(cat.getLayers().size());
        
        // Adding vector layers
        Iterator<LayerInfo> lIter = cat.getLayers().iterator();
        while(lIter.hasNext()) {
            LayerInfo li = lIter.next();            
            TileLayer tl = getLayer(li.getResource());
            //System.out.println(tl.getName() + " layerinfo");
            list.add(tl);
        }
        
        /** These seem to get duplicated as layerinfo objects anyway **/
        // Adding raster layers
        //Iterator<CoverageInfo> cIter = cat.getCoverages().iterator();
        //while(cIter.hasNext()) {
        //    CoverageInfo ci = cIter.next();
        //    TileLayer tl = getLayer(ci);
        //    System.out.println(tl.getName() + " coverageinfo");
        //    list.add(tl);
        //}
        
        // Adding layer groups 
        Iterator<LayerGroupInfo> lgIter = cat.getLayerGroups().iterator();
        while(lgIter.hasNext()) {
            LayerGroupInfo lgi = lgIter.next();
            
            TileLayer tl = getLayer(lgi);
            //System.out.println(tl.getName() + " layergroupinfo");
            list.add(tl);
        }
        
        log.fine("Responding with " + list.size() + " to getTileLayers() request from TileLayerDispatcher");
        
        return list;
    }
    
    synchronized private void updateList(WMSLayer wmsLayer) {
        if(this.list != null) {
            removeFromList(wmsLayer);
            addToList(wmsLayer);
        }
    }
    
    synchronized private void removeFromList(WMSLayer wmsLayer) {
        if(this.list != null) {
            Iterator<TileLayer> iter = list.iterator();
            int i = 0;
            while(iter.hasNext()) {
                TileLayer tl = iter.next();
                if(tl.getName().equals(wmsLayer.getName())) {
                    list.remove(i);
                }
                i++;
            }
        }
    }
    
    synchronized private void addToList(WMSLayer wmsLayer) {
        if(this.list != null) {
            list.add(wmsLayer);
        }
    }
    
    //private String getLayerName(Object obj) {
    //    return getLayer(obj).getName();
    //}
    
    //private WMSLayer getLayer(Object obj) {
    //    
    //}
    
    private WMSLayer getLayer(LayerGroupInfo lgi) {
        ReferencedEnvelope latLonBounds = null;
        try {
            latLonBounds = lgi.getBounds().transform(CRS.decode("EPSG:4326"), true);
        } catch (Exception e) {
            log.warning(e.getMessage());
        }
        
        if(latLonBounds == null) {
            log.fine("GWCCatalogListener had problems reprojecting " 
                    + lgi.getBounds() + " to EPSG:4326");
        }
        
        WMSLayer retLayer = new WMSLayer(
                lgi.getName(),
                getWMSUrl(),
                null, // Styles 
                lgi.getName(), 
                mimeFormats, 
                getGrids(latLonBounds), 
                metaFactors,
                null,
                true);
        
        retLayer.setBackendTimeout(120);
        return retLayer;
    }
    
    private WMSLayer getLayer(ResourceInfo fti) {
        WMSLayer retLayer = new WMSLayer(
                fti.getPrefixedName(),
                getWMSUrl(), 
                null, // Styles 
                fti.getPrefixedName(), 
                mimeFormats, 
                getGrids(fti.getLatLonBoundingBox()), 
                metaFactors,
                null,
                true);
        retLayer.setBackendTimeout(120);
        return retLayer;
    }
    
    private WMSLayer getLayer(CoverageInfo ci) {
        WMSLayer retLayer = new WMSLayer(
                ci.getPrefixedName(),
                getWMSUrl(), 
                null, // Styles 
                ci.getPrefixedName(), 
                mimeFormats, 
                getGrids(ci.getLatLonBoundingBox()), 
                metaFactors,
                null, 
                false);
        
        retLayer.setBackendTimeout(120);
        return retLayer;   
    }

    private String[] getWMSUrl() {
        String[] strs = { wmsUrl };
        return strs;
    }
    
    private Hashtable<SRS,Grid> getGrids(ReferencedEnvelope env) {
        double minX = env.getMinX();
        double minY = env.getMinY();
        double maxX = env.getMaxX();
        double maxY = env.getMaxY();

        BBOX bounds4326 = new BBOX(minX,minY,maxX,maxY);
 
        BBOX bounds900913 = new BBOX(
                longToSphericalMercatorX(minX),
                latToSphericalMercatorY(minY),
                longToSphericalMercatorX(maxX),
                latToSphericalMercatorY(maxY));
        
        Hashtable<SRS,Grid> grids = new Hashtable<SRS,Grid>(2);
        
        grids.put(SRS.getEPSG4326(), new Grid(SRS.getEPSG4326(), bounds4326, 
                BBOX.WORLD4326, GridCalculator.get4326Resolutions()));
        grids.put(SRS.getEPSG900913(), new Grid(SRS.getEPSG900913(), bounds900913,
                BBOX.WORLD900913, GridCalculator.get900913Resolutions()));
       
        return grids;
    }
    
    private double longToSphericalMercatorX(double x) {
        return (x/180.0)*20037508.34;
    }
    
    private double latToSphericalMercatorY(double y) {        
        if(y > 85.05112) {
            y = 85.05112;
        }
        
        if(y < -85.05112) {
            y = -85.05112;
        }
        
        y = (Math.PI/180.0)*y;
        double tmp = Math.PI/4.0 + y/2.0; 
        return 20037508.34 * Math.log(Math.tan(tmp)) / Math.PI;
    }
}


