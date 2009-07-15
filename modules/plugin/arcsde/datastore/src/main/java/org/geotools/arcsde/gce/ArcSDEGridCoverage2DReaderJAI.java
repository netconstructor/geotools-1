/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2009, Open Source Geospatial Foundation (OSGeo)
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
 *
 */
package org.geotools.arcsde.gce;

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.media.jai.InterpolationNearest;
import javax.media.jai.JAI;
import javax.media.jai.operator.FormatDescriptor;
import javax.media.jai.operator.MosaicDescriptor;

import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.TypeMap;
import org.geotools.coverage.grid.GeneralGridEnvelope;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.data.DefaultServiceInfo;
import org.geotools.data.ServiceInfo;
import org.geotools.factory.Hints;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.ColorInterpretation;
import org.opengis.coverage.grid.Format;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.parameter.GeneralParameterValue;

/**
 * 
 * @author Gabriel Roldan (OpenGeo)
 * @since 2.5.4
 * @version $Id$
 * @source $URL$
 */
@SuppressWarnings( { "deprecation", "nls" })
final class ArcSDEGridCoverage2DReaderJAI extends AbstractGridCoverage2DReader {

    private static final Logger LOGGER = Logging.getLogger("org.geotools.arcsde.gce");

    /**
     * @see LoggingHelper#log(RenderedImage, Long, String)
     */
    private static final boolean DEBUG_TO_DISK = Boolean
            .getBoolean("org.geotools.arcsde.gce.debug");

    private final LoggingHelper log = new LoggingHelper();

    private final ArcSDERasterFormat parent;

    private final RasterDatasetInfo rasterInfo;

    private DefaultServiceInfo serviceInfo;

    private RasterReaderFactory rasterReaderFactory;

    public ArcSDEGridCoverage2DReaderJAI(final ArcSDERasterFormat parent,
            final RasterReaderFactory rasterReaderFactory, final RasterDatasetInfo rasterInfo,
            final Hints hints) throws IOException {
        // check it's a supported format
        {
            final int bitsPerSample = rasterInfo.getBand(0, 0).getCellType().getBitsPerSample();
            if (rasterInfo.getNumBands() > 1 && (bitsPerSample == 1 || bitsPerSample == 4)) {
                throw new IllegalArgumentException(bitsPerSample
                        + "-bit rasters with more than one band are not supported");
            }
        }
        this.parent = parent;
        this.rasterReaderFactory = rasterReaderFactory;
        this.rasterInfo = rasterInfo;

        super.hints = hints;
        super.coverageFactory = CoverageFactoryFinder.getGridCoverageFactory(this.hints);
        super.crs = rasterInfo.getCoverageCrs();
        super.originalEnvelope = rasterInfo.getOriginalEnvelope();

        GeneralGridEnvelope gridRange = rasterInfo.getOriginalGridRange();
        super.originalGridRange = new GridEnvelope2D(gridRange.toRectangle());

        super.coverageName = rasterInfo.getRasterTable();
        final int numLevels = rasterInfo.getNumPyramidLevels(0);

        // level 0 is not an overview, but the raster itself
        super.numOverviews = numLevels - 1;

        // ///
        // 
        // setting the higher resolution avalaible for this coverage
        //
        // ///
        highestRes = super.getResolution(originalEnvelope, (Rectangle) originalGridRange, crs);
        // //
        //
        // get information for the successive images
        //
        // //
        // REVISIT may the different rasters in the raster dataset have different pyramid levels? I
        // guess so
        if (numOverviews > 0) {
            overViewResolutions = new double[numOverviews][2];
            for (int pyramidLevel = 1; pyramidLevel <= numOverviews; pyramidLevel++) {
                Rectangle levelGridRange = rasterInfo.getGridRange(0, pyramidLevel);
                GeneralEnvelope levelEnvelope = rasterInfo.getGridEnvelope(0, pyramidLevel);
                overViewResolutions[pyramidLevel - 1] = super.getResolution(levelEnvelope,
                        levelGridRange, crs);
            }
        } else {
            overViewResolutions = null;
        }
    }

    /**
     * @see GridCoverageReader#getFormat()
     */
    public Format getFormat() {
        return parent;
    }

    @Override
    public ServiceInfo getInfo() {
        if (serviceInfo == null) {
            serviceInfo = new DefaultServiceInfo();
            serviceInfo.setTitle(rasterInfo.getRasterTable() + " is an ArcSDE Raster");
            serviceInfo.setDescription(rasterInfo.toString());
            Set<String> keywords = new HashSet<String>();
            keywords.add("ArcSDE");
            serviceInfo.setKeywords(keywords);
        }
        return serviceInfo;
    }

