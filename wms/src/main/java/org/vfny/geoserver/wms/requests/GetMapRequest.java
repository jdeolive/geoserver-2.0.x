/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.vfny.geoserver.wms.requests;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.geoserver.ows.util.CaseInsensitiveMap;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geotools.image.palette.InverseColorMapOp;
import org.geotools.styling.Style;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Envelope;


/**
 * Represents a WMS GetMap request. as a extension to the WMS spec 1.1.
 *
 * @author Gabriel Roldan, Axios Engineering
 * @author Simone Giannecchini
 * @version $Id$
 */
public class GetMapRequest extends WMSRequest {
    /** DOCUMENT ME! */
    static final Color DEFAULT_BG = Color.white;

    /** DOCUMENT ME! */
    public static final String SE_XML = "SE_XML";
    private static final String TRANSACTION_REQUEST_TYPE = "GetMap";

    /** set of mandatory request's parameters */
    private MandatoryParameters mandatoryParams = new MandatoryParameters();

    /** set of optionals request's parameters */
    private OptionalParameters optionalParams = new OptionalParameters();

    /** format options */
    private Map /*<String,Object>*/ formatOptions = new CaseInsensitiveMap(new HashMap());

    /** raw kvp parameters non-parsed */
    private Map /*<String,String>*/ rawKvp;
    
