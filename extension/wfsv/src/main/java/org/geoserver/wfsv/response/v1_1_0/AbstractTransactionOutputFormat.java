/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfsv.response.v1_1_0;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import net.opengis.wfs.BaseRequestType;
import net.opengis.wfs.DeleteElementType;
import net.opengis.wfs.InsertElementType;
import net.opengis.wfs.PropertyType;
import net.opengis.wfs.TransactionType;
import net.opengis.wfs.UpdateElementType;
import net.opengis.wfs.WfsFactory;
import net.opengis.wfsv.GetDiffType;

import org.eclipse.emf.common.util.EList;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.ows.Response;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.ows.util.RequestUtils;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;

import org.geoserver.wfs.WFSException;
import org.geoserver.wfs.WFSInfo;
import org.geotools.data.postgis.FeatureDiff;
import org.geotools.data.postgis.FeatureDiffReader;
import org.geotools.xml.Configuration;
import org.geotools.xml.Encoder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.identity.FeatureId;



/**
 * WFS output format for a GetDiff operation whose output format is a WFS 1.1
 * transaction
 *
 * @author Andrea Aime, TOPP
 *
 */
public abstract class AbstractTransactionOutputFormat extends Response {
    /**
     * WFS configuration
     */
    WFSInfo wfs;
    Catalog catalog;
    GeoServerInfo global;

    /**
     * Xml configuration
     */
    Configuration configuration;

    /**
     * Filter factory used to build fid filters
     */
    FilterFactory filterFactory;
    
    /**
     * The element to be encoded
     */
    QName element;
    
    /**
     * The mime type for this output format
     */
    String mime;

    public AbstractTransactionOutputFormat(GeoServer gs, Configuration configuration,
        FilterFactory filterFactory, QName element, String mime) {
        super(FeatureDiffReader[].class);

        this.wfs = gs.getService( WFSInfo.class );
        this.global = gs.getGlobal();
        this.configuration = configuration;
        this.catalog = gs.getCatalog();
        this.filterFactory = filterFactory;
        this.element = element;
        this.mime = mime;
    }

    /**
     * @return "text/xml";
     */
    public String getMimeType(Object value, Operation operation)
        throws ServiceException {
        return "text/xml; subtype=wfs-transaction/1.1.0";
    }

    /**
     * Checks that the resultType is of type "hits".
     */
    public boolean canHandle(Operation operation) {
        GetDiffType request = (GetDiffType) OwsUtils.parameter(operation.getParameters(),
                GetDiffType.class);

        return (request != null)
        && request.getOutputFormat().equals(mime);
    }

    public void write(Object value, OutputStream output, Operation operation)
        throws IOException, ServiceException {
        final FeatureDiffReader[] diffReaders = (FeatureDiffReader[]) value;

        // create a new feature collcetion type with just the numbers
        final TransactionType transaction = WfsFactory.eINSTANCE.createTransactionType();

        for (int i = 0; i < diffReaders.length; i++) {
            final FeatureDiffReader diffReader = diffReaders[i];

            // create a single insert element, a single delete element, and as
            // many update elements as needed
            final SimpleFeatureType schema = diffReader.getSchema();
            final QName typeName = new QName(schema.getName().getNamespaceURI(),
                    schema.getTypeName());
            final Set deletedIds = new HashSet();
            final InsertElementType insert = WfsFactory.eINSTANCE.createInsertElementType();

            while (diffReader.hasNext()) {
                FeatureDiff diff = diffReader.next();

                switch (diff.getState()) {
                case FeatureDiff.INSERTED:
                    insert.getFeature().add(diff.getFeature());

                    break;

                case FeatureDiff.DELETED:
                    deletedIds.add(filterFactory.featureId(diff.getID()));

                    break;

                case FeatureDiff.UPDATED:

                    final UpdateElementType update = WfsFactory.eINSTANCE.createUpdateElementType();
                    final EList properties = update.getProperty();

                    SimpleFeature f = diff.getFeature();

                    for (Iterator it = diff.getChangedAttributes().iterator(); it.hasNext();) {
                        final PropertyType property = WfsFactory.eINSTANCE.createPropertyType();
                        String name = (String) it.next();
                        property.setName(new QName(name));
                        property.setValue(f.getAttribute(name));
                        properties.add(property);
                    }

                    FeatureId featureId = filterFactory.featureId(diff.getID());
                    final Filter filter = filterFactory.id(Collections.singleton(featureId));
                    update.setFilter(filter);
                    update.setTypeName(typeName);
                    transaction.getUpdate().add(update);

                    break;

                default:
                    throw new WFSException("Could not handle diff type " + diff.getState());
                }
            }

            // create insert and delete elements if needed
            if (insert.getFeature().size() > 0) {
                transaction.getInsert().add(insert);
            }

            if (deletedIds.size() > 0) {
                final DeleteElementType delete = WfsFactory.eINSTANCE.createDeleteElementType();
                delete.setFilter(filterFactory.id(deletedIds));
                delete.setTypeName(typeName);
                transaction.getDelete().add(delete);
            }
        }
        
        //declare wfs schema location
        BaseRequestType gft = (BaseRequestType) operation.getParameters()[0];

        Encoder encoder = new Encoder(configuration, configuration.schema());
        encodeWfsSchemaLocation(encoder, gft.getBaseUrl());

        encoder.setIndenting(true);
        encoder.setEncoding(Charset.forName( global.getCharset() ));

        // set up schema locations
        // round up the info objects for each feature collection
        HashMap /* <String,Set> */ ns2metas = new HashMap();

        for (int i = 0; i < diffReaders.length; i++) {
            final FeatureDiffReader diffReader = diffReaders[i];
            final SimpleFeatureType featureType = diffReader.getSchema();

            // load the metadata for the feature type
            String namespaceURI = featureType.getName().getNamespaceURI();
            FeatureTypeInfo meta = catalog.getFeatureTypeByName( namespaceURI, featureType.getName().getLocalPart() );

            // add it to the map
            Set metas = (Set) ns2metas.get(namespaceURI);

            if (metas == null) {
                metas = new HashSet();
                ns2metas.put(namespaceURI, metas);
            }

            metas.add(meta);
        }

        // declare application schema namespaces
        for (Iterator i = ns2metas.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();

            String namespaceURI = (String) entry.getKey();
            Set metas = (Set) entry.getValue();

            StringBuffer typeNames = new StringBuffer();

            for (Iterator m = metas.iterator(); m.hasNext();) {
                FeatureTypeInfo meta = (FeatureTypeInfo) m.next();
                typeNames.append(meta.getName());

                if (m.hasNext()) {
                    typeNames.append(",");
                }
            }

            // set the schema location
            encodeTypeSchemaLocation(encoder, gft.getBaseUrl(), namespaceURI, typeNames);
        }

        try {
            encoder.encode(transaction, element, output);
        } finally {
        	for (int i = 0; i < diffReaders.length; i++) {
				diffReaders[i].close();
			}
        }
    }

    protected abstract void encodeTypeSchemaLocation(Encoder encoder, String baseURL,
            String namespaceURI, StringBuffer typeNames);
    

    protected abstract void encodeWfsSchemaLocation(Encoder encoder, String baseURL);
    
}