    /**
     * @see GridCoverageReader#read(GeneralParameterValue[])
     */
    public GridCoverage read(GeneralParameterValue[] params) throws IOException {

        final GeneralEnvelope requestedEnvelope;
        final Rectangle requestedDim;
        final OverviewPolicy overviewPolicy;
        {
            final ReadParameters opParams = RasterUtils.parseReadParams(getOriginalEnvelope(),
                    params);
            overviewPolicy = opParams.overviewPolicy;
            requestedEnvelope = opParams.requestedEnvelope;
            requestedDim = opParams.dim;
        }

        /*
         * For each raster in the raster dataset, obtain the tiles, pixel range, and resulting
         * envelope
         */
        final List<RasterQueryInfo> queries;
        queries = findMatchingRasters(requestedEnvelope, requestedDim, overviewPolicy);
        if (queries.isEmpty()) {
            /*
             * none of the rasters match the requested envelope. This may happen by the tiled nature
             * of the raster dataset
             */
            return createFakeCoverage(requestedEnvelope, requestedDim);
        }

        final GeneralEnvelope resultEnvelope = getResultEnvelope(queries);

        log.appendLoggingGeometries(LoggingHelper.REQ_ENV, requestedEnvelope);
        log.appendLoggingGeometries(LoggingHelper.RES_ENV, resultEnvelope);

        /*
         * Once we collected the matching rasters and their image subsets, find out where in the
         * overall resulting mosaic they fit. If the rasters does not share the spatial resolution,
         * the QueryInfo.resultDimension and QueryInfo.mosaicLocation width or height won't match
         */
        RasterUtils.setMosaicLocations(rasterInfo, resultEnvelope, queries);

        /*
         * Gather the rendered images for each of the rasters that match the requested envelope
         */
        final Map<Long, RasterQueryInfo> byRasterIdQueries = new HashMap<Long, RasterQueryInfo>();
        for (RasterQueryInfo queryInfo : queries) {
            byRasterIdQueries.put(queryInfo.getRasterId(), queryInfo);
        }

        final TiledRasterReader rasterReader = rasterReaderFactory.create(rasterInfo);

        try {
            readAllTiledRasters(byRasterIdQueries, rasterReader);
        } finally {
            rasterReader.dispose();
        }

        log.log(LoggingHelper.REQ_ENV);
        log.log(LoggingHelper.RES_ENV);
        log.log(LoggingHelper.MOSAIC_ENV);
        log.log(LoggingHelper.MOSAIC_EXPECTED);

        final RenderedImage coverageRaster = createMosaic(queries);

        /*
         * BUILDING COVERAGE
         */
        GridSampleDimension[] bands = getSampleDimensions(coverageRaster);

        GridCoverage2D resultCoverage = coverageFactory.create(coverageName, coverageRaster,
                resultEnvelope, bands, null, null);

        return resultCoverage;
    }

    private GridSampleDimension[] getSampleDimensions(final RenderedImage coverageRaster)
            throws IOException {

        GridSampleDimension[] bands = rasterInfo.getGridSampleDimensions();

        // may the image have been promoted? build the correct band info then
        final int imageBands = coverageRaster.getSampleModel().getNumBands();
        if (bands.length == 1 && imageBands > 1) {
            LOGGER.fine(coverageName + " was promoted from 1 to "
                    + coverageRaster.getSampleModel().getNumBands()
                    + " bands, returning an appropriate set of GridSampleDimension");
            // stolen from super.createCoverage:
            final ColorModel cm = coverageRaster.getColorModel();
            bands = new GridSampleDimension[imageBands];

            // setting bands names.
            for (int i = 0; i < imageBands; i++) {
                final ColorInterpretation colorInterpretation;
                colorInterpretation = TypeMap.getColorInterpretation(cm, i);
                if (colorInterpretation == null) {
                    throw new IOException("Unrecognized sample dimension type");
                }
                bands[i] = new GridSampleDimension(colorInterpretation.name()).geophysics(true);
            }
        }

        return bands;
    }

