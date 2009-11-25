/** 
 * Copyright (c) 2001 - 2009 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Arne Kepp / OpenGeo
 */
package org.geowebcache.layer.wms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.geotools.util.logging.Logging;
import org.geowebcache.util.ServletUtils;

public class FakeHttpServletRequest implements HttpServletRequest {
    private static Logger log = Logging.getLogger(HttpServletRequest.class.toString());
    
    String wmsParams;
    
    HashMap<String,String> parameterMap = new HashMap<String,String>(10);
    
    public FakeHttpServletRequest(String wmsParams) {
        log.finer("Constructing from " + wmsParams);
        
        this.wmsParams = wmsParams;
        
        // This is a bit stupid... refactor parameters in GWC, again?
        String[] pairs = this.wmsParams.split("&");
        for(int i=0; i< pairs.length; i++) {
            //log.finest(pairs[i]);
            String[] key_value = pairs[i].split("=");
            if(key_value.length < 2) {
                parameterMap.put(key_value[0], "");
            } else {
                parameterMap.put(key_value[0],ServletUtils.URLDecode(key_value[1], "UTF-8"));
            }
            
        }
    }
    
    /**
     * Standard interface
     */

    public String getAuthType() {
        throw new ServletDebugException();
    }

    public String getContextPath() {
        return "/geoserver";
    }

    public Cookie[] getCookies() {
        throw new ServletDebugException();
    }

    public long getDateHeader(String arg0) {
        throw new ServletDebugException();
    }

    public String getHeader(String arg0) {
        throw new ServletDebugException();
    }

    public Enumeration getHeaderNames() {
        throw new ServletDebugException();
    }

    public Enumeration getHeaders(String arg0) {
        throw new ServletDebugException();
    }

    public int getIntHeader(String arg0) {
        throw new ServletDebugException();
    }

    public String getMethod() {
        return "GET";
    }

    public String getPathInfo() {
        throw new ServletDebugException();
    }

    public String getPathTranslated() {
        throw new ServletDebugException();
    }

    public String getQueryString() {
        throw new ServletDebugException();
    }

    public String getRemoteUser() {
        throw new ServletDebugException();
    }

    public String getRequestURI() {
        throw new ServletDebugException();
    }

    public StringBuffer getRequestURL() {
        throw new ServletDebugException();
    }

    public String getRequestedSessionId() {
        throw new ServletDebugException();
    }

    public String getServletPath() {
        throw new ServletDebugException();
    }

    public HttpSession getSession() {
        throw new ServletDebugException();
    }

    public HttpSession getSession(boolean arg0) {
        throw new ServletDebugException();
    }

    public Principal getUserPrincipal() {
        throw new ServletDebugException();
    }

    public boolean isRequestedSessionIdFromCookie() {
        throw new ServletDebugException();
    }

    public boolean isRequestedSessionIdFromURL() {
        throw new ServletDebugException();
    }

    public boolean isRequestedSessionIdFromUrl() {
        throw new ServletDebugException();
    }

    public boolean isRequestedSessionIdValid() {
        throw new ServletDebugException();
    }

    public boolean isUserInRole(String arg0) {
        throw new ServletDebugException();
    }

    public Object getAttribute(String arg0) {
        throw new ServletDebugException();
    }

    public Enumeration getAttributeNames() {
        throw new ServletDebugException();
    }

    public String getCharacterEncoding() {
        return "UTF-8";
    }

    public int getContentLength() {
        throw new ServletDebugException();
    }

    public String getContentType() {
        throw new ServletDebugException();
    }

    public ServletInputStream getInputStream() throws IOException {
        throw new ServletDebugException();
    }

    public String getLocalAddr() {
        throw new ServletDebugException();
    }

    public String getLocalName() {
        throw new ServletDebugException();
    }

    public int getLocalPort() {
        // TODO Auto-generated method stub
        return 0;
    }

    public Locale getLocale() {
        throw new ServletDebugException();
    }

    public Enumeration getLocales() {
        throw new ServletDebugException();
    }

    public String getParameter(String arg0) {
        throw new ServletDebugException();
    }

    public Map getParameterMap() {
        return parameterMap;
    }

    public Enumeration getParameterNames() {
        throw new ServletDebugException();
    }

    public String[] getParameterValues(String arg0) {
        throw new ServletDebugException();
    }

    public String getProtocol() {
        throw new ServletDebugException();
    }

    public BufferedReader getReader() throws IOException {
        throw new ServletDebugException();
    }

    public String getRealPath(String arg0) {
        throw new ServletDebugException();
    }

    public String getRemoteAddr() {
        return "127.0.0.1";
    }

    public String getRemoteHost() {
        return "localhost";
    }

    public int getRemotePort() {
        throw new ServletDebugException();
    }

    public RequestDispatcher getRequestDispatcher(String arg0) {
        throw new ServletDebugException();
    }

    public String getScheme() {
        return "http";
    }

    public String getServerName() {
        return "localhost";
    }

    public int getServerPort() {
        return 8080;
    }

    public boolean isSecure() {
        throw new ServletDebugException();
    }

    public void removeAttribute(String arg0) {
        throw new ServletDebugException();
    }

    public void setAttribute(String arg0, Object arg1) {
        throw new ServletDebugException();
    }

    public void setCharacterEncoding(String arg0)
            throws UnsupportedEncodingException {
        if(! arg0.equals("UTF-8")) {
            throw new ServletDebugException();
        }
    }

}
