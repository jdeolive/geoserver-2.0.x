/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.vfny.geoserver.sld;

import org.geoserver.platform.ServiceException;

public class SldException extends ServiceException {
    
    public SldException(String error) {
        super(error);
    }
}
