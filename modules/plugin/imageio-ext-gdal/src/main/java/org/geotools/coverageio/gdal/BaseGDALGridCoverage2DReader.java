/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2007-2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.coverageio.gdal;

import it.geosolutions.imageio.gdalframework.GDALCommonIIOImageMetadata;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;

import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverageio.BaseGridCoverage2DReader;
import org.geotools.data.DataSourceException;
import org.geotools.factory.Hints;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.metadata.iso.spatial.PixelTranslation;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.transform.ProjectiveTransform;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.TransformException;

/**
 * Base class for GridCoverage data access, leveraging on GDAL Java bindings
 * provided by the ImageIO-Ext project. See <a
 * href="http://imageio-ext.dev.java.net">ImageIO-Ext project</a>.
 * 
 * @author Daniele Romagnoli, GeoSolutions
 * @author Simone Giannecchini, GeoSolutions
 * 
 */
@SuppressWarnings("deprecation")
public abstract class BaseGDALGridCoverage2DReader extends
        BaseGridCoverage2DReader implements GridCoverageReader {

    /** Logger. */
    private final static Logger LOGGER = org.geotools.util.logging.Logging
            .getLogger("org.geotools.coverageio.gdal");

    /**
     * Creates a new instance of a {@link BaseGDALGridCoverage2DReader}. I
     * assume nothing about file extension.
     * 
     * @param input
     *                Source object for which we want to build a
     *                {@link BaseGDALGridCoverage2DReader}.
     * @param hints
     *                Hints to be used by this reader throughout his life.
     * @param worldFileExtension
     *                the specific world file extension of the underlying format
     * @param formatSpecificSpi
     *                an instance of a proper {@code ImageReaderSpi}.
     * @throws DataSourceException
     */
    protected BaseGDALGridCoverage2DReader(Object input, final Hints hints,
            final String worldFileExtension,
            final ImageReaderSpi formatSpecificSpi) throws DataSourceException {
        super(input, hints, worldFileExtension, formatSpecificSpi);
    }

    /**
     * Setting Envelope, GridRange and CRS from the given {@code ImageReader}
     * 
     * @param reader
     *                the {@code ImageReader} from which to retrieve metadata
     *                (if available) for setting properties
     * @throws IOException
     */
    protected void setCoverageProperties(ImageReader reader) throws IOException {
        // //
        //
        // Getting common metadata from GDAL
        //
        // //
        final IIOMetadata metadata = reader.getImageMetadata(0);
        if (!(metadata instanceof GDALCommonIIOImageMetadata)) {
            throw new DataSourceException(
                    "Unexpected error! Metadata should be an instance of the expected class:"
                            + " GDALCommonIIOImageMetadata.");
        }

        getPropertiesFromCommonMetadata((GDALCommonIIOImageMetadata) metadata);

        // //
        //
        // If common metadata doesn't have sufficient information to set CRS
        // envelope, try other ways, such as looking for a PRJ
        //
        // //
        if (getCoverageCRS() == null) {
            parsePRJFile();
        }

        if (getCoverageCRS() == null) {
            LOGGER.info("crs not found, proceeding with EPSG:4326");
            setCoverageCRS(AbstractGridFormat.getDefaultCRS());
        }

        // //
        //
        // If no sufficient information have been found to set the
        // envelope, try other ways, such as looking for a WorldFile
        //
        // //
        if (getCoverageEnvelope() == null) {
            parseWorldFile();
            if (getCoverageEnvelope() == null) {
                throw new DataSourceException(
                        "Unavailable envelope for this coverage");
            }
        }

        // setting the coordinate reference system for the envelope
        getCoverageEnvelope().setCoordinateReferenceSystem(getCoverageCRS());

        // Additional settings due to "final" methods getOriginalXXX
        originalEnvelope = getCoverageEnvelope();
        originalGridRange = getCoverageGridRange();
        crs = getCoverageCRS();
    }

    /**
     * Given a {@link GDALCommonIIOImageMetadata} metadata object, retrieves
     * several properties to properly set envelope, gridrange and crs.
     * 
     * @param metadata
     *                a {@link GDALCommonIIOImageMetadata} metadata instance
     *                from where to search needed properties.
     */
    private void getPropertiesFromCommonMetadata(
            GDALCommonIIOImageMetadata metadata) {

        // ////////////////////////////////////////////////////////////////////
        //
        // setting CRS and Envelope directly from GDAL, if available
        //
        // ////////////////////////////////////////////////////////////////////
        // //
        // 
        // 1) CRS
        //
        // //
    	try {
			setCoverageCRS(CRS.decode("EPSG:3079"));
		} catch (NoSuchAuthorityCodeException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (FactoryException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
//        if (getCoverageCRS() == null) {
//            final String wkt = metadata.getProjection();
//
//            if ((wkt != null) && !(wkt.equalsIgnoreCase(""))) {
//                try {
//                    setCoverageCRS(CRS.parseWKT(wkt));
//                    final Integer epsgCode = CRS.lookupEpsgCode(getCoverageCRS(), true);
//
//                    // Force the creation of the CRS directly from the
//                    // retrieved
//                    // EPSG code in order to prevent weird transformation
//                    // between
//                    // "same" CRSs having slight differences.
//                    // TODO: cache epsgCode-CRSs
//                    if (epsgCode != null) {
//                        setCoverageCRS(CRS.decode("EPSG:" + epsgCode));
//                    }
//                } catch (FactoryException fe) {
//                    // unable to get CRS from WKT
//                    if (LOGGER.isLoggable(Level.FINE)) {
//                        // LOGGER.log(Level.WARNING,
//                        // fe.getLocalizedMessage(), fe);
//                        LOGGER.log(Level.FINE,
//                                "Unable to get CRS from WKT contained in metadata."
//                                        + " Looking for a PRJ.");
//                    }
//
//                    setCoverageCRS(null);
//                }
//            }
//        }
        // //
        //
        // 2) Grid
        //
        // //
        if (getCoverageGridRange() == null)
            setCoverageGridRange(new GridEnvelope2D(new Rectangle(0, 0,
                    metadata.getWidth(), metadata.getHeight())));

        // //
        // 
        // 3) Envelope
        //
        // //

        final double[] geoTransform = metadata.getGeoTransformation();
        if ((geoTransform != null) && (geoTransform.length == 6)) {
            final AffineTransform tempTransform = new AffineTransform(
                    geoTransform[1], geoTransform[4], geoTransform[2],
                    geoTransform[5], geoTransform[0], geoTransform[3]);
            // ATTENTION: Gdal geotransform does not use the pixel is
            // centre convention like world files.
            if (getCoverageEnvelope() == null) {
                try {
                    // Envelope setting
                    setCoverageEnvelope(CRS.transform(ProjectiveTransform
                            .create(tempTransform), new GeneralEnvelope(
                            getCoverageGridRange())));
                } catch (IllegalStateException e) {
                    if (LOGGER.isLoggable(Level.WARNING)) {
                        LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
                    }
                } catch (TransformException e) {
                    if (LOGGER.isLoggable(Level.WARNING)) {
                        LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
                    }
                }
            }
            // Grid2World Transformation
            final double tr = -PixelTranslation
                    .getPixelTranslation(PixelInCell.CELL_CORNER);
            tempTransform.translate(tr, tr);
            this.raster2Model = ProjectiveTransform.create(tempTransform);
        }
    }
}
