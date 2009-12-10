/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.Info;
import org.geoserver.ows.util.ClassProperties;
import org.geoserver.ows.util.OwsUtils;

/**
 * Proxies an object storing any modifications to it.
 * <p>
 * Each time a setter is called through this invocation handler, the property
 * is stored and not set on the underlying object being proxied until 
 * {@link #commit()} is called. When a getter is called through this invocation
 * handler, the local properties are checked for one that has been previously 
 * set, if found it is returned, if not found the getter is forwarded to the 
 * underlying proxy object being called.  
 * </p>
 * <p>
 * Any collections handled through this interface are cloned and client code 
 * obtains a copy. The two collections will be synced on a call to {@link #commit()}.
 * </p>
 * 
 * @author Justin Deoliveira, The Open Planning Project
 * 
 * TODO: this class should use BeanUtils for all reflection stuff
 *
 */
public class ModificationProxy implements InvocationHandler, Serializable {

    /** 
     * the proxy object 
     */
    Object proxyObject;
    
    /**
     * reflection helper
     */
    transient ClassProperties cp;
    
    /** 
     * "dirty" properties 
     */
    HashMap<String,Object> properties;

    public ModificationProxy(Object proxyObject) {
        this.proxyObject = proxyObject;
    }

    private ClassProperties cp(){
        if(cp == null){
            this.cp = OwsUtils.getClassProperties(proxyObject.getClass());
        }
        return cp;
    }
    
    /**
     * Intercepts getter and setter methods.
     */
    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
        
        String property = null;
        if ( ( method.getName().startsWith( "get")  || method.getName().startsWith( "is" ) ) 
                && method.getParameterTypes().length == 0 ) {
            //intercept getter to check the dirty property set
            property = method.getName().substring( 
                method.getName().startsWith( "get") ? 3 : 2 );
            if ( properties != null && properties().containsKey( property ) ) {
                //return the previously set object
                return properties().get( property );
            }
            else {
                //if collection, create a wrapper
                if ( Collection.class.isAssignableFrom( method.getReturnType() ) ) {
                    Collection real = (Collection) method.invoke( proxyObject, null );
                    Collection wrap = real.getClass().newInstance();
                    wrap.addAll( real );
                    properties().put( property, wrap );
                    return wrap;
                }
                else {
                  //proceed with the invocation    
                }
                
            }
            
        }
        if ( method.getName().startsWith( "set") && args.length == 1) {
            //intercept setter and put new value in list
            property = method.getName().substring( 3 );
            properties().put( property, args[0] );
            
            return null;
        }

