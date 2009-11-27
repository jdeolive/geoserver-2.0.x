/** 
 * Copyright (c) 2001 - 2009 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Arne Kepp / OpenGeo
 */
package org.geowebcache.layer.wms;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Locale;
import java.util.logging.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.geotools.util.logging.Logging;

public class FakeHttpServletResponse implements HttpServletResponse {
    
    private static Logger log = Logging.getLogger(HttpServletResponse.class.toString());
    
    FakeServletOutputStream fos = new FakeServletOutputStream();
    
    String contentType;
    
    HashMap<String,String> headers = new HashMap<String,String>();
    
    public byte[] getBytes() {
        return fos.getBytes();
    }
    
    /**
     * Standard interface
     */
    public void addCookie(Cookie arg0) {
        throw new ServletDebugException();
        
    }

    public void addDateHeader(String arg0, long arg1) {
        log.finer("Added date header: "+arg0 + " : " + arg1);
        headers.put(arg0, Long.toString(arg1));
    }

    public void addHeader(String arg0, String arg1) {
        log.finer("Added string header: "+arg0 + " : " + arg1);
        headers.put(arg0, arg1); 
    }

    public void addIntHeader(String arg0, int arg1) {
        log.finer("Added integer header: "+arg0 + " : " + arg1);
        headers.put(arg0, Integer.toString(arg1));
    }

    public boolean containsHeader(String arg0) {
        throw new ServletDebugException();
    }

    public String encodeRedirectURL(String arg0) {
        throw new ServletDebugException();
    }

    public String encodeRedirectUrl(String arg0) {
        throw new ServletDebugException();
    }

    public String encodeURL(String arg0) {
        throw new ServletDebugException();
    }

    public String encodeUrl(String arg0) {
        throw new ServletDebugException();
    }

    public void sendError(int arg0) throws IOException {
        throw new ServletDebugException();
    }

    public void sendError(int arg0, String arg1) throws IOException {
        throw new ServletDebugException();
    }

    public void sendRedirect(String arg0) throws IOException {
        throw new ServletDebugException();
    }

    public void setDateHeader(String arg0, long arg1) {
        throw new ServletDebugException();
    }

    public void setHeader(String arg0, String arg1) {
        throw new ServletDebugException();
    }

    public void setIntHeader(String arg0, int arg1) {
        throw new ServletDebugException();
    }

    public void setStatus(int arg0) {
        throw new ServletDebugException();
    }

    public void setStatus(int arg0, String arg1) {
        throw new ServletDebugException();
    }

    public void flushBuffer() throws IOException {
        throw new ServletDebugException();
    }

    public int getBufferSize() {
        throw new ServletDebugException();
    }

    public String getCharacterEncoding() {
        throw new ServletDebugException();
    }

    public String getContentType() {
        return this.contentType;
    }

    public Locale getLocale() {
        throw new ServletDebugException();
    }

    public ServletOutputStream getOutputStream() throws IOException {
        log.finer("Returning output stream");
        return this.fos;
    }

    public PrintWriter getWriter() throws IOException {
        throw new ServletDebugException();
    }

    public boolean isCommitted() {
        throw new ServletDebugException();
    }

    public void reset() {
        throw new ServletDebugException();
        
    }

    public void resetBuffer() {
        throw new ServletDebugException();
        
    }

    public void setBufferSize(int arg0) {
        throw new ServletDebugException();
        
    }

    public void setCharacterEncoding(String arg0) {
        throw new ServletDebugException();
        
    }

    public void setContentLength(int arg0) {
        throw new ServletDebugException();
        
    }

    public void setContentType(String arg0) {
        log.finer("Content type set to " + arg0);
        this.contentType = arg0;
    }

    public void setLocale(Locale arg0) {
        throw new ServletDebugException();
        
    }

}