    private void readAllTiledRasters(final Map<Long, RasterQueryInfo> byRasterIdQueries,
            final TiledRasterReader rasterReader) throws IOException {

        Long currentRasterId;

        while ((currentRasterId = rasterReader.nextRaster()) != null) {
            if (!byRasterIdQueries.containsKey(currentRasterId)) {
                continue;
            }
            final RasterQueryInfo queryInfo = byRasterIdQueries.get(currentRasterId);
            final RenderedImage rasterImage;

            try {
                final int pyramidLevel = queryInfo.getPyramidLevel();
                final Rectangle matchingTiles = queryInfo.getMatchingTiles();
                // final Point imageLocation = queryInfo.getTiledImageSize().getLocation();
                rasterImage = rasterReader.read(pyramidLevel, matchingTiles);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Fetching data for " + queryInfo.toString(), e);
                throw e;
            }

            queryInfo.setResultImage(rasterImage);

            {
                LOGGER.finer(queryInfo.toString());
                log.appendLoggingGeometries(LoggingHelper.MOSAIC_EXPECTED, queryInfo
                        .getMosaicLocation());
                log
                        .appendLoggingGeometries(LoggingHelper.MOSAIC_ENV, queryInfo
                                .getResultEnvelope());

                final Rectangle tiledImageSize = queryInfo.getTiledImageSize();
                int width = rasterImage.getWidth();
                int height = rasterImage.getHeight();
                if (tiledImageSize.width != width || tiledImageSize.height != height) {
                    throw new IllegalStateException(
                            "Read image is not of the expected size. Image=" + width + "x" + height
                                    + ", expected: " + tiledImageSize.width + "x"
                                    + tiledImageSize.height);
                }
                if (tiledImageSize.x != rasterImage.getMinX()
                        || tiledImageSize.y != rasterImage.getMinY()) {
                    throw new IllegalStateException("Read image is not at the expected location "
                            + tiledImageSize.x + "," + tiledImageSize.y + ": "
                            + rasterImage.getMinX() + "," + rasterImage.getMinY());
                }
            }
        }
    }

    /**
     * Called when the requested envelope do overlap the coverage envelope but none of the rasters
     * in the dataset do
     * 
     * @param requestedEnvelope
     * @param requestedDim
     * @return
     */
    private GridCoverage createFakeCoverage(GeneralEnvelope requestedEnvelope,
            Rectangle requestedDim) {

        ImageTypeSpecifier its = rasterInfo.getRenderedImageSpec(0);
        SampleModel sampleModel = its.getSampleModel(requestedDim.width, requestedDim.height);
        ColorModel colorModel = its.getColorModel();

        WritableRaster raster = Raster.createWritableRaster(sampleModel, null);
        BufferedImage image = new BufferedImage(colorModel, raster, false, null);
        return coverageFactory.create(coverageName, image, requestedEnvelope);
    }

    private List<RasterQueryInfo> findMatchingRasters(final GeneralEnvelope requestedEnvelope,
            final Rectangle requestedDim, final OverviewPolicy overviewPolicy) {

        final List<RasterQueryInfo> matchingQueries;
        matchingQueries = RasterUtils.findMatchingRasters(rasterInfo, requestedEnvelope,
                requestedDim, overviewPolicy);

        if (matchingQueries.isEmpty()) {
            return matchingQueries;
        }

        for (RasterQueryInfo match : matchingQueries) {
            RasterUtils.fitRequestToRaster(requestedEnvelope, rasterInfo, match);
        }
        return matchingQueries;
    }

    private GeneralEnvelope getResultEnvelope(final List<RasterQueryInfo> queryInfos) {

        GeneralEnvelope finalEnvelope = null;

        for (RasterQueryInfo rasterQueryInfo : queryInfos) {
            // gather resulting envelope
            if (finalEnvelope == null) {
                finalEnvelope = new GeneralEnvelope(rasterQueryInfo.getResultEnvelope());
            } else {
                finalEnvelope.add(rasterQueryInfo.getResultEnvelope());
            }
        }
        if (finalEnvelope == null) {
            throw new IllegalStateException("Restult envelope is null, this shouldn't happen!! "
                    + "we checked the request overlaps the coverage envelope before!");
        }
        return finalEnvelope;
    }

