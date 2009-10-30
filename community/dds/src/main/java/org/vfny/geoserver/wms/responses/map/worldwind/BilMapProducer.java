/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.vfny.geoserver.wms.responses.map.worldwind;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.Interpolation;
import javax.media.jai.RenderedOp;
import javax.media.jai.TiledImage;
import javax.media.jai.operator.FormatDescriptor;

import org.geoserver.data.util.CoverageUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GeneralGridEnvelope;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.vfny.geoserver.util.WCSUtils;
import org.vfny.geoserver.wcs.WcsException;
import org.vfny.geoserver.wms.GetMapProducer;
import org.vfny.geoserver.wms.WmsException;
import org.vfny.geoserver.wms.requests.GetMapRequest;
import org.vfny.geoserver.wms.responses.AbstractGetMapProducer;


import com.sun.media.imageioimpl.plugins.raw.RawImageWriterSpi;
import com.sun.media.imageioimpl.plugins.tiff.TIFFImageWriterSpi;

/**
 * Map producer for producing Raw bil images out of an elevation model.
 * 
 * @author Tishampati Dhar
 * @since 2.0.x
 * 
 */
public final class BilMapProducer extends AbstractGetMapProducer implements
GetMapProducer {
	/** A logger for this class. */
	private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.vfny.geoserver.responses.wms.map.bil");
	
	/** Raw Image Writer **/
	private final static ImageWriterSpi writerSPI = new RawImageWriterSpi();
	private final static ImageWriterSpi twriterSPI = new TIFFImageWriterSpi();

	/** the only MIME type this map producer supports */
    static final String MIME_TYPE = "image/bil";

    private static final String[] OUTPUT_FORMATS = {MIME_TYPE,"application/bil",
    	"application/bil8","application/bil16", "application/bil32" };
    
    private WMS wmsConfig;

    /** GridCoverageFactory. */
    private final static GridCoverageFactory factory = CoverageFactoryFinder.getGridCoverageFactory(null);
    
    
    /**
     * Constructor for a {@link BilMapProducer}.
     *
     * @param wms
     *            that is asking us to encode the image.
     */
    public BilMapProducer(WMS wms) {
        super(MIME_TYPE, OUTPUT_FORMATS);
        this.wmsConfig = wms;
    }

	public void produceMap() throws WmsException {
		// TODO Auto-generated method stub
		if (mapContext == null) {
			throw new WmsException("The map context is not set");
		}
	}

	public void writeTo(OutputStream out) throws ServiceException, IOException {
		// TODO Get request tile size
		GetMapRequest request = mapContext.getRequest();
		
		String bilEncoding = (String) request.getFormat();
		
		int height = request.getHeight();
		int width = request.getWidth();
		
		if ((height>512)||(width>512)){
			throw new ServiceException("Cannot get WMS bil" +
					" tiles bigger than 512x512, try WCS");
		}
		
		MapLayerInfo[] reqlayers = request.getLayers();
		
		//Can't fetch bil for more than 1 layer
		if (reqlayers.length > 1) 
		{
			throw new ServiceException("Cannot combine layers into BIL output");
		}
		MapLayerInfo mapLayerInfo = reqlayers[0];
		
		/*
		final ParameterValueGroup writerParams = format.getWriteParameters();
        writerParams.parameter(AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString())
                    .setValue(wp);
		*/
		GridCoverageReader coverageReader = mapLayerInfo.getCoverageReader();
		
		/*
		 * Try to use a gridcoverage style render
		 */
		GridCoverage2D subCov = null;
		try {
			subCov = BilMapProducer.getFinalCoverage(request,
					mapLayerInfo, (AbstractGridCoverage2DReader)coverageReader);
		} catch (IndexOutOfBoundsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FactoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(subCov!=null)
		{
			/*
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
			writer.write(subCov.toString());
			writer.flush();
			writer.close();
			*/
	        RenderedImage image = subCov.getRenderedImage();
	        if(image!=null)
	        {
	        	int dtype = image.getData().getDataBuffer().getDataType();
	        	/* Throw exception if required to perform conversion */
	        	/*
	        	if((bilEncoding.equals("application/bil32"))&&(dtype!=DataBuffer.TYPE_FLOAT))	        	{
	        		throw new ServiceException("Cannot fetch BIL float data,"+
	        				"Wrong underlying data type");
	        	}
	        	if((bilEncoding.equals("application/bil16"))&&(dtype!=DataBuffer.TYPE_SHORT))	        	{
	        		throw new ServiceException("Cannot fetch BIL int data,"+
	        				"Wrong underlying data type");
	        	}
	        	if((bilEncoding.equals("application/bil8"))&&(dtype!=DataBuffer.TYPE_BYTE))	        	{
	        		throw new ServiceException("Cannot fetch BIL byte data,"+
	        				"Wrong underlying data type");
	        	}
	        	*/
	        	
	        	/*
	        	 * Perform format conversion
	        	 * Operator is not created if no conversion is necessary
	        	 */
	        	RenderedOp formcov = null;
	        	if((bilEncoding.equals("application/bil32"))&&(dtype!=DataBuffer.TYPE_FLOAT))	        	{
	        		formcov = FormatDescriptor.create(image,DataBuffer.TYPE_FLOAT ,null);
	        	}
	        	if((bilEncoding.equals("application/bil16"))&&(dtype!=DataBuffer.TYPE_SHORT))	        	{
	        		formcov = FormatDescriptor.create(image,DataBuffer.TYPE_SHORT ,null);
	        	}
	        	if((bilEncoding.equals("application/bil8"))&&(dtype!=DataBuffer.TYPE_BYTE))	        	{
	        		formcov = FormatDescriptor.create(image,DataBuffer.TYPE_BYTE ,null);
	        	}
	        	TiledImage tiled = null;
	        	if (formcov!= null)
	        		tiled = new TiledImage(formcov,width,height);
	        	else
	        		tiled = new TiledImage(image,width,height);
	        	final ImageOutputStream imageOutStream = ImageIO.createImageOutputStream(out);
		        final ImageWriter writer = writerSPI.createWriterInstance();
		        writer.setOutput(imageOutStream);
		        writer.write(tiled);
		        imageOutStream.flush();
		        imageOutStream.close();
	        }
	        else
	        {
	        	throw new ServiceException("Cannot render to BIL");
	        }
		}
		else
		{			
			throw new ServiceException("You requested a bil of size:"+
					height+"x"+width+",but you can't have it!!");

		}
	}

	/**
	 * GetCroppedCoverage
	 *
	 * @param request CoverageRequest
	 * @param meta CoverageInfo
	 * @param parameters
	 * @param coverage GridCoverage
	 * @return GridCoverage2D
	 * @throws WcsException
	 * @throws IOException
	 * @throws IndexOutOfBoundsException
	 * @throws FactoryException
	 * @throws TransformException
	 */
	private static GridCoverage2D getFinalCoverage(GetMapRequest request, MapLayerInfo meta,
	    AbstractGridCoverage2DReader coverageReader /*GridCoverage coverage*/)
	    throws WmsException, IOException, IndexOutOfBoundsException, FactoryException,
	        TransformException {
	    // This is the final Response CRS
	    final String responseCRS = request.getSRS();
	
	    // - first check if the responseCRS is present on the Coverage
	    // ResponseCRSs list
	    /*
	    if (!meta.getSRS().contains(responseCRS)) {
	        throw new WmsException("This Layer does not support the requested Response-CRS.");
	    }
		*/
	    // - then create the Coordinate Reference System
	    final CoordinateReferenceSystem targetCRS = CRS.decode(responseCRS);
	
	    // This is the CRS of the requested Envelope
	    final String requestCRS = request.getSRS();
	
	    // - first check if the requestCRS is present on the Coverage
	    // RequestCRSs list
	    /*
	    if (!meta.getSRS().contains(requestCRS)) {
	        throw new WmsException("This Layer does not support the requested CRS.");
	    }
		*/
	    // - then create the Coordinate Reference System
	    final CoordinateReferenceSystem sourceCRS = CRS.decode(requestCRS);
	
	    // This is the CRS of the Coverage Envelope
	    final CoordinateReferenceSystem cvCRS = ((GeneralEnvelope) coverageReader
	        .getOriginalEnvelope()).getCoordinateReferenceSystem();
	    final MathTransform GCCRSTodeviceCRSTransformdeviceCRSToGCCRSTransform = CRS
	        .findMathTransform(cvCRS, sourceCRS, true);
	    final MathTransform GCCRSTodeviceCRSTransform = CRS.findMathTransform(cvCRS, targetCRS, true);
	    final MathTransform deviceCRSToGCCRSTransform = GCCRSTodeviceCRSTransformdeviceCRSToGCCRSTransform
	        .inverse();
	
	    com.vividsolutions.jts.geom.Envelope envelope = request.getBbox();
	    GeneralEnvelope destinationEnvelope;
	    final boolean lonFirst = sourceCRS.getCoordinateSystem().getAxis(0).getDirection().absolute()
	                                      .equals(AxisDirection.EAST);
	
	    // the envelope we are provided with is lon,lat always
	    if (!lonFirst) {
	        destinationEnvelope = new GeneralEnvelope(new double[] {
	                    envelope.getMinY(), envelope.getMinX()
	                }, new double[] { envelope.getMaxY(), envelope.getMaxX() });
	    } else {
	        destinationEnvelope = new GeneralEnvelope(new double[] {
	                    envelope.getMinX(), envelope.getMinY()
	                }, new double[] { envelope.getMaxX(), envelope.getMaxY() });
	    }
	
	    destinationEnvelope.setCoordinateReferenceSystem(sourceCRS);
	
	    // this is the destination envelope in the coverage crs
	    final GeneralEnvelope destinationEnvelopeInSourceCRS = (!deviceCRSToGCCRSTransform
	        .isIdentity()) ? CRS.transform(deviceCRSToGCCRSTransform, destinationEnvelope)
	                       : new GeneralEnvelope(destinationEnvelope);
	    destinationEnvelopeInSourceCRS.setCoordinateReferenceSystem(cvCRS);
	
	    /**
	     * Reading Coverage on Requested Envelope
	    */
	    Rectangle destinationSize = null;
	    /*
	    if ((request.getGridLow() != null) && (request.getGridHigh() != null)) {
	        final int[] lowers = new int[] {
	                request.getGridLow()[0].intValue(), request.getGridLow()[1].intValue()
	            };
	        final int[] highers = new int[] {
	                request.getGridHigh()[0].intValue(), request.getGridHigh()[1].intValue()
	            };
	
	        destinationSize = new Rectangle(lowers[0], lowers[1], highers[0], highers[1]);
	    } else {
	        //destinationSize = coverageReader.getOriginalGridRange().toRectangle();
	        throw new WmsException("Neither Grid Size nor Grid Resolution have been specified.");
	    }
		*/
	    destinationSize = new Rectangle(0,0,request.getHeight(),request.getWidth());
	    /**
	     * Checking for supported Interpolation Methods
	     */
	    
	    /*
	    Interpolation interpolation = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
	    final String interpolationType = request.getInterpolation();
	
	    if (interpolationType != null) {
	        boolean interpolationSupported = false;
	        Iterator internal = meta.getInterpolationMethods().iterator();
	
	        while (internal.hasNext()) {
	            if (interpolationType.equalsIgnoreCase((String) internal.next())) {
	                interpolationSupported = true;
	            }
	        }
	
	        if (!interpolationSupported) {
	            throw new WcsException(
	                "The requested Interpolation method is not supported by this Coverage.");
	        } else {
	            if (interpolationType.equalsIgnoreCase("bilinear")) {
	                interpolation = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
	            } else if (interpolationType.equalsIgnoreCase("bicubic")) {
	                interpolation = Interpolation.getInstance(Interpolation.INTERP_BICUBIC);
	            }
	        }
	    }
		*/
	    Interpolation interpolation = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
	    
	    Map<Object,Object> parameters = new HashMap<Object,Object>();
	    // /////////////////////////////////////////////////////////
	    //
	    // Reading the coverage
	    //
	    // /////////////////////////////////////////////////////////
	    parameters.put(AbstractGridFormat.READ_GRIDGEOMETRY2D.getName().toString(),
	        new GridGeometry2D(new GeneralGridEnvelope(destinationSize), destinationEnvelopeInSourceCRS));
	
	    final GridCoverage coverage = coverageReader.read(CoverageUtils.getParameters(
	                coverageReader.getFormat().getReadParameters(), parameters, true));
	
	    if ((coverage == null) || !(coverage instanceof GridCoverage2D)) {
	        throw new IOException("The requested coverage could not be found.");
	    }
	
	    /**
	     * Band Select
	     */
	    /*
	    Coverage bandSelectedCoverage = null;
	
	    bandSelectedCoverage = WCSUtils.bandSelect(request.getParameters(), coverage);
		*/
	    /**
	     * Crop
	     */
	    final GridCoverage2D croppedGridCoverage = WCSUtils.crop(coverage,
	            (GeneralEnvelope) coverage.getEnvelope(), cvCRS, destinationEnvelopeInSourceCRS,
	            Boolean.TRUE);
	
	    /**
	     * Scale/Resampling (if necessary)
	     */
	    GridCoverage2D subCoverage = croppedGridCoverage;
	    final GeneralGridEnvelope newGridrange = new GeneralGridEnvelope(destinationSize);
	
	    /*if (!newGridrange.equals(croppedGridCoverage.getGridGeometry()
	                    .getGridRange())) {*/
	    subCoverage = WCSUtils.scale(croppedGridCoverage, newGridrange, croppedGridCoverage, cvCRS,
	            destinationEnvelopeInSourceCRS);
	    //}
	
	    /**
	     * Reproject
	     */
	    subCoverage = WCSUtils.reproject(subCoverage, sourceCRS, targetCRS, interpolation);
	    
	    return subCoverage;
	}
}
