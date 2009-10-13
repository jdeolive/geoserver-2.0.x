/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.acegisecurity.Authentication;
import org.acegisecurity.ui.AbstractProcessingFilter;
import org.acegisecurity.ui.ExceptionTranslationFilter;
import org.acegisecurity.ui.savedrequest.SavedRequest;
import org.acegisecurity.ui.webapp.AuthenticationProcessingFilter;
import org.apache.wicket.protocol.http.WebRequest;


/**
 * Base class for secured web pages. By default it only allows
 * 
 * @author Andrea Aime - TOPP
 * 
 */
public class GeoServerSecuredPage extends GeoServerBasePage {

    public static final PageAuthorizer DEFAULT_AUTHORIZER = new DefaultPageAuthorizer();

    public GeoServerSecuredPage() {
        super();
        Authentication auth = getSession().getAuthentication();
        if(auth == null || !auth.isAuthenticated()) {
            // emulate what acegi url control would do so that we get a proper redirect after login
            HttpServletRequest httpRequest = ((WebRequest) getRequest()).getHttpServletRequest();
            ExceptionTranslationFilter translator = (ExceptionTranslationFilter) getGeoServerApplication().getBean("consoleExceptionTranslationFilter");
            SavedRequest savedRequest = new SavedRequest(httpRequest, translator.getPortResolver());
            
            HttpSession session = httpRequest.getSession();
            session.setAttribute(AbstractProcessingFilter.ACEGI_SAVED_REQUEST_KEY, savedRequest);
            
            // then redirect to the login page
            setResponsePage(GeoServerLoginPage.class);
        }
        else if (!getPageAuthorizer().isAccessAllowed(this.getClass(), auth))
            setResponsePage(UnauthorizedPage.class);
    }

    /**
     * Override to use a page authorizer other than the default one. When you do
     * so, remember to perform the same change in the associated
     * {@link MenuPageInfo} instance
     * 
     * @return
     */
    protected PageAuthorizer getPageAuthorizer() {
        return DEFAULT_AUTHORIZER;
    }
}
