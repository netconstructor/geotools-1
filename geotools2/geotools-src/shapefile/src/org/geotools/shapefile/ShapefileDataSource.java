/*
 *    Geotools - OpenSource mapping toolkit
 *    (C) 2002, Centre for Computational Geography
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
 *    You should have received a copy of the GNU Lesser General Public
 *    License along with this library; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package org.geotools.shapefile;

import com.vividsolutions.jts.geom.*;

import org.geotools.data.DataSourceMetaData;
import org.geotools.data.DataSourceException;
import org.geotools.feature.*;
import org.geotools.feature.AttributeType;
import org.geotools.filter.Filter;
import org.geotools.shapefile.dbf.*;
import org.geotools.shapefile.shapefile.*;

import java.io.IOException;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Date;

import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;

import java.lang.ref.SoftReference;


/**
 * @version $Id: ShapefileDataSource.java,v 1.22 2003/03/30 20:21:09 ianschneider Exp $
 * @author James Macgill, CCG
 * @author Ian Schneider
 * @task TODO: add support for the optional spatial index files to improve
 *             loading of sub regions
 */

public class ShapefileDataSource implements org.geotools.data.DataSource {
  
  private URL shpURL;
  private URL dbfURL;
  private URL shxURL;
  
  private FeatureType schema = null;
  
  /**
   *
   */
  public ShapefileDataSource(URL url) throws java.net.MalformedURLException {
    
    String filename = java.net.URLDecoder.decode(url.getFile());
    
    String shpext = ".shp";
    String dbfext = ".dbf";
    String shxext = ".shx";
    
    if(filename.endsWith(shpext) || filename.endsWith(dbfext) || filename.endsWith(shxext)) {
        filename = filename.substring(0, filename.length() - 4);
    } else if(filename.endsWith(".SHP") || filename.endsWith(".DBF") || filename.endsWith(".SHX")) {
        filename = filename.substring(0, filename.length() - 4);
        shpext = ".SHP";
        dbfext = ".DBF";
        shxext = ".SHX";
    }

    shpURL = new URL(url, filename + shpext);
    dbfURL = new URL(url, filename + dbfext);
    shxURL = new URL(url, filename + shxext);
  }
  
  /**
   * Gets the Column names (used by FeatureTable) for this DataSource.
   * This is poorly thought out on my part (Ian S.), I just took what was there...
   * @task REVISIT: why is no IOException thrown here?
   */
  public String[] getColumnNames() {
    if (dbfURL == null) {
      return new String[]{"Geometry"};
    }
    
    try {
      DbaseFileHeader header = new DbaseFileHeader();
      header.readHeader(getReadChannel(dbfURL));
      return getColumnNames(header);
    } catch (IOException ioe) {
      // What now? This seems arbitrarily appropriate !
      throw new RuntimeException("Poorly designed API for DataSource - should be throwing IOException or something",ioe);
    }
  }
  
  /** Stops this DatataSource from loading.
   */
  public void abortLoading() {
    // let em suffer...
  }
  
  /** Adds all features from the passed feature collection to the datasource.
   *
   * @param collection The collection from which to add the features.
   * @throws DataSourceException If anything goes wrong or if exporting is
   * not supported.
   * @task TODO: Implement addFeatures method
   */
  public Set addFeatures(org.geotools.feature.FeatureCollection collection) throws DataSourceException {
    throw new DataSourceException("Removal of features is not yet supported by this datasource");
  }
  
  /** Gets the bounding box of this datasource using the default speed of
   * this datasource as set by the implementer.
   *
   * @return The bounding box of the datasource or null if unknown and too
   * expensive for the method to calculate.
   */
  public Envelope getBbox() {
    // This is way quick!!!
    try {
      ByteBuffer buffer = ByteBuffer.allocateDirect(100);
      ReadableByteChannel in = getReadChannel(shpURL);
      in.read(buffer);
      buffer.flip();
      ShapefileHeader header = new ShapefileHeader();
      header.read(buffer, true);
      return new Envelope(header.minX(),header.minY(),header.maxX(),header.maxY());
    } catch (IOException ioe) {
      // What now? This seems arbitrarily appropriate !
      throw new RuntimeException("Poorly designed API for DataSource - should be throwing IOException or something",ioe);
    }
  }
  