    /**
     * For each raster: crop->scale->translate->add to mosaic
     * 
     * @param queries
     * @return
     * @throws IOException
     */
    private RenderedImage createMosaic(final List<RasterQueryInfo> queries) throws IOException {
        List<RenderedImage> transformed = new ArrayList<RenderedImage>(queries.size());

        /*
         * Do we need to expand to RGB color space and then create a new colormapped image with the
         * whole mosaic?
         */
        boolean expandThenContractCM = queries.size() > 1 && rasterInfo.isColorMapped();
        if (expandThenContractCM) {
            LOGGER.info("Creating mosaic out of " + queries.size()
                    + " colormapped rasters. The mosaic tiles will be expanded to "
                    + "\nRGB space and the resulting mosaic reduced to a new IndexColorModel");
        }
        for (RasterQueryInfo query : queries) {
            RenderedImage image = query.getResultImage();
            log.log(image, query.getRasterId(), "01_original");

            image = cropToRequiredDimension(image, query.getResultDimensionInsideTiledImage());
            log.log(image, query.getRasterId(), "02_crop");

            final Rectangle mosaicLocation = query.getMosaicLocation();
            // scale
            Float scaleX = Float.valueOf((float) (mosaicLocation.getWidth() / image.getWidth()));
            Float scaleY = Float.valueOf((float) (mosaicLocation.getHeight() / image.getHeight()));
            Float translateX = null;
            Float translateY = null;

            ParameterBlock pb = new ParameterBlock();
            pb.addSource(image);
            pb.add(scaleX);
            pb.add(scaleY);
            pb.add(translateX);
            pb.add(translateY);
            pb.add(new InterpolationNearest());

            image = JAI.create("scale", pb);
            log.log(image, query.getRasterId(), "03_scale");

            int width = image.getWidth();
            int height = image.getHeight();

            assert mosaicLocation.width == width;
            assert mosaicLocation.height == height;

            // translate
            pb = new ParameterBlock();
            pb.addSource(image);
            pb.add(Float.valueOf(mosaicLocation.x - image.getMinX()));
            pb.add(Float.valueOf(mosaicLocation.y - image.getMinY()));
            pb.add(null);

            image = JAI.create("translate", pb);
            log.log(image, query.getRasterId(), "04_translate");

            assert image.getMinX() == mosaicLocation.x : image.getMinX() + " != "
                    + mosaicLocation.x;
            assert image.getMinY() == mosaicLocation.y : image.getMinY() + " != "
                    + mosaicLocation.y;
            assert image.getWidth() == mosaicLocation.width : image.getWidth() + " != "
                    + mosaicLocation.width;
            assert image.getHeight() == mosaicLocation.height : image.getHeight() + " != "
                    + mosaicLocation.height;

            if (expandThenContractCM) {
                if (LOGGER.isLoggable(Level.FINER)) {
                    LOGGER.finer("Creating color expanded version of tile for raster #"
                            + query.getRasterId());
                }

                /*
                 * reformat the image as a 4 band rgba backed by byte data
                 */
                image = FormatDescriptor.create(image, Integer.valueOf(DataBuffer.TYPE_BYTE), null);

                log.log(image, query.getRasterId(), "04_1_colorExpanded");
            }

            transformed.add(image);
        }

        final RenderedImage result;
        if (queries.size() == 1) {
            result = transformed.get(0);
        } else {
            ParameterBlock mosaicParams = new ParameterBlock();

            for (RenderedImage img : transformed) {
                mosaicParams.addSource(img);
                log.appendLoggingGeometries(LoggingHelper.MOSAIC_RESULT, img);
            }
            log.log(LoggingHelper.MOSAIC_RESULT);

            mosaicParams.add(MosaicDescriptor.MOSAIC_TYPE_OVERLAY); // mosaic type
            mosaicParams.add(null); // alpha mask
            mosaicParams.add(null); // source ROI mask
            mosaicParams.add(null); // source threshold
            mosaicParams.add(null); // destination background value

            LOGGER.fine("Creating mosaic out of " + queries.size() + " raster tiles");
            RenderedImage mosaic = JAI.create("Mosaic", mosaicParams);
            log.log(mosaic, 0L, "05_mosaic_result");

            result = mosaic;
        }
        return result;
    }

    private RenderedImage cropToRequiredDimension(final RenderedImage fullTilesRaster,
            final Rectangle cropTo) {

        int minX = fullTilesRaster.getMinX();
        int minY = fullTilesRaster.getMinY();
        int width = fullTilesRaster.getWidth();
        int height = fullTilesRaster.getHeight();

        Rectangle origDim = new Rectangle(minX, minY, width, height);
        if (!origDim.contains(cropTo)) {
            throw new IllegalArgumentException("Original image (" + origDim
                    + ") does not contain desired dimension (" + cropTo + ")");
        } else if (origDim.equals(cropTo)) {
            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.finer("No need to crop image, full tiled dimension and target one "
                        + "do match: original: " + width + "x" + height + ", target: "
                        + cropTo.width + "x" + cropTo.height);
            }
            return fullTilesRaster;
        }

