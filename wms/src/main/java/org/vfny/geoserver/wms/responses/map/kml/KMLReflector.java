/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.vfny.geoserver.wms.responses.map.kml;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WebMapService;
import org.vfny.geoserver.wms.WmsException;
import org.vfny.geoserver.wms.requests.GetMapRequest;
import org.vfny.geoserver.wms.responses.GetMapResponse;


/**
 * KML reflecting service.
 * <p>
 * This
 * </p>
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 *
 */
public class KMLReflector {
    private static Logger LOGGER = 
        org.geotools.util.logging.Logging.getLogger("org.vfny.geoserver.wms.responses.map.kml");

    /** default 'kmscore' value */
    public static final Integer KMSCORE = new Integer(50);

    /** default 'kmattr' value */
    public static final Boolean KMATTR = Boolean.TRUE;

    /** default 'kmplacemark' value */
    public static final Boolean KMPLACEMARK = Boolean.FALSE;

    /** default 'format' value */
    public static final String FORMAT = KMLMapProducer.MIME_TYPE;

    private static Map<String, Map<String, String> > MODES;

    static {
        Map temp = new HashMap();
        Map options;
        
        options = new HashMap();
        options.put("superoverlay", true);
        temp.put("superoverlay", options);

        options = new HashMap();
        options.put("superoverlay", false);
        options.put("regionatemode", null);
        options.put("kmscore", null);
        temp.put("download", options);

        options = new HashMap();
        options.put("superoverlay", false);
        temp.put("refresh", options);

        MODES = temp;
    }

    /**
     * web map service
     */
    WebMapService wms;

    public KMLReflector(WebMapService wms) {
        this.wms = wms;
    }

    public void wms(GetMapRequest request, HttpServletResponse response) throws Exception {
        doWms(request, response, wms);
    }
        
