/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.io.IOException;

import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.SecureCatalogImpl.Response;
import org.geoserver.security.SecureCatalogImpl.WrapperPolicy;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;

/**
 * Given a {@link DataStore} subclass makes sure no write operations can be
 * performed through it
 * 
 * @author Andrea Aime - TOPP
 */
public class ReadOnlyDataStore extends DecoratingDataStore {
    

    WrapperPolicy policy;

    protected ReadOnlyDataStore(DataStore delegate, WrapperPolicy policy) {
        super(delegate);
        this.policy = policy;
    }

    @Override
    public FeatureSource<SimpleFeatureType, SimpleFeature> getFeatureSource(Name typeName)
            throws IOException {
        final FeatureSource<SimpleFeatureType, SimpleFeature> fs = super.getFeatureSource(typeName);
        return wrapFeatureSource(fs);
    }

    @Override
    public FeatureSource<SimpleFeatureType, SimpleFeature> getFeatureSource(String typeName)
            throws IOException {
        final FeatureSource<SimpleFeatureType, SimpleFeature> fs = super.getFeatureSource(typeName);
        return wrapFeatureSource(fs);
            
    }

    @SuppressWarnings("unchecked")
    FeatureSource<SimpleFeatureType, SimpleFeature> wrapFeatureSource(
            final FeatureSource<SimpleFeatureType, SimpleFeature> fs) {
        if (fs == null)
            return null;
        
        return (FeatureSource) SecuredObjects.secure(fs, policy);
    }

    @Override
    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter(String typeName,
            Filter filter, Transaction transaction) throws IOException {
        throw notifyUnsupportedOperation();
    }

    @Override
    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter(String typeName,
            Transaction transaction) throws IOException {
        throw notifyUnsupportedOperation();
    }

    @Override
    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriterAppend(String typeName,
            Transaction transaction) throws IOException {
        throw notifyUnsupportedOperation();
    }

    @Override
    public void updateSchema(Name typeName, SimpleFeatureType featureType) throws IOException {
        throw notifyUnsupportedOperation();
    }

    @Override
    public void updateSchema(String typeName, SimpleFeatureType featureType) throws IOException {
        throw notifyUnsupportedOperation();
    }

    @Override
    public void createSchema(SimpleFeatureType featureType) throws IOException {
        throw notifyUnsupportedOperation();
    }
    
    /**
     * Notifies the caller the requested operation is not supported, using a plain {@link UnsupportedOperationException}
     * in case we have to conceal the fact the data is actually writable, using an Acegi security exception otherwise
     * to force an authentication from the user
     */
    protected RuntimeException notifyUnsupportedOperation() {
        if(policy.response == Response.CHALLENGE) {
            return SecureCatalogImpl.unauthorizedAccess();
        } else
            return new UnsupportedOperationException("This datastore is read only");
    }

}
