/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.util.ArrayList;
import java.util.List;

import org.geoserver.platform.GeoServerExtensions;
import org.geotools.data.Parameter;
import org.springframework.context.ApplicationContext;

/**
 * Represents the input / output of a parameter in a process.
 * <p>
 * Instances of this interface are registered in a spring context to handle 
 * additional types of 
 * </p>
 * 
 * @author Lucas Reed, Refractions Research Inc
 * @author Justin Deoliveira, OpenGEO
 *
 */
public abstract class ProcessParameterIO {

    /**
     * list of default ppios supported out of the box 
     */
    static List<ProcessParameterIO> defaults;
    static {
        defaults = new ArrayList<ProcessParameterIO>();
        defaults.add( new LiteralPPIO( Double.class ) );
        defaults.add( new LiteralPPIO( Integer.class ) );
        defaults.add( new LiteralPPIO( String.class ) );
        
        defaults.add( new GMLPPIO.GML3.Geometry() );
        defaults.add( new GMLPPIO.GML2.Geometry());
        defaults.add( new WFSPPIO() );
        
    }
    
    public static ProcessParameterIO find(Parameter<?> p,ApplicationContext context) {
        //TODO: come up with some way to flag one as "default"
        List<ProcessParameterIO> all = findAll( p, context );
        if ( all.isEmpty() ) {
            return null;
        }
        
        return all.get( 0 );
    }
    
    public static List<ProcessParameterIO> findAll(Parameter<?> p,ApplicationContext context) {
        //load all extensions
        List<ProcessParameterIO> l = GeoServerExtensions.extensions( ProcessParameterIO.class, context );

        //find parameters that match
        List<ProcessParameterIO> matches = new ArrayList<ProcessParameterIO>();
        
        //do a two phase search, first try to match the identifier
        for ( ProcessParameterIO ppio : l ) {
            if ( ppio.getIdentifer() != null && ppio.getIdentifer().equals( p.key ) && 
                    ppio.getType().isAssignableFrom( p.type ) ) {
                matches.add( ppio );
            }
        }
        
        //if no matches, look for just those which match by type
        if ( matches.isEmpty() ) {
            //add defaults to the mix
            l.addAll( defaults );
            
            for ( ProcessParameterIO ppio : l ) {
                if ( ppio.getType().isAssignableFrom( p.type ) ) {
                    matches.add( ppio );
                }
            }
        }
        
        return matches;
    }
    
    /**
     * java class of parameter when reading and writing i/o.
     */
    final protected Class externalType;
    /**
     * java class of parameter when running internal process.
     */
    final protected Class internalType; 
    
    /**
     * identifier for the parameter
     */
    protected String identifer;
    
    protected ProcessParameterIO( Class externalType, Class internalType ) {
        this( externalType, internalType, null );
    }
    
    protected ProcessParameterIO( Class externalType, Class internalType, String identifier ) {
        this.externalType = externalType;
        this.internalType = internalType;
        this.identifer = identifier;
    }
    
    /**
     * The type of the parameter with regard to doing I/O.
     * <p>
     * The external type is used when reading and writing the parameter from an 
     * external source.
     * </p>
     */
    public final Class getExternalType() {
        return externalType;
    }
    
    /**
     * The type of the parameter corresponding to {@link Parameter#type}.
     * <p>
     * The internal type is used when going to and from the internal process engine. 
     * </p>
     */
    public final Class getType() {
        return internalType;
    }
    
    /**
     * The identifier for the parameter, this value may be null.
     */
    public final String getIdentifer() {
        return identifer;
    }
}
