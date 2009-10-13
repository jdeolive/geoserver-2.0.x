/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import net.opengis.wfs.GetCapabilitiesType;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.util.RequestUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.CapabilitiesTransformer.WFS1_0;
import org.geotools.util.Version;

/**
 * Web Feature Service GetCapabilities operation.
 * <p>
 * This operation returns a {@link org.geotools.xml.transform.TransformerBase} instance
 * which will serialize the wfs capabilities document. This class uses ows version negotiation
 * to determine which version of the wfs capabilities document to return.
 * </p>
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 *
 */
public class GetCapabilities {
    /**
     * WFS service configuration
     */
    WFSInfo wfs;

    /**
     * The catalog
     */
    Catalog catalog;

    /**
     * Creates a new wfs GetCapabilitis operation.
     *
     * @param wfs The wfs configuration
     * @param catalog The geoserver catalog.
     */
    public GetCapabilities(WFSInfo wfs, Catalog catalog) {
        this.wfs = wfs;
        this.catalog = catalog;
    }

    public CapabilitiesTransformer run(GetCapabilitiesType request)
        throws WFSException {
        //cite requires that we fail when we see an "invalid" update sequence,
        // since we dont support update sequences, all are invalid, but we take
        // our more lax approach and just ignore it when not doint the cite thing
        if (wfs.isCiteCompliant()) {
            if (request.getUpdateSequence() != null) {
                throw new WFSException("Invalid update sequence", "InvalidUpdateSequence");
            }
        }

        //TODO: the rest of this routine should be done by the dispatcher
        //make sure service is set, cite conformance thing
        //JD - We wrap this in a cite conformance check because cite stricly 
        // tests that every request includes the 'service=WFS' key value pair.
        // However often the the context of the request is good enough to 
        // determine what the service is, like in 'geoserver/wfs?request=GetCapabilities'
        if (wfs.isCiteCompliant()) {
            if (!request.isSetService()) {
                //give up 
                throw new WFSException("Service not set", "MissingParameterValue", "service");
            }
        }

        //do the version negotiation dance
        List<String> provided = new ArrayList<String>();
        provided.add("1.0.0");
        provided.add("1.1.0");
        List<String> accepted = null;
        if(request.getAcceptVersions() != null)
            accepted = request.getAcceptVersions().getVersion();
        String version = RequestUtils.getVersionPreOws(provided, accepted);

        final CapabilitiesTransformer capsTransformer;
        if ("1.0.0".equals(version)) {
            capsTransformer = new CapabilitiesTransformer.WFS1_0(wfs, catalog);
        }else if ("1.1.0".equals(version)) {
            capsTransformer = new CapabilitiesTransformer.WFS1_1(wfs, catalog);
        }else{
            throw new WFSException("Could not understand version:" + version);
        }
        capsTransformer.setEncoding(Charset.forName( wfs.getGeoServer().getGlobal().getCharset() ));
        return capsTransformer;
    }
    
    
}
