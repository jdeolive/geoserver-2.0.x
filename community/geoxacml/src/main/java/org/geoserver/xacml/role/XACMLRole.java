/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.xacml.role;

import java.util.HashSet;
import java.util.Set;

import org.acegisecurity.GrantedAuthority;

import com.sun.xacml.ctx.Attribute;

/**
 * @author Christian Mueller
 * 
 *         Class for holding a security role, roles can have attributes. This role class is intended
 *         for building a role object for an xacml request.
 * 
 *         According to the RBAC XACML specification, roles can be disabled.
 * 
 *         An example for a role is "EMPLOYEE" with a role parameter PERSONAL_NUMBER
 * 
 *         For integration into acegi security framework, this class implements the acegi
 *         GrantedAuthority interface.
 * 
 * 
 */
public class XACMLRole implements GrantedAuthority {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private String authority;

    private Set<Attribute> attributes;

    private boolean enabled;

    private boolean roleAttributesProcessed;

    public boolean isRoleAttributesProcessed() {
        return roleAttributesProcessed;
    }

    public void setRoleAttributesProcessed(boolean roleAttributesProcessed) {
        this.roleAttributesProcessed = roleAttributesProcessed;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public XACMLRole(String authority) {
        this(authority, null);
    }

    public XACMLRole(String authority, Set<Attribute> attributes) {
        this.authority = authority;
        this.attributes = attributes;
        this.enabled = true;
        this.roleAttributesProcessed = false;
    }

    public String getAuthority() {
        return authority;
    }

    public Set<Attribute> getAttributes() {
        if (attributes == null)
            attributes = new HashSet<Attribute>();
        return attributes;
    }

    public boolean hasAttributes() {
        return attributes != null && attributes.isEmpty() == false;
    }

}