  /** Gets the bounding box of this datasource using the speed of
   * this datasource as set by the parameter.
   *
   * @param speed If true then a quick (and possibly dirty) estimate of
   * the extent is returned. If false then a slow but accurate extent
   * will be returned
   * @return The extent of the datasource or null if unknown and too
   * expensive for the method to calculate.
   */
  public Envelope getBbox(boolean speed) {
    return getBbox();
  }
  
  /**
   * Loads features from the datasource into the returned collection, based
   * on the passed filter.
   *
   * @param filter An OpenGIS filter; specifies which features to retrieve.
   * @return Collection The collection to put the features into.
   * @throws DataSourceException For all data source errors.
   */
  public FeatureCollection getFeatures(Filter filter) throws DataSourceException {
    FeatureCollection fc = new org.geotools.feature.FeatureCollectionDefault();
    getFeatures(fc, filter);
    return fc;
  }
  
  /**
   * Loads features from the datasource into the passed collection, based
   * on the passed filter.  Note that all data sources must support this
   * method at a minimum.
   *
   * @param collection The collection to put the features into.
   * @param filter An OpenGIS filter; specifies which features to retrieve.
   * @throws DataSourceException For all data source errors.
   */
  public void getFeatures(FeatureCollection collection,final Filter filter) throws DataSourceException {
    try {
      
      // Open a channel for our URL
      ShapefileReader shp = new ShapefileReader(getReadChannel(shpURL));
      
      // Start the DBaseFile, if it exists
      DbaseFileReader dbf = createDbaseReader();
      
      // Create the FeatureType based on the dbf and shapefile
      FeatureType type = getSchema( shp, dbf );
      
      // Make a feature factory
      FeatureFactory featureFactory = new FeatureFactory(type);
      
      // FeatureMaker is like an iterator
      FeatureMaker features = new FeatureMaker(dbf,shp,featureFactory);
      
      // an array to copy features into
      Feature[] array = new Feature[1];
      // read until done
      while (features.hasNext()) {
        array[0] = features.next();
        // short circuit null filter!!!!
        // this wasn't done before
        if (filter == null || filter.contains(array[0]))
          collection.addFeatures(array);
      }
      shp.close();
      if (dbf != null)
        dbf.close();
    }
    catch (java.io.IOException ioe){
      throw new DataSourceException("IO Exception loading data",ioe);
    }
    catch (com.vividsolutions.jts.geom.TopologyException te){
      throw new DataSourceException("Topology Exception loading data", te);
    }
    catch (org.geotools.feature.IllegalFeatureException ife){
      throw new DataSourceException("Illegal Feature Exception loading data",ife);
    }
    catch (org.geotools.shapefile.shapefile.InvalidShapefileException ise){
      throw new DataSourceException("Illegal Feature Exception loading data",ise);
    }
  }
  
  /** Modifies the passed attribute types with the passed objects in all
   * features that correspond to the passed OGS filter.  A convenience
   * method for single attribute modifications.
   *
   * @param type The attributes to modify.
   * @param value The values to put in the attribute types.
   * @param filter An OGC filter to note which attributes to modify.
   * @throws DataSourceException If modificaton is not supported, if
   * the object type do not match the attribute type.
   * @task TODO: Implement support for modification of features (single attribute)
   */
  public void modifyFeatures(AttributeType type, Object value, Filter filter) throws DataSourceException {
    throw new DataSourceException("Modification of features is not yet supported by this datasource");
  }
  
  /** Modifies the passed attribute types with the passed objects in all
   * features that correspond to the passed OGS filter.
   *
   * @param type The attributes to modify.
   * @param value The values to put in the attribute types.
   * @param filter An OGC filter to note which attributes to modify.
   * @throws DataSourceException If modificaton is not supported, if
   * the attribute and object arrays are not eqaul length, or if the object
   * types do not match the attribute types.
   * @task TODO: Implement support for modification of feature (multi attribute)
   */
  public void modifyFeatures(AttributeType[] type, Object[] value, Filter filter) throws DataSourceException {
    throw new DataSourceException("Modification of features is not yet supported by this datasource");
  }
  
