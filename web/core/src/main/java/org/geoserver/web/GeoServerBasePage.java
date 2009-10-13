/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.acegisecurity.Authentication;
import org.apache.wicket.Application;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.protocol.http.WebResponse;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.web.acegi.GeoServerSession;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.util.logging.Logging;

/**
 * Base class for web pages in GeoServer web application.
 * <ul>
 * <li>The basic layout</li>
 * <li>An OO infrastructure for common elements location</li>
 * <li>An infrastructure for locating subpages in the Spring context and
 * creating links</li>
 * </ul>
 *
 * @author Andrea Aaime, The Open Planning Project
 * @author Justin Deoliveira, The Open Planning Project
 */
public class GeoServerBasePage extends WebPage implements IAjaxIndicatorAware {
    
    /**
     * The id of the panel sitting in the page-header, right below the page description
     */
    protected static final String HEADER_PANEL = "headerPanel";

    protected static final Logger LOGGER = Logging.getLogger(GeoServerBasePage.class);

    /**
     * feedback panel for subclasses to report errors and information.
     */
    protected FeedbackPanel feedbackPanel;

	@SuppressWarnings("serial")
    public GeoServerBasePage() {
        //add css and javascript header contributions
	    ResourceReference faviconReference = null;
        List<HeaderContribution> cssContribs = 
            getGeoServerApplication().getBeansOfType(HeaderContribution.class);
        for (HeaderContribution csscontrib : cssContribs) {
            try {
                if (csscontrib.appliesTo(this)) {
                    ResourceReference ref = csscontrib.getCSS();
                    if (ref != null) {
                        add(HeaderContributor.forCss(ref));
                    }
                    
                    ref = csscontrib.getJavaScript();
                    if (ref != null) {
                        add(HeaderContributor.forJavaScript(ref));
                    }
                    
                    ref = csscontrib.getFavicon();
                    if(ref != null) {
                        faviconReference = ref;
                    }
                }
            }
            catch( Throwable t ) {
                LOGGER.log(Level.WARNING, "Problem adding header contribution", t );
            }
        }
        
        // favicon
        if(faviconReference == null) {
            faviconReference = new ResourceReference(GeoServerBasePage.class, "favicon.ico");
        }
        String faviconUrl = RequestCycle.get().urlFor(faviconReference).toString();
        add(new ExternalLink("faviconLink", faviconUrl, null));
	    
	    // page title
	    add(new Label("pageTitle", getPageTitle()));

        // login form
        WebMarkupContainer loginForm = new WebMarkupContainer("loginform");
        add(loginForm);
        final Authentication user = GeoServerSession.get().getAuthentication();
        final boolean anonymous = user == null;
        loginForm.setVisible(anonymous);

        WebMarkupContainer logoutForm = new WebMarkupContainer("logoutform");
        logoutForm.setVisible(user != null);

        add(logoutForm);
        logoutForm.add(new Label("username", anonymous ? "Nobody" : user.getName()));

        // home page link
        add( new BookmarkablePageLink( "home", GeoServerHomePage.class )
            .add( new Label( "label", new StringResourceModel( "home", (Component)null, null ) )  ) );
        
        // dev buttons
        DeveloperToolbar devToolbar = new DeveloperToolbar("devButtons");
        add(devToolbar);
        devToolbar.setVisible(Application.DEVELOPMENT.equalsIgnoreCase(
                getApplication().getConfigurationType()));
        
        final Map<Category,List<MenuPageInfo>> links = splitByCategory(
            filterSecured(getGeoServerApplication().getBeansOfType(MenuPageInfo.class))
        );

        List<MenuPageInfo> standalone = links.containsKey(null) 
            ? links.get(null)
            : new ArrayList<MenuPageInfo>();
        links.remove(null);

        List<Category> categories = new ArrayList(links.keySet());
        Collections.sort(categories);

        add(new ListView("category", categories){
            public void populateItem(ListItem item){
                Category category = (Category)item.getModelObject();
                item.add(new Label("category.header", new StringResourceModel(category.getNameKey(), (Component) null, null)));
                item.add(new ListView("category.links", links.get(category)){
                    public void populateItem(ListItem item){
                        MenuPageInfo info = (MenuPageInfo)item.getModelObject();
                        BookmarkablePageLink link = new BookmarkablePageLink("link", info.getComponentClass());
                        link.add(new AttributeModifier("title", true, new StringResourceModel(info.getDescriptionKey(), (Component) null, null)));
                        link.add(new Label("link.label", new StringResourceModel(info.getTitleKey(), (Component) null, null)));
                        Image image;
                        if(info.getIcon() != null) {
                            image = new Image("link.icon", new ResourceReference(info.getComponentClass(), info.getIcon()));
                        } else {
                            image = new Image("link.icon", new ResourceReference(GeoServerBasePage.class, "img/icons/silk/wrench.png"));
                        }
                        image.add(new AttributeModifier("alt", true, new ParamResourceModel(info.getTitleKey(), null)));
                        link.add(image);
                        item.add(link);
                    }
                });
            }
        });

        add(new ListView("standalone", standalone){
                    public void populateItem(ListItem item){
                        MenuPageInfo info = (MenuPageInfo)item.getModelObject();
                        BookmarkablePageLink link = new BookmarkablePageLink("link", info.getComponentClass());
                        link.add(new AttributeModifier("title", true, new StringResourceModel(info.getDescriptionKey(), (Component) null, null)));
                        link.add(new Label("link.label", new StringResourceModel(info.getTitleKey(), (Component) null, null)));
                        item.add(link);
                        
                    }
                }
        );

        add(feedbackPanel = new FeedbackPanel("feedback"));
        feedbackPanel.setOutputMarkupId( true );
        
        // ajax feedback image
        add(new Image("ajaxFeedbackImage", 
                new ResourceReference(GeoServerBasePage.class, "img/ajax-loader.gif")));
        
        add(new WebMarkupContainer(HEADER_PANEL));
    }
	
