/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.flow.config.DefaultControlFlowConfigurator;
import org.geoserver.ows.HttpErrorCodeException;
import org.geoserver.ows.Request;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Operation;
import org.geotools.util.logging.Logging;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Callback that controls the flow of OWS requests based on user specified rules and makes sure
 * GeoServer does not get overwhelmed by too many concurrent ones. Can also be used to provide
 * different quality of service on different users.
 * 
 * @author Andrea Aime - OpenGeo
 */
public class ControlFlowCallback extends AbstractDispatcherCallback implements
        ApplicationContextAware {
    static final Logger LOGGER = Logging.getLogger(ControlFlowCallback.class);

    static ThreadLocal<List<FlowController>> REQUEST_CONTROLLERS = new ThreadLocal<List<FlowController>>();

    List<FlowController> controllers = Collections.emptyList();
    long timeout = -1;

    ControlFlowConfigurator configurator;

    public void finished(Request request) {
        // call back the same controllers we used when the operation started
        if (REQUEST_CONTROLLERS.get() != null) {
            List<FlowController> fcl = REQUEST_CONTROLLERS.get();
            for (FlowController flowController : fcl) {
                flowController.requestComplete(request);
            }
        }
        // clean up the thread local
        REQUEST_CONTROLLERS.remove();
    }

    public Operation operationDispatched(Request request, Operation operation) {
        // check if we need to rebuild the flow controller list
        if (configurator.isStale())
            reloadConfiguration();

        // scan through the existing controllers and set the list in a thread local
        // so that this request will get exactly the same list when the operation finishes
        List<FlowController> controllers = this.controllers;
        if (controllers.size() > 0) {
            REQUEST_CONTROLLERS.set(controllers);
            long maxTime = timeout > 0 ? System.currentTimeMillis() + timeout : -1;
            for (FlowController flowController : controllers) {
                if(timeout > 0) {
                    long maxWait = maxTime - System.currentTimeMillis();
                    if(!flowController.requestIncoming(request, maxWait)) 
                        throw new HttpErrorCodeException(503, "Requested timeout out while waiting to be executed");
                 } else {
                    flowController.requestIncoming(request, -1);
                }
            }
        }
        return operation;
    }

    /**
     * Reloads the flow controller list, sorts them by priority, and replaces the existing ones
     */
    void reloadConfiguration() {
        try {
            List<FlowController> newControllers = new ArrayList<FlowController>(configurator
                    .buildFlowControllers());
            Collections.sort(newControllers, new ControllerPriorityComparator());
            controllers = newControllers;
            timeout = configurator.getTimeout();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error occurerd during flow controllers reconfiguration");
        }
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // look for a ControlFlowConfigurator in the application context, if none is found, use the
        // default one
        configurator = GeoServerExtensions.bean(ControlFlowConfigurator.class, applicationContext);
        if (configurator == null)
            configurator = new DefaultControlFlowConfigurator();
    }

    /**
     * Sorts the flow controllers based on their priority (lower number means higher priority)
     * 
     * @author Andrea Aime - OpenGeo
     * 
     */
    static class ControllerPriorityComparator implements Comparator<FlowController> {

        public int compare(FlowController o1, FlowController o2) {
            return o1.getPriority() - o2.getPriority();
        }

    }

}