        ParameterBlock cropParams = new ParameterBlock();

        cropParams.addSource(fullTilesRaster);// Source
        cropParams.add(Float.valueOf(cropTo.x)); // x origin for each band
        cropParams.add(Float.valueOf(cropTo.y)); // y origin for each band
        cropParams.add(Float.valueOf(cropTo.width));// width for each band
        cropParams.add(Float.valueOf(cropTo.height));// height for each band

        final RenderingHints hints = null;
        RenderedImage image = JAI.create("Crop", cropParams, hints);

        assert cropTo.x == image.getMinX();
        assert cropTo.y == image.getMinY();
        assert cropTo.width == image.getWidth();
        assert cropTo.height == image.getHeight();
        return image;
    }

    static class ReadParameters {
        GeneralEnvelope requestedEnvelope;

        Rectangle dim;

        OverviewPolicy overviewPolicy;
    }

    /**
     * A simple helper class to guard and easy logging the mosaic geometries in both geographical
     * and pixel ranges
     */
    private static class LoggingHelper {

        private static final File debugDir = new File(System.getProperty("user.home")
                + File.separator + "arcsde_test");

        static {
            if (DEBUG_TO_DISK) {
                debugDir.mkdir();
            }
        }

        public Level GEOM_LEVEL = Level.FINER;

        public static String REQ_ENV = "Requested envelope";

        public static String RES_ENV = "Resulting envelope";

        public static String MOSAIC_ENV = "Resulting mosaiced envelopes";

        public static String MOSAIC_EXPECTED = "Expected mosaic layout (in pixels)";

        public static String MOSAIC_RESULT = "Resulting image mosaic layout (in pixels)";

        private Map<String, StringBuilder> geoms = null;

        LoggingHelper() {
            // not much to to
        }

        private StringBuilder getGeom(String geomName) {
            if (geoms == null) {
                geoms = new HashMap<String, StringBuilder>();
            }
            StringBuilder sb = geoms.get(geomName);
            if (sb == null) {
                sb = new StringBuilder("MULTIPOLYGON(\n");
                geoms.put(geomName, sb);
            }
            return sb;
        }

        public void appendLoggingGeometries(String geomName, RenderedImage img) {
            if (LOGGER.isLoggable(GEOM_LEVEL)) {
                appendLoggingGeometries(geomName, new Rectangle(img.getMinX(), img.getMinY(), img
                        .getWidth(), img.getHeight()));
            }
        }

        public void appendLoggingGeometries(String geomName, Rectangle env) {
            if (LOGGER.isLoggable(GEOM_LEVEL)) {
                appendLoggingGeometries(geomName, new GeneralEnvelope(env));
            }
        }

        public void appendLoggingGeometries(String geomName, GeneralEnvelope env) {
            if (LOGGER.isLoggable(GEOM_LEVEL)) {
                StringBuilder sb = getGeom(geomName);
                sb.append("  ((" + env.getMinimum(0) + " " + env.getMinimum(1) + ", "
                        + env.getMaximum(0) + " " + env.getMinimum(1) + ", " + env.getMaximum(0)
                        + " " + env.getMaximum(1) + ", " + env.getMinimum(0) + " "
                        + env.getMaximum(1) + ", " + env.getMinimum(0) + " " + env.getMinimum(1)
                        + ")),");
            }
        }

        public void log(String geomName) {
            if (LOGGER.isLoggable(GEOM_LEVEL)) {
                StringBuilder sb = getGeom(geomName);
                sb.setLength(sb.length() - 1);
                sb.append("\n)");
                LOGGER.log(GEOM_LEVEL, geomName + ":\n" + sb.toString());
            }
        }

        public void log(RenderedImage image, Long rasterId, String fileName) {
            if (DEBUG_TO_DISK) {
                LOGGER.warning("BEWARE THE DEBUG FLAG IS TURNED ON! "
                        + "IF IN PRODUCTION THIS IS A SEVERE MISTAKE!!!");
                // ImageIO.write(FormatDescriptor.create(image,
                // Integer.valueOf(DataBuffer.TYPE_BYTE),
                // null), "TIFF", new File(debugDir, rasterId.longValue() + fileName + ".tiff"));

                try {
                    ImageIO.write(image, "TIFF", new File(debugDir, rasterId.longValue() + fileName
                            + ".tiff"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }
}
