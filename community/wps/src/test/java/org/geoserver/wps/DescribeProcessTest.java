package org.geoserver.wps;

import static org.custommonkey.xmlunit.XMLAssert.*;
import junit.framework.Test;

import org.w3c.dom.Document;
public class DescribeProcessTest extends WPSTestSupport {

    //read-only test
    public static Test suite() {
        return new OneTimeTestSetup(new DescribeProcessTest());
    }
    
    public void testGetBuffer() throws Exception {
        Document d = getAsDOM( root() + "service=wps&request=describeprocess&identifier=gt:buffer");
        print(d);
        testBufferDescription(d);
    }
    
    public void testPostBuffer() throws Exception {
        String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + 
        		"<DescribeProcess xmlns=\"http://www.opengis.net/wps/1.0.0\" " +
        		"xmlns:ows=\"http://www.opengis.net/ows/1.1\" " +
        		"xmlns:xlink=\"http://www.w3.org/1999/xlink\" " +
        		"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n" + 
        		"    <ows:Identifier>gt:buffer</ows:Identifier>\r\n" + 
        		"</DescribeProcess>";
        Document d = postAsDOM(root(), request);
        print(d);
        testBufferDescription(d);
    }

    private void testBufferDescription(Document d) throws Exception {
        // first off, let's check it's schema compliant ... it's not unfortunately, prefix issues
        // prevent even the most basic validation...
        checkValidationErrors(d);
        assertXpathExists( "/wps:ProcessDescriptions", d );
        
        String base = "/wps:ProcessDescriptions/wps:ProcessDescription/wps:DataInputs";
        
        //first parameter
        assertXpathExists( base + "/wps:Input[1]", d );
        assertXpathEvaluatesTo("buffer", base + "/wps:Input[1]/ows:Identifier/child::text()", d );
        assertXpathExists( base + "/wps:Input[1]/wps:LiteralData", d );

        assertXpathEvaluatesTo("double", base + "/wps:Input[1]/wps:LiteralData/ows:DataType/child::text()", d );
        
        //second parameter
        base += "/wps:Input[2]";
        assertXpathExists( base , d );
        assertXpathExists( base + "/wps:ComplexData", d );
        
        base += "/wps:ComplexData";
        assertXpathEvaluatesTo("text/xml; subtype=gml/3.1.1", 
                base + "/wps:Default/wps:Format/wps:MimeType/child::text()", d);
        assertXpathEvaluatesTo("text/xml; subtype=gml/3.1.1", 
                base + "/wps:Supported/wps:Format[1]/wps:MimeType/child::text()", d);
        assertXpathEvaluatesTo("text/xml; subtype=gml/2.1.2", 
                base + "/wps:Supported/wps:Format[2]/wps:MimeType/child::text()", d);
    
        //output
        base = "/wps:ProcessDescriptions/wps:ProcessDescription/wps:ProcessOutputs";
        assertXpathExists( base + "/wps:Output", d );
        assertXpathExists( base + "/wps:Output/wps:ComplexOutput", d );
    }
}
