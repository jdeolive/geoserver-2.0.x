/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.vfny.geoserver.wms;

import java.awt.Color;

import org.geotools.image.palette.InverseColorMapOp;
import org.geotools.map.GraphicEnhancedMapContext;
import org.geotools.map.MapLayer;
import org.vfny.geoserver.wms.requests.GetMapRequest;

/**
 * Extends DefaultMapContext to provide the whole set of request parameters a
 * WMS GetMap request can have.
 * 
 * <p>
 * In particular, adds holding for the following parameter values:
 * 
 * <ul>
 * <li> WIDTH </li>
 * <li> HEIGHT </li>
 * <li> BGCOLOR </li>
 * <li> TRANSPARENT </li>
 * </ul>
 * </p>
 * 
 * @author Gabriel Roldan, Axios Engineering
 * @author Simone Giannecchini - GeoSolutions SAS
 * @version $Id$
 */
public class WMSMapContext extends GraphicEnhancedMapContext {
	/** requested map image width in output units (pixels) */
	private int mapWidth;

	/** requested map image height in output units (pixels) */
	private int mapHeight;

	/** Requested BGCOLOR, defaults to white according to WMS spec */
	private Color bgColor = Color.white;

	/** true if background transparency is requested */
	private boolean transparent;

	/**
	 * the rendering buffer used to avoid issues with tiled rendering and big
	 * strokes that may cross tile boundaries
	 */
	private int buffer;

	/**
	 * The {@link InverseColorMapOp} that actually does the color inversion.
	 */
	private InverseColorMapOp paletteInverter;

	private GetMapRequest request; // hold onto it so we can grab info from it

	// (request URL etc...)


	public WMSMapContext() {
		super();
	}

	public WMSMapContext(GetMapRequest req) {
		super();
		request = req;
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @param layers
	 */
	public WMSMapContext(MapLayer[] layers) {
		super(layers);
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 */
	public Color getBgColor() {
		return this.bgColor;
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @param bgColor
	 *            DOCUMENT ME!
	 */
	public void setBgColor(Color bgColor) {
		this.bgColor = bgColor;
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 */
	public int getMapHeight() {
		return this.mapHeight;
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @param mapHeight
	 *            DOCUMENT ME!
	 */
	public void setMapHeight(int mapHeight) {
		this.mapHeight = mapHeight;
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 */
	public int getMapWidth() {
		return this.mapWidth;
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @param mapWidth
	 *            DOCUMENT ME!
	 */
	public void setMapWidth(int mapWidth) {
		this.mapWidth = mapWidth;
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 */
	public boolean isTransparent() {
		return this.transparent;
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @param transparent
	 *            DOCUMENT ME!
	 */
	public void setTransparent(boolean transparent) {
		this.transparent = transparent;
	}

	public GetMapRequest getRequest() {
		return request;
	}

	public void setRequest(GetMapRequest request) {
		this.request = request;
	}

	public int getBuffer() {
		return buffer;
	}

	public void setBuffer(int buffer) {
		this.buffer = buffer;
	}

	public InverseColorMapOp getPaletteInverter() {
		return paletteInverter;
	}

	public void setPaletteInverter(InverseColorMapOp paletteInverter) {
		this.paletteInverter = paletteInverter;
	}
}
