/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs;

import net.opengis.wfs.TransactionResponseType;
import net.opengis.wfs.TransactionType;
import org.eclipse.emf.ecore.EObject;
import org.geotools.data.FeatureStore;
import java.util.Map;
import javax.xml.namespace.QName;


/**
 * Transaction elements are an open ended set, both thanks to the Native element
 * type, and to the XSD sustitution group concept (xsd inheritance). Element
 * handlers know how to process a certain element in a wfs transaction request.
 *
 * @author Andrea Aime - TOPP
 *
 */
public interface TransactionElementHandler {
    /**
     * Returns the element class this handler can proces
     */
    Class getElementClass();

    /**
     * Returns the qualified names of feature types needed to handle this
     * element
     */
    QName[] getTypeNames(EObject element) throws WFSTransactionException;

    /**
     * Checks the element content is valid, throws an exception otherwise
     *
     * @param element
     *            the transaction element we're checking
     * @param featureTypeInfos
     *            a map from {@link QName} to {@link FeatureTypeInfo}, where
     *            the keys contain all the feature type names reported by
     *            {@link #getTypeNames(EObject)}
     */
    void checkValidity(EObject element, Map featureTypeInfos)
        throws WFSTransactionException;

    /**
     * Executes the element against the provided feature sources
     *
     * @param element
     *            the tranaction element to be executed
     * @param request
     *            the transaction request
     * @param featureStores
     *            map from {@link QName} to {@link FeatureStore}, where the
     *            keys do contain all the feature type names reported by
     *            {@link #getTypeNames(EObject)}
     * @param response
     *            the transaction response, that the element will update
     *            according to the processing done
     * @param listener
     *            a transaction listener that will be called before and after
     *            each change performed against the data stores
     */
    void execute(EObject element, TransactionType request, Map featureStores,
        TransactionResponseType response, TransactionListener listener)
        throws WFSTransactionException;
}
