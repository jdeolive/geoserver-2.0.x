package org.geoserver.web;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.config.GeoServerLoader;

/**
 * Small utility panel showed only in dev mode that allows developers to control
 * some Wicket behavior
 */
@SuppressWarnings("serial")
public class DeveloperToolbar extends Panel {

    private AjaxCheckBox wicketIds;
    private AjaxCheckBox xhtml;

    public DeveloperToolbar(String id) {
        super(id);

        // Clears the resource caches
        add(new IndicatingAjaxLink("clearCache") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                GeoServerApplication.get().clearWicketCaches();
            }
        });
        
        // Reloads the whole catalog and config from the file system
        add(new IndicatingAjaxLink("reload") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                try {
                    GeoServerLoader loader = (GeoServerLoader) GeoServerApplication.get().getBean("geoServerLoader");
                    loader.reload();
                    info("Catalog and configuration reloaded");
                } catch(Exception e) {
                    error(e);
                }
            }
        });

        IModel gsApp = new GeoServerApplicationModel();

        // controls whether wicket paths are being generated
        final AjaxCheckBox wicketPaths = new AjaxCheckBox("wicketPaths",
                new PropertyModel(gsApp, "debugSettings.outputComponentPath")) {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {

            }

        };
        wicketPaths.setOutputMarkupId(true);
        add(wicketPaths);
        
        // controls the xhtml validation filter
        xhtml = new AjaxCheckBox("xhtml", new XHTMLModel()) {
            
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                wicketIds.setModelObject(Boolean.TRUE);
                wicketPaths.setModelObject(Boolean.FALSE);
                target.addComponent(wicketIds);
                target.addComponent(wicketPaths);
            }
        };
        add(xhtml);

        // controls whether wicket ids are being generated
        wicketIds = new AjaxCheckBox("wicketIds", new PropertyModel(gsApp,
                "markupSettings.stripWicketTags")) {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                wicketPaths.setModelObject(Boolean.FALSE);
                target.addComponent(wicketPaths);
            }

        };
        wicketIds.setOutputMarkupId(true);
        add(wicketIds);
        
        // controls whether the ajax debug is enabled or not
        add(new AjaxCheckBox("ajaxDebug", new PropertyModel(gsApp, "debugSettings.ajaxDebugModeEnabled")) {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                // nothing to do, the property binding does the work for us
            }
            
        });

    }

    static class GeoServerApplicationModel extends LoadableDetachableModel {

        GeoServerApplicationModel() {
            super(GeoServerApplication.get());
        }

        @Override
        protected Object load() {
            return GeoServerApplication.get();
        }

    }
    
    static class XHTMLModel implements IModel {

        public Object getObject() {
            GeoServerApplication app = GeoServerApplication.get();
            boolean enabled = false;
            for (Object filter : app.getRequestCycleSettings().getResponseFilters()) {
                if(filter instanceof GeoServerHTMLValidatorResponseFilter) {
                    enabled = ((GeoServerHTMLValidatorResponseFilter) filter).enabled;
                }
            }
            return enabled;
        }

        public void setObject(Object object) {
            GeoServerApplication app = GeoServerApplication.get();
            for (Object filter : app.getRequestCycleSettings().getResponseFilters()) {
                if(filter instanceof GeoServerHTMLValidatorResponseFilter) {
                    ((GeoServerHTMLValidatorResponseFilter) filter).enabled = (Boolean) object;
                }
            }
            
        }

        public void detach() {
            // nothing to do here
        }
        
    }

}
