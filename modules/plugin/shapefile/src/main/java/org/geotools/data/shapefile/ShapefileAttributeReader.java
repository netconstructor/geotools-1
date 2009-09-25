/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2008, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.data.shapefile;

import java.io.IOException;
import java.util.List;

import org.geotools.data.AbstractAttributeIO;
import org.geotools.data.AttributeReader;
import org.geotools.data.shapefile.dbf.DbaseFileHeader;
import org.geotools.data.shapefile.dbf.DbaseFileReader;
import org.geotools.data.shapefile.shp.ShapeType;
import org.geotools.data.shapefile.shp.ShapefileReader;
import org.geotools.data.shapefile.shp.ShapefileReader.Record;
import org.opengis.feature.type.AttributeDescriptor;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * An AttributeReader implementation for Shapefile. Pretty straightforward.
 * <BR/>The default geometry is at position 0, and all dbf columns follow.
 * <BR/>The dbf file may not be necessary, if not, just pass null as the
 * DbaseFileReader
 */
public class ShapefileAttributeReader extends AbstractAttributeIO implements
        AttributeReader {

    protected ShapefileReader shp;
    protected DbaseFileReader dbf;
    protected DbaseFileReader.Row row;
    protected ShapefileReader.Record record;
    int cnt;
    int[] dbfindexes;
    protected Envelope targetBBox;
    double simplificationDistance;

    public ShapefileAttributeReader(List<AttributeDescriptor> atts,
            ShapefileReader shp, DbaseFileReader dbf) {
        this(atts.toArray(new AttributeDescriptor[0]), shp, dbf);
    }
    
    /**
     * Sets a search area. If the geometry does not fall into it
     * it won't be read and will return a null geometry instead 
     * @param envelope
     */
    public void setTargetBBox(Envelope envelope) {
        this.targetBBox = envelope;
    }
    
    public void setSimplificationDistance(double distance) {
        this.simplificationDistance = distance;
    }

    /**
     * Create the shapefile reader
     * 
     * @param atts -
     *                the attributes that we are going to read.
     * @param shp -
     *                the shapefile reader, required
     * @param dbf -
     *                the dbf file reader. May be null, in this case no
     *                attributes will be read from the dbf file
     */
    public ShapefileAttributeReader(AttributeDescriptor[] atts,
            ShapefileReader shp, DbaseFileReader dbf) {
        super(atts);
        this.shp = shp;
        this.dbf = dbf;
        
        if(dbf != null) {
            dbfindexes = new int[atts.length];
            DbaseFileHeader head = dbf.getHeader();
            for (int i = 1; i < atts.length; i++) {
                String attName = atts[i].getLocalName();
                for(int j = 0; j < head.getNumFields(); j++) {
                    if(head.getFieldName(j).equals(attName))
                        dbfindexes[i] = j;
                }
            }
        }
    }

    public void close() throws IOException {
        try {
            if (shp != null) {
                shp.close();
            }

            if (dbf != null) {
                dbf.close();
            }
        } finally {
            row = null;
            record = null;
            shp = null;
            dbf = null;
        }
    }

    public boolean hasNext() throws IOException {
        int n = shp.hasNext() ? 1 : 0;

        if (dbf != null) {
            n += (dbf.hasNext() ? 2 : 0);
        }

        if ((n == 3) || ((n == 1) && (dbf == null))) {
            return true;
        }

        if (n == 0) {
            return false;
        }

        throw new IOException(((n == 1) ? "Shp" : "Dbf") + " has extra record");
    }

    public void next() throws IOException {
        record = shp.nextRecord();

        if (dbf != null) {
            row = dbf.readRow();
        }
    }

    public Object read(int param) throws IOException,
            java.lang.ArrayIndexOutOfBoundsException {
        switch (param) {
        case 0:
            Envelope envelope = record.envelope();
            if(targetBBox != null && !targetBBox.isNull() && !targetBBox.intersects(envelope))
                    return null;
            else if(simplificationDistance > 0 && envelope.getWidth() < simplificationDistance && envelope.getHeight() < simplificationDistance )
                return record.getSimplifiedShape();
            else
                return record.shape();

        default:

            if (row != null) {
                return row.read(dbfindexes[param]);
            }

            return null;
        }
    }
}
