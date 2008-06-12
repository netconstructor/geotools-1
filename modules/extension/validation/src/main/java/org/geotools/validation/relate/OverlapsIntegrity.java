/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2004-2008, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.validation.relate;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Logger;

import org.geotools.data.FeatureSource;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.filter.AttributeExpression;
import org.geotools.filter.BBoxExpression;
import org.geotools.filter.FilterFactory;
import org.geotools.filter.FilterFactoryFinder;
import org.geotools.filter.FilterType;
import org.geotools.filter.GeometryFilter;
import org.geotools.filter.IllegalFilterException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.validation.ValidationResults;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;


/**
 * OverlapsIntegrity<br>
 * @author bowens, ptozer<br>
 * Created Apr 27, 2004<br>
 * @source $URL$
 * @version <br>
 * 
 * <b>Puropse:</b><br>
 * <p>
 * Tests to see if a Geometry overlaps, partially or entirely, with another Geometry.
 * 
 * <b>Description:</b><br>
 * <p>
 * If only one layer is provided, the geometries of that layer are compared with each other.
 * If two layers are provided, then the geometries are compared across the layers.
 * </p>
 * 
 * <b>Usage:</b><br>
 * <p>
 * 		OverlapsIntegrity overlap = new OverlapsIntegrity();
 *		overlap.setExpected(false);
 *		overlap.setGeomTypeRefA("my:line");
 *		
 *		Map map = new HashMap();
 *		try
 *		{
 *			map.put("my:line", mds.getFeatureSource("line"));
 *		} catch (IOException e1)
 *		{
 *			e1.printStackTrace();
 *		}
 *		
 *		try
 *		{
 *			assertFalse(overlap.validate(map, lineBounds, vr));
 *		} catch (Exception e)
 *		{
 *			e.printStackTrace();
 *		}
 * </p>
 */
public class OverlapsIntegrity extends RelationIntegrity 
{
	private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geotools.validation");
	private static HashSet usedIDs;
	private boolean showPrintLines = true;
	
	/**
	 * OverlapsIntegrity Constructor
	 * 
	 */
	public OverlapsIntegrity()
	{
		super();
		usedIDs = new HashSet();	//TODO: remove me later, memory inefficient
	}
	
	
	/* (non-Javadoc)
	 * @see org.geotools.validation.IntegrityValidation#validate(java.util.Map, com.vividsolutions.jts.geom.Envelope, org.geotools.validation.ValidationResults)
	 */
	public boolean validate(Map layers, ReferencedEnvelope envelope,
			ValidationResults results) throws Exception 
	{
		LOGGER.finer("Starting test "+getName()+" ("+getClass().getName()+")" );
		String typeRef1 = getGeomTypeRefA();
		LOGGER.finer( typeRef1 +": looking up FeatureSource" );		
		FeatureSource<SimpleFeatureType, SimpleFeature> geomSource1 = (FeatureSource<SimpleFeatureType, SimpleFeature>) layers.get( typeRef1 );
		LOGGER.finer( typeRef1 +": found "+ geomSource1.getSchema().getTypeName() );
		
		String typeRef2 = getGeomTypeRefB();
		if (typeRef2 == EMPTY || typeRef1.equals(typeRef2))
			return validateSingleLayer(geomSource1, isExpected(), results, envelope);
		else
		{
			LOGGER.warning( typeRef2 +": looking up FeatureSource<SimpleFeatureType, SimpleFeature> " );        
			FeatureSource<SimpleFeatureType, SimpleFeature> geomSource2 = (FeatureSource<SimpleFeatureType, SimpleFeature>) layers.get( typeRef2 );
			LOGGER.finer( typeRef2 +": found "+ geomSource2.getSchema().getTypeName() );
			return validateMultipleLayers(geomSource1, geomSource2, isExpected(), results, envelope);
		}	
	
	}


