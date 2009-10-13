package org.vfny.geoserver.wms.responses.map.kml;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.util.WMSRequests;
import org.geotools.styling.Style;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xml.transform.Translator;
import org.vfny.geoserver.wms.requests.GetMapRequest;
import org.xml.sax.ContentHandler;

import com.vividsolutions.jts.geom.Envelope;

/**
 * Encodes a KML document contianing a network link.
 * <p>
 * This transformer transforms a {@link GetMapRequest} object.
 * </p>
 * 
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 *
 */
public class KMLNetworkLinkTransformer extends TransformerBase {

    /**
     * logger
     */
    static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.kml");
    
    /**
     * flag controlling wether the network link should be a super overlay.
     */
    boolean encodeAsRegion = false;
 
    public Translator createTranslator(ContentHandler handler) {
        return new KMLNetworkLinkTranslator( handler );
    }
    
    public void setEncodeAsRegion(boolean encodeAsRegion) {
        this.encodeAsRegion = encodeAsRegion;
    }
    
    class KMLNetworkLinkTranslator extends TranslatorSupport {

        public KMLNetworkLinkTranslator(ContentHandler contentHandler) {
            super(contentHandler, null,null);
        }
        
        public void encode(Object o) throws IllegalArgumentException {
            GetMapRequest request = (GetMapRequest) o;
            
            start( "kml" );
            start( "Folder" );
        
            if ( encodeAsRegion ) {
                encodeAsSuperOverlay( request );
            }
            else {
                encodeAsOverlay( request );
            }
            
            //look at
            encodeLookAt( request );
            
            end( "Folder" );
            end( "kml" );
        }
        
        protected void encodeAsSuperOverlay( GetMapRequest request ) {
            MapLayerInfo[] layers = request.getLayers();
            List<Style> styles = request.getStyles();
            for ( int i = 0; i < layers.length; i++ ) {
                start("NetworkLink");
                element( "name", layers[i].getName() );
                element( "open", "1" );
                element( "visibility", "1" );
             
                //region
                start( "Region" );
                
                Envelope bbox = request.getBbox();
                start( "LatLonAltBox" );
                element( "north", ""+bbox.getMaxY() );
                element( "south", ""+bbox.getMinY() );
                element( "east", ""+bbox.getMaxX() );
                element( "west", ""+bbox.getMinX() );
                end( "LatLonAltBox");
                
                start( "Lod" );
                element( "minLodPixels", "256" );
                element( "maxLodPixels", "-1" );
                end( "Lod" );
                
                end( "Region" );
                
                //link
                start("Link" );
  
                String style = i < styles.size()? styles.get(i).getName() : null;
                String href = WMSRequests.getGetMapUrl(request, layers[i].getName(), i, style, null, null);
                start( "href" );
                cdata( href );
                end( "href" );
                
//                element( "viewRefreshMode", "onRegion" );
                end( "Link" );
                
                end( "NetworkLink");
            }
        }
        
        protected void encodeAsOverlay( GetMapRequest request ) {
            MapLayerInfo[] layers = request.getLayers();
            List<Style> styles = request.getStyles();
            for ( int i = 0; i < layers.length; i++ ) {
                start("NetworkLink");
                element( "name", layers[i].getName() );
                element( "open", "1" );
                element( "visibility", "1" );
                
                start( "Url" );
                
                //set bbox to null so its not included in the request, google 
                // earth will append it for us
                request.setBbox(null);
                
                String style = i < styles.size()? styles.get(i).getName() : null;
                String href = WMSRequests.getGetMapUrl(request, layers[i].getName(), i, style, null, null);
                start( "href" );
                cdata( href );
                end( "href" );
                
                element( "viewRefreshMode", "onStop" );
                element( "viewRefreshTime", "1" );
                end( "Url" );
                
                end( "NetworkLink" );
            }
        }
        
        private void encodeLookAt(GetMapRequest request){
            
            Envelope e = new Envelope();
            e.setToNull();
            
            for ( int i = 0; i < request.getLayers().length; i++ ) {
                MapLayerInfo layer = request.getLayers()[i];
                
                Envelope b = null;
                try {
                    b = request.getLayers()[i].getLatLongBoundingBox();
                } catch (IOException e1) {
                    LOGGER.warning( "Unable to calculate bounds for " + layer.getName() );
                    continue;
                } 
                if ( e.isNull() ) {
                    e.init( b );
                }
                else {
                    e.expandToInclude( b );
                }
            }
            
            if ( e.isNull() ) {
                return;
            }
            
            double lon1 = e.getMinX();
            double lat1 = e.getMinY();
            double lon2 = e.getMaxX();
            double lat2 = e.getMaxY();
            
            double R_EARTH = 6.371 * 1000000; // meters
            double VIEWER_WIDTH = 22 * Math.PI / 180; // The field of view of the google maps camera, in radians
            double[] p1 = getRect(lon1, lat1, R_EARTH);
            double[] p2 = getRect(lon2, lat2, R_EARTH);
            double[] midpoint = new double[]{
              (p1[0] + p2[0])/2,
                (p1[1] + p2[1])/2,
                (p1[2] + p2[2])/2
            };
            
            midpoint = getGeographic(midpoint[0], midpoint[1], midpoint[2]);
            
            double distance = distance(p1, p2);
            
            double height = distance/ (2 * Math.tan(VIEWER_WIDTH));
            
            LOGGER.fine("lat1: " + lat1 + "; lon1: " + lon1);
            LOGGER.fine("lat2: " + lat2 + "; lon2: " + lon2);
            LOGGER.fine("latmid: " + midpoint[1] + "; lonmid: " + midpoint[0]);
            
            
            start( "LookAt" );
            element( "longitude", ""+midpoint[0] );
            element( "latitude", "" +midpoint[1] );
            element( "altitude", "0" );
            element( "range", ""+ distance );
            element( "tilt", "0" );
            element( "heading", "0" );
            element( "altitudeMode", "clampToGround" );
            end( "LookAt" );
          }
          
          private double[] getRect(double lat, double lon, double radius){
            double theta = (90 - lat) * Math.PI/180;
            double phi   = (90 - lon) * Math.PI/180;
            
            double x = radius * Math.sin(phi) * Math.cos(theta);
            double y = radius * Math.sin(phi) * Math.sin(theta);
            double z = radius * Math.cos(phi);
            return new double[]{x, y, z};
          }
          
          private double[] getGeographic(double x, double y, double z){
            double theta, phi, radius;
            radius = distance(new double[]{x, y, z}, new double[]{0,0,0});
            theta = Math.atan2(Math.sqrt(x * x + y * y) , z);
            phi = Math.atan2(y , x);
            
            double lat = 90 - (theta * 180 / Math.PI);
            double lon = 90 - (phi * 180 / Math.PI);
            
            return new double[]{(lon > 180 ? lon - 360 : lon), lat, radius};
          }
          
          private double distance(double[] p1, double[] p2){
            double dx = p1[0] - p2[0];
            double dy = p1[1] - p2[1];
            double dz = p1[2] - p2[2];
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
          }
    }
}
