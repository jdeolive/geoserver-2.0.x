/*
 * Created on Feb 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.vfny.geoserver.action.validation;

import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.vfny.geoserver.action.ConfigAction;
import org.vfny.geoserver.config.validation.TestSuiteConfig;
import org.vfny.geoserver.config.validation.ValidationConfig;
import org.vfny.geoserver.global.UserContainer;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * ValidationTestDoIt purpose.
 * <p>
 * Description of ValidationTestDoIt ...
 * </p>
 *
 * @author dzwiers, Refractions Research, Inc.
 * @author $Author: sploreg $ (last modification)
 * @version $Id$
 */
public class ValidationTestDoIt extends ConfigAction {
    public ActionForward execute(ActionMapping mapping, ActionForm incomingForm,
        UserContainer user, HttpServletRequest request, HttpServletResponse response) {
        boolean stopThread = false;
        String parameter = mapping.getParameter();

        if ((parameter != null) && parameter.equals("stop")) {
            stopThread = true;
        }

        //Checks to see if previous Validation has even finished executing yet.
        Thread oldThread = (Thread) request.getSession().getAttribute(ValidationRunnable.KEY);

        if ((oldThread != null) && oldThread.isAlive()) {
            //OldThread has not finished execution; Shouldn't start a new one.
            //Alternatively, we could wait.
            if (stopThread == true) {
                oldThread.stop(); //This is decprecated, but is there another way to stop a Runnable?
            }
        } else {
            ServletContext context = this.getServlet().getServletContext();
            ValidationConfig validationConfig = (ValidationConfig) context.getAttribute(ValidationConfig.CONFIG_KEY);
            TestSuiteConfig suiteConfig = (TestSuiteConfig) request.getSession()
                                                                   .getAttribute(TestSuiteConfig.CURRENTLY_SELECTED_KEY);
            Map plugins = new HashMap();
            Map testSuites = new HashMap();
            validationConfig.toDTO(plugins, testSuites); // return by ref.

            TestValidationResults results = new TestValidationResults();

            try {
                ValidationRunnable testThread = new ValidationRunnable(request);
                testThread.setup(results, getDataConfig().toRepository(context), plugins, testSuites);

                Thread thread = new Thread(testThread);
                thread.start();

                request.getSession().setAttribute(ValidationRunnable.KEY, thread);
            } catch (Exception erp) {
                ActionErrors errors = new ActionErrors();
                errors.add(ActionErrors.GLOBAL_ERROR,
                    new ActionError("error.cannotRunValidation", erp));
                saveErrors(request, errors);
            }
        }

        return mapping.findForward("config.validation.displayResults");
    }
}
