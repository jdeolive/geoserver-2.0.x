/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.vfny.geoserver.action.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Level;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.MessageResources;
import org.vfny.geoserver.wms.responses.map.kml.KMLUtils;
import org.vfny.geoserver.wms.responses.map.kml.RegionatingStrategy;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.vfny.geoserver.action.ConfigAction;
import org.vfny.geoserver.action.HTMLEncoder;
import org.vfny.geoserver.config.AttributeTypeInfoConfig;
import org.vfny.geoserver.config.ConfigRequests;
import org.vfny.geoserver.config.DataConfig;
import org.vfny.geoserver.config.DataStoreConfig;
import org.vfny.geoserver.config.FeatureTypeConfig;
import org.vfny.geoserver.form.data.AttributeForm;
import org.vfny.geoserver.form.data.TypesEditorForm;
import org.vfny.geoserver.global.FeatureTypeInfo;
import org.vfny.geoserver.global.MetaDataLink;
import org.vfny.geoserver.global.UserContainer;
import org.vfny.geoserver.util.DataStoreUtils;

import com.vividsolutions.jts.geom.Envelope;


/**
 * These Action handles all the buttons for the SimpleFeatureType Editor.
 *
 * <p>
 * This one is more complicated then usual since not all the actions require
 * the form bean to be validated! I am going to have to hack a little bit to
 * make that happen, I may end up making the form bean validation differ
 * depending on the selected action.
 * </p>
 *
 * <p>
 * Buttons that make this action go:
 *
 * <ul>
 * <li>
 * Submit: update the FeatureTypeConfig held by the user, punt it back into
 * DataConfig and return to the FeatureTypeSelect screen.
 * </li>
 * <li>
 * Up and Down (for each attribute): not quite sure how to make these work yet
 * - I hope I dont have to give them different names.
 * </li>
 * </ul>
 *
 * As usual we will have to uninternationlize the action name provided to us.
 * </p>
 *
 * @author Richard Gould
 * @author Jody Garnett
 */
public class TypesEditorAction extends ConfigAction {
    public ActionForward execute(ActionMapping mapping, ActionForm form, UserContainer user,
        HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.finer(new StringBuffer("form bean:").append(form.getClass().getName()).toString());
        }

        TypesEditorForm typeForm = (TypesEditorForm) form;

