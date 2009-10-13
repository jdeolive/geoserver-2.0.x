/*
 * Created on Jan 22, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.vfny.geoserver.form.validation;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.vfny.geoserver.config.validation.ValidationConfig;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;


/**
 * ValidationTestSuiteSelectForm purpose.
 * <p>
 * Used to store data coming in from the web form, to be passed to
 * the ValidationTestSuiteSelectAction.
 * </p>
 *
 * @author rgould, Refractions Research, Inc.
 * @author $Author: dmzwiers $ (last modification)
 * @version $Id$
 */
public class ValidationTestSuiteSelectForm extends ActionForm {
    private String selectedTestSuite;
    private String buttonAction;

    public void reset(ActionMapping arg0, HttpServletRequest request) {
        super.reset(arg0, request);

        selectedTestSuite = "";
        buttonAction = "";
    }

    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        ActionErrors errors = new ActionErrors();

        return errors;
    }

    public SortedSet getTestSuites() {
        try {
            ServletContext context = getServlet().getServletContext();
            ValidationConfig validationConfig = (ValidationConfig) context.getAttribute(ValidationConfig.CONFIG_KEY);

            if ((validationConfig != null) && (validationConfig.getTestSuiteNames() != null)) {
                return new TreeSet(validationConfig.getTestSuiteNames());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new TreeSet();
    }

    /**
     * Access selectedTestSuite property.
     *
     * @return Returns the selectedTestSuite.
     */
    public String getSelectedTestSuite() {
        if (selectedTestSuite != null) {
            return selectedTestSuite;
        }

        return "";
    }

    /**
     * Set selectedTestSuite to selectedTestSuite.
     *
     * @param selectedTestSuite The selectedTestSuite to set.
     */
    public void setSelectedTestSuite(String selectedTestSuite) {
        this.selectedTestSuite = selectedTestSuite;
    }

    /**
     * Access buttonAction property.
     *
     * @return Returns the buttonAction.
     */
    public String getButtonAction() {
        return buttonAction;
    }

    /**
     * Set buttonAction to buttonAction.
     *
     * @param buttonAction The buttonAction to set.
     */
    public void setButtonAction(String buttonAction) {
        this.buttonAction = buttonAction;
    }
}
