/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.namespace.QName;

import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.GetFeatureType;
import net.opengis.wfs.QueryType;

import org.apache.commons.lang.StringUtils;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.response.dxf.DXFWriter;
import org.geoserver.wfs.response.dxf.DXFWriterFinder;
import org.geoserver.wfs.response.dxf.LineType;
import org.geotools.util.logging.Logging;


/**
 * This class returns a dxf  encoded results of the users's query
 * (optionally zipped).
 * Several format options are available to control output generation.
 *  - version: (number) creates a DXF in the specified version format (a DXFWriter
 *    implementation supporting the requested version needs to be available
 *    or an exception will be thrown); the default implementation creates
 *    a version 14 DXF.
 *  - asblock: (true/false) if true, all geometries are written 
 *    as blocks and then inserted as entities. If false, simple geometries
 *    are directly written as entities.
 *  - colors: (comma delimited list of numbers): colors to be used for 
 *    the DXF layers, in sequence. If layers are more than the specified
 *    colors, they will be reused many times. A set of default colors is
 *    used if the option is not used. Colors are AutoCad color numbers
 *    (7=white, etc.).
 *  - ltypes: (comma delimited list of line type descriptors): line types
 *    to be used for the DXF layers, in sequence. If layers are more than 
 *    the specified line types, they will be reused many times. If not specified,
 *    all layers will be given a solid, continuous line type. A descriptor
 *    has the following format: <name>!<repeatable pattern>[!<base length>], where 
 *    <name> is the name assigned to the line type, <base length> (optional) 
 *    is a real number that tells how long is each part of the line pattern 
 *    (defaults to 0.125), and <repeatable pattern> is a visual description
 *    of the repeatable part of the line pattern, as a sequence of - (solid line),
 *    * (dot) and _ (empty space).
 *  - layers: (comma delimited list of strings) names to be assigned to
 *    the DXF layers. If specified, must contain a name for each requested
 *    query. By default a standard name will be assigned to layers.
 *    
 *  A different layer will be generated for each requested query.
 *  Layer names can be chosen using the layers format option, or 
 *  in POST mode, using the handle attribute of the Query tag.
 *  The name of the resulting file can be chosen using the handle
 *  attribute of the GetFeature tag. By default, the names of layers
 *  concatenated with _ will be used.  
 * 
 * @author Mauro Bartolomeoli, mbarto@infosia.it
 *
 */
public class DXFOutputFormat extends WFSGetFeatureOutputFormat {

    private static final Logger LOGGER = Logging.getLogger(DXFOutputFormat.class);
    
    public static final Set formats = new HashSet();

    static {
        // list of supported output formats
        formats.add("DXF");
        formats.add("DXF-ZIP");
    }

    public DXFOutputFormat() {
        super(formats);        
    }

    /**
     * Gets current request extension (dxf or zip).
     * 
     * @param operation
     * @return
     */
    public String getExtension(Operation operation) {
        GetFeatureType request = (GetFeatureType) OwsUtils.parameter(operation.getParameters(),
                GetFeatureType.class);

        String outputFormat = request.getOutputFormat().toUpperCase();
        // DXF
        if (outputFormat.equals("DXF"))
            return "dxf";
        // DXF-ZIP
        return "zip";
    }

