/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2006-2008, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.gce.imagemosaic;

import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReaderSpi;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;

import javax.imageio.ImageReadParam;
import javax.imageio.spi.ImageReaderSpi;

import junit.framework.JUnit4TestAdapter;
import junit.textui.TestRunner;

import org.apache.commons.io.filefilter.FileFilterUtils;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.DecimationPolicy;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.coverage.grid.io.UnknownFormat;
import org.geotools.factory.Hints;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.operation.builder.GridToEnvelopeMapper;
import org.geotools.referencing.operation.matrix.XAffineTransform;
import org.geotools.test.TestData;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Testing {@link OverviewsController}.
 * 
 * @author Daniele Romagnoli, GeoSolutions SAS
 * 
 */
public class OverviewsControllerTest extends Assert{

    private static CoordinateReferenceSystem WGS84;
    static{
        try{
            WGS84 = CRS.decode("EPSG:4326",true);
        } catch (FactoryException fe){
            WGS84 = DefaultGeographicCRS.WGS84;
        }
    }
    
    private final static Logger LOGGER = Logger.getLogger(OverviewsControllerTest.class.toString());
    
    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(OverviewsControllerTest.class);
    }

    private static final ReferencedEnvelope TEST_BBOX_A = new ReferencedEnvelope(-180,0,-90,90,WGS84);
    private static final ReferencedEnvelope TEST_BBOX_B = new ReferencedEnvelope(0,180,0,90,WGS84);

    private static final ImageReaderSpi spi = new TIFFImageReaderSpi();
    
    private URL heterogeneousGranulesURL;
	
    /**
     * Tests the {@link OverviewsController} with support for different
     * resolutions/different number of overviews.
     * 
     * world_a.tif => Pixel Size = (0.833333333333333,-0.833333333333333); 4 overviews 
     * world_b1.tif => Pixel Size = (1.406250000000000,-1.406250000000000); 2 overviews 
     * 
     * @throws IOException
     * @throws MismatchedDimensionException
     * @throws FactoryException
     * @throws TransformException 
     */
    @Test
    public void testHeterogeneousGranules() throws IOException,
            MismatchedDimensionException, FactoryException, TransformException {

        // //
        //
        // Initialize mosaic variables
        //
        // //
        final AbstractGridFormat format = getFormat(heterogeneousGranulesURL);
        final ImageMosaicReader reader = getReader(heterogeneousGranulesURL, format);
        final RasterManager rasterManager = new RasterManager(reader);
        
        // //
        //
        // Initialize granules related variables 
        //
        // //
        final File g1File = new File(heterogeneousGranulesURL.getPath()+"/world_a.tif");
        final File g2File = new File(heterogeneousGranulesURL.getPath()+"/world_b1.tif");
        final ImageReadParam readParamsG1 = new ImageReadParam();
        final ImageReadParam readParamsG2 = new ImageReadParam();
        int levelIndexG1 = 0;
        int levelIndexG2 = 0;
        
        final GranuleDescriptor granuleDescriptor1 = new GranuleDescriptor(g1File.getAbsolutePath(), TEST_BBOX_A, spi, (Geometry) null, true);
        final GranuleDescriptor granuleDescriptor2 = new GranuleDescriptor(g2File.getAbsolutePath(), TEST_BBOX_B, spi, (Geometry) null, true);
        assertNotNull(granuleDescriptor1.toString());
        assertNotNull(granuleDescriptor2.toString());
        
        final OverviewsController ovControllerG1 = granuleDescriptor1.overviewsController;
        final OverviewsController ovControllerG2 = granuleDescriptor2.overviewsController;
        
        // //
        //
        // Initializing read request
        //
        // //
        final GeneralEnvelope envelope = reader.getOriginalEnvelope();
        final GridEnvelope originalRange = reader.getOriginalGridRange();
        final Rectangle rasterArea = new Rectangle(0, 0, (int) Math.ceil(originalRange.getSpan(0) / 9.0), (int) Math.ceil(originalRange.getSpan(1) / 9.0));
        final GridEnvelope2D range = new GridEnvelope2D(rasterArea);
        final GridToEnvelopeMapper geMapper = new GridToEnvelopeMapper(range, envelope);
        geMapper.setPixelAnchor(PixelInCell.CELL_CENTER);
        final AffineTransform gridToWorld = geMapper.createAffineTransform();
        final double requestedResolution[] = new double[]{XAffineTransform.getScaleX0(gridToWorld), XAffineTransform.getScaleY0(gridToWorld)}; 
        
        // //
        //
        // Starting OverviewsController tests
        //
        // //
        
        OverviewPolicy ovPolicy = OverviewPolicy.QUALITY;
        
        LOGGER.info("Testing with OverviewPolicy = QUALITY");
        levelIndexG1 = ReadParamsController.setReadParams(requestedResolution, ovPolicy, DecimationPolicy.ALLOW, readParamsG1, rasterManager, ovControllerG1);
        levelIndexG2 = ReadParamsController.setReadParams(requestedResolution, ovPolicy, DecimationPolicy.ALLOW, readParamsG2, rasterManager, ovControllerG2);
        assertTrue(levelIndexG1 == 3);
        assertTrue(levelIndexG2 == 2);
        assertTrue(readParamsG1.getSourceXSubsampling() == 1);
        assertTrue(readParamsG1.getSourceYSubsampling() == 1);
        assertTrue(readParamsG2.getSourceXSubsampling() == 1);
        assertTrue(readParamsG2.getSourceYSubsampling() == 2);

        LOGGER.info("Testing with OverviewPolicy = SPEED");
        ovPolicy = OverviewPolicy.SPEED;
        levelIndexG1 = ReadParamsController.setReadParams(requestedResolution, ovPolicy, DecimationPolicy.ALLOW, readParamsG1, rasterManager, ovControllerG1);
        levelIndexG2 = ReadParamsController.setReadParams(requestedResolution, ovPolicy, DecimationPolicy.ALLOW, readParamsG2, rasterManager, ovControllerG2);
        assertTrue(levelIndexG1 == 4);
        assertTrue(levelIndexG2 == 2);
        assertTrue(readParamsG1.getSourceXSubsampling() == 1);
        assertTrue(readParamsG1.getSourceYSubsampling() == 1);
        assertTrue(readParamsG2.getSourceXSubsampling() == 1);
        assertTrue(readParamsG2.getSourceYSubsampling() == 2);
        
        LOGGER.info("Testing with OverviewPolicy = NEAREST");
        ovPolicy = OverviewPolicy.NEAREST;
        levelIndexG1 = ReadParamsController.setReadParams(requestedResolution, ovPolicy, DecimationPolicy.ALLOW, readParamsG1, rasterManager, ovControllerG1);
        levelIndexG2 = ReadParamsController.setReadParams(requestedResolution, ovPolicy, DecimationPolicy.ALLOW, readParamsG2, rasterManager, ovControllerG2);
        assertTrue(levelIndexG1 == 3);
        assertTrue(levelIndexG2 == 2);
        assertTrue(readParamsG1.getSourceXSubsampling() == 1);
        assertTrue(readParamsG1.getSourceYSubsampling() == 1);
        assertTrue(readParamsG2.getSourceXSubsampling() == 1);
        assertTrue(readParamsG2.getSourceYSubsampling() == 2);
        
        LOGGER.info("Testing with OverviewPolicy = IGNORE");
        ovPolicy = OverviewPolicy.IGNORE;
        levelIndexG1 = ReadParamsController.setReadParams(requestedResolution, ovPolicy, DecimationPolicy.ALLOW, readParamsG1, rasterManager, ovControllerG1);
        levelIndexG2 = ReadParamsController.setReadParams(requestedResolution, ovPolicy, DecimationPolicy.ALLOW, readParamsG2, rasterManager, ovControllerG2);
        assertTrue(levelIndexG1 == 0);
        assertTrue(levelIndexG2 == 0);
        assertTrue(readParamsG1.getSourceXSubsampling() == 9);
        assertTrue(readParamsG1.getSourceYSubsampling() == 9);
        assertTrue(readParamsG2.getSourceXSubsampling() == 5);
        assertTrue(readParamsG2.getSourceYSubsampling() == 5);
    }

    /**
     * returns an {@link AbstractGridCoverage2DReader} for the provided
     * {@link URL} and for the providede {@link AbstractGridFormat}.
     * 
     * @param testURL
     *            points to a valid object to create an
     *            {@link AbstractGridCoverage2DReader} for.
     * @param format
     *            to use for instantiating such a reader.
     * @return a suitable {@link ImageMosaicReader}.
     * @throws FactoryException
     * @throws NoSuchAuthorityCodeException
     */
    private ImageMosaicReader getReader(URL testURL, final AbstractGridFormat format)
            throws NoSuchAuthorityCodeException, FactoryException {

        final Hints hints = new Hints(Hints.DEFAULT_COORDINATE_REFERENCE_SYSTEM, WGS84);
        final ImageMosaicReader reader = (ImageMosaicReader) format.getReader(testURL, hints);
        Assert.assertNotNull(reader);
        return reader;
    }

    /**
     * Tries to get an {@link AbstractGridFormat} for the provided URL.
     * 
     * @param testURL
     *            points to a shapefile that is the index of a certain mosaic.
     * @return a suitable {@link AbstractGridFormat}.
     * @throws FactoryException
     * @throws NoSuchAuthorityCodeException
     */
    private AbstractGridFormat getFormat(URL testURL) throws NoSuchAuthorityCodeException, FactoryException {

        final Hints hints = new Hints(Hints.DEFAULT_COORDINATE_REFERENCE_SYSTEM, WGS84);

        // Get format
        final AbstractGridFormat format = (AbstractGridFormat) GridFormatFinder.findFormat(testURL, hints);
        Assert.assertNotNull(format);
        Assert.assertFalse("UknownFormat", format instanceof UnknownFormat);
        return format;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        TestRunner.run(OverviewsControllerTest.suite());

    }

    @Before
    public void setUp() throws Exception {
        // remove generated file

        cleanUp();

        heterogeneousGranulesURL = TestData.url(this, "heterogeneous");
    }

    /**
     * Cleaning up the generated files (shape and properties so that we recreate
     * them).
     * 
     * @throws FileNotFoundException
     * @throws IOException
     */
    private void cleanUp() throws FileNotFoundException, IOException {
        File dir = TestData.file(this, "heterogeneous/");
        File[] files = dir
                .listFiles((FilenameFilter) FileFilterUtils.notFileFilter(FileFilterUtils.orFileFilter(
                        FileFilterUtils.orFileFilter(
                                FileFilterUtils.suffixFileFilter("tif"),
                                FileFilterUtils.suffixFileFilter("aux")),
                        FileFilterUtils.nameFileFilter("datastore.properties"))));
        for (File file : files) {
            file.delete();
        }
    }

    @After
    public void tearDown() throws FileNotFoundException, IOException {
        cleanUp();
    }

}