        String action = typeForm.getAction();

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.finer(new StringBuffer("TypesEditorAction is ").append(action).toString());
        }

        Locale locale = (Locale) request.getLocale();
        MessageResources messages = getResources(request);
        final String SUBMIT = HTMLEncoder.decode(messages.getMessage(locale, "label.submit"));
        final String ADD = HTMLEncoder.decode(messages.getMessage(locale, "label.add"));
        final String BBOX = HTMLEncoder.decode(messages.getMessage(locale,
                    "config.data.calculateBoundingBox.label"));
        final String LOOKUP_SRS = HTMLEncoder.decode(messages.getMessage(locale,
                    "config.data.lookupSRS.label"));

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.finer(new StringBuffer("BBOX: ").append(BBOX).toString());
        }

        final String NEWSLD = HTMLEncoder.decode(messages.getMessage(locale,
                    "config.data.sldWizard.label"));

        if (typeForm.getAutoGenerateExtent().equals("true")) {
            if ((typeForm.getSRS() == null) || typeForm.getSRS().trim().equals("0")) {
                executeLookupSRS(mapping, typeForm, user, request);
            }

            executeBBox(mapping, typeForm, user, request);

            return executeSubmit(mapping, typeForm, user, request);
        }

        if (SUBMIT.equals(action)) {
            return executeSubmit(mapping, typeForm, user, request);
        }

        if (action.equals(BBOX)) {
            return executeBBox(mapping, typeForm, user, request);
        }

        if (action.equals(LOOKUP_SRS)) {
            return executeLookupSRS(mapping, typeForm, user, request);
        }

        if (action.equals(NEWSLD)) { // if the SLDWizard button was hit

            return mapping.findForward("SLDWizard");
        }

        List attributes = typeForm.getAttributes();

        if (action.startsWith("up_")) {
            int index = Integer.parseInt(action.substring(3));
            Object attribute = attributes.remove(index);
            attributes.add(index - 1, attribute);
        } else if (action.startsWith("down_")) {
            int index = Integer.parseInt(action.substring(5));
            Object attribute = attributes.remove(index);
            attributes.add(index + 1, attribute);
        } else if (action.startsWith("delete_")) {
            int index = Integer.parseInt(action.substring(7));
            attributes.remove(index);
        } else if (action.equals(ADD)) {
            executeAdd(mapping, typeForm, user, request);
        }

        // Update, Up, Down, Add, Remove need to resync
        sync(typeForm, user.getFeatureTypeConfig(), request);
        form.reset(mapping, request);

        return mapping.findForward("config.data.type.editor");
    }

    private ActionForward executeLookupSRS(ActionMapping mapping, TypesEditorForm typeForm,
        UserContainer user, HttpServletRequest request)
        throws IOException, ServletException {
        DataConfig dataConfig = getDataConfig();
        DataStoreConfig dsConfig = dataConfig.getDataStore(typeForm.getDataStoreId());
        DataStore dataStore = null;
        try {
            dataStore = dsConfig.findDataStore(request.getSession().getServletContext());
            SimpleFeatureType featureType = dataStore.getSchema(typeForm.getTypeName());
            FeatureSource<SimpleFeatureType, SimpleFeature> fs;
            fs = dataStore.getFeatureSource(featureType.getTypeName());

            CoordinateReferenceSystem crs = fs.getSchema().getCoordinateReferenceSystem();
            String s = CRS.lookupIdentifier(crs, true);

            if (s == null) {
                typeForm.setSRS("UNKNOWN");
            } else if (s.indexOf(':') != -1) {
                typeForm.setSRS(s.substring(s.indexOf(':') + 1));
            } else {
                typeForm.setSRS(s);
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error occurred trying to lookup the SRS", e);
            typeForm.setSRS("UNKNOWN");
        } finally {
            if(dataStore != null) dataStore.dispose();
        }

        return mapping.findForward("config.data.type.editor");
    }

    /**
     * Populate the bounding box fields from the source and pass control back
     * to the UI
     *
     * @param mapping DOCUMENT ME!
     * @param typeForm DOCUMENT ME!
     * @param user DOCUMENT ME!
     * @param request DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     *
     * @throws IOException DOCUMENT ME!
     * @throws ServletException DOCUMENT ME!
     */
    private ActionForward executeBBox(ActionMapping mapping, TypesEditorForm typeForm,
        UserContainer user, HttpServletRequest request)
        throws IOException, ServletException {
        DataConfig dataConfig = getDataConfig();
        DataStoreConfig dsConfig = dataConfig.getDataStore(typeForm.getDataStoreId());
        DataStore dataStore = null;
        try {
            dataStore = dsConfig.findDataStore(request.getSession().getServletContext());
            SimpleFeatureType featureType = dataStore.getSchema(typeForm.getTypeName());
            FeatureSource<SimpleFeatureType, SimpleFeature> fs;
            fs = dataStore.getFeatureSource(featureType.getTypeName());
    
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(new StringBuffer("calculating bbox for their dataset").toString());
            }
    
            Envelope envelope = DataStoreUtils.getBoundingBoxEnvelope(fs);
    
            if (envelope.isNull()) // there's no data in the featuretype!!
             {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine(new StringBuffer("SimpleFeatureType '").append(featureType.getTypeName())
                                                                 .append("' has a null bounding box")
                                                                 .toString());
                }
    
                ActionErrors errors = new ActionErrors();
                errors.add(ActionErrors.GLOBAL_ERROR,
                    new ActionError("error.data.nullBBOX", featureType.getTypeName()));
                saveErrors(request, errors);
    
                return mapping.findForward("config.data.type.editor");
            }
    
            // do a translation from the data's coordinate system to lat/long
    
            //TODO: DJB: NOTE: 1/2 of the config stuff has the srs as an int, 1/2 as string!!  We should be more consistent!
            String srs = typeForm.getSRS(); // what the user typed in for the srs in the form
    
            if (srs.indexOf(':') == -1) { // check to see if its of the form "EPSG:#" (or some such thing)
                srs = "EPSG:" + srs; //assume they wanted to use an EPSG number
            }

            CoordinateReferenceSystem crsDeclared = CRS.decode(srs);
            CoordinateReferenceSystem original = null;

            if (featureType.getGeometryDescriptor() != null) {
                original = featureType.getCoordinateReferenceSystem();
            }

            if (original == null) {
                original = crsDeclared;
            }

            CoordinateReferenceSystem crsLatLong = CRS.decode("EPSG:4326"); // latlong

            // let's show coordinates in the declared crs, not in the native one, to
            // avoid confusion (since on screen we do have the declared one, the native is
            // not visible)
            Envelope declaredEnvelope = envelope;

            if (!CRS.equalsIgnoreMetadata(original, crsDeclared)) {
                if(typeForm.getSrsHandlingCode() == FeatureTypeInfo.REPROJECT) {
                    MathTransform xform = CRS.findMathTransform(original, crsDeclared, true);
                    declaredEnvelope = JTS.transform(envelope, null, xform, 10); //convert data bbox to lat/long
                } else if(typeForm.getSrsHandlingCode() == FeatureTypeInfo.FORCE) {
                    declaredEnvelope = new ReferencedEnvelope(envelope, crsDeclared);
                }
            }

            LOGGER.finer("Seeting form's data envelope: " + declaredEnvelope);
            typeForm.setDataMinX(Double.toString(declaredEnvelope.getMinX()));
            typeForm.setDataMaxX(Double.toString(declaredEnvelope.getMaxX()));
            typeForm.setDataMinY(Double.toString(declaredEnvelope.getMinY()));
            typeForm.setDataMaxY(Double.toString(declaredEnvelope.getMaxY()));
            
            // preserve the actual native envelope as well
            typeForm.setNativeMinX(Double.toString(envelope.getMinX()));
            typeForm.setNativeMaxX(Double.toString(envelope.getMaxX()));
            typeForm.setNativeMinY(Double.toString(envelope.getMinY()));
            typeForm.setNativeMaxY(Double.toString(envelope.getMaxY()));

            MathTransform xform = CRS.findMathTransform(original, crsLatLong, true);
            Envelope xformed_envelope = JTS.transform(envelope, xform); //convert data bbox to lat/long

            typeForm.setMinX(Double.toString(xformed_envelope.getMinX()));
            typeForm.setMaxX(Double.toString(xformed_envelope.getMaxX()));
            typeForm.setMinY(Double.toString(xformed_envelope.getMinY()));
            typeForm.setMaxY(Double.toString(xformed_envelope.getMaxY()));
        } catch (NoSuchAuthorityCodeException e) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(e.getLocalizedMessage());
                LOGGER.fine(e.getStackTrace().toString());
            }

            ActionErrors errors = new ActionErrors();
            errors.add(ActionErrors.GLOBAL_ERROR,
                new ActionError("error.data.couldNotFindSRSAuthority", e.getLocalizedMessage(),
                    e.getAuthorityCode()));
            saveErrors(request, errors);

            return mapping.findForward("config.data.type.editor");
        } catch (FactoryException fe) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(fe.getLocalizedMessage());
                LOGGER.fine(fe.getStackTrace().toString());
            }

            ActionErrors errors = new ActionErrors();
            errors.add(ActionErrors.GLOBAL_ERROR,
                new ActionError("error.data.factoryException", fe.getLocalizedMessage()));
            saveErrors(request, errors);

            return mapping.findForward("config.data.type.editor");
        } catch (TransformException te) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(te.getLocalizedMessage());
                LOGGER.fine(te.getStackTrace().toString());
            }

            ActionErrors errors = new ActionErrors();
            errors.add(ActionErrors.GLOBAL_ERROR, new ActionError("error.data.transformException"));
            saveErrors(request, errors);

            return mapping.findForward("config.data.type.editor");
        } finally {
            if(dataStore != null) dataStore.dispose();
        }

        return mapping.findForward("config.data.type.editor");
    }

    /**
     * Sync generated attributes with schemaBase.
     *
     * @param form
     * @param config
     */
    private void sync(TypesEditorForm form, FeatureTypeConfig config, HttpServletRequest request) {
        config.setName(form.getTypeName());
        if(form.getAlias() != null && "".equals(form.getAlias().trim()))
            config.setAlias(null);
        else
            config.setAlias(form.getAlias());
        config.setAbstract(form.getAbstract());
        config.setDefaultStyle(form.getStyleId());

        config.getStyles().clear();
        if (form.getOtherSelectedStyles() != null) {

            for (int i = 0; i < form.getOtherSelectedStyles().length; i++) {
                config.addStyle(form.getOtherSelectedStyles()[i]);
            }
        }

        config.setSRS(Integer.parseInt(form.getSRS()));
        config.setTitle(form.getTitle());
        Envelope latLonBbox = getBoundingBox(form);
        config.setLatLongBBox(latLonBbox);
        config.setNativeBBox(getNativeBBox(form));
        
        config.setKeywords(keyWords(form));
        config.setMetadataLinks(metadataLinks(form));
        config.setWmsPath(form.getWmsPath());
        config.setCacheMaxAge(form.getCacheMaxAge());
        config.setCachingEnabled(form.isCachingEnabled());
        config.setIndexingEnabled(form.isIndexingEnabled());
        config.setMaxFeatures(Integer.parseInt(form.getMaxFeatures()));

        // Regionating stuff.  We need to check for changes and clear the regionating cache if 
        // settings changed.
        boolean regionatorNeedsCleaning = false;
        Integer limit = null;
        
        regionatorNeedsCleaning 
            |= config.getRegionateAttribute() != null
            && !config.getRegionateAttribute().equals(form.getRegionateAttribute());
        regionatorNeedsCleaning 
            |= config.getRegionateStrategy() != null 
            && !config.getRegionateStrategy().equals(form.getRegionateStrategy());
        try {
            limit = Integer.valueOf(form.getRegionateFeatureLimit());
            regionatorNeedsCleaning |= limit.intValue() != (config.getRegionateFeatureLimit());
        } catch (NumberFormatException nfe){
            // leave the previous value
        }

        if (regionatorNeedsCleaning) {
            String dsName = config.getDataStoreId();
            if (dsName != null && getData().getDataStoreInfo(dsName) != null){
                String nsPrefix = getData().getDataStoreInfo(dsName).getNamesSpacePrefix();
                String qualifiedname = 
                    getData().getDataStoreInfo(config.getDataStoreId()).getNamesSpacePrefix() 
                    + ":" + config.getName();
                    try {
                        FeatureTypeInfo fti = getData().getFeatureTypeInfo(qualifiedname);
                        RegionatingStrategy rs = KMLUtils.findStrategyByName(config.getRegionateStrategy());
                        if (rs != null) rs.clearCache((org.geoserver.catalog.FeatureTypeInfo)fti.getLayerInfo().getResource());
                    } catch (NoSuchElementException e){
                        LOGGER.log(
                            Level.FINE,
                            "Changed regionating settings on new featuretype, no cleanup needed."
                            );
                    }
            }
        } 

        if (!(null == form.getNameTemplate() || "null".equals(form.getNameTemplate()))){
            config.setNameTemplate(form.getNameTemplate());
        }

        config.setRegionateAttribute(form.getRegionateAttribute());
        config.setRegionateStrategy(form.getRegionateStrategy());
        if (limit != null) config.setRegionateFeatureLimit(limit);

        config.setSRSHandling(form.getSrsHandlingCode());

        if (!form.isCachingEnabledChecked()) {
            config.setCachingEnabled(false);
        }

        if (!form.isIndexingEnabledChecked()){
            config.setIndexingEnabled(false);
        }

        String schemaBase = form.getSchemaBase();

        if ((schemaBase == null) || schemaBase.equals("") || schemaBase.equals("--")) {
            config.setSchemaBase(null);
            config.setSchemaName(null);
            config.setSchemaAttributes(null);
        } else {
            config.setSchemaBase(schemaBase);

            String schemaName = config.getSchemaName();
            List schemaAttributes = config.getSchemaAttributes();
            System.out.println("in non null sb, sname: " + schemaName + ", satts: "
                + schemaAttributes);

            if ((schemaName == null) || (schemaName.trim().length() == 0)) {
                schemaName = form.getTypeName() + "_Type";
                //HACK: For some reason only when editing an already exisitng
                //featureType, on the first time of switching to the editor
                //it gets a full schemaAttribute list, and I can't find where
                //so for now we are just relying on schemaName being null or
                schemaAttributes = null;
                //System.out.println("testing on schemaAtts: " + schemaAttributes);               
                config.setSchemaName(schemaName);
            } else {
                config.setSchemaName(form.getSchemaName());
            }

            if ((schemaAttributes == null) || schemaAttributes.isEmpty()) {
                schemaAttributes = new ArrayList();

                List createList = form.getCreateableAttributes();
                System.out.println("schemaAtts null, createList: " + createList);

                SimpleFeatureType fType = getFeatureType(form, request);

                for (int i = 0; i < fType.getAttributeCount(); i++) {
                    AttributeDescriptor attType = fType.getDescriptor(i);
                    AttributeTypeInfoConfig attributeConfig = new AttributeTypeInfoConfig(attType);
                    schemaAttributes.add(attributeConfig);

                    //new ArrayList();
                    //DataStoreConfig dsConfig = config.
                    //SimpleFeatureType featureType = config.get
                }

                config.setSchemaAttributes(schemaAttributes);
            } else {
                config.setSchemaAttributes(form.toSchemaAttributes());
            }
        }

        //            config.setSchemaAttributes(form.toSchemaAttributes());
        LOGGER.fine("config schema atts is " + config.getSchemaAttributes());

        //config.setSchemaAttributes(form.toSchemaAttributes());
    }

    /**
     * Convert a boudning box in latintute/longitude coordinates to another CRS, specified by name.
     * @param latLonBbox the latitude/longitude boudning box
     * @param crsName the name of the CRS to which it should be converted
     * @return the converted bounding box
     * @throws Exception if anything goes wrong
     */
    private Envelope convertBBoxFromLatLon(Envelope latLonBbox, String crsName) throws Exception {
        CoordinateReferenceSystem latLon = CRS.decode("EPSG:4326");
        CoordinateReferenceSystem nativeCRS = CRS.decode(crsName);

        Envelope env = null;
        if (!CRS.equalsIgnoreMetadata(latLon, nativeCRS)){
            MathTransform xform = CRS.findMathTransform(latLon, nativeCRS, true);
            env = JTS.transform(latLonBbox, null, xform, 10); // convert databbox to native CRS
        } else {
            env = latLonBbox;
        }

        return env;
    }

    private void executeAdd(ActionMapping mapping, TypesEditorForm form, UserContainer user,
        HttpServletRequest request) {
        String attributeName = form.getNewAttribute();

        SimpleFeatureType fType = getFeatureType(form, request);
        AttributeForm newAttribute = newAttributeForm(attributeName, fType);
        form.getAttributes().add(newAttribute);
    }

    private AttributeForm newAttributeForm(String attributeName, SimpleFeatureType featureType) {
        AttributeDescriptor attributeType = featureType.getDescriptor(attributeName);
        AttributeTypeInfoConfig attributeConfig = new AttributeTypeInfoConfig(attributeType);
        AttributeForm newAttribute = new AttributeForm(attributeConfig, attributeType);

        return newAttribute;
    }

    private SimpleFeatureType getFeatureType(TypesEditorForm form, HttpServletRequest request) {
        SimpleFeatureType featureType = null;

        DataStore dataStore = null; 
        try {
            DataConfig config = ConfigRequests.getDataConfig(request);
            DataStoreConfig dataStoreConfig = config.getDataStore(form.getDataStoreId());
            dataStore = dataStoreConfig.findDataStore(getServlet().getServletContext());
            featureType = dataStore.getSchema(form.getTypeName());
        } catch (IOException e) {
            // DataStore unavailable!
        } finally {
            if(dataStore != null) dataStore.dispose();
        }

        return featureType;
    }

    /**
     * Execute Submit Action.
     *
     * @param mapping
     * @param form
     * @param user
     * @param request
     *
     * @return
     */
    private ActionForward executeSubmit(ActionMapping mapping, TypesEditorForm form,
        UserContainer user, HttpServletRequest request) {
        FeatureTypeConfig config = user.getFeatureTypeConfig();
        
        // clean up old names (but make sure not to delete another config which happens
        // to use the same name
        DataConfig dataConfig = (DataConfig) getDataConfig();
        String keyName = config.getDataStoreId() + ":" + config.getName();
        String keyAlias = config.getDataStoreId() + ":" + config.getAlias();
        FeatureTypeConfig oldConfig = dataConfig.getFeatureTypeConfig(keyName);
        if(oldConfig != null && oldConfig == config)
            dataConfig.removeFeatureType(keyName);
        if(config.getAlias() != null) {
            oldConfig = dataConfig.getFeatureTypeConfig(keyAlias);
            if(oldConfig != null && oldConfig == config)
                dataConfig.removeFeatureType(keyAlias);
        }
        
        sync(form, config, request);
        // recompute after synch
        keyName = config.getDataStoreId() + ":" + config.getName();
        keyAlias = config.getDataStoreId() + ":" + config.getAlias();
        if(config.getAlias() != null && !"".equals(config.getAlias()))
            dataConfig.addFeatureType(keyAlias, config);
        else
            dataConfig.addFeatureType(keyName, config);

        // Don't think reset is needed (as me have moved on to new page)
        // form.reset(mapping, request);
        getApplicationState().notifyConfigChanged();

        // Feature no longer selected
        user.setFeatureTypeConfig(null);

        return mapping.findForward("config.data.type");
    }

    /**
     * DOCUMENT ME!
     *
     * @param typeForm
     *
     * @return Bounding box in lat long
     */
    private Envelope getBoundingBox(TypesEditorForm typeForm) {
        return new Envelope(Double.parseDouble(typeForm.getMinX()),
            Double.parseDouble(typeForm.getMaxX()), Double.parseDouble(typeForm.getMinY()),
            Double.parseDouble(typeForm.getMaxY()));
    }

    /**
     * DOCUMENT ME!
     *
     * @param typeForm
     *
     * @return Bounding box in lat long
     */
    private Envelope getNativeBBox(TypesEditorForm typeForm) {
        // here, we try to use the native bbox computed during "generate", but if the
        // user specified the bbox by hand, we have to resort to back-project the lat/lon one
        try {
            return new Envelope(Double.parseDouble(typeForm.getNativeMinX()),
                Double.parseDouble(typeForm.getNativeMaxX()), Double.parseDouble(typeForm.getNativeMinY()),
                Double.parseDouble(typeForm.getNativeMaxY()));
        } catch(NumberFormatException e) {
            return null;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param typeForm
     *
     * @return Set of keywords
     */
    private Set keyWords(TypesEditorForm typeForm) {
        HashSet keywords = new HashSet();
        String[] array = (typeForm.getKeywords() != null)
            ? typeForm.getKeywords().split(",") : new String[0];

        for (int i = 0; i < array.length; i++) {
            keywords.add(array[i].trim());
        }

        return keywords;
    }

    private Set metadataLinks(TypesEditorForm typeForm) {
        HashSet links = new HashSet();

        MetaDataLink link = getLink(typeForm, 0);

        if (link != null) {
            links.add(link);
        }

        link = getLink(typeForm, 1);

        if (link != null) {
            links.add(link);
        }

        return links;
    }

    private MetaDataLink getLink(TypesEditorForm typeForm, int index) {
        MetaDataLink link = typeForm.getMetadataLink(index);

        if ((link.getContent() == null) || link.getContent().trim().equals("")) {
            return null;
        }

        return link;
    }

    DataStore aquireDataStore(String dataStoreID) throws IOException {
        DataConfig dataConfig = getDataConfig();
        DataStoreConfig dataStoreConfig = dataConfig.getDataStore(dataStoreID);

        Map params = dataStoreConfig.getConnectionParams();

        return DataStoreUtils.getDataStore(params);
    }

    SimpleFeatureType getSchema(String dataStoreID, String typeName)
        throws IOException {
        DataStore dataStore = null;
        try {
            dataStore = aquireDataStore(dataStoreID);
            return dataStore.getSchema(typeName);
        } finally {
            if(dataStore != null) dataStore.dispose();
        }
    }
}