    /**
     * Mime type: application/dxf or application/zip
     */
    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return "application/" + getExtension(operation);
    }

    /**
     * Gets output filename.
     * If the handle attribute is defined on the GetFeature tag it
     * will be used, else the name is obtained concatenating lauer names
     * with underscore as a separator (up to a maximum name length).
     */
    private String getFileName(Operation operation) {
        GetFeatureType request = (GetFeatureType) OwsUtils.parameter(operation.getParameters(),
                GetFeatureType.class);
        
        if (request.getHandle() != null) {
            LOGGER.log(Level.FINE,"Using handle for file name: "+request.getHandle());
            return request.getHandle();
        }
        
        StringBuffer sb = new StringBuffer();
        for (Iterator f = request.getQuery().iterator(); f.hasNext();) {
            QueryType query = (QueryType) f.next();
            sb.append(getLayerName(query) + "_");
        }        
        sb.setLength(sb.length() - 1);
        LOGGER.log(Level.FINE,"Using layer names for file name: "+sb.toString());
        if (sb.length() > 20) {
            LOGGER.log(Level.WARNING,"Calculated filename too long. Returing a shorter one: "+sb.toString().substring(0, 20));
            return sb.toString().substring(0, 20);
        }
        return sb.toString();
    }

    /**
     * Add headers for automatic file save request in browsers.
     */
    @Override
    public String[][] getHeaders(Object value, Operation operation) throws ServiceException {

        return (String[][]) new String[][] { { "Content-Disposition",
                "attachment; filename=" + getFileName(operation) + "." + getExtension(operation) } };
    }

    /**
     * Actually write the given featurecollection as a dxf file to
     * the output stream.
     * 
     * @see org.geoserver.wfs.WFSGetFeatureOutputFormat#write(net.opengis.wfs.FeatureCollectionType,
     *      java.io.OutputStream, org.geoserver.platform.Operation)
     */
    @Override
    protected void write(FeatureCollectionType featureCollection, OutputStream output,
            Operation operation) throws IOException, ServiceException {
        // output format (zipped or not)
        String format = getExtension(operation);
        BufferedWriter w = null;
        ZipOutputStream zipStream = null;
        // DXF: use a simple buffered writer
        if (format.equals("dxf")) {
            LOGGER.log(Level.FINE,"Plain DXF output");
            w = new BufferedWriter(new OutputStreamWriter(output));
        } else {
            LOGGER.log(Level.FINE,"Zipped DXF output");
            // DXF-ZIP: use a zip stream wrapped with the buffered writer
            zipStream = new ZipOutputStream(output);
            ZipEntry entry = new ZipEntry(getFileName(operation) + ".dxf");
            zipStream.putNextEntry(entry);
            w = new BufferedWriter(new OutputStreamWriter(zipStream));
        }
        // extract format_options (GET mode)
        GetFeatureType gft = (GetFeatureType) operation.getParameters()[0];
        String version = (String) gft.getFormatOptions().get("VERSION");
        String blocks = (String) gft.getFormatOptions().get("ASBLOCKS");
        String colors = (String) gft.getFormatOptions().get("COLORS");
        String ltypes = (String) gft.getFormatOptions().get("LTYPES");
        String layerNames = (String) gft.getFormatOptions().get("LAYERS");
        LOGGER.log(Level.FINE,"Format options: "+version+"; "+blocks+"; "+colors+"; "+ltypes+"; "+layerNames);
        // get a suitable DXFWriter, for the requested version (null -> get any writer)
        DXFWriter dxfWriter = DXFWriterFinder.getWriter(version, w);
        
        if (dxfWriter != null) {
            LOGGER.log(Level.INFO,"DXFWriter: "+dxfWriter.getDescription());
            String[] layers = null;
            if(layerNames!=null)
                layers=layerNames.toUpperCase().split(",");
            else
                layers=getLayerNames(gft.getQuery().iterator());
            LOGGER.log(Level.FINE,"Layers names: "+StringUtils.join(layers,","));
            dxfWriter.setOption("layers", layers);
            if (blocks != null && blocks.toLowerCase().equals("true"))
                dxfWriter.setOption("geometryasblock", true);
            // set optional colors
            if (colors != null) {
                try {
                    String[] sColors = colors.split(",");
                    int[] icolors = new int[sColors.length];
                    for (int count = 0; count < sColors.length; count++)
                        icolors[count] = Integer.parseInt(sColors[count]);
                    dxfWriter.setOption("colors", icolors);
                } catch (Throwable t) {
                    LOGGER.log(Level.WARNING,"format option colors ignored by DXFOutputFormat due to a wrong format: "+t.getMessage());
                }
            }
            // set optional line types
            if (ltypes != null) {
                try {
                    String[] sLTypes = ltypes.split(",");
                    LineType[] ltypesArr = new LineType[sLTypes.length];
                    for (int count = 0; count < sLTypes.length; count++)
                        ltypesArr[count] = LineType.parse(sLTypes[count]);
                    dxfWriter.setOption("linetypes", ltypesArr);

                } catch (Throwable t) {
                    LOGGER
                            .warning("format option ltypes ignored by DXFOutputFormat due to a wrong format: "+t.getMessage());
                }
            }

            // do the real job, please
            dxfWriter.write(featureCollection.getFeature(),version);

            w.flush();            
            if (zipStream != null) {
                zipStream.closeEntry();
                zipStream.close();
            }
            dxfWriter = null;
            zipStream = null;
            w = null;

        } else
            throw new UnsupportedOperationException("Version " + version
                    + " not supported by dxf output format");

    }

    /**
     * Gets a list of names for layers, one
     * for each query.
     * @param it
     * @return
     */
    private String[] getLayerNames(Iterator it) {
        List<String> names = new ArrayList<String>();
        while (it.hasNext()) {
            QueryType query = (QueryType) it.next();
            names.add(getLayerName(query).toUpperCase());
        }

        return names.toArray(new String[] {});
    }

    /**
     * Gets a layer name from a query.
     * The name can be:
     *  - an handle, if available
     *  - the typename
     * @param query
     * @return
     */
    private String getLayerName(QueryType query) {
        if (query.getHandle() != null)
            return query.getHandle();
        return ((QName) query.getTypeName().get(0)).getLocalPart();

    }

}
