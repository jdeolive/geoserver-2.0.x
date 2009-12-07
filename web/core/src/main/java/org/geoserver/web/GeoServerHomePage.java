/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.util.HashMap;

import org.acegisecurity.Authentication;
import org.acegisecurity.GrantedAuthority;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.web.data.layer.LayerPage;
import org.geoserver.web.data.layer.NewLayerPage;
import org.geoserver.web.data.store.NewDataPage;
import org.geoserver.web.data.store.StorePage;
import org.geoserver.web.data.workspace.WorkspaceNewPage;
import org.geoserver.web.data.workspace.WorkspacePage;
import org.geotools.util.Version;

/**
 * Hope page, shows just the introduction and the capabilities link
 * 
 * @author Andrea Aime - TOPP
 * 
 */
public class GeoServerHomePage extends GeoServerBasePage {

    public GeoServerHomePage() {
        GeoServer gs = getGeoServer();
        ContactInfo contact = gs.getGlobal().getContact();

        //add some contact info
        add(new ExternalLink("contactURL", contact.getOnlineResource())
            .add( new Label("contactName", contact.getContactOrganization())));
        {
            String version = String.valueOf(new ResourceModel("version").getObject());
            String contactEmail = contact.getContactEmail();
            HashMap<String, String>params = new HashMap<String, String>();
            params.put("version", version);
            params.put("contactEmail", contactEmail);
            Label label = new Label("footerMessage", new StringResourceModel("GeoServerHomePage.footer", this, new Model(params)));
            label.setEscapeModelStrings(false);
            add(label);
        }
        
        Authentication auth = getSession().getAuthentication();
        if(isAdmin(auth)) {
            Fragment f = new Fragment("catalogLinks", "catalogLinksFragment", this);
            Catalog catalog = getCatalog();
            f.add(new BookmarkablePageLink("layersLink", LayerPage.class)
                .add(new Label( "nlayers", ""+catalog.getLayers().size())));
            f.add(new BookmarkablePageLink("addLayerLink", NewLayerPage.class));
            
            f.add(new BookmarkablePageLink("storesLink",StorePage.class)
                .add(new Label( "nstores", ""+catalog.getStores(StoreInfo.class).size())));
            f.add(new BookmarkablePageLink("addStoreLink", NewDataPage.class));
            
            f.add(new BookmarkablePageLink("workspacesLink",WorkspacePage.class)
                .add(new Label( "nworkspaces", ""+catalog.getWorkspaces().size())));
            f.add(new BookmarkablePageLink("addWorkspaceLink", WorkspaceNewPage.class));
            add(f);
        } else {
            Label placeHolder = new Label("catalogLinks");
            placeHolder.setVisible(false);
            add(placeHolder);
        }
        
        // when hacking this service listing code please refer to 
        // http://jira.codehaus.org/browse/GEOS-2114
        ListView view = new ListView("services", getServices()) {
            @Override
            protected void populateItem(ListItem item) {
                ServiceInfo service = (ServiceInfo) item.getModelObject();
                final String serviceId = service.getId();
                item.add( new Label("service", service.getId().toUpperCase()) );
                item.add( new ListView( "versions", service.getVersions()) {
                    @Override
                    protected void populateItem(ListItem item) {
                        Version version = (Version) item.getModelObject();
                        ExternalLink link = new ExternalLink("link", "../ows?service=" + serviceId
                                + "&version=" + version.toString() + "&request=GetCapabilities");
                        item.add( link );
                        
                        link.add( new Label( "version", version.toString() ) );
                    }
                });
            }
        };
        add(view);
    }
    
    /**
     * Checks if the current user is authenticated and is the administrator
     */
    private boolean isAdmin(Authentication authentication) {
        if(authentication == null || !authentication.isAuthenticated())
            return false;
        
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if ("ROLE_ADMINISTRATOR".equals(authority.getAuthority()))
                return true;
        }
        return false;
    }

    private IModel getServices() {
        return new LoadableDetachableModel() {
            @Override
            protected Object load() {
                return getGeoServerApplication().getGeoServer().getServices();
            }
        };
    }
}
