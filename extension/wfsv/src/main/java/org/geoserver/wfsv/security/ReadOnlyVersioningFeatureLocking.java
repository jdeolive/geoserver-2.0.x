/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfsv.security;

import java.io.IOException;

import org.geoserver.security.SecureCatalogImpl.WrapperPolicy;
import org.geoserver.security.decorators.ReadOnlyFeatureLocking;
import org.geotools.data.FeatureLocking;
import org.geotools.data.Query;
import org.geotools.data.VersioningFeatureLocking;
import org.geotools.data.VersioningFeatureSource;
import org.geotools.data.postgis.FeatureDiffReader;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

/**
 * Read only wrapper around a {@link VersioningFeatureLocking}
 * 
 * @author Andrea Aime - TOPP
 */
public class ReadOnlyVersioningFeatureLocking extends
        ReadOnlyFeatureLocking<SimpleFeatureType, SimpleFeature> implements
        VersioningFeatureLocking {

    ReadOnlyVersioningFeatureLocking(FeatureLocking delegate, WrapperPolicy policy) {
        super(delegate, policy);
    }

    public FeatureDiffReader getDifferences(String fromVersion, String toVersion, Filter filter,
            String[] userIds) throws IOException {
        return ((VersioningFeatureSource) delegate).getDifferences(fromVersion, toVersion, filter,
                userIds);
    }

    public FeatureCollection<SimpleFeatureType, SimpleFeature> getLog(String fromVersion,
            String toVersion, Filter filter, String[] userIds, int maxRows) throws IOException {
        return ((VersioningFeatureSource) delegate).getLog(fromVersion, toVersion, filter, userIds,
                maxRows);
    }

    public FeatureCollection<SimpleFeatureType, SimpleFeature> getVersionedFeatures()
            throws IOException {
        return ((VersioningFeatureSource) delegate).getVersionedFeatures();
    }

    public FeatureCollection<SimpleFeatureType, SimpleFeature> getVersionedFeatures(Query q)
            throws IOException {
        return ((VersioningFeatureSource) delegate).getVersionedFeatures(q);
    }

    public FeatureCollection<SimpleFeatureType, SimpleFeature> getVersionedFeatures(Filter f)
            throws IOException {
        return ((VersioningFeatureSource) delegate).getVersionedFeatures(f);
    }

    public void rollback(String toVersion, Filter filter, String[] users) throws IOException {
        throw unsupportedOperation();
    }

}
