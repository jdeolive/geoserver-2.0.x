/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.ows;

import static org.geoserver.ows.util.ResponseUtils.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import javax.servlet.http.HttpServletResponse;

import net.opengis.ows11.ExceptionReportType;
import net.opengis.ows11.Ows11Factory;

import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.RequestUtils;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geotools.ows.v1_1.OWS;
import org.geotools.ows.v1_1.OWSConfiguration;
import org.geotools.xml.Encoder;


/**
 * A default implementation of {@link ServiceExceptionHandler} which outputs
 * as service exception in a <code>ows:ExceptionReport</code> document.
 * <p>
 * This service exception handler will generate an OWS exception report,
 * see {@linkplain http://schemas.opengis.net/ows/1.1.0/owsExceptionReport.xsd}.
 * </p>
 *
 * @author Justin Deoliveira, The Open Planning Project
 *
 */
public class OWS11ServiceExceptionHandler extends ServiceExceptionHandler {
    protected boolean verboseExceptions = false;
    
    /**
     * Constructor to be called if the exception is not for a particular service.
     *
     */
    public OWS11ServiceExceptionHandler() {
        super(Collections.EMPTY_LIST);
    }

    /**
     * Constructor to be called if the exception is for a particular service.
     *
     * @param services List of services this handler handles exceptions for.
     */
    public OWS11ServiceExceptionHandler(List services) {
        super(services);
    }
    
    /**
     * Constructor to be called if the exception is for a particular service.
     *
     * @param services List of services this handler handles exceptions for.
     */
    public OWS11ServiceExceptionHandler(Service service) {
        super(Arrays.asList(service));
    }

    /**
     * Writes out an OWS ExceptionReport document.
     */
    public void handleServiceException(ServiceException exception, Request request) {
        Ows11Factory factory = Ows11Factory.eINSTANCE;

        ExceptionReportType report = Ows11Util.exceptionReport( exception, verboseExceptions );
        
        HttpServletResponse response = request.getHttpResponse();
        response.setContentType("application/xml");

        //response.setCharacterEncoding( "UTF-8" );
        OWSConfiguration configuration = new OWSConfiguration();

        Encoder encoder = new Encoder(configuration, configuration.schema());
        encoder.setIndenting(true);
        encoder.setIndentSize(2);
        encoder.setLineWidth(60);

        String schemaLocation = buildSchemaURL(baseURL(request.getHttpRequest()), "ows/1.1.0/owsAll.xsd");
        encoder.setSchemaLocation(OWS.NAMESPACE, schemaLocation);

        try {
            encoder.encode(report, OWS.ExceptionReport,
                response.getOutputStream());
        } catch (Exception ex) {
            //throw new RuntimeException(ex);
            // Hmm, not much we can do here.  I guess log the fact that we couldn't write out the exception and be done with it...
            LOGGER.log(Level.INFO, "Problem writing exception information back to calling client:", ex);
        } finally {
            try {
                response.getOutputStream().flush();
            } catch (IOException ioe) {
            }
        }
    }
}
