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
 *
 */
package org.geotools.coverageio.gdal.mrsid;

import it.geosolutions.imageio.stream.input.FileImageInputStreamExtImpl;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.geotools.coverage.grid.GeneralGridRange;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.coverageio.gdal.BaseGDALGridFormat;
import org.geotools.data.ServiceInfo;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.operation.matrix.XAffineTransform;
import org.geotools.test.TestData;
import org.opengis.coverage.grid.Format;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;

/**
 * @author Daniele Romagnoli, GeoSolutions
 * @author Simone Giannecchini (simboss), GeoSolutions
 * 
 * Testing {@link MrSIDReader}
 */
public final class MrSIDTest extends AbstractMrSIDTestCase {
    /**
     * file name of a valid MrSID sample data to be used for tests.
     * 
     * We suggest to download a valid MrSID sample file from this site:
     * https://zulu.ssc.nasa.gov/mrsid/
     * 
     * For each .SID file, a .MET file exists. Use the last one to build a valid
     * .PRJ for the sample. If you are only interested in reading/rendering
     * capabilities, or displaying coverages as simple rasters, the .PRJ is not
     * necessary. However, a valid .PRJ file is required anytime you need to use
     * the sample data as a coherently GeoReferenced coverage, by means of, as
     * an instance, uDIG.
     */
    private final static String fileName = "n13250i.sid";

    /**
     * Creates a new instance of {@link MrSIDTest}
     * 
     * @param name
     */
    public MrSIDTest(String name) {
        super(name);
    }

    /**
     * Test for reading a grid coverage from a MrSID source
     * 
     * @throws Exception
     */
    public void test() throws Exception {
        if (!testingEnabled()) {
            return;
        }
        // read in the grid coverage
        if (fileName.equalsIgnoreCase("")) {
            LOGGER
                    .info("==================================================================\n"
                            + " Warning! No valid test File has been specified.\n"
                            + " Please provide a valid sample in the source code and repeat this test!\n"
                            + "========================================================================");

            return;
        }
        
        File file =null;
        try {
            file = TestData.file(this, fileName);
        }catch (FileNotFoundException fnfe){
            LOGGER.warning("test-data not found: " + fileName + "\nTests are skipped");
            return;
        } catch (IOException ioe) {
            LOGGER.warning("test-data not found: " + fileName + "\nTests are skipped");
            return;
        }

        // get a reader
        final MrSIDReader reader = new MrSIDReader(file);
        
        // /////////////////////////////////////////////////////////////////////
        //
        // read once
        //
        // /////////////////////////////////////////////////////////////////////
        GridCoverage2D gc = (GridCoverage2D) reader.read(null);
        forceDataLoading(gc);

        // /////////////////////////////////////////////////////////////////////
        //
        // read again with subsampling and crop
        //
        // /////////////////////////////////////////////////////////////////////
        final int originalW = gc.getRenderedImage().getWidth();
        final int originalH = gc.getRenderedImage().getHeight();
        final Rectangle range = reader.getOriginalGridRange().toRectangle();
        final GeneralEnvelope originalEnvelope = reader.getOriginalEnvelope();
        final GeneralEnvelope reducedEnvelope = new GeneralEnvelope(new double[] {
                originalEnvelope.getLowerCorner().getOrdinate(0),
                originalEnvelope.getLowerCorner().getOrdinate(1)},
                new double[] { originalEnvelope.getMedian().getOrdinate(0),
                        originalEnvelope.getMedian().getOrdinate(1)});
        reducedEnvelope.setCoordinateReferenceSystem(reader.getCrs());

        final ParameterValue gg = (ParameterValue) ((AbstractGridFormat) reader
                .getFormat()).READ_GRIDGEOMETRY2D.createValue();
        gg.setValue(new GridGeometry2D(new GeneralGridRange(new Rectangle(0, 0,
                (int) (range.width / 2.0),
                (int) (range.height / 2.0))), reducedEnvelope));
        gc = (GridCoverage2D) reader.read(new GeneralParameterValue[] { gg });
        assertNotNull(gc);
        // NOTE: in some cases might be too restrictive
        assertTrue(reducedEnvelope.equals(gc.getEnvelope(), XAffineTransform
                .getScale(((AffineTransform) ((GridGeometry2D) gc
                        .getGridGeometry()).getGridToCRS2D())) / 2, true));
        // this should be fine since we give 1 pixel tolerance
        assertEquals(originalW / 2.0 , gc.getRenderedImage()
                .getWidth(), 1);
        assertEquals(originalH / 2.0 , gc.getRenderedImage()
                .getHeight(), 1);

        forceDataLoading(gc);

        // /////////////////////////////////////////////////////////////////////
        //
        // Read ignoring overviews with subsampling and crop, using Jai,
        // multithreading and customized tilesize
        //
        // /////////////////////////////////////////////////////////////////////
        final ParameterValue policy = (ParameterValue) ((AbstractGridFormat) reader
                .getFormat()).OVERVIEW_POLICY.createValue();
        policy.setValue(OverviewPolicy.IGNORE);
        
        // //
        //
        // Enable multithreading read
        //
        // //
        final ParameterValue mt = (ParameterValue) ((BaseGDALGridFormat) reader
                .getFormat()).USE_MULTITHREADING.createValue();
        mt.setValue(true);
        
        // //
        //
        // Customizing Tile Size
        //
        // //
        final ParameterValue tilesize = (ParameterValue) ((BaseGDALGridFormat) reader
                .getFormat()).SUGGESTED_TILE_SIZE.createValue();
        tilesize.setValue("512,512");
        
        // //
        //
        // Setting read type: use JAI ImageRead 
        //
        // //
        final ParameterValue useJaiRead = (ParameterValue) ((BaseGDALGridFormat) reader
                .getFormat()).USE_JAI_IMAGEREAD.createValue();
        useJaiRead.setValue(true);

        gc = (GridCoverage2D) reader.read(new GeneralParameterValue[] { gg,
                policy, mt, tilesize, useJaiRead });
        
        assertNotNull(gc);
        // NOTE: in some cases might be too restrictive
        assertTrue(reducedEnvelope.equals(gc.getEnvelope(), XAffineTransform
                .getScale(((AffineTransform) ((GridGeometry2D) gc
                        .getGridGeometry()).getGridToCRS2D())) / 2, true));
        // this should be fine since we give 1 pixel tolerance
        assertEquals(originalW / 2, gc.getRenderedImage().getWidth(), 1);
        assertEquals(originalH / 2, gc.getRenderedImage().getHeight(), 1);

        forceDataLoading(gc);

        if (TestData.isInteractiveTest()) {
            // printing CRS information
            LOGGER.info(gc.getCoordinateReferenceSystem().toWKT());
            LOGGER.info(gc.getEnvelope().toString());
        }
    }
    
