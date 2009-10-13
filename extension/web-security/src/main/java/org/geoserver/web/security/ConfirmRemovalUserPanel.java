package org.geoserver.web.security;

import java.util.Arrays;
import java.util.List;

import org.acegisecurity.userdetails.User;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.StringResourceModel;

public class ConfirmRemovalUserPanel extends Panel {

    List<User> roots;
    
    public ConfirmRemovalUserPanel(String id, User... roots) {
        this(id, Arrays.asList(roots));
    }

    public ConfirmRemovalUserPanel(String id, List<User> roots) {
        super(id);
        this.roots = roots;

        // add roots
        WebMarkupContainer root = new WebMarkupContainer("rootObjects");
        root.add(new Label("rootObjectNames", names(roots)));
        root.setVisible(!roots.isEmpty());
        add(root);

        // removed objects root (we show it if any removed object is on the list)
        WebMarkupContainer removed = new WebMarkupContainer("removedObjects");

        add(removed);

        // removed workspaces
        WebMarkupContainer rulesRemoved = new WebMarkupContainer("rulesRemoved");
        removed.add(rulesRemoved);
        List<User> rules = roots;
        if (rules.size() == 0)
            rulesRemoved.setVisible(false);
        rulesRemoved.add(new Label("rules", names(rules)));
    }

    String names(List objects) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < objects.size(); i++) {
            sb.append(name(objects.get(i)));
            if (i < (objects.size() - 1))
                sb.append(", ");
        }
        return sb.toString();
    }

    String name(Object object) {
        try {
            return (String) BeanUtils.getProperty(object, "username");
        } catch (Exception e) {
            throw new RuntimeException("A data object that does not have "
                    + "a 'name' property has been used, this is unexpected", e);
        }
    }

    protected StringResourceModel canRemove(User data) {
        return null;
    }

}