    public static void doWms(GetMapRequest request, HttpServletResponse response, WebMapService wms)
        throws Exception {
            //set the content disposition
        StringBuffer filename = new StringBuffer();
        boolean containsRasterData = false;
        boolean isRegionatingFriendly = true;
        for ( int i = 0; i < request.getLayers().length; i++ ) {
            MapLayerInfo layer = request.getLayers()[i];
            String name = layer.getName();
 
            containsRasterData = containsRasterData || (layer.getType() == MapLayerInfo.TYPE_RASTER);

            if (layer.getType() == MapLayerInfo.TYPE_VECTOR) {
                isRegionatingFriendly = isRegionatingFriendly && 
                    layer.getFeature().getFeatureSource(null,null).getQueryCapabilities().isReliableFIDSupported(); 
            } else if (layer.getType() == MapLayerInfo.TYPE_REMOTE_VECTOR) {
                isRegionatingFriendly = isRegionatingFriendly &&
                    layer.getRemoteFeatureSource().getQueryCapabilities().isReliableFIDSupported();
            }

            //strip off prefix
            int j = name.indexOf(':');
            if ( j > -1 ) {
                name = name.substring( j + 1 );
            }

            filename.append(name + "_");
        }


        String mode = caseInsensitiveParam(
                request.getHttpServletRequest().getParameterMap(), 
                "mode", "refresh");

        if (!MODES.containsKey(mode)){
            throw new WmsException("Unknown KML mode: " + mode);
        }

        Map modeOptions = new HashMap(MODES.get(mode));


        if ("superoverlay".equals(mode)){
            String submode = caseInsensitiveParam(
                request.getHttpServletRequest().getParameterMap(), 
                "superoverlay_mode",
                "auto"
                );

            if ("raster".equalsIgnoreCase(submode)) {
                modeOptions.put("overlaymode", "raster");
            } else if ("overview".equalsIgnoreCase(submode)) {
                modeOptions.put("overlaymode", "overview");
            } else if ("hybrid".equalsIgnoreCase(submode)) {
                modeOptions.put("overlaymode", "hybrid");
            } else if ("auto".equalsIgnoreCase(submode)) {
                modeOptions.put("overlaymode", "auto");
            } else {
                throw new WmsException("Unknown overlay mode: " + submode);
            }
        } else if ("refresh".equals(mode)){
            String submode = caseInsensitiveParam(
                request.getHttpServletRequest().getParameterMap(), 
                "refresh_mode",
                null
                );
        } else if ("download".equals(mode)){
            String submode = caseInsensitiveParam(
                request.getHttpServletRequest().getParameterMap(), 
                "download_mode",
                null
                );
        }

        //first set up some of the normal wms defaults
        if ( request.getWidth() < 1 ) {
            request.setWidth(mode.equals("refresh") || containsRasterData ? 1024 : 256);
        } 

        if ( request.getHeight() < 1 ) {
            request.setHeight(mode.equals("refresh") || containsRasterData ? 1024 : 256);
        }

        // Force srs to lat/lon for KML output.
        request.setSRS("EPSG:4326");
        
        //set rest of the wms defaults
        wms.reflect(request);
        
        //set some kml specific defaults
        Map fo = request.getFormatOptions();

        merge(fo, modeOptions);

        if ( fo.get( "kmattr") == null ) {
            fo.put( "kmattr", KMATTR );
        } if ( fo.get( "kmscore" ) == null ) {
            fo.put( "kmscore", KMSCORE );
        } if (fo.get("kmplacemark") == null) {
            fo.put("kmplacemark", KMPLACEMARK);
        }

        //set the format
        //TODO: create a subclass of GetMapRequest to store these values
 
        Boolean superoverlay = (Boolean)fo.get("superoverlay");
        if (superoverlay == null) superoverlay = Boolean.FALSE;
        String formatExtension = ".kmz";
        if (superoverlay){
            request.setFormat(KMZMapProducer.MIME_TYPE);
            request.setBbox(KMLUtils.expandToTile(request.getBbox()));
        } else if (mode.equals("refresh") || containsRasterData) {
            request.setFormat(KMZMapProducer.MIME_TYPE);
        } else if (!Arrays.asList(KMZMapProducer.OUTPUT_FORMATS).contains( request.getFormat() ) ) {
            //if the user did not explicitly request kml give them back KMZ
            request.setFormat(KMLMapProducer.MIME_TYPE);
            formatExtension = ".kml";
        }

        response.setContentType(request.getFormat());

        filename.setLength(filename.length()-1);
        response.setHeader("Content-Disposition", 
                "attachment; filename=" + filename.toString() + formatExtension);

        if ("download".equals(mode)){
            GetMapResponse wmsResponse = wms.getMap(request);
            wmsResponse.execute(request);
            wmsResponse.writeTo(response.getOutputStream());
        } else {
            KMLNetworkLinkTransformer transformer = new KMLNetworkLinkTransformer();
            transformer.setIndentation(3);
            Charset encoding = request.getWMS().getCharSet();
            transformer.setEncoding(encoding);
            transformer.setEncodeAsRegion(superoverlay);
            transformer.transform( request, response.getOutputStream() );
        }
    }

    private static String caseInsensitiveParam(Map params, String paramname, String defaultValue){
        String value = defaultValue;

        for (Object o : params.entrySet()) {
            Map.Entry entry = (Map.Entry)o;
            if (entry.getKey() instanceof String) {
                if (paramname.equalsIgnoreCase((String) entry.getKey())){
                    Object obj = entry.getValue();
                    value = (obj instanceof String[]) ? ((String[]) obj)[0].toLowerCase() : value;
                }
            }
        }

        return value;
    }

    private static void merge(Map options, Map addition){
        for (Object o : addition.entrySet()){
            Map.Entry entry = (Map.Entry) o;
            if (entry.getValue() == null) 
                options.remove(entry.getKey());
            else
                options.put(entry.getKey(), entry.getValue());
        }
    }
}