	/**
	 * <b>validateMultipleLayers Purpose:</b> <br>
	 * <p>
	 * This validation tests for a geometry overlaps another geometry. 
	 * Uses JTS' Geometry.overlaps(Geometry) and  Geometry.contains(Geometry)method.
	 * The DE-9IM intersection matrix for overlaps is:
     * T*T***T** (for two points or two surfaces)
     * 1*T***T** (for two curves) 
     * Contains DE-9IM intersection matrix is T*F**F***.
	 * </p>
	 * 
	 * <b>Description:</b><br>
	 * <p>
	 * The function filters the FeatureSources using the given bounding box.
	 * It creates iterators over both filtered FeatureSources. It calls overlaps() and contains()using the
	 * geometries in the FeatureSource<SimpleFeatureType, SimpleFeature> layers. Tests the results of the method call against
	 * the given expected results. Returns true if the returned results and the expected results 
	 * are true, false otherwise.
	 * 
	 * </p>
	 * 
	 * Author: bowens<br>
	 * Created on: Apr 27, 2004<br>
	 * @param featureSourceA - the FeatureSource<SimpleFeatureType, SimpleFeature> to pull the original geometries from. This geometry is the one that is tested for overlaping with the other
	 * @param featureSourceB - the FeatureSource<SimpleFeatureType, SimpleFeature> to pull the other geometries from - these geometries will be those that may overlap the first geometry
	 * @param expected - boolean value representing the user's expected outcome of the test
	 * @param results - ValidationResults
	 * @param bBox - Envelope - the bounding box within which to perform the overlaps() and contains()
	 * @return boolean result of the test
	 * @throws Exception - IOException if iterators improperly closed
	 */
	private boolean validateMultipleLayers(	FeatureSource<SimpleFeatureType, SimpleFeature> featureSourceA, 
											FeatureSource<SimpleFeatureType, SimpleFeature> featureSourceB, 
											boolean expected, 
											ValidationResults results, 
											ReferencedEnvelope bBox) 
	throws Exception
	{
		boolean success = true;
		int errors = 0;
		int countInterval = 100;
		int counter = 0;
		SimpleFeatureType ft = featureSourceA.getSchema();
		
		Filter filter = filterBBox(bBox, ft);

		//FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = featureSourceA.getFeatures(filter);
		FeatureCollection<SimpleFeatureType, SimpleFeature> collectionA = featureSourceA.getFeatures();
		
		FeatureIterator<SimpleFeature> fr1 = null;
		FeatureIterator<SimpleFeature> fr2 = null;
		try 
		{
			fr1 = collectionA.features();

			if (fr1 == null)
				return success;
		
			while (fr1.hasNext())
			{
				counter++;
				SimpleFeature f1 = fr1.next();
				
				Geometry g1 = (Geometry)f1.getDefaultGeometry();
				Filter filter2 = filterBBox(ReferencedEnvelope.reference(g1.getEnvelope().getEnvelopeInternal()), ft);

				FeatureCollection<SimpleFeatureType, SimpleFeature> collectionB = featureSourceB.getFeatures(filter2);
				
				fr2 = collectionB.features();
				try 
				{
					while (fr2 != null && fr2.hasNext())
					{
						SimpleFeature f2 = fr2.next();
						Geometry g2 = (Geometry)f2.getDefaultGeometry();
						if (!usedIDs.contains(f2.getID()))
						{
							
							if (!f1.getID().equals(f2.getID()))	// if they are the same feature, move onto the next one
							{
								if(g1.overlaps(g2) != expected || g1.contains(g2) != expected)
								{
									//results.error( f1, f1.getDefaultGeometry().getGeometryType()+" "+getGeomTypeRefA()+"("+f1.getID()+")"+" overlapped "+getGeomTypeRefA()+"("+f2.getID()+"), Result was not "+expected );
									results.error( f1, getGeomTypeRefA()+"("+f1.getID()+")"+" overlapped "+getGeomTypeRefB()+"("+f2.getID()+")");
									if (showPrintLines)
									{
										//System.out.println(f1.getDefaultGeometry().getGeometryType()+" "+getGeomTypeRefA()+"("+f1.getID()+")"+" overlapped "+getGeomTypeRefA()+"("+f2.getID()+"), Result was not "+expected);
										//System.out.println(f1.getID().substring(8)+ " " + f2.getID().substring(8));
									}
									success = false;
									errors++;
								}
							}
						}
					}
					usedIDs.add(f1.getID());
					if (counter%countInterval == 0 && showPrintLines)
						System.out.println("count: " + counter);
						
				}finally{
					collectionB.close( fr2 );
				}
			}// end while 1
		}finally
		{
			collectionA.close( fr1 );
		}
		
		return success;
	}

