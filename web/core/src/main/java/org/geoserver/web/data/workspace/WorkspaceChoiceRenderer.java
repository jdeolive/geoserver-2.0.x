package org.geoserver.web.data.workspace;

import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.geoserver.catalog.WorkspaceInfo;

/**
 * Simple choice renderer for {@link WorkspaceInfo}
 */
@SuppressWarnings("serial")
public class WorkspaceChoiceRenderer implements IChoiceRenderer {

    public Object getDisplayValue(Object object) {
        return ((WorkspaceInfo) object).getName();
    }

    public String getIdValue(Object object, int index) {
        return ((WorkspaceInfo) object).getId();
    }

}
