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
package org.geotools.shapefile.shapefile;

import com.vividsolutions.jts.geom.*;
import java.nio.ByteBuffer;
import java.io.*;


/**
 * Wrapper for a Shapefile point.
 *
 * @author aaime
 * @author Ian Schneider
 * @version
 */
public class PointHandler implements ShapeHandler {

  final ShapeType shapeType;
  GeometryFactory geometryFactory = new GeometryFactory();
  
  public PointHandler(ShapeType type) throws InvalidShapefileException {
    if ((type != ShapeType.POINT) && (type != ShapeType.POINTM) && (type != ShapeType.POINTZ)) { // 2d, 2d+m, 3d+m
      throw new InvalidShapefileException(
      "PointHandler constructor: expected a type of 1, 11 or 21");
    }
    
    shapeType = type;
  }
  
  public PointHandler() {
    shapeType = ShapeType.POINT; //2d
  }
  
  /**
   * Returns the shapefile shape type value for a point
   * @return int Shapefile.POINT
   */
  public ShapeType getShapeType() {
    return shapeType;
  }
  
  
  public int getLength(Object geometry) {
    if (shapeType == ShapeType.POINT)
      return 20;
    if (shapeType == ShapeType.POINTM)
      return 28;
    if (shapeType == ShapeType.POINTZ)
      return 36;
    throw new IllegalStateException("Expected ShapeType of Point, got" + shapeType);
  }
  
  public Object read(ByteBuffer buffer, ShapeType type) {
    if (type == ShapeType.NULL)
      return createNull();
    
    double x = buffer.getDouble();
    double y = buffer.getDouble();
    double m;
    double z = Double.NaN;
    
    if (shapeType == ShapeType.POINTM) {
      z = buffer.getDouble();
    }
    
    if (shapeType == ShapeType.POINTZ) {
      m = buffer.getDouble();
    }
    
    return geometryFactory.createPoint(new Coordinate(x, y, z));
  }
  
  private Object createNull() {
    return geometryFactory.createPoint(new Coordinate(Double.NaN,Double.NaN,Double.NaN));
  }
  
  public void write(ByteBuffer buffer, Object geometry) {
    Coordinate c = ((Point) geometry).getCoordinate();
    
    buffer.putDouble(c.x);
    buffer.putDouble(c.y);
    
    if (shapeType == ShapeType.POINTZ) {
      if (Double.isNaN(c.z)) { // nan means not defined
        buffer.putDouble(0.0);
      } else {
        buffer.putDouble(c.z);
      }
    }
    
    if ((shapeType == ShapeType.POINTZ) || (shapeType == ShapeType.POINTM)) {
      buffer.putDouble(-10E40); //M
    }
  }
  
}