    /**
     * Test class methods
     * 
     * @throws Exception
     */
    public void test2() throws Exception {
        if (!testingEnabled()) {
            return;
        }
        // read in the grid coverage
        if (fileName.equalsIgnoreCase("")) {
            LOGGER
                    .info("==================================================================\n"
                            + " Warning! No valid test File has been specified.\n"
                            + " Please provide a valid sample in the source code and repeat this test!\n"
                            + "========================================================================");
            return;
        }

        // get a reader
        final File file = TestData.file(this, fileName);
        
        final MrSIDFormatFactory factory = new MrSIDFormatFactory();
        final BaseGDALGridFormat format = (BaseGDALGridFormat) factory.createFormat();
        
        assertTrue(format.accepts(file));
        MrSIDReader reader = (MrSIDReader) format.getReader(file);
        
        final int numImages = reader.getGridCoverageCount();
        final boolean hasMoreGridCoverages = reader.hasMoreGridCoverages();
        assertEquals(1, numImages);
        assertEquals(false, hasMoreGridCoverages);
        
        final ServiceInfo serviceInfo = reader.getInfo();
        reader.getInfo("coverage");
        reader.dispose();

        boolean writersAvailable = true;
        boolean hasWriteParams = true;
        try{
            format.getWriter(new File("test"));
        }catch (UnsupportedOperationException uoe){
            writersAvailable = false;
        }
        assertFalse(writersAvailable);

        try{
            format.getDefaultImageIOWriteParameters();
        }catch (UnsupportedOperationException uoe){
            hasWriteParams = false;
        }
        assertFalse(hasWriteParams);

        // Testing sections of code involving URLs
        final StringBuilder sb = new StringBuilder("file:///").append(file.getAbsolutePath());
        final URL url = new URL(sb.toString());
        reader = (MrSIDReader) format.getReader(url);
        reader.getInfo();
        reader.dispose();
        
        FileImageInputStreamExtImpl fiis = new FileImageInputStreamExtImpl(file);
        reader = new MrSIDReader(fiis);
        reader.dispose();
    }

    public static Test suite() {
        TestSuite suite = new TestSuite();

        // Test read exploiting common JAI operations (Crop-Translate-Rotate)
        suite.addTest(new MrSIDTest("test"));
        
        suite.addTest(new MrSIDTest("test2"));

        return suite;
    }

    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
        
    }
}