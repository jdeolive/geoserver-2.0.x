/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

/**
 * Information about a page that should be linked from the main menu in the GeoServer UI.
 * The "category" field is a category object identifying the menu section into which the 
 * link should be placed.
 * The "category" can be null; in this case the link will be placed outside of any category,
 * as a 'standalone' link.
 * The "order" field is a sort key for the link within the category. 
 * (Categories also have an order field.)
 *
 * @author David Winslow <dwinslow@opengeo.org>
 */
@SuppressWarnings("serial")
public class MenuPageInfo extends ComponentInfo<GeoServerBasePage> implements Comparable<MenuPageInfo> {
    Category category;
    int order;
    String icon;

    public void setCategory(Category category){
        this.category = category;
    }

    public Category getCategory(){
        return category;
    }

    public void setOrder(int order){
        this.order = order;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public int getOrder(){
        return order;
    }

    public int compareTo(MenuPageInfo other){
        return getOrder() - other.getOrder();
    }
    
    /**
     * Returns the object used to check whether this page can be accessed or not.<br>
     * It's used to hide the pages that cannot be accessed from the left menu.<br>
     * This method is invoked only if the page happens to be a {@link GeoServerSecuredPage}. 
     * <p>If you do override this method, make sure to override the authorizer grabbing
     * method in the page as well</p>
     * @return
     */
    public PageAuthorizer getPageAuthorizer() {
        return GeoServerSecuredPage.DEFAULT_AUTHORIZER;
    }
}