  /** Removes all of the features specificed by the passed filter from the
   * collection.
   *
   * @param filter An OpenGIS filter; specifies which features to remove.
   * @throws DataSourceException If anything goes wrong or if deleting is
   * not supported.
   * @task TODO: Implement support for removal of features
   */
  public void removeFeatures(Filter filter) throws DataSourceException {
    throw new DataSourceException("Removal of features is not yet supported by this datasource");
  }
  
  
  /**
   * Begins a transaction(add, remove or modify) that does not commit as 
   * each modification call is made.  If an error occurs during a transaction
   * after this method has been called then the datasource should rollback: 
   * none of the transactions performed after this method was called should
   * go through.
   */
  public void startMultiTransaction() throws DataSourceException {
      throw new DataSourceException("MultiTransactions are not supported by this datasource");
  }

  /**
  * Ends a transaction after startMultiTransaction has been called.  Similar
   * to a commit call in sql, it finalizes all of the transactions called
   * after a startMultiTransaction.
   */
  public void endMultiTransaction() throws DataSourceException {
      throw new DataSourceException("MultiTransactions are not supported by this datasource");
  }

  /**************************************************
    Data source utility methods.
   **************************************************/
  /**
   * Gets the DatasSourceMetaData object associated with this datasource.  
   * This is the preferred way to find out which of the possible datasource
   * interface methods are actually implemented, query the DataSourceMetaData
   * about which methods the datasource supports.
   */
  public org.geotools.data.DataSourceMetaData getMetaData() {
      return null;
  }
  
  private FeatureType getSchema(ShapefileReader shp,DbaseFileReader dbf) 
  throws DataSourceException,IOException,InvalidShapefileException {
    if (schema == null) {
        if (shp == null)
          shp = new ShapefileReader(getReadChannel(shpURL));
        if (dbf == null)
          dbf = createDbaseReader();
        // Create the FeatureType based on the dbf and shapefile
        return getFeatureType( dbf,shp );
    }
    
    return schema;
  }
    
  
  /**
    * Retrieves the featureType that features extracted from this datasource
    * will be created with.
    */
  public FeatureType getSchema() {
    try {
      return getSchema(null,null);
    } catch (DataSourceException e) {
      throw new RuntimeException("Whoah, better API needed - runtime error",e);
    } catch (InvalidShapefileException e) {
      throw new RuntimeException("Whoah, better API needed - runtime error",e);
    } catch (IOException e) {
      throw new RuntimeException("Whoah, better API needed - runtime error",e);
    }
    
  }

  /**
   * Sets the schema that features extrated from this datasource will be 
   * created with.  This allows the user to obtain the attributes he wants,
   * by calling getSchema and then creating a new schema using the 
   * attributeTypes from the currently used schema.  
   * @param schema the new schema to be used to create features.
   */
  public void setSchema(FeatureType schema) throws DataSourceException {
    this.schema = schema;
  }
    
  
  
  private DbaseFileReader createDbaseReader() throws IOException {
    if (dbfURL == null) return null;
    ReadableByteChannel channel = getReadChannel(dbfURL);
    if (channel == null) return null;
    return new DbaseFileReader(channel);
  }
  
  private String[] getColumnNames(DbaseFileHeader header) {
    String[] names = new String[header.getNumFields() + 1];
    names[0] = "Geometry";
    for (int i = 1, ii = names.length; i < ii; i++) {
      names[i] = header.getFieldName(i);
    }
    return names;
  }    
  
  private static ReadableByteChannel getReadChannel(URL url) throws IOException {
    if (url.getProtocol().equals("file")) {
      File file = new File(url.getFile());
      if (! file.exists() || !file.canRead())
        return null;
      FileInputStream in = new FileInputStream(file);
      return in.getChannel();
    } else {
      InputStream in = url.openConnection().getInputStream();
      return Channels.newChannel(in);
    }
  }
  
  private static WritableByteChannel getWriteChannel(URL url) throws IOException {
    if (url.getProtocol().equals("file")) {
      File f = new File(url.getFile());
      f.delete();
      if (!f.exists() && !f.createNewFile())
        throw new IOException("Cannot create file " + f);
      RandomAccessFile raf = new RandomAccessFile(f,"rw");
      return raf.getChannel();
    } else {
      OutputStream out = url.openConnection().getOutputStream();
      return Channels.newChannel(out);
    }
  }
  