	/**
	 * <b>validateSingleLayer Purpose:</b> <br>
	 * <p>
	 * This validation tests for a geometry that overlaps with itself. 
	 * Uses JTS' Geometry.overlaps(Geometry) and  Geometry.contains(Geometry)method.
	 * The DE-9IM intersection matrix for overlaps is:
     * T*T***T** (for two points or two surfaces)
     * 1*T***T** (for two curves) 
     * Contains DE-9IM intersection matrix is T*F**F***.
	 * </p>
	 * 
	 * <b>Description:</b><br>
	 * <p>
	 * The function filters the FeatureSource<SimpleFeatureType, SimpleFeature> using the given bounding box.
	 * It creates iterators over the filtered FeatureSource. It calls overlaps() and contains() using the
	 * geometries in the FeatureSource<SimpleFeatureType, SimpleFeature> layer. Tests the results of the method calls against
	 * the given expected results. Returns true if the returned results and the expected results 
	 * are true, false otherwise.
	 * 
	 * </p>	 * 
	 * Author: bowens<br>
	 * Created on: Apr 27, 2004<br>
	 * @param featureSourceA - the FeatureSource<SimpleFeatureType, SimpleFeature> to pull the original geometries from. This geometry is the one that is tested for overlapping itself
	 * @param expected - boolean value representing the user's expected outcome of the test
	 * @param results - ValidationResults
	 * @param bBox - Envelope - the bounding box within which to perform the overlaps() and contains()
	 * @return boolean result of the test
	 * @throws Exception - IOException if iterators improperly closed
	 */
	private boolean validateSingleLayer(FeatureSource<SimpleFeatureType, SimpleFeature> featureSourceA, 
										boolean expected, 
										ValidationResults results, 
										ReferencedEnvelope bBox) 
	throws Exception
	{
		boolean success = true;
		int errors = 0;
		Date date1 = new Date();
		int countInterval = 100;
		int counter = 0;
		SimpleFeatureType ft = featureSourceA.getSchema();
		
		
		System.out.println("---------------- In Overlaps Integrity ----------------");

		FeatureCollection<SimpleFeatureType, SimpleFeature> collectionA = null;
		
		if(bBox != null && !bBox.isNull() && bBox.getHeight() != 0.0 && bBox.getWidth() != 0.0)
		{
			Filter filter = filterBBox(bBox, ft);
			collectionA = featureSourceA.getFeatures(filter);
		}
		else
			collectionA = featureSourceA.getFeatures();
		
		FeatureIterator<SimpleFeature> fr1 = null;
		FeatureIterator<SimpleFeature> fr2 = null;
		try 
		{
			fr1 = collectionA.features();
			if (fr1 == null)
				return success;
		
			while (fr1.hasNext())
			{
				counter++;
				SimpleFeature f1 = fr1.next();
				
				Geometry g1 = (Geometry) f1.getDefaultGeometry();
				Filter filter2 = filterBBox(ReferencedEnvelope.reference(g1.getEnvelope().getEnvelopeInternal()), ft);

				FeatureCollection<SimpleFeatureType, SimpleFeature> collectionB = featureSourceA.getFeatures(filter2);
				
				fr2 = collectionB.features();
				try 
				{
					while (fr2 != null && fr2.hasNext())
					{
						SimpleFeature f2 = fr2.next();
						Geometry g2 = (Geometry) f2.getDefaultGeometry();
						if (!usedIDs.contains(f2.getID()))
						{
							
							if (!f1.getID().equals(f2.getID()))	// if they are the same feature, move onto the next one
							{
								//if(g1.overlaps(g2) != expected || g1.contains(g2) != expected)
								if (g1.relate(g2, "1********") != expected)
								{
									//results.error( f1, f1.getDefaultGeometry().getGeometryType()+" "+getGeomTypeRefA()+"("+f1.getID()+")"+" overlapped "+getGeomTypeRefA()+"("+f2.getID()+"), Result was not "+expected );
									if( results != null ){
										results.error( f1, ""+getGeomTypeRefA()+"("+f1.getID()+")"+" overlapped "+getGeomTypeRefA()+"("+f2.getID()+")");
									}
									if (showPrintLines)
									{
										//System.out.println(f1.getDefaultGeometry().getGeometryType()+" "+getGeomTypeRefA()+"("+f1.getID()+")"+" overlapped "+getGeomTypeRefA()+"("+f2.getID()+"), Result was not "+expected);
										//System.out.println(f1.getID().substring(11)+ " " + f2.getID().substring(11));
									}
									success = false;
									errors++;
								}
							}
						}
					}
					usedIDs.add(f1.getID());
					//if (counter%countInterval == 0 && showPrintLines)
					//	System.out.println("count: " + counter);
						
				}finally{
					collectionB.close( fr2 );
				}
			}// end while 1
		}finally
		{
			Date date2 = new Date();
			float dt = date2.getTime() - date1.getTime();
			if (showPrintLines)
			{
				System.out.println("########## Validation duration: " + dt);
				System.out.println("########## Validation errors: " + errors);
			}
			
            collectionA.close( fr1 );
		}
		
		return success;
	}
	
	
	
	/** Try and Filter by the provided bbox, will default to Filter.EXCLUDE if null */
	static public Filter filterBBox(Envelope bBox, SimpleFeatureType ft)
		throws FactoryRegistryException, IllegalFilterException
	{
		if( bBox == null ){
			return Filter.INCLUDE;
		}
		FilterFactory ff = FilterFactoryFinder.createFilterFactory();
		BBoxExpression bboxExpr = ff.createBBoxExpression(bBox);
		//GeometryFilter bbFilter = ff.createGeometryFilter(Filter.GEOMETRY_BBOX);
		AttributeExpression geomExpr = ff.createAttributeExpression(ft, ft.getDefaultGeometry().getLocalName());
		GeometryFilter disjointFilter = ff.createGeometryFilter(FilterType.GEOMETRY_DISJOINT);
		disjointFilter.addLeftGeometry(geomExpr);
		disjointFilter.addRightGeometry(bboxExpr);
		Filter filter = disjointFilter.not();
		
		return filter;
	}
}
