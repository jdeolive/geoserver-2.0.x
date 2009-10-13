/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.security;

import org.acegisecurity.ui.webapp.AuthenticationProcessingFilter;
import javax.servlet.http.HttpServletRequest;


/**
 * A little variant on the {@link AuthenticationProcessingFilter} that uses
 * different keys for username and password ("username", "password") in order to
 * ensure backwards compatibility with the old login page
 *
 * @author Andrea Aime - TOPP
 *
 */
public class GeoserverAuthenticationProcessingFilter extends AuthenticationProcessingFilter {
    protected String obtainPassword(HttpServletRequest request) {
        return request.getParameter("password");
    }

    protected String obtainUsername(HttpServletRequest request) {
        return request.getParameter("username");
    }
}
