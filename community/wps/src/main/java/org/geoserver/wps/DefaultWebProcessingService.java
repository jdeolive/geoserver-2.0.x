/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.opengis.wps10.DescribeProcessType;
import net.opengis.wps10.ExecuteResponseType;
import net.opengis.wps10.ExecuteType;
import net.opengis.wps10.GetCapabilitiesType;
import net.opengis.wps10.ProcessDescriptionsType;
import net.opengis.wps10.WPSCapabilitiesType;

import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Default Web Processing Service class
 *
 * @author Lucas Reed, Refractions Research Inc
 */
public class DefaultWebProcessingService implements WebProcessingService, ApplicationContextAware {
    protected WPSInfo  wps;
    protected GeoServerInfo gs;
    protected ApplicationContext context;

    public DefaultWebProcessingService(GeoServer gs) {
        this.wps = gs.getService( WPSInfo.class );
        this.gs = gs.getGlobal();
    }

    /**
     * @see org.geoserver.wps.WebProcessingService#getCapabilities
     */
    public WPSCapabilitiesType getCapabilities(GetCapabilitiesType request) throws WPSException {
        return new GetCapabilities(this.wps).run(request);
    }
    
    
    /**
     * @see org.geoserver.wps.WebProcessingService#describeProcess
     */
    public ProcessDescriptionsType describeProcess(DescribeProcessType request) throws WPSException {
        return new DescribeProcess(this.wps,context).run(request);
    }

    /**
     * @see org.geoserver.wps.WebProcessingService#execute
     */
    public ExecuteResponseType execute(ExecuteType request) throws WPSException {
        return new Execute(wps,gs,context).run(request);
    }

    /**
     * @see org.geoserver.wps.WebProcessingService#getSchema
     */
    public void getSchema(HttpServletRequest request, HttpServletResponse response)
        throws WPSException {
        new GetSchema(this.wps).run(request, response);
    }

    /**
     * @see org.springframework.context.ApplicationContextAware#setApplicationContext
     */
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = context;
    }
}
