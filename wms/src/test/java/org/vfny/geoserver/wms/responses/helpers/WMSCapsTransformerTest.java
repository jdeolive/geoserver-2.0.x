/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.vfny.geoserver.wms.responses.helpers;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.impl.ContactInfoImpl;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.config.impl.GeoServerInfoImpl;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSInfoImpl;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.referencing.CRS;
import org.vfny.geoserver.wms.requests.WMSCapabilitiesRequest;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * 
 * @author Gabriel Roldan
 * @version $Id$
 */
public class WMSCapsTransformerTest extends TestCase {

    private XpathEngine XPATH;

    /** default base url to feed a WMSCapsTransformer with for it to append the DTD location */
    private static final String baseUrl = "http://localhost/geoserver";

    /** test map formats to feed a WMSCapsTransformer with */
    private static final Set<String> mapFormats = Collections.singleton("image/png");

    /** test legend formats to feed a WMSCapsTransformer with */
    private static final Set<String> legendFormats = Collections.singleton("image/png");

    /**
     * a mocked up {@link GeoServer} config, almost empty after setUp(), except for the
     * {@link WMSInfo}, {@link GeoServerInfo} and empty {@link Catalog}, Specific tests should add
     * content as needed
     */
    private GeoServerImpl geosConfig;

    /**
     * a mocked up {@link GeoServerInfo} for {@link #geosConfig}. Specific tests should set its
     * properties as needed
     */
    private GeoServerInfoImpl geosInfo;

    /**
     * a mocked up {@link WMSInfo} for {@link #geosConfig}, empty except for the WMSInfo after
     * setUp(), Specific tests should set its properties as needed
     */
    private WMSInfoImpl wmsInfo;

    /**
     * a mocked up {@link Catalog} for {@link #geosConfig}, empty after setUp(), Specific tests
     * should add content as needed
     */
    private CatalogImpl catalog;

    private WMSCapabilitiesRequest req;

