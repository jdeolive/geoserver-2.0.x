/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.translator.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.MetaDataKey;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.translator.controller.TranslationController;
import org.geoserver.web.translator.model.TranslateBean;

/**
 * Page to edit a translation, split in two panels, one to select the resource key to translate and
 * anther to perform the translation.
 * 
 * @author Gabriel Roldan
 * @version $Id$
 * @since 2.0
 */
public class TranslationEditPage extends GeoServerBasePage {

    /**
     * The key to store the current translation progress in the user's {@link Session}
     */
    public static final MetaDataKey TRANSLATION_BEAN = new MetaDataKey(TranslateBean.class) {
        private static final long serialVersionUID = 1L;
    };

    private Form toolbarForm;

    private Form filterForm;

    private Form translateForm;

    private TextField filter;

    private AjaxButton hiddenSubmit;

    private Form changeUILanguageForm;

    private ResourceKeyTreePanel keyTreePanel;

    final TranslationResourcePanel translationPanel;

    public TranslationEditPage() {
        this(null);
    }

    public TranslationEditPage(final Locale targetLocale) {
        super();

        final IModel translationModel;
        {
            TranslateBean translateBean;
            final GeoServerApplication application = getGeoServerApplication();
            translateBean = (TranslateBean) application.getMetaData(TRANSLATION_BEAN);
            if (translateBean == null) {
                TranslationController controller = getController();
                Map<Locale, Map<String, String>> translatedResources;
                translatedResources = controller.getTranslatedResources();

                // default locale is keyed by null
                Locale baseLocale = null;
                translateBean = new TranslateBean(baseLocale, translatedResources);
                application.setMetaData(TRANSLATION_BEAN, translateBean);
            }

            translateBean.setTargetLanguage(targetLocale);

            // translationModel = new Model(translateBean);
            translationModel = new LoadableDetachableModel() {
                private static final long serialVersionUID = 1L;

                @Override
                protected Object load() {
                    final Application application = getApplication();
                    TranslateBean translateState = (TranslateBean) application
                            .getMetaData(TRANSLATION_BEAN);
                    return translateState;
                }
            };
        }

        setModel(translationModel);
        add(toolbar());
        add(filter());

        add(translateForm = new Form("translateForm"));
        translateForm
                .add(keyTreePanel = new ResourceKeyTreePanel("keyTreePanel", translationModel));
        translateForm.add(translationPanel = new TranslationResourcePanel("resourcePanel",
                translationModel));
        add(changeUILocale());

        final IModel currentKeyModel = new PropertyModel(translationModel, "currentKey");
        keyTreePanel.getTree().add(new AjaxFormSubmitBehavior(translateForm, "onclick") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                String selectedNodeKey = keyTreePanel.getSelectedNodeKey();
                currentKeyModel.setObject(selectedNodeKey);
                translationPanel.refresh(target);
            }

            @Override
            protected void onError(final AjaxRequestTarget target) {
                // TODO Auto-generated method stub
            }
        });
    }

    private Component changeUILocale() {
        changeUILanguageForm = new Form("changeUILanguageForm");
        changeUILanguageForm.add(changeUiLanguageChoice());
        return changeUILanguageForm;
    }

    private Component filter() {
        // build the filter form
        filterForm = new Form("filterForm");

        final IModel filterModel = new PropertyModel(getModel(), "filter");
        filterForm.add(filter = new TextField("filter", filterModel));
        filter.add(new SimpleAttributeModifier("title", String.valueOf(new ResourceModel(
                "ResourceKeyTreePanel.search", "Search").getObject())));
        filterForm.add(hiddenSubmit = hiddenSearchSubmit());
        filterForm.setDefaultButton(hiddenSubmit);

        return filterForm;
    }

    private Component toolbar() {
        toolbarForm = new Form("toolbarForm");
        toolbarForm.add(expandAllLink());
        toolbarForm.add(collapseAllLink());
        toolbarForm.add(treeViewLink());
        toolbarForm.add(flatViewLink());
        toolbarForm.add(showMissingOnlyComponent());
        return toolbarForm;
    }

    private CheckBox showMissingOnlyComponent() {
        final IModel model = new PropertyModel(getModel(), "showMissingOnly");
        final CheckBox checkBox = new CheckBox("showMissingOnly", model);

        checkBox.add(new AjaxFormSubmitBehavior(toolbarForm, "onChange") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                keyTreePanel.refresh();
                target.addComponent(keyTreePanel);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                // Ignore
            }

        });

        return checkBox;
    }

    /**
     * The hidden button that will submit the form when the user presses enter in the text field
     */
    private AjaxButton hiddenSearchSubmit() {
        return new AjaxButton("submit") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                keyTreePanel.refresh();
                target.addComponent(keyTreePanel);
            }
        };
    }

    private AjaxLink flatViewLink() {
        return new AjaxLink("flatView", new PropertyModel(getModel(), "flatView")) {
            private static final long serialVersionUID = 6747039733052048287L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                setModelObject(Boolean.TRUE);
                keyTreePanel.refresh();
                target.addComponent(keyTreePanel);
            }
        };
    }

    private AjaxLink treeViewLink() {
        return new AjaxLink("treeView", new PropertyModel(getModel(), "flatView")) {
            private static final long serialVersionUID = 6747039733052048287L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                setModelObject(Boolean.FALSE);
                keyTreePanel.refresh();
                target.addComponent(keyTreePanel);
            }
        };
    }

    private AjaxLink collapseAllLink() {
        return new AjaxLink("collapseAll") {
            private static final long serialVersionUID = 6747039733052048287L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                keyTreePanel.collapseAll(target);
            }
        };
    }

    private AjaxLink expandAllLink() {
        return new AjaxLink("expandAll") {
            private static final long serialVersionUID = 6747039733052048287L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                keyTreePanel.expandAll(target);
            }
        };
    }

    private Component changeUiLanguageChoice() {
        final IModel sessionModel = new Model(getSession());
        final IModel uiLocaleModel = new PropertyModel(sessionModel, "locale");
        IModel choices = new LocaleListDetachableModel(true, uiLocaleModel);

        final Component uiLanguageChoice = new LocaleDropDown("changeUiLanguage", uiLocaleModel,
                uiLocaleModel, choices);

        uiLanguageChoice.add(new AjaxFormSubmitBehavior(changeUILanguageForm, "onChange") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                getGeoServerApplication().clearWicketCaches();
                setResponsePage(TranslationEditPage.this);
            }

            @Override
            protected void onError(final AjaxRequestTarget target) {
                //
            }
        });
        return uiLanguageChoice;
    }

    private TranslationController getController() {
        GeoServerApplication application = getGeoServerApplication();
        TranslationController controller = application.getBeanOfType(TranslationController.class);
        return controller;
    }

    /**
     * A model for the list of available translations
     */
    public static class LocaleListDetachableModel extends LoadableDetachableModel {

        private static final long serialVersionUID = 1L;

        private boolean includeDefault;

        private IModel uiLocaleModel;

        public LocaleListDetachableModel(final boolean includeDefault, final IModel uiLocaleModel) {
            this.includeDefault = includeDefault;
            this.uiLocaleModel = uiLocaleModel;
        }

        @Override
        protected Object load() {
            Set<Locale> availableTranslations;
            {
                final GeoServerApplication geoServerApplication = GeoServerApplication.get();
                final TranslateBean translateState = (TranslateBean) geoServerApplication
                        .getMetaData(TRANSLATION_BEAN);
                availableTranslations = translateState.getAvailableTranslations();
            }
            List<Locale> sorted = new ArrayList<Locale>(availableTranslations);
            if (includeDefault) {
                sorted.add(Locale.ENGLISH);
            }
            Collections.sort(sorted, new Comparator<Locale>() {
                public int compare(Locale o1, Locale o2) {
                    Locale uiLocale = (Locale) uiLocaleModel.getObject();
                    String displayName1 = o1.getDisplayName(uiLocale);
                    String displayName2 = o2.getDisplayName(uiLocale);
                    return displayName1.compareTo(displayName2);
                }
            });
            return sorted;
        }
    }
}
