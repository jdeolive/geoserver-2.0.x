/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.io.Serializable;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * An abstract OK/cancel dialog, subclasses will have to provide the actual contents and behavior
 * for OK/cancel
 */
@SuppressWarnings("serial")
public class GeoServerDialog extends Panel {

    ModalWindow window;

    DialogDelegate delegate;

    public GeoServerDialog(String id) {
        super(id);
        add(window = new ModalWindow("dialog"));
    }

    /**
     * Sets the window title
     * 
     * @param title
     */
    public void setTitle(IModel title) {
        window.setTitle(title);
    }

    public String getHeightUnit() {
        return window.getHeightUnit();
    }

    public int getInitialHeight() {
        return window.getInitialHeight();
    }

    public int getInitialWidth() {
        return window.getInitialWidth();
    }

    public String getWidthUnit() {
        return window.getWidthUnit();
    }

    public void setHeightUnit(String heightUnit) {
        window.setHeightUnit(heightUnit);
    }

    public void setInitialHeight(int initialHeight) {
        window.setInitialHeight(initialHeight);
    }

    public void setInitialWidth(int initialWidth) {
        window.setInitialWidth(initialWidth);
    }

    public void setWidthUnit(String widthUnit) {
        window.setWidthUnit(widthUnit);
    }

    public int getMinimalHeight() {
        return window.getMinimalHeight();
    }

    public int getMinimalWidth() {
        return window.getMinimalWidth();
    }

    public void setMinimalHeight(int minimalHeight) {
        window.setMinimalHeight(minimalHeight);
    }

    public void setMinimalWidth(int minimalWidth) {
        window.setMinimalWidth(minimalWidth);
    }

    /**
     * Shows an OK/cancel dialog. The delegate will provide contents and behavior for the OK button
     * (and if needed, for the cancel one as well)
     * 
     * @param target
     * @param delegate
     */
    public void showOkCancel(AjaxRequestTarget target, final DialogDelegate delegate) {
        // wire up the contents
        window.setPageCreator(new ModalWindow.PageCreator() {

            public Page createPage() {
                return new ContentsPage(delegate.getContents("userPanel"));
            }
        });
        // make sure close == cancel behavior wise
        window.setCloseButtonCallback(new ModalWindow.CloseButtonCallback() {

            public boolean onCloseButtonClicked(AjaxRequestTarget target) {
                return delegate.onCancel(target);
            }
        });
        window.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            public void onClose(AjaxRequestTarget target) {
                delegate.onClose(target);
            }
        });

        // show the window
        this.delegate = delegate;
        window.show(target);
    }

    /**
     * Submit link that will forward to the {@link DialogDelegate}
     * 
     * @return
     */
    AjaxSubmitLink sumbitLink(Component contents) {
        AjaxSubmitLink link = new AjaxSubmitLink("submit") {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                if (delegate.onSubmit(target, (Component) this.getModelObject())) {
                    window.close(target);
                    delegate = null;
                }
            }

        };
        link.setModel(new Model(contents));
        return link;
    }

    /**
     * Link that will forward to the {@link DialogDelegate}
     * 
     * @return
     */
    Component cancelLink() {
        return new AjaxLink("cancel") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                if (delegate.onCancel(target)) {
                    window.close(target);
                    delegate = null;
                }
            }

        };
    }

    /**
     * This represents the contents of the dialog.
     * <p>
     * As of wicket 1.3.6 it still has to be a page, see
     * http://www.nabble.com/Nesting-ModalWindow-td19925848.html for details (ajax submit buttons
     * won't work with a panel)
     */
    protected class ContentsPage extends WebPage {

        public ContentsPage(Component contents) {
            Form form = new Form("form");
            add(form);
            form.add(contents);
            AjaxSubmitLink submit = sumbitLink(contents);
            form.add(submit);
            form.add(cancelLink());
            form.setDefaultButton(submit);
        }

    }

    /**
     * A {@link DialogDelegate} provides the bits needed to actually open a dialog:
     * <ul>
     * <li>a content pane, that will be hosted inside a {@link Form}</li>
     * <li>a behavior for the OK button</li>
     * <li>an eventual behavior for the Cancel button (the base implementation just returns true to
     * make the window close)</li>
     */
    public abstract static class DialogDelegate implements Serializable {

        /**
         * Builds the contents for this dialog
         * 
         * @param id
         * @return
         */
        protected abstract Component getContents(String id);

        /**
         * Called when the dialog is closed, allows the delegate to perform ajax updates on the page
         * underlying the dialog
         * 
         * @param target
         */
        public void onClose(AjaxRequestTarget target) {
            // by default do nothing
        }

        /**
         * Called when the dialog is submitted
         * 
         * @param target
         * @return true if the dialog is to be closed, false otherwise
         */
        protected abstract boolean onSubmit(AjaxRequestTarget target, Component contents);

        /**
         * Called when the dialog is canceled.
         * 
         * @param target
         * @return true if the dialog is to be closed, false otherwise
         */
        protected boolean onCancel(AjaxRequestTarget target) {
            return true;
        }
    }

}
