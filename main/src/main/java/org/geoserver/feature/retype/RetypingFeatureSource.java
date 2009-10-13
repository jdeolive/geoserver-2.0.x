/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.feature.retype;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.geoserver.feature.RetypingFeatureCollection;
import org.geotools.data.DataStore;
import org.geotools.data.DefaultQuery;
import org.geotools.data.FeatureListener;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.QueryCapabilities;
import org.geotools.data.ResourceInfo;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;

/**
 * Renaming wrapper for a {@link FeatureSource} instance, to be used along with
 * {@link RetypingDataStore}
 */
public class RetypingFeatureSource implements FeatureSource<SimpleFeatureType, SimpleFeature>{

    FeatureSource<SimpleFeatureType, SimpleFeature> wrapped;

    FeatureTypeMap typeMap;

    RetypingDataStore store;

    Map listeners = new HashMap();

    RetypingFeatureSource(RetypingDataStore ds,
            FeatureSource<SimpleFeatureType, SimpleFeature> wrapped, FeatureTypeMap typeMap) {
        this.store = ds;
        this.wrapped = wrapped;
        this.typeMap = typeMap;
    }

    /**
     * Returns the same name than the feature type (ie,
     * {@code getSchema().getName()} to honor the simple feature land common
     * practice of calling the same both the Features produces and their types
     * 
     * @since 1.7
     * @see FeatureSource#getName()
     */
    public Name getName() {
        return getSchema().getName();
    }

    public void addFeatureListener(FeatureListener listener) {
        FeatureListener wrapper = new WrappingFeatureListener(this, listener);
        listeners.put(listener, wrapper);
        wrapped.addFeatureListener(wrapper);
    }

    public void removeFeatureListener(FeatureListener listener) {
        FeatureListener wrapper = (FeatureListener) listeners.get(listener);
        if (wrapper != null) {
            wrapped.removeFeatureListener(wrapper);
            listeners.remove(listener);
        }
    }

    public ReferencedEnvelope getBounds() throws IOException {
        // not fully correct if we use this to shave attributes too, but this is
        // not in the scope now
        return wrapped.getBounds();
    }

    public ReferencedEnvelope getBounds(Query query) throws IOException {
        // not fully correct if we use this to shave attributes too, but this is
        // not in the scope now
        return wrapped.getBounds(store.retypeQuery(query, typeMap));
    }

    public int getCount(Query query) throws IOException {
        return wrapped.getCount(store.retypeQuery(query, typeMap));
    }

    public DataStore getDataStore() {
        return store;
    }

    public FeatureCollection<SimpleFeatureType, SimpleFeature> getFeatures() throws IOException {
        return getFeatures(Query.ALL);
    }

    public FeatureCollection<SimpleFeatureType, SimpleFeature> getFeatures(Query query) throws IOException {
        if (query.getTypeName() == null) {
            query = new DefaultQuery(query);
            ((DefaultQuery) query).setTypeName(typeMap.getName());
        } else if (!typeMap.getName().equals(query.getTypeName())) {
            throw new IOException("Cannot query this feature source with " + query.getTypeName()
                    + " since it serves only " + typeMap.getName());
        }
        
        //GEOS-3210, if the query specifies a subset of property names we need to take that into 
        // account
        SimpleFeatureType target = typeMap.getFeatureType();
        if ( query.getPropertyNames() != Query.ALL_NAMES ) {
            target = SimpleFeatureTypeBuilder.retype(target, query.getPropertyNames());
        }
        return new RetypingFeatureCollection(wrapped.getFeatures(store.retypeQuery(query, typeMap)),
                target);
    }

    public FeatureCollection<SimpleFeatureType, SimpleFeature> getFeatures(Filter filter) throws IOException {
        return getFeatures(new DefaultQuery(typeMap.getName(), filter));
    }

    public SimpleFeatureType getSchema() {
        return typeMap.getFeatureType();
    }

    public Set getSupportedHints() {
        return wrapped.getSupportedHints();
    }

    public ResourceInfo getInfo() {
        return wrapped.getInfo();
    }

    public QueryCapabilities getQueryCapabilities() {
        return wrapped.getQueryCapabilities();
    }

}