    /**
     * Creates a GetMapRequest object.
     * 
     * @param wms The WMS service config.
     */
    public GetMapRequest(WMS wms) {
        super(TRANSACTION_REQUEST_TYPE, wms);
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public Envelope getBbox() {
        return this.mandatoryParams.bbox;
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public java.awt.Color getBgColor() {
        return this.optionalParams.bgColor;
    }

    /**
     * DJB: spec says SRS is *required*, so if they dont specify one, we should throw an error
     *      instead we use "NONE" - which is no-projection.
     *      Previous behavior was to the WSG84 lat/long (4326)
     *
     * @return request CRS, or <code>null</code> if not set.
     * TODO: make CRS manditory as for spec conformance
     */
    public CoordinateReferenceSystem getCrs() {
        return this.optionalParams.crs;
    }

    public String getSRS() {
        return this.optionalParams.srs;
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public String getExceptions() {
        return this.optionalParams.exceptions;
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public String getFormat() {
        return this.mandatoryParams.format;
    }

    /**
     * Map of String,Object which contains kvp's which are specific to a
     * particular output format.
     */
    public Map getFormatOptions() {
        return formatOptions;
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public int getHeight() {
        return this.mandatoryParams.height;
    }

    /**
     * @return the non null list of layers, may be empty
     */
    public MapLayerInfo[] getLayers() {
        List<MapLayerInfo> layers = mandatoryParams.layers;
        return layers.toArray(new MapLayerInfo[layers.size()]);
    }

    /**
     * Gets a list of the styles to be returned by the server.
     *
     * @return A list of {@link Style}
     */
    public List<Style> getStyles() {
        return this.mandatoryParams.styles;
    }

    /**
     * Gets the url specified by the "SLD" parameter.
     */
    public URL getSld() {
        return this.optionalParams.sld;
    }

    /**
     * Gets the string specified the "SLD_BODY" parameter.
     */
    public String getSldBody() {
        return this.optionalParams.sldBody;
    }

    /**
     * Gets the value of the "VALIDATESCHEMA" parameter which controls wether
     * the value of the "SLD paramter is schema validated.
     */
    public Boolean getValidateSchema() {
        return this.optionalParams.validateSLD;
    }

    /**
     * Gets a list of the the filters that will be applied to each layer before rendering
     *
     * @return -
     * @deprecated use {@link #getFilter()}.
     */
    public List getFilters() {
        return this.optionalParams.filters;
    }

    /**
     * Gets a list of the the filters that will be applied to each layer before rendering
     *
     * @return  A list of {@link Filter}.
     *
     */
    public List getFilter() {
        return this.optionalParams.filters;
    }

    /**
     * Gets a list of the cql filtesr that will be applied to each layer before
     * rendering.
     *
     * @return A list of {@link Filter}.
     *
     */
    public List getCQLFilter() {
        return this.optionalParams.cqlFilters;
    }

    /**
     * Gets a list of the feature ids that will be used to filter each layer
     * before rendering.
     *
     * @return A list of {@link String}.
     */
    public List getFeatureId() {
        return this.optionalParams.featureIds;
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     *
     */
    public boolean isTransparent() {
        return this.optionalParams.transparent;
    }

    /**
     * <a href="http://wiki.osgeo.org/index.php/WMS_Tiling_Client_Recommendation">WMS-C specification</a> tiling hint
     *
     */
    public boolean isTiled() {
        return this.optionalParams.tiled;
    }

    public Point2D getTilesOrigin() {
        return this.optionalParams.tilesOrigin;
    }

    public int getBuffer() {
        return this.optionalParams.buffer;
    }

	public InverseColorMapOp getPalette() {
		return this.optionalParams.paletteInverter;
	}

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public int getWidth() {
        return this.mandatoryParams.width;
    }

    /**
     * @return the KML/KMZ score value for image vs. vector response
     * @deprecated use <code>getFormatOptions().get( "kmscore" )</code>
     */
    public int getKMScore() {
        Integer kmscore = (Integer) getFormatOptions().get("kmscore");

        if (kmscore != null) {
            return kmscore.intValue();
        }

        return 40; //old default
    }

    /**
     * @return true: return full attribution for placemark <description>
     * @deprecated use <code>getFormatOptions().get( "kmattr" )</code>
     */
    public boolean getKMattr() {
        Boolean kmattr = (Boolean) getFormatOptions().get("kmattr");

        if (kmattr != null) {
            return kmattr.booleanValue();
        }

        return true; //old default
    }

//    /**
//     * @return super overlay flag, <code>true</code> if super overlay requested.
//     * @deprecated use <code>getFormatOptions().get( "superoverlay" )</code>
//     */
//    public boolean getSuperOverlay() {
//        Boolean superOverlay = (Boolean) getFormatOptions().get("superoverlay");
//
//        if (superOverlay != null) {
//            return superOverlay.booleanValue();
//        }
//
//        return false; //old default
//    }

    /**
     * @return kml legend flag, <code>true</code> if legend is enabled.
     * @deprecated use <code>getFormatOptions().get( "legend" )</code>
     */
    public boolean getLegend() {
        Boolean legend = (Boolean) getFormatOptions().get("legend");

        if (legend != null) {
            return legend.booleanValue();
        }

        return false; //old default
    }

    /**
     * @return The time request parameter.
     */
    public List getTime() {
        return this.optionalParams.time;
    }

    /**
     * @return The elevation request parameter.
     */
    public Integer getElevation() {
        return this.optionalParams.elevation;
    }

    /**
     * Returs the feature version optional parameter
     * @return
     */
    public String getFeatureVersion() {
        return this.optionalParams.featureVersion;
    }
    
    /**
     * Returns the remote OWS type
     * @return
     */
    public String getRemoteOwsType() {
        return optionalParams.remoteOwsType;
    }

    /**
     * Returs the remote OWS URL
     * @return
     */
    public URL getRemoteOwsURL() {
        return optionalParams.remoteOwsURL;
    }

    /**
     * Gets the raw kvp parameters which were used to create the request.
     */
    public Map getRawKvp() {
        return rawKvp;
    }
    
    /**
     * DOCUMENT ME!
     *
     * @param bbox DOCUMENT ME!
     */
    public void setBbox(Envelope bbox) {
        this.mandatoryParams.bbox = bbox;
    }

    /**
     * DOCUMENT ME!
     *
     * @param bgColor DOCUMENT ME!
     */
    public void setBgColor(java.awt.Color bgColor) {
        this.optionalParams.bgColor = bgColor;
    }

    /**
     * DOCUMENT ME!
     *
     * @param crs DOCUMENT ME!
     */
    public void setCrs(CoordinateReferenceSystem crs) {
        this.optionalParams.crs = crs;
    }

    /**
     * DOCUMENT ME!
     *
     * @param crs DOCUMENT ME!
     */
    public void setSRS(String srs) {
        this.optionalParams.srs = srs;
    }

    /**
     * DOCUMENT ME!
     *
     * @param exceptions DOCUMENT ME!
     */
    public void setExceptions(String exceptions) {
        this.optionalParams.exceptions = exceptions;
    }

    /**
     * Sets the GetMap request value for the FORMAT parameter, which is
     * the MIME type for the kind of image required.
     *
     * @param format DOCUMENT ME!
     */
    public void setFormat(String format) {
        this.mandatoryParams.format = format;
    }

    /**
     * Sets the format options.
     *
     * @param formatOptions A map of String,Object
     * @see #getFormatOptions()
     */
    public void setFormatOptions(Map formatOptions) {
        this.formatOptions = formatOptions;
    }

    /**
     * DOCUMENT ME!
     *
     * @param height DOCUMENT ME!
     */
    public void setHeight(int height) {
        this.mandatoryParams.height = height;
    }

    public void setHeight(Integer height) {
        this.mandatoryParams.height = height.intValue();
    }

    /**
     * DOCUMENT ME!
     *
     * @param layers DOCUMENT ME!
     */
    public void setLayers(MapLayerInfo[] layers) {
        this.mandatoryParams.layers = layers == null ? Collections.EMPTY_LIST : Arrays
                .asList(layers);
    }

    /**
     * DOCUMENT ME!
     *
     * @param styles List&lt;org.geotools.styling.Style&gt;
     */
    public void setStyles(List<Style> styles) {
        this.mandatoryParams.styles = styles == null? Collections.EMPTY_LIST : new ArrayList<Style>(styles);
    }

    /**
     * Sets the url specified by the "SLD" parameter.
     */
    public void setSld(URL sld) {
        this.optionalParams.sld = sld;
    }

    /**
     * Sets the string specified by the "SLD_BODY" parameter
     */
    public void setSldBody(String sldBody) {
        this.optionalParams.sldBody = sldBody;
    }

    /**
     * Sets the flag to validate the "SLD" parameter or not.
     * //TODO
     */
    public void setValidateSchema(Boolean validateSLD) {
        this.optionalParams.validateSLD = validateSLD;
    }

    /**
     * Sets a list of filters, one for each layer
     *
     * @param filters A list of {@link Filter}.
     * @deprecated use {@link #setFilter(List)}.
     */
    public void setFilters(List filters) {
        setFilter(filters);
    }

    /**
     * Sets a list of filters, one for each layer
     *
     * @param filters A list of {@link Filter}.
     */
    public void setFilter(List filters) {
        this.optionalParams.filters = filters;
    }

    /**
     * Sets a list of filters ( cql ), one for each layer.
     *
     * @param cqlFilters A list of {@link Filter}.
     */
    public void setCQLFilter(List cqlFilters) {
        this.optionalParams.cqlFilters = cqlFilters;
    }

    /**
     * Sets a list of feature ids, one for each layer.
     *
     * @param featureIds A list of {@link String}.
     */
    public void setFeatureId(List featureIds) {
        this.optionalParams.featureIds = featureIds;
    }

    /**
     * DOCUMENT ME!
     *
     * @param transparent DOCUMENT ME!
     */
    public void setTransparent(boolean transparent) {
        this.optionalParams.transparent = transparent;
    }

    public void setTransparent(Boolean transparent) {
        this.optionalParams.transparent = (transparent != null) ? transparent.booleanValue() : false;
    }

    public void setBuffer(int buffer) {
        this.optionalParams.buffer = buffer;
    }

	public void setPalette(InverseColorMapOp paletteInverter) {
		this.optionalParams.paletteInverter = paletteInverter;
	}
    public void setBuffer(Integer buffer) {
        this.optionalParams.buffer = (buffer != null) ? buffer.intValue() : 0;
    }


    public void setTiled(boolean tiled) {
        this.optionalParams.tiled = tiled;
    }

    public void setTiled(Boolean tiled) {
        this.optionalParams.tiled = (tiled != null) ? tiled.booleanValue() : false;
    }

    public void setTilesOrigin(Point2D origin) {
        this.optionalParams.tilesOrigin = origin;
    }

    /**
     * DOCUMENT ME!
     *
     * @param width DOCUMENT ME!
     */
    public void setWidth(int width) {
        this.mandatoryParams.width = width;
    }

    public void setWidth(Integer width) {
        this.mandatoryParams.width = width.intValue();
    }

    /**
     * @param score the KML/KMZ score value for image vs. vector response, from 0 to 100
     * @deprecated use <code>getFormatOptions().put( "kmscore", new Integer( score ) );</code>
     */
    public void setKMScore(int score) {
        getFormatOptions().put("kmscore", new Integer(score));
    }

    /**
     * @param on true: full attribution; false: no attribution
     * @deprecated use <code>getFormatOptions().put( "kmattr", new Boolean( on ) );</code>
     */
    public void setKMattr(boolean on) {
        getFormatOptions().put("kmattr", new Boolean(on));
    }

    /**
     * Sets the super overlay parameter on the request.
     * @deprecated use <code>getFormatOptions().put( "superoverlay", new Boolean( superOverlay ) );</code>
     */
    public void setSuperOverlay(boolean superOverlay) {
        getFormatOptions().put("superoverlay", new Boolean(superOverlay));
    }

    /**
     * Sets the kml legend parameter of the request.
     * @deprecated use <code>getFormatOptions().put( "legend", new Boolean( legend ) );</code>
     */
    public void setLegend(boolean legend) {
        getFormatOptions().put("legend", new Boolean(legend));
    }

    /**
     * Sets the time request parameter.
     *
     */
    public void setTime(List time) {
        this.optionalParams.time = time;
    }

    /**
     * Sets the elevation request parameter.
     */
    public void setElevation(Integer elevation) {
        this.optionalParams.elevation = elevation;
    }

    /**
     * Sets the feature version optional param
     * @param featureVersion
     */
    public void setFeatureVersion(String featureVersion) {
        this.optionalParams.featureVersion = featureVersion;
    }
    
    public void setRemoteOwsType(String remoteOwsType) {
        this.optionalParams.remoteOwsType = remoteOwsType;
    }

    public void setRemoteOwsURL(URL remoteOwsURL) {
        this.optionalParams.remoteOwsURL = remoteOwsURL;
    }

    /**
     * Sets the maximum number of features to fetch in this request.
     * <p>
     * This property only applies if the reqeust is for a vector layer.
     * </p>
     */
    public void setMaxFeatures( Integer maxFeatures ) {
        this.optionalParams.maxFeatures = maxFeatures;
    }
    
    /**
     * The maximum number of features to fetch in this request.
     */
    public Integer getMaxFeatures() {
        return this.optionalParams.maxFeatures;
    }

    /**
     * Sets the offset or start index at which to start returning features in 
     * the request.
     * <p>
     * It is used in conjunction with {@link #getMaxFeatures()} to page through
     * a feature set. This property only applies if the request is for a vector 
     * layer.
     * </p>
     */
    public void setStartIndex( Integer startIndex ) {
        this.optionalParams.startIndex = startIndex;
    }
    
    /**
     * The offset or start index at which to start returning features in 
     * the request.
     */
    public Integer getStartIndex() {
        return this.optionalParams.startIndex;
    }

    /**
     * Sets the raw kvp parameters which were used to create the request.
     */
    public void setRawKvp( Map rawKvp ) {
        this.rawKvp = rawKvp;
    }
    
    /**
     * DOCUMENT ME!
     *
     * @author Gabriel Roldan, Axios Engineering
     * @version $Id$
     */
    private class MandatoryParameters {
        /** ordered list of requested layers */
        List<MapLayerInfo> layers = Collections.emptyList();

        /**
         * ordered list of requested layers' styles, in a one to one
         * relationship with <code>layers</code>
         */
        List<Style> styles = Collections.emptyList();

        /** DOCUMENT ME!  */
        Envelope bbox;

        /** DOCUMENT ME!  */
        int width;

        /** DOCUMENT ME!  */
        int height;

        /** DOCUMENT ME!  */
        String format;
    }

    /**
     * DOCUMENT ME!
     *
     * @author Gabriel Roldan, Axios Engineering
     * @version $Id$
     */
    private class OptionalParameters {
        /**
         * the map's background color requested, or the default (white) if not
         * specified
         */
        Color bgColor = DEFAULT_BG;

        /** from SRS (1.1) or CRS (1.2) param */
        CoordinateReferenceSystem crs;

        /** EPSG code for the SRS */
        String srs;

        /** vendor extensions, allows to filter each layer with a user defined filter */
        List filters;

        /** cql filters */
        List cqlFilters;

        /** feature id filters */
        List featureIds;

        /** DOCUMENT ME!  */
        String exceptions = SE_XML;

        /** DOCUMENT ME!  */
        boolean transparent = false;

        /**
         * Tiling hint, according to the
         * <a href="http://wiki.osgeo.org/index.php/WMS_Tiling_Client_Recommendation">WMS-C specification</a>
         */
        boolean tiled;

        /**
         * Temporary hack since finding a good tiling origin would require us to compute
         * the bbox on the fly
         * TODO: remove this once we cache the real bbox of vector layers
         */
        public Point2D tilesOrigin;

        /** the rendering buffer, in pixels **/
        int buffer;

		/** The paletteInverter used for rendering, if any */
		InverseColorMapOp paletteInverter;

        /** time elevation parameter, a list since many pattern setup can be possible, see
         *  for example http://mapserver.gis.umn.edu/docs/howto/wms_time_support/#time-patterns */
        List time;

        /** time elevation parameter */
        Integer elevation;

        /**
         * SLD parameter
         */
        URL sld;

        /**
         * SLD_BODY parameter
         */
        String sldBody;

        /** flag to validate SLD parameter */
        Boolean validateSLD = Boolean.FALSE;

        /** feature version (for versioned requests) */
        String featureVersion;
        
        /** Remote OWS type */
        String remoteOwsType;
        
        /** Remote OWS url */
        URL remoteOwsURL;
        
        /** paging parameters */
        Integer maxFeatures;
        Integer startIndex;
    }

    /**
     * Standard override of toString()
     *
     * @return a String representation of this request.
     */
    public String toString() {
        StringBuffer returnString = new StringBuffer("\nGetMap Request");
        returnString.append("\n version: " + version);
        returnString.append("\n output format: " + mandatoryParams.format);
        returnString.append("\n width height: " + mandatoryParams.height + ","
            + mandatoryParams.width);
        returnString.append("\n bbox: " + mandatoryParams.bbox);
        returnString.append("\n layers: ");

        for (Iterator<MapLayerInfo> i = mandatoryParams.layers.iterator();i.hasNext();) {
            returnString.append(i.next().getName());
            if (i.hasNext()) {
                returnString.append(",");
            }
        }

        returnString.append("\n styles: ");

        for (Iterator it = mandatoryParams.styles.iterator(); it.hasNext();) {
            Style s = (Style) it.next();
            returnString.append(s.getName());

            if (it.hasNext()) {
                returnString.append(",");
            }
        }

        //returnString.append("\n inside: " + filter.toString());
        return returnString.toString();
    }
}
