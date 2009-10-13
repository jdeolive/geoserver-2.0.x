/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.AbstractValidator;
import org.apache.wicket.validation.validator.NumberValidator;
import org.geoserver.web.services.BaseServiceAdminPage;
import org.geoserver.web.util.MapModel;
import org.geoserver.web.wicket.LiveCollectionModel;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSInfo.WMSInterpolation;
import org.geoserver.wms.WatermarkInfo.Position;
import org.geotools.referencing.CRS;

/**
 * Edits the WMS service details 
 */
@SuppressWarnings("serial")
public class WMSAdminPage extends BaseServiceAdminPage<WMSInfo> {
    
    static final List<String> SVG_RENDERERS = Arrays.asList(new String[] {WMS.SVG_BATIK, WMS.SVG_SIMPLE});
    
    protected Class<WMSInfo> getServiceClass() {
        return WMSInfo.class;
    }
    
    protected void build(IModel info, Form form) {
        // limited srs list
        TextArea srsList = new TextArea("srs", LiveCollectionModel.list(new PropertyModel(info, "sRS"))) {
            @Override
            public IConverter getConverter(Class type) {
                return new SRSListConverter();
            }
                
        };
        srsList.add(new SRSListValidator());
        srsList.setType(List.class);
        form.add(srsList);

        // general
    	form.add(new DropDownChoice("interpolation", Arrays.asList(WMSInfo.WMSInterpolation.values()), new InterpolationRenderer()));
    	// resource limits
    	TextField maxMemory = new TextField("maxRequestMemory");
    	maxMemory.add(NumberValidator.minimum(0.0));
    	form.add(maxMemory);
    	TextField maxTime = new TextField("maxRenderingTime");
    	maxTime.add(NumberValidator.minimum(0.0));
        form.add(maxTime);
        TextField maxErrors = new TextField("maxRenderingErrors");
        maxErrors.add(NumberValidator.minimum(0.0));
        form.add(maxErrors);
    	// watermark
    	form.add(new CheckBox("watermark.enabled"));
    	form.add(new TextField("watermark.uRL"));
    	TextField transparency = new TextField("watermark.transparency");
    	transparency.add(NumberValidator.range(0, 100));
        form.add(transparency);
    	form.add(new DropDownChoice("watermark.position", Arrays.asList(Position.values()), new WatermarkPositionRenderer()));
    	// svg
    	PropertyModel metadataModel = new PropertyModel(info, "metadata");
        form.add(new CheckBox("svg.antialias", new MapModel(metadataModel, "svgAntiAlias")));
    	form.add(new DropDownChoice("svg.producer", new MapModel(metadataModel, "svgRenderer"), SVG_RENDERERS, new SVGMethodRenderer()));
    }
    
    protected String getServiceName(){
        return "WMS";
    }

    private class WatermarkPositionRenderer implements  IChoiceRenderer {

        public Object getDisplayValue(Object object) {
            return new StringResourceModel(((Position) object).name(), WMSAdminPage.this, null).getString();
        }

        public String getIdValue(Object object, int index) {
            return ((Position) object).name();
        }
        
    }
    
    private class InterpolationRenderer implements  IChoiceRenderer {

        public Object getDisplayValue(Object object) {
            return new StringResourceModel(((WMSInterpolation) object).name(), WMSAdminPage.this, null).getString();
        }

        public String getIdValue(Object object, int index) {
            return ((WMSInterpolation) object).name();
        }
        
    }
    
    private class SVGMethodRenderer implements  IChoiceRenderer {

        public Object getDisplayValue(Object object) {
            return new StringResourceModel("svg." + object, WMSAdminPage.this, null).getString();
        }

        public String getIdValue(Object object, int index) {
            return (String) object;
        }
        
    }
    
    private static class SRSListConverter implements IConverter {
            static final Pattern COMMA_SEPARATED = Pattern.compile("\\s*,\\s*", Pattern.MULTILINE); 
            
            public String convertToString(Object value, Locale locale) {
                List<String> srsList = (List<String>) value;
                if(srsList.isEmpty())
                    return "";
                    
                StringBuffer sb = new StringBuffer();
                for (String srs : srsList) {
                    sb.append(srs).append(", ");
                }
                sb.setLength(sb.length() - 2);
                return sb.toString();
            }
            
            public Object convertToObject(String value, Locale locale) {
                if(value == null || value.trim().equals(""))
                    return Collections.emptyList();
                return new ArrayList<String>(Arrays.asList(COMMA_SEPARATED.split(value)));
            }
    }
    
    private static class SRSListValidator extends AbstractValidator {

        @Override
        protected void onValidate(IValidatable validatable) {
            List<String> srsList = (List<String>) validatable.getValue();
            List<String> invalid = new ArrayList<String>();
            for (String srs : srsList) {
                try {
                    CRS.decode("EPSG:" + srs);
                } catch(Exception e) {
                    invalid.add(srs);
                }
            }
            
            if(invalid.size() > 0)
                error(validatable, "WMSAdminPage.unknownEPSGCodes", Collections.singletonMap("codes", invalid.toString()));
            
        }
        
    }
}