    /**
     * Sets up the configuration objects with default values. Since they're live, specific tests can
     * modify their state before running the assertions
     */
    protected void setUp() throws Exception {
        geosConfig = new GeoServerImpl();

        geosInfo = new GeoServerInfoImpl(geosConfig);
        geosInfo.setContact(new ContactInfoImpl());
        geosConfig.setGlobal(geosInfo);

        wmsInfo = new WMSInfoImpl();
        geosConfig.add(wmsInfo);

        catalog = new CatalogImpl();
        geosConfig.setCatalog(catalog);

        req = new WMSCapabilitiesRequest(new WMS(geosConfig));
        req.setBaseUrl(baseUrl);

        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
        XPATH = XMLUnit.newXpathEngine();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testHeader() throws Exception {
        WMSCapsTransformer tr = new WMSCapsTransformer(baseUrl, mapFormats, legendFormats);
        StringWriter writer = new StringWriter();
        tr.transform(req, writer);
        String content = writer.getBuffer().toString();

        assertTrue(content.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        String dtdDef = "<!DOCTYPE WMT_MS_Capabilities SYSTEM \"" + baseUrl
                + "/schemas/wms/1.1.1/WMS_MS_Capabilities.dtd\">";
        assertTrue(content.contains(dtdDef));
    }

    public void testRootElement() throws Exception {
        WMSCapsTransformer tr = new WMSCapsTransformer(baseUrl, mapFormats, legendFormats);

        Document dom = WMSTestSupport.transform(req, tr);
        Element root = dom.getDocumentElement();
        assertEquals("WMT_MS_Capabilities", root.getNodeName());
        assertEquals("1.1.1", root.getAttribute("version"));
        assertEquals("0", root.getAttribute("updateSequence"));

        geosInfo.setUpdateSequence(10);
        tr = new WMSCapsTransformer(baseUrl, mapFormats, legendFormats);
        dom = WMSTestSupport.transform(req, tr);
        root = dom.getDocumentElement();
        assertEquals("10", root.getAttribute("updateSequence"));
    }

    public void testServiceSection() throws Exception {
        wmsInfo.setTitle("title");
        wmsInfo.setAbstract("abstract");
        wmsInfo.getKeywords().add("k1");
        wmsInfo.getKeywords().add("k2");
        // @REVISIT: this is not being respected, but the onlineresource is being set based on the
        // proxyBaseUrl... not sure if that's correct
        wmsInfo.setOnlineResource("http://onlineresource/fake");

        ContactInfo contactInfo = new ContactInfoImpl();
        geosInfo.setContact(contactInfo);
        contactInfo.setContactPerson("contactPerson");
        contactInfo.setContactOrganization("contactOrganization");
        contactInfo.setContactPosition("contactPosition");
        contactInfo.setAddress("address");
        contactInfo.setAddressType("addressType");
        contactInfo.setAddressCity("city");
        contactInfo.setAddressState("state");
        contactInfo.setAddressPostalCode("postCode");
        contactInfo.setAddressCountry("country");
        contactInfo.setContactVoice("voice");
        contactInfo.setContactEmail("email");
        contactInfo.setContactFacsimile("fax");

        wmsInfo.setFees("fees");
        wmsInfo.setAccessConstraints("accessConstraints");

        WMSCapsTransformer tr = new WMSCapsTransformer(baseUrl, mapFormats, legendFormats);
        tr.setIndentation(2);
        Document dom = WMSTestSupport.transform(req, tr);

        String service = "/WMT_MS_Capabilities/Service";
        assertXpathEvaluatesTo("OGC:WMS", service + "/Name", dom);

        assertXpathEvaluatesTo("title", service + "/Title", dom);
        assertXpathEvaluatesTo("abstract", service + "/Abstract", dom);
        assertXpathEvaluatesTo("k1", service + "/KeywordList/Keyword[1]", dom);
        assertXpathEvaluatesTo("k2", service + "/KeywordList/Keyword[2]", dom);
        // @REVISIT: shouldn't it be WmsInfo.getOnlineResource?
        assertXpathEvaluatesTo(baseUrl + "/wms", service + "/OnlineResource/@xlink:href", dom);

        assertXpathEvaluatesTo("contactPerson", service
                + "/ContactInformation/ContactPersonPrimary/ContactPerson", dom);
        assertXpathEvaluatesTo("contactOrganization", service
                + "/ContactInformation/ContactPersonPrimary/ContactOrganization", dom);
        assertXpathEvaluatesTo("contactPosition", service + "/ContactInformation/ContactPosition",
                dom);
        assertXpathEvaluatesTo("address", service + "/ContactInformation/ContactAddress/Address",
                dom);
        assertXpathEvaluatesTo("addressType", service
                + "/ContactInformation/ContactAddress/AddressType", dom);
        assertXpathEvaluatesTo("city", service + "/ContactInformation/ContactAddress/City", dom);
        assertXpathEvaluatesTo("state", service
                + "/ContactInformation/ContactAddress/StateOrProvince", dom);
        assertXpathEvaluatesTo("postCode", service + "/ContactInformation/ContactAddress/PostCode",
                dom);
        assertXpathEvaluatesTo("country", service + "/ContactInformation/ContactAddress/Country",
                dom);
        assertXpathEvaluatesTo("voice", service + "/ContactInformation/ContactVoiceTelephone", dom);
        assertXpathEvaluatesTo("fax", service + "/ContactInformation/ContactFacsimileTelephone",
                dom);
        assertXpathEvaluatesTo("email", service
                + "/ContactInformation/ContactElectronicMailAddress", dom);

        assertXpathEvaluatesTo("fees", service + "/Fees", dom);
        assertXpathEvaluatesTo("accessConstraints", service + "/AccessConstraints", dom);
    }

    public void testCRSList() throws Exception {
        WMSCapsTransformer tr = new WMSCapsTransformer(baseUrl, mapFormats, legendFormats);
        tr.setIndentation(2);
        Document dom = WMSTestSupport.transform(req, tr);
        final Set<String> supportedCodes = CRS.getSupportedCodes("EPSG");
        NodeList allCrsCodes = XPATH.getMatchingNodes("/WMT_MS_Capabilities/Capability/Layer/SRS",
                dom);
        assertEquals(supportedCodes.size(), allCrsCodes.getLength());
    }

    public void testLimitedCRSList() throws Exception {
        wmsInfo.getSRS().add("EPSG:3246");
        wmsInfo.getSRS().add("EPSG:23030");

        WMSCapsTransformer tr = new WMSCapsTransformer(baseUrl, mapFormats, legendFormats);
        tr.setIndentation(2);
        Document dom = WMSTestSupport.transform(req, tr);
        NodeList limitedCrsCodes = XPATH.getMatchingNodes("/WMT_MS_Capabilities/Capability/Layer/SRS",
                dom);
        assertEquals(2, limitedCrsCodes.getLength());    
    }
}
