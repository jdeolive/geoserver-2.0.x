/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.filters;

import org.geoserver.test.GeoServerTestSupport;

import com.mockrunner.mock.web.MockHttpServletResponse;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

public class GZIPResponseStreamTest extends GeoServerTestSupport {
    public void testStream() throws Exception {
        ByteStreamCapturingHttpServletResponse response = 
            new ByteStreamCapturingHttpServletResponse(new MockHttpServletResponse());
        GZIPResponseStream stream = new GZIPResponseStream(response);
        stream.write("Hello world!".getBytes());
        stream.flush();
        stream.close();
        assertEquals("Hello world!", new String(unzip(response.toByteArray())));
    }

    private byte[] unzip(byte[] zipped) throws Exception {
        InputStream stream  =
            new GZIPInputStream(new ByteArrayInputStream(zipped));
        int character;
        ArrayList<Byte> builder = new ArrayList<Byte>();
        while ((character = stream.read()) != -1){
            builder.add((byte)character);
        }

        byte[] results = new byte[builder.size()];
        for (int i = 0; i < builder.size(); i++)
            results[i] = builder.get(i).byteValue();
        return results;
    }

    private static class CapturingByteOutputStream extends ServletOutputStream {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        public void write(int b){
            bos.write(b);
        }

        public byte[] toByteArray(){
            return bos.toByteArray();
        }
    }

    private static class ByteStreamCapturingHttpServletResponse 
        extends HttpServletResponseWrapper {
            CapturingByteOutputStream myOutputStream;

            public ByteStreamCapturingHttpServletResponse(
                    HttpServletResponse r){
                super(r);
            }



            public ServletOutputStream getOutputStream() throws IOException {
                if (myOutputStream == null) 
                    myOutputStream = new CapturingByteOutputStream();
                return myOutputStream;
            }

            public byte[] toByteArray() {
                return myOutputStream.toByteArray();
            }
        }
}