        try{
            Object result = method.invoke( proxyObject, args ); 

            //intercept result and wrap it in a proxy if it is another Info object
            if ( result != null && shouldProxyProperty(result.getClass())) {
                //avoid double proxy
                Object o = ModificationProxy.unwrap( result );
                if ( o == result ) {
                    result = ModificationProxy.create( result, (Class) method.getReturnType() );
                    
                    //cache the proxy, in case it is modified itself
                    properties().put( property, result );
                }
                else {
                    //result was already proxied, leave as is
                }
            }
            return result;
        }catch(InvocationTargetException e){
            Throwable targetException = e.getTargetException();
            throw targetException;
        }
    }
    
    public Object getProxyObject() {
        return proxyObject;
    }
    
    public HashMap<String,Object> getProperties() {
        return properties();
    }
    
    public void commit() {
        synchronized (proxyObject) {
            //commit changes to the proxy object
            for ( Map.Entry<String,Object> e : properties().entrySet() ) {
                String p = e.getKey();
                Object v = e.getValue();
                
                //use the getter to figure out the type for the setter
                try {
                    Method g = getter(p);
                    
                    //handle collection case
                    if ( Collection.class.isAssignableFrom( g.getReturnType() ) ) {
                        Collection c = (Collection) g.invoke(proxyObject,null);
                        c.clear();
                        c.addAll( (Collection) v );
                    }
                    else {
                        Method s = setter(p,g.getReturnType());
                        
                        if ( Info.class.isAssignableFrom( g.getReturnType() ) ) {
                            //another info is the changed property, it could be one of two cases
                            // 1) the info object was changed in place: x.getY().setFoo(...)
                            // 2) a new info object was set x.setY(...)
                            Info original = (Info) g.invoke(proxyObject, null);
                            Info modified = (Info) unwrap(v);
                            if ( original == modified ) {
                                //case 1, in this case get the proxy and commit it
                                if ( v instanceof Proxy ) {
                                    ModificationProxy h = handler( v );
                                    if ( h != null && h.isDirty() ) {
                                        h.commit();
                                    }
                                }
                            }
                            else if ( s != null ){
                                //case 2, just call the setter with the new object
                                s.invoke( proxyObject, v );
                            }
                            else {
                                throw new IllegalStateException( "New info object set, but no setter for it.");
                            }
                        }
                        else {
                            //call the setter
                            s.invoke( proxyObject, v );
                        }
                    }
                } 
                catch( Exception ex ) {
                    throw new RuntimeException( ex );
                }
            } 
            
            //reset
            properties = null;
        }
    }
    
    /**
     * Helper method for determining if a property of a proxied object should also 
     * be proxied.
     */
    boolean shouldProxyProperty(Class propertyType) {
        if (Catalog.class.isAssignableFrom(propertyType)) {
            //never proxy the catalog
            return false;
        }
        return Info.class.isAssignableFrom(propertyType); 
    }
    
    HashMap<String,Object> properties() {
        if ( properties != null ) {
            return properties;
        }
        
        synchronized (this) {
            if ( properties != null ) {
                return properties;
            }
            
            properties = new HashMap<String,Object>();
        }
        
        return properties;
    }
    
    /**
     * Flag which indicates whether any properties of the object being proxied 
     * are changed.
     */
    public boolean isDirty() {
        boolean dirty = false;
        for ( Iterator i = properties().entrySet().iterator(); i.hasNext() && !dirty; ) {
            Map.Entry e = (Map.Entry) i.next();
            if ( e.getValue() instanceof Proxy ) {
                ModificationProxy h = handler( e.getValue() );
                if ( h != null && !h.isDirty() ) {
                    continue;
                }
            }
            
            dirty = true;
        }
        return dirty;
    }
    
    List<String> getDirtyProperties() {
        List<String> propertyNames = new ArrayList<String>();
        
        for ( String propertyName : properties().keySet() ) {
            //in the case this property is another proxy, check that it is actually dirty
            Object value = properties.get( propertyName );
            if ( value instanceof Proxy ) {
                ModificationProxy h = handler( value );
                if (h != null && !h.isDirty()) {
                    //proxy reports it is not dirty, only return this property if the underling
                    // value is not the same as the current value of the property on the object
                    Object curr = unwrap( value );
                    try {
                        Object orig = unwrap( getter( propertyName ).invoke( proxyObject, null));
                        if ( curr == orig ) {
                            continue;
                        }
                    } 
                    catch (Exception e) {
                        throw new RuntimeException( e );
                    }
                }
            }
            propertyNames.add( propertyName );
        }
        
        return propertyNames;
    }
    
    /**
     * Returns the names of any changed properties.
     */
    public List<String> getPropertyNames() {
        List<String> propertyNames = getDirtyProperties();
        
        for ( int i = 0; i < propertyNames.size(); i++ ) {
            String name = propertyNames.get( i );
            propertyNames.set( i , Character.toLowerCase( name.charAt( 0 ) )
                    + name.substring(1) );
        }
        
        return propertyNames;
    }
    
    /**
     * Returns the old values of any changed properties.
     */
    public List<Object> getOldValues() {
        List<Object> oldValues = new ArrayList<Object>();
        for ( String propertyName : getDirtyProperties() ) {
            try {
                Method g = getter(propertyName);
                if ( g == null ) {
                    throw new IllegalArgumentException( "No such property: " + propertyName );
                }
                
                oldValues.add( g.invoke( proxyObject, null ) );
            }
            catch (Exception e) {
                throw new RuntimeException( e );
            }
        }
        
        return oldValues;
    }
    
    /**
     * Returns the new values of any changed properties.
     */
    public List<Object> getNewValues() {
        ArrayList newValues = new ArrayList();
        for ( String propertyName : getDirtyProperties()) {
            newValues.add( properties().get( propertyName ) );
        }
        return newValues;
    }
    
    /*
     * Helper method for looking up a getter method.
     */
    Method getter( String propertyName ) {
        Method g = null;
        try {
            g = proxyObject.getClass().getMethod( "get" + propertyName , null );
        }
        catch( NoSuchMethodException e1 ) {
            //could be boolean
            try {
                g = proxyObject.getClass().getMethod( "is" + propertyName , null );    
            }
            catch( NoSuchMethodException e2 ) {}
        }
        
        if ( g == null ) {
            g = cp().getter(propertyName, null);
        }
        
        return g;
    }

    /*
     * Helper method for looking up a getter method.
     */
    Method setter( String propertyName, Class type ) {
        Method s = null;
        try {
            s = proxyObject.getClass().getMethod( "set" + propertyName, type );
        }
        catch( NoSuchMethodException e ) {
            s = cp().setter(propertyName, type);
        }
        return s;
    }

    /**
     * Wraps an object in a proxy.
     * 
     * @throws RuntimeException If creating the proxy fails.
     */
    public static <T> T create( T proxyObject, Class<T> clazz ) {
        InvocationHandler h = new ModificationProxy( proxyObject );
        
        // proxy all interfaces implemented by the source object
        List<Class> proxyInterfaces = (List) Arrays.asList( proxyObject.getClass().getInterfaces() );
        
        // ensure that the specified class is included
        boolean add = true;
        for ( Class interfce : proxyObject.getClass().getInterfaces() ) {
            if ( clazz.isAssignableFrom( interfce) ) {
                add = false;
                break;
            }
        }
        if( add ) {
            // make the list mutable (Arrays.asList is not) and then add the extra interfaces
            proxyInterfaces = new ArrayList<Class>(proxyInterfaces);
            proxyInterfaces.add( clazz );
        }
        
        Class proxyClass = Proxy.getProxyClass( clazz.getClassLoader(), 
            (Class[]) proxyInterfaces.toArray(new Class[proxyInterfaces.size()]) );
        
        T proxy;
        try {
            proxy = (T) proxyClass.getConstructor(
                new Class[] { InvocationHandler.class }).newInstance(new Object[] { h } );
        }
        catch( Exception e ) {
            throw new RuntimeException( e );
        }
        
        return proxy;
    }
    
    /**
     * Wraps a list in a decorator which proxies each item in the list.
     *
     */
    public static <T> List<T> createList( List<T> proxyList, Class<T> clazz ) {
        return new list( proxyList, clazz );
    }
    
    /**
     * Wraps a proxy instance.
     * <p>
     * This method is safe in that if the object passed in is not a proxy it is
     * simply returned. If the proxy is not an instance of {@link ModificationProxy}
     * it is also returned untouched. 
     *</p>
     * 
     */
    public static <T> T unwrap( T object ) {
        if ( object instanceof Proxy ) {
            ModificationProxy h = handler( object );
            if ( h != null ) {
                return (T) h.getProxyObject();
            }
        }
        if ( object instanceof ProxyList ) {
            return (T) ((ProxyList)object).proxyList;
        }
        
        return object;
    }
    
    /**
     * Returns the ModificationProxy invocation handler for an proxy object.
     * <p>
     * This method will return null in the case where the object is not a proxy, or
     * it is being proxies by another invocation handler.
     * </p>
     */
    public static ModificationProxy handler( Object object ) {
        if ( object instanceof Proxy ) {
            InvocationHandler h = Proxy.getInvocationHandler( object );
            if ( h instanceof ModificationProxy ) {
                return (ModificationProxy) h;
            }
        }
        
        return null;
    }
    static class list<T> extends ProxyList {

        list( List<T> list, Class<T> clazz ) {
            super( list, clazz );
        }
        
        protected <T> T createProxy(T proxyObject, Class<T> proxyInterface) {
            return ModificationProxy.create( proxyObject, proxyInterface );
        }
        
        protected <U> U unwrapProxy(U proxy, java.lang.Class<U> proxyInterface) {
            return ModificationProxy.unwrap( proxy );
        };
    }
}
