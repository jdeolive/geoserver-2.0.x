/*
 * Copyright (c) 2001 - 2009 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.geotools.data.complex.AppSchemaDataAccess;
import org.geotools.data.complex.DataAccessRegistry;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Abstract base class for WFS test cases that test integration of {@link AppSchemaDataAccess} with
 * GeoServer.
 * 
 * <p>
 * 
 * The implementation takes care to ensure that private {@link XMLUnit} namespace contexts are used
 * for each mock data instance, to avoid collisions. Use of static {@link XMLAssert} methods risks
 * collisions in the static namespace context. This class avoids such problems by providing its own
 * instance methods like those in XMLAssert.
 * 
 * @author Ben Caradoc-Davies, CSIRO Exploration and Mining
 */
public abstract class AbstractAppSchemaWfsTestSupport extends GeoServerAbstractTestSupport {

    /**
     * WFS namespaces, for use by XMLUnit. A seen in WFSTestSupport, plus xlink.
     */
    @SuppressWarnings("serial")
    private final Map<String, String> WFS_NAMESPACES = Collections
            .unmodifiableMap(new HashMap<String, String>() {
                {
                    put("wfs", "http://www.opengis.net/wfs");
                    put("ows", "http://www.opengis.net/ows");
                    put("ogc", "http://www.opengis.net/ogc");
                    put("xs", "http://www.w3.org/2001/XMLSchema");
                    put("xsd", "http://www.w3.org/2001/XMLSchema");
                    put("gml", "http://www.opengis.net/gml");
                    put("xlink", "http://www.w3.org/1999/xlink");
                    put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
                }
            });

    /**
     * The XpathEngine to be used for this namespace context.
     */
    private XpathEngine xpathEngine;

    /**
     * Subclasses override this to construct the test data.
     * 
     * <p>
     * 
     * Override to narrow return type and remove checked exception.
     * 
     * @see org.geoserver.test.GeoServerAbstractTestSupport#buildTestData()
     */
    @Override
    protected abstract NamespaceTestData buildTestData();

    /**
     * Return the test data.
     * 
     * <p>
     * 
     * Override to narrow return type.
     * 
     * @see org.geoserver.test.GeoServerAbstractTestSupport#getTestData()
     */
    @Override
    public NamespaceTestData getTestData() {
        return (NamespaceTestData) super.getTestData();
    }  
    
    /**
     * Unregister all data access from registry to avoid stale data access being used by other unit
     * tests.
     */
    @Override
    protected void oneTimeTearDown() throws Exception {
        super.oneTimeTearDown();
        DataAccessRegistry.unregisterAll();
    }