  /** Determine and create a feature type.
   */
  private FeatureType getFeatureType(DbaseFileReader dbf,ShapefileReader shp) throws IOException, DataSourceException {
    ShapeType type = shp.getHeader().getShapeType();
    AttributeType geometryAttribute = new org.geotools.feature.AttributeTypeDefault(type.toString(), Geometry.class);
    FeatureType shapefileType;
    if(dbf != null) {
      DbaseFileHeader header = dbf.getHeader();
      
      AttributeType[] types = new AttributeType[ header.getNumFields() + 1];
      types[0] = geometryAttribute;
      for (int i = 1, ii = types.length; i < ii; i++) {
        char c = header.getFieldType(i - 1);
        String name = header.getFieldName(i - 1);
        Class clazz = void.class;
        switch (c) {
          // L,C,D,N,F
          case 'l': case 'L':
            clazz = Boolean.class;
            break;
          case 'c': case 'C':
            clazz = String.class;
            break;
          case 'd': case 'D':
            clazz = java.util.Date.class;
            break;
          case 'n': case 'N':
            if (header.getFieldDecimalCount(i-1) > 0)
              clazz = Double.class;
            else              
              clazz = Integer.class;
            break;
          case 'f': case 'F':
            clazz = Double.class;
            break;
        }
        types[i] = new org.geotools.feature.AttributeTypeDefault(name, clazz);
      }
      try{
        shapefileType = new org.geotools.feature.FeatureTypeFlat(types);
      }
      catch(org.geotools.feature.SchemaException se){
        throw new DataSourceException("Schema Error",se);
      }
    }
    // no dbf file, just return a geometry feature type...
    else {
      shapefileType = new org.geotools.feature.FeatureTypeFlat(geometryAttribute);
    }
    shapefileType = shapefileType.setTypeName(type.toString());
    return shapefileType;
  }
  
  
  
  
  
  
  /** An iterator-like class to encapsulate the multi-plexing of the dbf and
   * shape file reading. Fields and class are final for optimization.
   *
   * This should be part of the general package for parsing shapefiles.
   */
  static final class FeatureMaker {
    
    final DbaseFileReader dbf;
    final ShapefileReader shp;
    final FeatureFactory factory;
    final Object[] stash;
    
    // if the dbf is null, we create a 1 length object array,
    // otherwise it is dbf.numFields + 1
    // the extra is for geometry!
    public FeatureMaker(DbaseFileReader dbf,ShapefileReader shp,FeatureFactory factory) {
      this.dbf = dbf;
      this.shp = shp;
      this.factory = factory;
      if (dbf != null) {
        stash = new Object[dbf.getHeader().getNumFields() + 1];
      }
      else {
        stash = new Object[1];
      }
      
    }
    
    public boolean hasNext() throws IOException {
      int both = (shp.hasNext() ? 1 : 0) + (dbf.hasNext() ? 2 : 0);
      if (both == 3)
        return true;
      else if (both == 0)
        return false;
      throw new IllegalStateException(
        (both == 1 ? "shape" : "dbf") + "file has extra record"
      );
    }
    
