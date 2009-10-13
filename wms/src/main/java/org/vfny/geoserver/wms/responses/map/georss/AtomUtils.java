package org.vfny.geoserver.wms.responses.map.georss;

import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.geoserver.wms.util.WMSRequests;
import org.geotools.map.MapLayer;
import org.opengis.feature.simple.SimpleFeature;
import org.vfny.geoserver.util.Requests;
import org.vfny.geoserver.wms.WMSMapContext;
import org.vfny.geoserver.wms.responses.featureInfo.FeatureTemplate;

/**
 * The AtomUtils class provides some static methods useful in producing atom metadata related to 
 * GeoServer features.
 *
 * @author David Winslow
 */
public final class AtomUtils {

    /**
     * A date formatting object that does most of the formatting work for RFC3339.  Note that since 
     * Java's SimpleDateFormat does not provide all the facilities needed for RFC3339 there is still
     * some custom code to finish the job.
     */
    private static DateFormat rfc3339 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * A number formatting object to format the the timezone offset info in RFC3339 output.
     */
    private static NumberFormat doubleDigit = new DecimalFormat("00");

    /**
     * A FeatureTemplate used for formatting feature info.
     * @TODO: Are these things threadsafe?
     */
    private static FeatureTemplate featureTemplate = new FeatureTemplate();

    /**
     * This is a utility class so don't allow instantiation.
     */
    private AtomUtils(){ /* Nothing to do */ }

    /**
     * Format dates as specified in rfc3339 (required for Atom dates)
     * @param d the Date to be formatted
     * @return the formatted date
     */
    public static String dateToRFC3339(Date d){
        StringBuilder result = new StringBuilder(rfc3339.format(d));
        Calendar cal = new GregorianCalendar();
        cal.setTime(d);
        cal.setTimeZone(TimeZone.getDefault());
        int offset_millis = cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET);
        int offset_hours = Math.abs(offset_millis / (1000 * 60 * 60));
        int offset_minutes = Math.abs((offset_millis / (1000 * 60)) % 60);

        if (offset_millis == 0) {
            result.append("Z");
        } else {
            result
                .append((offset_millis > 0) ? "+" : "-")
                .append(doubleDigit.format(offset_hours))
                .append(":")
                .append(doubleDigit.format(offset_minutes));
        }

        return result.toString();
    }

    //TODO: use an html based output format
    public static String getEntryURL(SimpleFeature feature, WMSMapContext context){
        String nsUri = feature.getType().getName().getNamespaceURI();
        String nsPrefix = context.getRequest().getWMS().getNameSpacePrefix(nsUri);

        return Requests.getBaseUrl(
                context.getRequest().getHttpServletRequest(),
                context.getRequest().getWMS().getGeoServer()
                )
            + "wms/reflect?format=application/atom+xml&layers=" 
            + nsPrefix + ":" + feature.getType().getTypeName() 
            + "&featureid=" + feature.getID();
    }

    public static String getEntryURI(SimpleFeature feature, WMSMapContext context){
        return getEntryURL(feature, context);
    }

    public static String getFeatureTitle(SimpleFeature feature){
        try{
            return featureTemplate.title(feature);
        } catch (IOException ioe){
            return feature.getID();
        }
    }

    public static String getFeatureDescription(SimpleFeature feature){
        try{
            return featureTemplate.description(feature);
        } catch (IOException ioe) {
            return feature.getID();
        }
    }

    public static String getFeedURL(WMSMapContext context){
        return WMSRequests.getGetMapUrl(context.getRequest(), null, 0, null, null).replace(' ', '+');
    }

    public static String getFeedURI(WMSMapContext context){
        return getFeedURL(context);
    }

    public static String getFeedTitle(WMSMapContext context){
        StringBuffer title = new StringBuffer();

        for (int i = 0; i < context.getLayerCount(); i++) {
            MapLayer layer = context.getLayer(i);
            title.append(layer.getTitle()).append(",");
        }
        title.setLength(title.length()-1);
        return title.toString();
    }


    private static String commaSeparatedLayers(WMSMapContext con){
        StringBuilder layers = new StringBuilder();
        MapLayer[] mapLayers = con.getLayers();
        for (int i = 0; i < mapLayers.length; i++){
            layers.append(mapLayers[i].getTitle());
            if (i < mapLayers.length - 1) layers.append(",");
        }
        return layers.toString();
    }
}
