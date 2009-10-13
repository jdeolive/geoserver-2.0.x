/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.ows.kvp;

import org.geoserver.ows.KvpParser;
import org.geoserver.ows.util.CaseInsensitiveMap;
import org.geoserver.platform.GeoServerExtensions;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;


/**
 * Parses the format options parameter which is of the form:
 * <pre>FORMAT_OPTIONS=opt1:val1,val2;opt2:val1;opt3:...</pre>
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 *
 */
public class FormatOptionsKvpParser extends KvpParser implements ApplicationContextAware {
    /**
     * application context used to lookup KvpParsers
     */
    ApplicationContext applicationContext;

    public FormatOptionsKvpParser() {
        super("format_options", Map.class);
    }

    public void setApplicationContext(ApplicationContext applicationContext)
        throws BeansException {
        this.applicationContext = applicationContext;
    }

    public Object parse(String value) throws Exception {
        List parsers = GeoServerExtensions.extensions(KvpParser.class, applicationContext);
        Map formatOptions = new CaseInsensitiveMap(new HashMap());

        //TODO: refactor some of this routine out into utility class since 
        // much of the logic is duplicated from the dispatcher
        StringTokenizer st = new StringTokenizer(value, ";");

        while (st.hasMoreTokens()) {
            String kvp = (String) st.nextToken();
            String[] kv = kvp.split(":");

            String key = null;
            String raw = null;

            if (kv.length == 1) {
                //assume its a on/off (boolean) kvp
                key = kv[0];
                raw = "true";
            } else {
                key = kv[0];
                raw = kv[1];
            }

            Object parsed = null;

            for (Iterator p = parsers.iterator(); p.hasNext();) {
                KvpParser parser = (KvpParser) p.next();
                if ( key.equalsIgnoreCase( parser.getKey() ) ) {
                    parsed = parser.parse( raw );
                    if ( parsed != null ) {

                        break;
                    }
                }
            }

            if (parsed == null) {
                if(LOGGER.isLoggable(Level.FINER))
                    LOGGER.finer( "Could not find kvp parser for: '" + key + "'. Storing as raw string.");
                parsed = raw;
            }

            formatOptions.put(key, parsed);
        }

        return formatOptions;
    }
}