    public Feature next() throws IOException, IllegalFeatureException {
      // read the geometry
      stash[0] = shp.nextRecord().shape();
      
      // dbf is not null, read the rest of the features
      // System.out.println(current);
      if (dbf != null) {
        dbf.readEntry(stash,1);
      }
      
      // becuase I know that FeatureFlat copies the array,
      // I've chosen to reuse it.
      // This could be changed.
      return factory.create(stash);
    }
    
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  /**
   * Overwrites the file writing the feature passed as parameters
   * @param collection - the collection to be written
   */
  public void setFeatures(org.geotools.feature.FeatureCollection collection)
  throws DataSourceException {
    
    try {
      // create a good geometry collection
      // this gc will be a collection of either multi-points, multi-polygons, or multi-linestrings
      // polygons will have the rings in the correct order
      GeometryCollection gc = makeShapeGeometryCollection(collection);
      
      // guess shape dimensions
      int shapeDims = 2;
      if(gc.getNumGeometries() > 0)
        shapeDims = JTSUtilities.guessCoorinateDims(gc.getGeometryN(0).getCoordinates());
      
      ShapefileWriter writer = new ShapefileWriter(getWriteChannel(shpURL),getWriteChannel(shxURL));
      
      writer.write(gc, JTSUtilities.getShapeType(gc.getGeometryN(0), shapeDims));
      writeDbf(collection);
    } catch(Exception e) {
      throw new DataSourceException("Something went wrong during shapefile saving", e);
    }
  }
  
  
  /**
   * Write a dbf file with the information from the featureCollection.
   * @param featureCollection column data from collection
   * @param fname name of the dbf file to write to
   */
  private void writeDbf(FeatureCollection featureCollection) throws DbaseFileException,IOException {
    // precondition: all features have the same schema
    Feature[] features = featureCollection.getFeatures();
    AttributeType[] types = features[0].getSchema().getAttributeTypes();
    
    // compute how many supported attributes are there.
    // TODO: handle Calendar, BigDecimal and BigInteger as well
    int numAttributes = 0;
    
    for(int i = 0; i < types.length; i++) {
      Class currType = types[i].getType();
      
      if((currType == String.class) || (currType == Boolean.class) ||
      Number.class.isAssignableFrom(currType) ||
      Date.class.isAssignableFrom(currType))
        numAttributes++;
      else if(Geometry.class.isAssignableFrom(currType)) {
        // do nothing
      } else {
        throw new DbaseFileException(
          "Shapefile: unsupported type found in feature schema : " + 
          currType.getName()
        );
      }
    }
    
    DbaseFileHeader header = new DbaseFileHeader();
    
    for(int i = 0; i < types.length; i++) {
      Class colType = types[i].getType();
      String colName = types[i].getName();
      
      if((colType == Integer.class) || (colType == Short.class) || (colType == Byte.class)) {
        header.addColumn(colName, 'N', 16, 0);
      } else if((colType == Double.class) || (colType == Float.class)) {
        header.addColumn(colName, 'N', 33, 16);
      } else if(Date.class.isAssignableFrom(colType)) {
        header.addColumn(colName, 'D', 8, 0);
      } else if(colType == String.class) {
        int maxlength = findMaxStringLength(featureCollection, i);
        
        if(maxlength > 255) {
          throw new DbaseFileException(
          "Shapefile does not support strings longer than 255 characters");
        }
        
        header.addColumn(colName, 'C', maxlength, 0);
      } else if (Geometry.class.isAssignableFrom(colType)) {
        continue;
      } else {
        throw new DbaseFileException(
        "Unable to write : " + colType.getName());
      }
    }
    header.setNumRecords(features.length);
    
    // write header
    DbaseFileWriter dbf = new DbaseFileWriter(header,getWriteChannel(dbfURL));
    
    // write rows. Prepare calendar object for null dates
    Calendar nullCal = Calendar.getInstance();
    nullCal.clear();
    for(int i = 0; i < features.length; i++) {
      Feature feature = features[i];
      Object[] DBFrow = new Object[numAttributes];
      Object[] atts = feature.getAttributes();
      
      // make data for each column in this feature (row)
      int f = 0;
      for(int j = 0; j < atts.length; j++) {
        Class colType = types[j].getType();
        
        if(colType == Integer.class) {
          if(atts[j] == null) {
            DBFrow[f] = new Integer(0);
          } else {
            DBFrow[f] = atts[j];
          }
          f++;
          
        } else if((colType == Short.class) || (colType == Byte.class)) {
          if(atts[j] == null) {
            DBFrow[f] = new Integer(0);
          } else {
            DBFrow[f] = new Integer(((Number) atts[j]).intValue());
          }
          f++;
          
        } else if(colType == Double.class) {
          if(atts[j] == null) {
            DBFrow[f] = new Double(0.0);
          } else {
            DBFrow[f] = atts[j];
          }
          f++;
          
        } else if(colType == Float.class) {
          if(atts[j] == null) {
            DBFrow[f] = new Double(0.0);
          } else {
            DBFrow[f] = new Double(((Number) atts[j]).doubleValue());
          }
          f++;
          
        } else if(colType == String.class) {
          if(atts[j] == null) {
            DBFrow[f] = new String("");
          } else {
            if(atts[j] instanceof String)
              DBFrow[f] = atts[j];
            else
              DBFrow[f] = atts[j].toString();
          }
          f++;
        } else if(Date.class.isAssignableFrom(colType)) {
          if(atts[j] == null) {
            DBFrow[f] = nullCal.getTime();
          } else {
            if(atts[j] instanceof Date)
              DBFrow[f] = atts[j];
          }
          f++;
        }
        
      }
      dbf.write(DBFrow);
    }
    
    dbf.close();
  }
  
  
  /**
   *look at all the data in the column of the featurecollection, and find the largest string!
   *@param fc features to look at
   *@param attributeNumber which of the column to test.
   */
  private int findMaxStringLength(FeatureCollection fc, int attributeNumber) {
    Feature[] features = fc.getFeatures();
    
    int maxlen = 0;
    
    for(int i = 0; i < features.length; i++) {
      String s = (String) (features[i].getAttributes())[attributeNumber];
      int len = s.length();
      
      if(len > maxlen) {
        maxlen = len;
      }
    }
    
    return maxlen;
  }
  
  
  
  
  
