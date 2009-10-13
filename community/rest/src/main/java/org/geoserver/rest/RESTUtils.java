package org.geoserver.rest;

import javax.servlet.http.HttpServletRequest;

import org.restlet.data.Reference;
import org.restlet.data.Request;

import com.noelios.restlet.ext.servlet.ServletCall;
import com.noelios.restlet.http.HttpRequest;

/**
 * Utility class for Restlets.
 * 
 * @author Justin Deoliveira, The Open Planning Project
 *
 */
public class RESTUtils {

    /**
     * Returns the underlying HttpServletRequest from a Restlet Request object.
     * <p>
     * Note that this only returns a value in the case where the Restlet 
     * request/call is originating from a servlet.
     * </p>
     * @return The HttpServletRequest, or null.
     */
    public static HttpServletRequest getServletRequest( Request request ) {
        if ( request instanceof HttpRequest ) {
            HttpRequest httpRequest = (HttpRequest) request;
            if ( httpRequest.getHttpCall() instanceof ServletCall ) {
                ServletCall call = (ServletCall) httpRequest.getHttpCall();
                return call.getRequest();
            }
        }
        
        return null;
    }
    
    public static String getBaseURL( Request request ) {
        Reference ref = request.getResourceRef();
        HttpServletRequest servletRequest = getServletRequest(request);
        if ( servletRequest != null ) {
            String baseURL = ref.getIdentifier();
            return baseURL.substring(0, baseURL.length()-servletRequest.getPathInfo().length());
        }
        else {
            return ref.getParentRef().getIdentifier();
        }
    }
}
