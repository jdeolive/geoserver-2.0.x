/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */

package org.geoserver.xacml.security;

import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.userdetails.User;
import org.geoserver.security.GeoserverUserDao;
import org.geoserver.xacml.geoxacml.GeoXACMLConfig;

public class XACMLGeoserverUserDao extends GeoserverUserDao {

    @Override
    protected User createUserObject(String username, String password, boolean isEnabled,
            GrantedAuthority[] authorities) {
        User user = super.createUserObject(username, password, isEnabled, authorities);
        GeoXACMLConfig.getXACMLRoleAuthority().transformUserDetails(user);
        return user;
    }

}