  /**
   * return a single geometry collection <Br>
   *  result.GeometryN(i) = the i-th feature in the FeatureCollection<br>
   *   All the geometry types will be the same type (ie. all polygons) - or they will be set to<br>
   *     NULL geometries<br>
   *<br>
   * GeometryN(i) = {Multipoint,Multilinestring, or Multipolygon)<br>
   *
   *@param fc feature collection to make homogeneous
   */
  public GeometryCollection makeShapeGeometryCollection(FeatureCollection fc) throws DataSourceException {
    GeometryCollection result;
    Feature[] features = fc.getFeatures();
    Geometry[] allGeoms = new Geometry[features.length];
    
    final ShapeType type = JTSUtilities.findBestGeometryType(features[0].getDefaultGeometry());
    
    if (type == ShapeType.NULL) {
      throw new DataSourceException(
      "Could not determine shapefile type - data is either all GeometryCollections or empty");
    }
    
    for(int t = 0; t < features.length; t++) {
      Geometry geom;
      geom = features[t].getDefaultGeometry();
      
      if (type == ShapeType.POINT) {
          
          if((geom instanceof Point)) {
            allGeoms[t] = geom;
          } else {
            allGeoms[t] = new MultiPoint(null, new PrecisionModel(), 0);
          }
          
      } else if (type == ShapeType.ARC) {
          
          if((geom instanceof LineString)) {
            LineString[] l = new LineString[1];
            l[0] = (LineString) geom;
            
            allGeoms[t] = new MultiLineString(l, new PrecisionModel(), 0);
          } else if(geom instanceof MultiLineString) {
            allGeoms[t] = geom;
          } else {
            allGeoms[t] = new MultiLineString(null, new PrecisionModel(), 0);
          }
      } else if (type == ShapeType.POLYGON) {
          
          if(geom instanceof Polygon) {
            //good!
            Polygon[] p = new Polygon[1];
            p[0] = (Polygon) geom;
            
            allGeoms[t] = JTSUtilities.makeGoodShapeMultiPolygon(new MultiPolygon(p,
            geom.getPrecisionModel(),geom.getSRID()));
          } else if(geom instanceof MultiPolygon) {
            allGeoms[t] = JTSUtilities.makeGoodShapeMultiPolygon((MultiPolygon) geom);
          } else {
            allGeoms[t] = new MultiPolygon(null, geom.getPrecisionModel(),geom.getSRID());
          }
          
      }  else if (type == ShapeType.MULTIPOINT) {
          
          if((geom instanceof Point)) {
            Point[] p = new Point[1];
            p[0] = (Point) geom;
            
            allGeoms[t] = new MultiPoint(p, geom.getPrecisionModel(),geom.getSRID());
          } else if(geom instanceof MultiPoint) {
            allGeoms[t] = geom;
          } else {
            allGeoms[t] = new MultiPoint(null, geom.getPrecisionModel(),geom.getSRID());
          }
          
         
      }
    } // end big crazy for loop
    
    result = new GeometryCollection(allGeoms, allGeoms[0].getPrecisionModel(),allGeoms[0].getSRID());
    
    return result;
  }
  
  
  // Just a Test
  public static final void main(String[] args) throws Exception {
    File src = new File(args[0]);
    ShapefileDataSource ds = new ShapefileDataSource(src.toURL());
    FeatureCollection features = ds.getFeatures(null);
    Feature[] f = features.getFeatures();
    for (int i = 0, ii = f.length; i < ii; i++) {
      System.out.println(f[i]);
    }
      
    
    
  }
}