	/**
	 * Gets the page title from the PageName.title resource, falling back on "GeoServer" if not found
	 * @return
	 */
	String getPageTitle() {
	    try {
	        ParamResourceModel model = new ParamResourceModel("title", this);
	        return "GeoServer: " + model.getString();
	    } catch(Exception e) {
	        LOGGER.warning(getClass().getSimpleName() + " does not have a title set");
	    }
	    return "GeoServer";
    }

    /**
     * The base page is built with an empty panel in the page-header section that can be filled by
     * subclasses calling this method
     * 
     * @param component
     *            The component to be placed at the bottom of the page-header section. The component
     *            must have "page-header" id
     */
    protected void setHeaderPanel(Component component) {
        if (!HEADER_PANEL.equals(component.getId()))
            throw new IllegalArgumentException(
                    "The header panel component must have 'headerPanel' id");
        remove(HEADER_PANEL);
        add(component);
    }

    /**
     * Returns the application instance.
     */
    protected GeoServerApplication getGeoServerApplication() {
        return (GeoServerApplication) getApplication();
    }
    
    @Override
    public GeoServerSession getSession() {
        return (GeoServerSession) super.getSession();
    }

    /**
     * Convenience method for pages to get access to the geoserver
     * configuration.
     */
    protected GeoServer getGeoServer() {
        return getGeoServerApplication().getGeoServer();
    }

    /**
     * Convenience method for pages to get access to the catalog.
     */
    protected Catalog getCatalog() {
        return getGeoServerApplication().getCatalog();
    }
    
    /**
     * Splits up the pages by category, turning the list into a map keyed by category
     * @param pages
     * @return
     */
    private Map<Category,List<MenuPageInfo>> splitByCategory(List<MenuPageInfo> pages){
        Collections.sort(pages);
        HashMap<Category,List<MenuPageInfo>> map = new HashMap<Category,List<MenuPageInfo>>();

        for (MenuPageInfo page : pages){
            Category cat = page.getCategory();

            if (!map.containsKey(cat)) 
                map.put(cat, new ArrayList<MenuPageInfo>());

            map.get(cat).add(page);
        }

        return map;
    }
    
    /**
     * Filters out all of the pages that cannot be accessed by the current user
     * @param pageList
     * @return
     */
    private List<MenuPageInfo> filterSecured(List<MenuPageInfo> pageList) {
        Authentication user = getSession().getAuthentication();
        List<MenuPageInfo> result = new ArrayList<MenuPageInfo>();
        for (MenuPageInfo page : pageList) {
            final Class<GeoServerBasePage> pageClass = page.getComponentClass();
            if(GeoServerSecuredPage.class.isAssignableFrom(pageClass) &&
                    !page.getPageAuthorizer().isAccessAllowed(pageClass, user))
                continue;
            result.add(page);
        }
        return result;
    }
    
    @Override
    protected void configureResponse() {
        super.configureResponse();

        // this is to avoid https://issues.apache.org/jira/browse/WICKET-923 in Firefox
        final WebResponse response = getWebRequestCycle().getWebResponse();
        response.setHeader("Cache-Control", "no-cache, max-age=0, must-revalidate, no-store");
    }
    
   
   /**
    * Returns the id for the component used as a veil for the whole page while Wicket is processing
    * an ajax request, so it is impossible to trigger the same ajax action multiple times (think of
    * saving/deleting a resource, etc)
    *
    * @see IAjaxIndicatorAware#getAjaxIndicatorMarkupId()
    */
   public String getAjaxIndicatorMarkupId() {
       return "ajaxFeedback";
   }
   
   /**
    * Returns the feedback panel included in the GeoServer base page
    * @return
    */
   public FeedbackPanel getFeedbackPanel() {
       return feedbackPanel;
   }

}
