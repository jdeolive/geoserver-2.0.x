/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.SelectionRemovalLink;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.SimpleAjaxLink;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;

/**
 * Lists layer groups, allows removal and editing
 */
@SuppressWarnings("serial")
public class LayerGroupPage extends GeoServerSecuredPage {
    
    GeoServerTablePanel<LayerGroupInfo> table;
    GeoServerDialog dialog;
    SelectionRemovalLink removal;

    public LayerGroupPage() {
        LayerGroupProvider provider = new LayerGroupProvider();
        add(table = new GeoServerTablePanel<LayerGroupInfo>( "table", provider, true ) {

            @Override
            protected Component getComponentForProperty(String id, IModel itemModel,
                    Property<LayerGroupInfo> property) {
                
                if ( property == LayerGroupProvider.NAME ) {
                    return layerGroupLink( id, itemModel ); 
                }
                
                return null;
            }
            
            @Override
            protected void onSelectionUpdate(AjaxRequestTarget target) {
                removal.setEnabled(table.getSelection().size() > 0);
                target.addComponent(removal);
            }  
        });
        table.setOutputMarkupId(true);
        add(table);
        
        // the confirm dialog
        add(dialog = new GeoServerDialog("dialog"));
        setHeaderPanel(headerPanel());
    }
    
    protected Component headerPanel() {
        Fragment header = new Fragment(HEADER_PANEL, "header", this);
        
        // the add button
        header.add(new BookmarkablePageLink("addNew", LayerGroupNewPage.class));
        
        // the removal button
        header.add(removal = new SelectionRemovalLink("removeSelected", table, dialog));
        removal.setOutputMarkupId(true);
        removal.setEnabled(false);
        
        return header;
    }
    
    Component layerGroupLink(String id, IModel itemModel) {
        return new SimpleAjaxLink(id, LayerGroupProvider.NAME.getModel(itemModel)) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                String lgName = getModelObjectAsString();
                LayerGroupInfo lg = getCatalog().getLayerGroupByName( lgName );
                setResponsePage( new LayerGroupEditPage( lg ) );
            }
        };
    }
   
}
