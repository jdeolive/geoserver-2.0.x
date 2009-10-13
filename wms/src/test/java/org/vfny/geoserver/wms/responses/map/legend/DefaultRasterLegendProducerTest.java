/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.vfny.geoserver.wms.responses.map.legend;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.media.jai.widget.ScrollingImagePanel;
import javax.swing.JFrame;
import javax.xml.namespace.QName;

import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.feature.FeatureCollection;
import org.geotools.resources.coverage.FeatureUtilities;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.vfny.geoserver.wms.requests.GetLegendGraphicRequest;
import org.vfny.geoserver.wms.responses.DefaultRasterLegendProducer;
import org.vfny.geoserver.wms.responses.LegendUtils;
import org.vfny.geoserver.wms.responses.featureinfo.GetFeatureInfoTest;
import org.vfny.geoserver.wms.servlets.GetLegendGraphic;


/**
 * Tets the functioning of the abstract legend producer for raster formats,
 * which relies on Geotools' StyledShapePainter.
 *
 * @author Gabriel Roldan, Axios Engineering
 * @version $Id$
 */
public class DefaultRasterLegendProducerTest extends WMSTestSupport {
    /** DOCUMENT ME! */
    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(DefaultRasterLegendProducerTest.class.getPackage()
                                                                                               .getName());

    /** DOCUMENT ME! */
    private DefaultRasterLegendProducer legendProducer;
    GetLegendGraphic service;

    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);
        
        dataDirectory.addCoverage(new QName("http://www.geo-solutions.it", "world", "gs"), MockData.class.getResource("world.tiff"),"tiff", "raster");
        dataDirectory.addStyle("rainfall", MockData.class.getResource("rainfall.sld"));
        dataDirectory.addStyle("rainfall_ramp", MockData.class.getResource("rainfall_ramp.sld"));
        dataDirectory.addStyle("rainfall_classes", MockData.class.getResource("rainfall_classes.sld"));
    }
    /**
     * DOCUMENT ME!
     *
     * @throws Exception DOCUMENT ME!
     * @throws UnsupportedOperationException DOCUMENT ME!
     */
    public void setUpInternal() throws Exception {
        super.setUpInternal();
        this.legendProducer = new DefaultRasterLegendProducer() {
                    public void writeTo(OutputStream out)
                        throws ServiceException, IOException {
                        throw new UnsupportedOperationException();
                    }

                    public String getContentType() throws java.lang.IllegalStateException {
                        throw new UnsupportedOperationException();
                    }
                };

        service = new GetLegendGraphic(getWMS());
    }

    /**
     * DOCUMENT ME!
     *
     * @throws Exception DOCUMENT ME!
     */
    public void tearDownInternal() throws Exception {
        this.legendProducer = null;
        super.tearDownInternal();
    }

    /**
     * Tests that a legend is produced for the explicitly specified rule, when
     * the FeatureTypeStyle has more than one rule, and one of them is
     * requested by the RULE parameter.
     *
     * @throws Exception DOCUMENT ME!
     */
    public void testUserSpecifiedRule() throws Exception {
        //load a style with 3 rules
        Style multipleRulesStyle = getCatalog().getStyleByName(MockData.ROAD_SEGMENTS.getLocalPart()).getStyle();
        assertNotNull(multipleRulesStyle);
        
        Rule rule = multipleRulesStyle.getFeatureTypeStyles()[0].getRules()[0];
        LOGGER.info("testing single rule " + rule.getName() + " from style "
            + multipleRulesStyle.getName());

        GetLegendGraphicRequest req = new GetLegendGraphicRequest(getWMS());
        FeatureTypeInfo ftInfo = getCatalog().getFeatureTypeByName(MockData.ROAD_SEGMENTS.getNamespaceURI(), MockData.ROAD_SEGMENTS.getLocalPart());
        req.setLayer(ftInfo.getFeatureType());
        req.setStyle(multipleRulesStyle);
        req.setRule(rule);
        req.setLegendOptions(new HashMap());

        final int HEIGHT_HINT = 30;
        req.setHeight(HEIGHT_HINT);

        //use default values for the rest of parameters
        this.legendProducer.produceLegendGraphic(req);

        BufferedImage legend = this.legendProducer.getLegendGraphic();

        //was the legend painted?
        assertNotBlank("testUserSpecifiedRule", legend, LegendUtils.DEFAULT_BG_COLOR);

        //was created only one rule?
        String errMsg = "expected just one legend of height " + HEIGHT_HINT + ", for the rule "
            + rule.getName();
        int resultLegendCount = legend.getHeight() / HEIGHT_HINT;
        assertEquals(errMsg, 1, resultLegendCount);
    }

	/**
	 * Tests that a legend is produced for the explicitly specified rule, when
	 * the FeatureTypeStyle has more than one rule, and one of them is
	 * requested by the RULE parameter.
	 *
	 * @throws Exception DOCUMENT ME!
	 */
	public void testRainfall() throws Exception {
	    //load a style with 3 rules
	    Style multipleRulesStyle = getCatalog().getStyleByName("rainfall").getStyle();
	    
	    assertNotNull(multipleRulesStyle);
	    
	
	    GetLegendGraphicRequest req = new GetLegendGraphicRequest(getWMS());
	    CoverageInfo cInfo = getCatalog().getCoverageByName("world");
	    assertNotNull(cInfo);
	    
        GridCoverage coverage = cInfo.getGridCoverage(null, null);
        FeatureCollection<SimpleFeatureType, SimpleFeature> feature;
        feature = FeatureUtilities.wrapGridCoverage((GridCoverage2D) coverage);
        req.setLayer(feature.getSchema());
	    req.setStyle(multipleRulesStyle);
	    req.setLegendOptions(new HashMap());
	
	    final int HEIGHT_HINT = 30;
	    req.setHeight(HEIGHT_HINT);
	
	    //use default values for the rest of parameters
	    this.legendProducer.produceLegendGraphic(req);
	
	    BufferedImage legend = this.legendProducer.getLegendGraphic();
	
	    //was the legend painted?
	    assertNotBlank("testRainfall", legend, LegendUtils.DEFAULT_BG_COLOR);
	    

	    //was the legend painted?
	    assertNotBlank("testRainfall", legend, LegendUtils.DEFAULT_BG_COLOR);
	    
	}

}
