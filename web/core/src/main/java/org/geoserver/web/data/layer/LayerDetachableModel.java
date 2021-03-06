/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.web.GeoServerApplication;

/**
 * A loadable model for layers. Warning, don't use it in a tabbed form
 * or in any other places where you might need to keep the modifications
 * in a resource stable across page loads.
 */
@SuppressWarnings("serial")
public class LayerDetachableModel extends LoadableDetachableModel {
    String name;

    public LayerDetachableModel(LayerInfo layer) {
        super(layer);
        this.name = layer.getName();
    }

    @Override
    protected Object load() {
        return GeoServerApplication.get().getCatalog().getLayerByName(name);
    }
}
