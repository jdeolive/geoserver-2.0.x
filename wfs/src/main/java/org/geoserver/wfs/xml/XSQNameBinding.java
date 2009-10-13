/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */

package org.geoserver.wfs.xml;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geotools.xml.InstanceComponent;
import org.geotools.xml.impl.DatatypeConverterImpl;

/**
 * Overrides the base class parsing code so that prefix can be resolved
 * to URI's using the GeoServer {@link Data} catalog as well
 * @author Andrea Aime - TOPP
 */
public class XSQNameBinding extends org.geotools.xs.bindings.XSQNameBinding {

    Catalog data;

    public XSQNameBinding(NamespaceContext namespaceContext, Catalog data) {
        super(namespaceContext);
        this.data = data;
    }
    
    /**
     * <!-- begin-user-doc -->
     * This binding returns objects of type {@link QName}.
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Object parse(InstanceComponent instance, Object value)
        throws Exception {
        
//        QName qName = null;
//        try {
//            qName = DatatypeConverterImpl.getInstance()
//                .parseQName((String) value, namespaceContext);
//        }
//        catch( Exception e ) {
//        }
//            
//        if (qName != null) {
//            return qName;
//        }

        if (value == null) {
            return new QName(null);
        }

        String s = (String) value;
        int i = s.indexOf(':');

        if (i != -1) {
            String prefix = s.substring(0, i);
            String local = s.substring(i + 1);
            String namespaceURI = null;
            NamespaceInfo nsInfo = data.getNamespaceByPrefix(prefix);
            if(nsInfo != null)
                namespaceURI = nsInfo.getURI();

            return new QName(namespaceURI, local, prefix);
        }

        return new QName(null, s);
    }

}