    /**
     * Return the response for a GET request for a path (starts with "wfs?").
     * 
     * <p>
     * 
     * Override to remove checked exception.
     * 
     * @see org.geoserver.test.GeoServerAbstractTestSupport#getAsDOM(java.lang.String)
     */
    @Override
    protected Document getAsDOM(String path) {
        try {
            return super.getAsDOM(path);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Return the response for a POST request to a path (typically "wfs"). The request XML is a
     * String.
     * 
     * <p>
     * 
     * Override to remove checked exception.
     * 
     * @see org.geoserver.test.GeoServerAbstractTestSupport#postAsDOM(java.lang.String,
     *      java.lang.String)
     */
    @Override
    protected Document postAsDOM(String path, String xml) {
        try {
            return super.postAsDOM(path, xml);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Return the XpathEngine, configured for this namespace context.
     * 
     * <p>
     * 
     * Note that the engine is configured lazily, to ensure that the mock data has been created and
     * is ready to report data namespaces, which are then put into the namespace context.
     * 
     * @return configured XpathEngine
     */
    private XpathEngine getXpathEngine() {
        if (xpathEngine == null) {
            xpathEngine = XMLUnit.newXpathEngine();
            Map<String, String> namespaces = new HashMap<String, String>();
            namespaces.putAll(WFS_NAMESPACES);
            namespaces.putAll(getTestData().getNamespaces());
            xpathEngine.setNamespaceContext(new SimpleNamespaceContext(namespaces));
        }
        return xpathEngine;
    }

    /**
     * Return the flattened value corresponding to an XPath expression from a document.
     * 
     * @param xpath
     *            XPath expression
     * @param document
     *            the document under test
     * @return flattened string value
     */
    protected String evaluate(String xpath, Document document) {
        try {
            return getXpathEngine().evaluate(xpath, document);
        } catch (XpathException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Return the list of nodes in a document that match an XPath expression.
     * 
     * @param xpath
     *            XPath expression
     * @param document
     *            the document under test
     * @return list of matching nodes
     */
    private NodeList getMatchingNodes(String xpath, Document document) {
        try {
            return getXpathEngine().getMatchingNodes(xpath, document);
        } catch (XpathException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Assertion that the flattened value of an XPath expression in document is equal to the
     * expected value.
     * 
     * @param expected
     *            expected value of expression
     * @param xpath
     *            XPath expression
     * @param document
     *            the document under test
     */
    protected void assertXpathEvaluatesTo(String expected, String xpath, Document document) {
        assertEquals(expected, evaluate(xpath, document));
    }

    /**
     * Assert that there are count matches of and XPath expression in a document.
     * 
     * @param count
     *            expected number of matches
     * @param xpath
     *            XPath expression
     * @param document
     *            document under test
     */
    protected void assertXpathCount(int count, String xpath, Document document) {
        assertEquals(count, getMatchingNodes(xpath, document).getLength());
    }

    /**
     * Assert that the flattened value of an XPath expression in a document matches a regular
     * expression.
     * 
     * @param regex
     *            regular expression that must be matched
     * @param xpath
     *            XPath expression
     * @param document
     *            document under test
     * @throws Exception
     */
    protected void assertXpathMatches(String regex, String xpath, Document document) {
        assertTrue(evaluate(xpath, document).matches(regex));
    }

    /**
     * Assert that the flattened value of an XPath expression in a document doe not match a regular
     * expression.
     * 
     * @param regex
     *            regular expression that must not be matched
     * @param xpath
     *            XPath expression
     * @param document
     *            document under test
     * @throws Exception
     */
    protected void assertXpathNotMatches(String regex, String xpath, Document document) {
        assertFalse(evaluate(xpath, document).matches(regex));
    }

    /**
     * Return {@link Document} as a pretty-printed string.
     * 
     * @param document
     *            document to be prettified
     * @return the prettified string
     */
    protected String prettyString(Document document) {
        OutputStream output = new ByteArrayOutputStream();
        prettyPrint(document, output);
        return output.toString();
    }

    /**
     * Pretty-print a {@link Document} to an {@link OutputStream}.
     * 
     * @param document
     *            document to be prettified
     * @param output
     *            stream to which output is written
     */
    protected void prettyPrint(Document document, OutputStream output) {
        OutputFormat format = new OutputFormat(document);
        format.setLineWidth(80);
        format.setIndenting(true);
        format.setIndent(4);
        XMLSerializer serializer = new XMLSerializer(output, format);
        try {
            serializer.serialize(document);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Find the first file matching the supplied path, starting from the supplied root. This doesn't
     * support multiple matching files.
     * 
     * @param path
     *            Supplied path
     * @param root
     *            Directory to start searching from
     * @return Matching file
     */
    protected File findFile(String path, File root) {
        File target = null;
        List<File> files = Arrays.asList(root.listFiles());
        String[] steps = path.split("/");
        for (int i = 0; i < steps.length; i++) {
            for (File file : files) {
                if (file.getName().equals(steps[i])) {
                    if (i < steps.length - 1) {
                        return findFile(path.substring(steps[i].length() + 1, path.length()), file);
                    } else {
                        return file;
                    }
                }
            }
        }
        return target;
    }
}
