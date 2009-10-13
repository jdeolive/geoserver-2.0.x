/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.AjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.AjaxPreprocessingCallDecorator;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.lang.Bytes;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.style.StyleDetachableModel;
import org.geoserver.web.wicket.EditAreaBehavior;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.wms.web.publish.StyleChoiceRenderer;
import org.geoserver.wms.web.publish.StylesModel;
import org.geotools.sld.SLDConfiguration;
import org.geotools.xml.Parser;

/**
 * Base page for creating/editing styles
 */
@SuppressWarnings("serial")
public abstract class AbstractStylePage extends GeoServerSecuredPage {

    protected TextField nameTextField;

    protected FileUploadField fileUploadField;

    protected DropDownChoice styles;

    protected AjaxSubmitLink copyLink;

    protected Form uploadForm;

    protected Form styleForm;

    private TextArea editor;
    
    String rawSLD;

    public AbstractStylePage() {
        this(null);
    }

    public AbstractStylePage(StyleInfo style) {
        styleForm = new Form("form", new CompoundPropertyModel(style != null ? new StyleDetachableModel(style) : getCatalog().getFactory().createStyle())) {
            @Override
            protected void onSubmit() {
                onStyleFormSubmit();
            }
        };
        styleForm.setMarkupId("mainForm");
        add(styleForm);

        styleForm.add(nameTextField = new TextField("name"));
        nameTextField.setRequired(true);
        
        styleForm.add( editor = new TextArea("editor", new PropertyModel(this, "rawSLD")) );
        // force the id otherwise this blasted thing won't be usable from other forms
        editor.setMarkupId("editor");
        editor.setOutputMarkupId(true);
        editor.add(new EditAreaBehavior());
        styleForm.add(editor);

        if (style != null) {
            try {
                setRawSLD(readFile(style));
            } catch (IOException e) {
                // ouch, the style file is gone! Register a generic error message
                Session.get().error(new ParamResourceModel("sldNotFound", this, style.getFilename()).getString());
            }
        }

        // style copy functionality
        styles = new DropDownChoice("existingStyles", new Model(), new StylesModel(), new StyleChoiceRenderer());
        styles.setOutputMarkupId(true);
        styles.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                styles.validate();
                copyLink.setEnabled(styles.getConvertedInput() != null);
                target.addComponent(copyLink);
            }
        });
        styleForm.add(styles);
        copyLink = copyLink();
        copyLink.setEnabled(false);
        styleForm.add(copyLink);

        uploadForm = uploadForm(styleForm);
        uploadForm.setMultiPart(true);
        uploadForm.setMaxSize(Bytes.megabytes(1));
        uploadForm.setMarkupId("uploadForm");
        add(uploadForm);

        uploadForm.add(fileUploadField = new FileUploadField("filename"));

        add(validateLink());
        Link cancelLink = new Link("cancel") {
            @Override
            public void onClick() {
                setResponsePage(StylePage.class);
            }
        };
        add(cancelLink);
    }

    Form uploadForm(final Form form) {
        return new Form("uploadForm") {
            @Override
            protected void onSubmit() {
                FileUpload upload = fileUploadField.getFileUpload();
                if (upload == null) {
                    warn("No file selected.");
                    return;
                }
                ByteArrayOutputStream bout = new ByteArrayOutputStream();

                try {
                    IOUtils.copy(upload.getInputStream(), bout);
                    setRawSLD(new InputStreamReader(new ByteArrayInputStream(bout.toByteArray())));
                    editor.setModelObject(rawSLD);
                } catch (IOException e) {
                    throw new WicketRuntimeException(e);
                }

                // update the style object
                StyleInfo s = (StyleInfo) form.getModelObject();
                if (s.getName() == null || "".equals(s.getName().trim())) {
                    // set it
                    nameTextField.setModelValue(ResponseUtils.stripExtension(upload
                            .getClientFileName()));
                    nameTextField.modelChanged();
                }
            }
        };
    }
    
    Component validateLink() {
        return new GeoServerAjaxFormLink("validate", styleForm) {
            
            @Override
            protected void onClick(AjaxRequestTarget target, Form form) {
                editor.validate();
                List<Exception> errors = validateSLD();
                
                if ( errors.isEmpty() ) {
                    form.info( "No validation errors.");
                } else {
                    for( Exception e : errors ) {
                        form.error( e );
                    }    
                }        
            }
            
            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
                // we need to force EditArea to update the textarea contents (which it hid)
                // before submitting the form, otherwise the validation will go bye bye
                return new AjaxCallDecorator() {
                    @Override
                    public CharSequence decorateScript(CharSequence script) {
                        return "document.getElementById('editor').value = editAreaLoader.getValue('editor');" + script;
                    }
                };
            }
        };
    }
    
    List<Exception> validateSLD() {
        Parser parser = new Parser(new SLDConfiguration());
        try {
            parser.validate( new ByteArrayInputStream(editor.getInput().getBytes()) );
        } catch( Exception e ) {
            return Arrays.asList( e );
        }
        
        return parser.getValidationErrors();
    }

    AjaxSubmitLink copyLink() {
        return new AjaxSubmitLink("copy") {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                // we need to force validation or the value won't be converted
                styles.processInput();
                StyleInfo style = (StyleInfo) styles.getConvertedInput();

                if (style != null) {
                    try {
                        // same here, force validation or the field won't be udpated
                        editor.validate();
                        editor.clearInput();
                        setRawSLD(readFile(style));
                    } catch (Exception e) {
                        error("Errors occurred loading the '" + style.getName() + "' style");
                    }
                    target.addComponent(styleForm);
                }
            }

            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
                return new AjaxPreprocessingCallDecorator(super.getAjaxCallDecorator()) {

                    @Override
                    public CharSequence preDecorateScript(CharSequence script) {
                        return "if(editAreaLoader.getValue('"
                                + editor.getMarkupId()
                                + "') != '' &&"
                                + "!confirm('"
                                + new ParamResourceModel("confirmOverwrite", AbstractStylePage.this)
                                        .getString() + "')) return false;" + script;
                    }
                };
            }

            @Override
            public boolean getDefaultFormProcessing() {
                return false;
            }

        };
    }

    Reader readFile(StyleInfo style) throws IOException {
        ResourcePool pool = getCatalog().getResourcePool();
        return pool.readStyle(style);
    }
    
    public void setRawSLD(Reader in) throws IOException {
        BufferedReader bin = null;
        if ( in instanceof BufferedReader ) {
            bin = (BufferedReader) in;
        }
        else {
            bin = new BufferedReader( in );
        }
        
        StringBuilder builder = new StringBuilder();
        String line = null;
        while ((line = bin.readLine()) != null ) {
            builder.append(line).append("\n");
        }

        this.rawSLD = builder.toString();
        editor.setModelObject(rawSLD);
        in.close();
    }

    /**
     * Subclasses must implement to define the submit behavior
     */
    protected abstract void onStyleFormSubmit();
}
