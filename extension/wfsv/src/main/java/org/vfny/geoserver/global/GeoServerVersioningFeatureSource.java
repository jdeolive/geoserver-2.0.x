/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.vfny.geoserver.global;

import org.geotools.data.DataSourceException;
import org.geotools.data.DefaultQuery;
import org.geotools.data.Query;
import org.geotools.data.VersioningFeatureLocking;
import org.geotools.data.VersioningFeatureSource;
import org.geotools.data.VersioningFeatureStore;
import org.geotools.data.postgis.FeatureDiffReader;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import java.io.IOException;


/**
 * Versioning wrapper that preserves the versioning nature of a feature source
 *
 * @TODO: once the experiment is over, move this back to the main module
 * @author Andrea Aime, TOPP
 *
 */
public class GeoServerVersioningFeatureSource extends GeoServerFeatureSource implements
        VersioningFeatureSource {
    GeoServerVersioningFeatureSource(VersioningFeatureSource source, SimpleFeatureType schema,
        Filter definitionQuery, CoordinateReferenceSystem declaredCRS, int srsHandling) {
        super(source, schema, definitionQuery, declaredCRS, srsHandling);
    }

    public FeatureDiffReader getDifferences(String fromVersion, String toVersion, Filter filter, String[] users)
        throws IOException {
        // TODO: if we are bound to a smaller schema, we should remove the
        // hidden attributes from the differences
        return ((VersioningFeatureSource) source).getDifferences(fromVersion, toVersion, filter, users);
    }

    public FeatureCollection<SimpleFeatureType, SimpleFeature> getLog(String fromVersion,
            String toVersion, Filter filter, String[] users, int maxFeatures) throws IOException {
        return ((VersioningFeatureSource) source).getLog(fromVersion, toVersion, filter, users, maxFeatures);
    }

    /**
     * Factory that make the correct decorator for the provided featureSource.
     *
     * <p>
     * This factory method is public and will be used to create all required
     * subclasses. By comparison the constructors for this class have package
     * visibiliy.
     * </p>
     *
     * @param featureSource
     * @param schema
     * @param definitionQuery
     * @param declaredCRS
     *            
     *
     * @return
     */
    public static GeoServerFeatureSource create(VersioningFeatureSource featureSource,
        SimpleFeatureType schema, Filter definitionQuery, CoordinateReferenceSystem declaredCRS, int srsHandling) {
        if (featureSource instanceof VersioningFeatureLocking) {
            return new GeoServerVersioningFeatureLocking((VersioningFeatureLocking) featureSource,
                schema, definitionQuery, declaredCRS, srsHandling);
        } else if (featureSource instanceof VersioningFeatureStore) {
            return new GeoServerVersioningFeatureStore((VersioningFeatureStore) featureSource,
                schema, definitionQuery, declaredCRS, srsHandling);
        }

        return new GeoServerVersioningFeatureSource(featureSource, schema, definitionQuery,
            declaredCRS, srsHandling);
    }
    
    

    public FeatureCollection<SimpleFeatureType, SimpleFeature> getVersionedFeatures(Query query)
            throws IOException {
        final VersioningFeatureSource versioningSource = ((VersioningFeatureSource) source);
        Query newQuery = adaptQuery(query, versioningSource.getVersionedFeatures().getSchema());
        
        CoordinateReferenceSystem targetCRS = query.getCoordinateSystemReproject();
        try {
            //this is the raw "unprojected" feature collection
            
            FeatureCollection<SimpleFeatureType, SimpleFeature> fc;
            fc = versioningSource.getVersionedFeatures(newQuery);

            return reprojectFeatureCollection(targetCRS, fc);
        } catch (Exception e) {
            throw new DataSourceException(e);
        }
    }

    public FeatureCollection<SimpleFeatureType, SimpleFeature> getVersionedFeatures(Filter filter)
            throws IOException {
        return getFeatures(new DefaultQuery(schema.getTypeName(), filter));
    }

    public FeatureCollection<SimpleFeatureType, SimpleFeature> getVersionedFeatures()
            throws IOException {
        return getFeatures(Query.ALL);
    }
}
