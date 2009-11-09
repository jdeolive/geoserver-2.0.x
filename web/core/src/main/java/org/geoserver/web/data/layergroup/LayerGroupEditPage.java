/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import java.util.Arrays;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.PageParameters;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.layer.LayerDetachableModel;
import org.geoserver.web.data.store.StorePage;
import org.geoserver.web.data.style.StyleDetachableModel;
import org.geoserver.web.wicket.CRSPanel;
import org.geoserver.web.wicket.EnvelopePanel;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.SimpleAjaxLink;
import org.geoserver.web.wicket.GeoServerDataProvider.BeanProperty;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Edits a layer group
 */
@SuppressWarnings("serial")
public class LayerGroupEditPage extends GeoServerSecuredPage {

    public static final String GROUP = "group";
    IModel lgModel;
    EnvelopePanel envelopePanel;
    CRSPanel crsPanel;
    LayerGroupEntryPanel lgEntryPanel;
    
    public LayerGroupEditPage(PageParameters parameters) {
        String groupName = parameters.getString(GROUP);
        LayerGroupInfo lg = getCatalog().getLayerGroupByName(groupName);
        
        if(lg == null) {
            error(new ParamResourceModel("LayerGroupEditPage.notFound", this, groupName).getString());
            setResponsePage(LayerGroupPage.class);
            return;
        }
        
        initUI(lg);
    }
    
    public LayerGroupEditPage( LayerGroupInfo layerGroup ) {
        initUI(layerGroup);
    }

    private void initUI(LayerGroupInfo layerGroup) {
        lgModel = new LayerGroupDetachableModel( layerGroup );
        
        Form form = new Form( "form", new CompoundPropertyModel( lgModel ) );
        add(form);
        TextField name = new TextField("name");
        name.setRequired(true);
        form.add(name);
        
        //bounding box
        form.add(envelopePanel = new EnvelopePanel( "bounds" )/*.setReadOnly(true)*/);
        envelopePanel.setRequired(true);
        envelopePanel.setOutputMarkupId( true );
        
        CoordinateReferenceSystem crs = layerGroup.getBounds() != null 
            ? layerGroup.getBounds().getCoordinateReferenceSystem() : null;

        form.add(crsPanel = (crs != null) ? new CRSPanel( "crs", crs ) : new CRSPanel( "crs", new Model() ));
        crsPanel.setOutputMarkupId( true );
        crsPanel.setRequired(true);
        
        form.add(new GeoServerAjaxFormLink( "generateBounds") {
            @Override
            public void onClick(AjaxRequestTarget target, Form form) {
                // force update of the crs panel contents
                crsPanel.processInput();
                
                // build a layer group with the current contents of the group
                LayerGroupInfo lg = getCatalog().getFactory().createLayerGroup();
                for ( LayerGroupEntry entry : lgEntryPanel.getEntries() ) {
                    lg.getLayers().add(entry.getLayer());
                    lg.getStyles().add(entry.getStyle());
                }
                
                try {
                    CoordinateReferenceSystem crs = crsPanel.getCRS();
                    if ( crs != null ) {
                        //ensure the bounds calculated in terms of the user specified crs
                        new CatalogBuilder( getCatalog() ).calculateLayerGroupBounds( lg, crs );
                    }
                    else {
                        //calculate from scratch
                        new CatalogBuilder( getCatalog() ).calculateLayerGroupBounds( lg );
                    }
                    
                    envelopePanel.setModelObject( lg.getBounds() );
                    target.addComponent( envelopePanel );
                    
                    if ( crs == null ) {
                        //update the crs as well
                        crsPanel.setModelObject( lg.getBounds().getCoordinateReferenceSystem() );
                        target.addComponent( crsPanel );
                    }
                    
                } 
                catch (Exception e) {
                    throw new WicketRuntimeException( e );
                }
            }
        });
        
        form.add(lgEntryPanel = new LayerGroupEntryPanel( "layers", layerGroup ));
        form.add(new SubmitLink("save"){
            @Override
            public void onSubmit() {
                LayerGroupInfo lg = (LayerGroupInfo) lgModel.getObject();
                
                if(lgEntryPanel.getEntries().size() == 0) {
                    error((String) new ParamResourceModel("oneLayerMinimum", getPage()).getObject());
                    return;
                }
                
                // update the layer group entries
                lg.getLayers().clear();
                lg.getStyles().clear();
                
                for ( LayerGroupEntry entry : lgEntryPanel.getEntries() ) {
                    lg.getLayers().add(entry.getLayer());
                    lg.getStyles().add(entry.getStyle());
                }
                
                getCatalog().save( lg );
                setResponsePage(LayerGroupPage.class);
            }
        });
        form.add(new BookmarkablePageLink("cancel", LayerGroupPage.class));
    }
    
    abstract static class StyleListPanel extends GeoServerTablePanel<StyleInfo> {

        static Property<StyleInfo> NAME = 
            new BeanProperty<StyleInfo>("name", "name");
        
        public StyleListPanel(String id) {
            super(id, new GeoServerDataProvider<StyleInfo>() {
                @Override
                protected List<StyleInfo> getItems() {
                    return getCatalog().getStyles();
                }

                @Override
                protected List<Property<StyleInfo>> getProperties() {
                    return Arrays.asList( NAME );
                }

                public IModel model(Object object) {
                    return new StyleDetachableModel( (StyleInfo) object );
                }
            });
            getTopPager().setVisible(false);
        }

        @Override
        protected Component getComponentForProperty(String id, IModel itemModel,
                Property<StyleInfo> property) {
            final StyleInfo style = (StyleInfo) itemModel.getObject();
            if ( property == NAME ) {
                return new SimpleAjaxLink( id, NAME.getModel( itemModel ) ) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        handleStyle(style, target);
                    }
                };
            }
            
            return null;
        }
        
        protected abstract void handleStyle( StyleInfo style, AjaxRequestTarget target );

    }

    abstract static class LayerListPanel extends GeoServerTablePanel<LayerInfo> {
        static Property<LayerInfo> NAME = 
            new BeanProperty<LayerInfo>("name", "name");
        
        static Property<LayerInfo> STORE = 
            new BeanProperty<LayerInfo>("store", "resource.store.name");
        
        static Property<LayerInfo> WORKSPACE = 
            new BeanProperty<LayerInfo>("workspace", "resource.store.workspace.name");
        
        LayerListPanel( String id ) {
            super( id, new GeoServerDataProvider<LayerInfo>() {

                @Override
                protected List<LayerInfo> getItems() {
                    return getCatalog().getLayers();
                }

                @Override
                protected List<Property<LayerInfo>> getProperties() {
                    return Arrays.asList( NAME, STORE, WORKSPACE );
                }

                public IModel model(Object object) {
                    return new LayerDetachableModel((LayerInfo)object);
                }

            });
            getTopPager().setVisible(false);
        }
        
        @Override
        protected Component getComponentForProperty(String id, final IModel itemModel,
                Property<LayerInfo> property) {
            IModel model = property.getModel( itemModel );
            if ( NAME == property ) {
                return new SimpleAjaxLink( id, model ) {
                    @Override
                    protected void onClick(AjaxRequestTarget target) {
                        LayerInfo layer = (LayerInfo) itemModel.getObject();
                        handleLayer( layer, target );
                    }
                };
            }
            else {
                return new Label( id, model );
            }
        }
        
        protected void handleLayer( LayerInfo layer, AjaxRequestTarget target ) {
        }
    }
}
