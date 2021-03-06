/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs.response.dxf.util;

import java.util.GregorianCalendar;

import junit.framework.TestCase;

public class JulianDateTest extends TestCase {
    /**
     * Test a simple conversion: 01-Jan-2000 at noon.
     */
    public void testSimple() {
        GregorianCalendar calendar = new GregorianCalendar(2000, 0, 1, 12, 0, 0);
        assertEquals(2451545.0, JulianDate.toJulian(calendar.getTime()));
    }
}
