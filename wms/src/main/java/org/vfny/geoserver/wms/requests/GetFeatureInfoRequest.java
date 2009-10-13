/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.vfny.geoserver.wms.requests;

import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.vfny.geoserver.wms.servlets.WMService;


/**
 * Represents a WMS 1.1.1 GetFeatureInfo request.
 * <p>
 * The "GetMap" part of the request is represented by a
 * <code>GetMapRequest</code> object by itself. It is
 * intended to provide enough map context information about
 * the map over the GetFeatureInfo request is performed.
 * </p>
 *
 * @author Gabriel Roldan, Axios Engineering
 * @version $Id$
 */
public class GetFeatureInfoRequest extends WMSRequest {
    private static final String DEFAULT_EXCEPTION_FORMAT = "application/vnd.ogc.se_xml";
    private static final int DEFAULT_MAX_FEATURES = 1;

    /**
     * Holds the GetMap part of the GetFeatureInfo request, wich is meant
     * to provide enough context information about the map over the
     * GetFeatureInfo request is being made.
     */
    private GetMapRequest getMapRequest;

    /**
     * List of FeatureTypeInfo's parsed from the <code>QUERY_LAYERS</code>
     * mandatory parameter.
     */
    private MapLayerInfo[] queryLayers;

    /**
     * Holder for the <code>INFO_FORMAT</code> optional parameter
     */
    private String infoFormat;

    /**
     * Holder for the <code>FEATURE_COUNT</code> optional parameter.
     * Deafults to 1.
     */
    private int featureCount = DEFAULT_MAX_FEATURES;

    /**
     * Holds the value of the required <code>X</code> parameter
     */
    private int XPixel;

    /**
     * Holds the value of the requiered <code>Y</code> parameter
     */
    private int YPixel;

    /**
     * Holder for the optional <code>EXCEPTIONS</code> parameter,
     * defaults to <code>"application/vnd.ogc.se_xml"</code>
     */
    private String exeptionFormat = DEFAULT_EXCEPTION_FORMAT;

    /**
     * Creates a new GetMapRequest object.
     * @param service The service that will handle the request
     * 
     * @deprecated use {@link #GetFeatureInfoRequest(WMS)}
     */
//    public GetFeatureInfoRequest(WMService service) {
//        this(service.getWMS());
//        //super("GetFeatureInfo", service);
//    }

    /**
     * Creates a new GetFeatureInfoRequest object.
     * @param wms The WMS configuration object.
     */
    public GetFeatureInfoRequest(WMS wms) {
        super("GetFeatureInfo", wms);
    }
    
    /**
     * @return Returns the exeptionFormat.
     */
    public String getExeptionFormat() {
        return exeptionFormat;
    }

    /**
     * @param exeptionFormat The exeptionFormat to set.
     */
    public void setExeptionFormat(String exeptionFormat) {
        this.exeptionFormat = exeptionFormat;
    }

    /**
     * @return Returns the featureCount.
     */
    public int getFeatureCount() {
        return featureCount;
    }

    /**
     * @param featureCount The featureCount to set.
     */
    public void setFeatureCount(int featureCount) {
        this.featureCount = featureCount;
    }

    /**
     * @return Returns the getMapRequest.
     */
    public GetMapRequest getGetMapRequest() {
        return getMapRequest;
    }

    /**
     * @param getMapRequest The getMapRequest to set.
     */
    public void setGetMapRequest(GetMapRequest getMapRequest) {
        this.getMapRequest = getMapRequest;
    }

    /**
     * @return Returns the infoFormat.
     */
    public String getInfoFormat() {
        return infoFormat;
    }

    /**
     * @param infoFormat The infoFormat to set.
     */
    public void setInfoFormat(String infoFormat) {
        this.infoFormat = infoFormat;
    }

    /**
     * @return Returns the queryLayers.
     */
    public MapLayerInfo[] getQueryLayers() {
        return queryLayers;
    }

    /**
     * @param queryLayers The queryLayers to set.
     */
    public void setQueryLayers(MapLayerInfo[] queryLayers) {
        this.queryLayers = queryLayers;
    }

    /**
     * @return Returns the xPixel.
     */
    public int getXPixel() {
        return XPixel;
    }

    /**
     * @param pixel The xPixel to set.
     */
    public void setXPixel(int pixel) {
        XPixel = pixel;
    }

    /**
     * @return Returns the yPixel.
     */
    public int getYPixel() {
        return YPixel;
    }

    /**
     * @param pixel The yPixel to set.
     */
    public void setYPixel(int pixel) {
        YPixel = pixel;
    }
}
