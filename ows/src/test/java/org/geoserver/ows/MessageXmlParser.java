/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.ows;

import org.geotools.util.Version;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import java.io.Reader;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


public class MessageXmlParser extends XmlRequestReader {
    public MessageXmlParser() {
        super(new QName(null, "Hello"), new Version("1.0.0"), "hello");
    }

    public Object read(Object request, Reader reader, Map kvp) throws Exception {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

        Document doc = builder.parse(new InputSource(reader));
        String message = doc.getDocumentElement().getAttribute("message");

        return new Message(message);
    }
}
