/*
 *    Geotools2 - OpenSource mapping toolkit
 *    http://geotools.org
 *    (C) 2002, Geotools Project Managment Committee (PMC)
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
package org.geotools.filter;

import com.esri.sde.sdk.client.*;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;
import org.geotools.data.sde.GeometryBuilder;
import org.geotools.data.sde.GeometryBuildingException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


/**
 * Encodes the geometry and FID related parts of a filter into a set of
 * <code>SeFilter</code> objects and provides a method to get the resulting
 * filters suitable to set up an SeQuery's spatial constraints.
 *
 * <p>
 * Although not all filters support is coded yet, the strategy to filtering
 * queries for ArcSDE datasources is separated in two parts, the SQL where
 * clause construction, provided by <code>SQLEncoderSDE</code> and the spatial
 * filters (or spatial constraints, in SDE vocabulary) provided here;
 * mirroring the java SDE api approach
 * </p>
 *
 * @author Gabriel Rold�n
 */
public class GeometryEncoderSDE implements org.geotools.filter.FilterVisitor
{
    /** Standard java logger */
    private static Logger log = Logger.getLogger("org.geotools.filter");

    /** DOCUMENT ME! */
    private static FilterCapabilities capabilities = new FilterCapabilities();

    static
    {
        /*
           capabilities.addType(AbstractFilter.LOGIC_AND);
           capabilities.addType(AbstractFilter.LOGIC_OR);
           capabilities.addType(AbstractFilter.LOGIC_NOT);
         */
        capabilities.addType(AbstractFilter.GEOMETRY_BBOX);
        capabilities.addType(AbstractFilter.GEOMETRY_INTERSECTS);
        capabilities.addType(AbstractFilter.FID);
    }

    /** DOCUMENT ME! */
    private List sdeSpatialFilters = null;

    /** DOCUMENT ME! */
    private SeLayer sdeLayer;

    /**
     */
    public GeometryEncoderSDE()
    {
    }

    /**
     * DOCUMENT ME!
     *
     * @param layer DOCUMENT ME!
     */
    public void setLayer(SeLayer layer)
    {
        this.sdeLayer = layer;
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public static FilterCapabilities getCapabilities()
    {
        return capabilities;
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public SeFilter[] getSpatialFilters()
    {
        SeFilter[] filters = new SeFilter[sdeSpatialFilters.size()];

        return (SeFilter[]) sdeSpatialFilters.toArray(filters);
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     *
     * @throws IllegalStateException DOCUMENT ME!
     */
    private String getLayerName()
    {
        if (sdeLayer == null)
        {
            throw new IllegalStateException("SDE layer has not been set");
        }

        return sdeLayer.getName();
    }

    /**
     * overriden just to avoid the "WHERE" keyword
     *
     * @param filter DOCUMENT ME!
     *
     * @throws GeometryEncoderException DOCUMENT ME!
     */
    public void encode(Filter filter) throws GeometryEncoderException
    {
        sdeSpatialFilters = new ArrayList();

        if (capabilities.fullySupports(filter))
        {
            filter.accept(this);
        }
        else
        {
            throw new GeometryEncoderException("Filter type not supported");
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param filter DOCUMENT ME!
     *
     * @throws RuntimeException DOCUMENT ME!
     */
    public void visit(GeometryFilter filter)
    {
        if (filter.getFilterType() == AbstractFilter.GEOMETRY_BBOX)
        {
            addBBoxFilter(filter);
        }
        else if (filter.getFilterType() == AbstractFilter.GEOMETRY_INTERSECTS)
        {
            addIntersectsFilter(filter);
        }
        else
        {
            String warning = "exporting unknown filter type, supported filters are:";
            log.warning(warning);

            StringBuffer msg = new StringBuffer(warning).append("BBox")
                                                        .append(", Intersects")
                                                        .append(", FID");
            throw new RuntimeException(msg.toString());
        }
    }

    /**
     * This only exists the fulfill the interface - unless There is a way of
     * determining the FID column in the database...
     *
     * @param filter the Fid Filter.
     */
    public void visit(FidFilter filter)
    {
        String[] fids = filter.getFids();
        SeShapeIdFilter idFilter;
        SeObjectId objectId;

        /**
         * @task TODO: figure out how to establish seaching method based on
         *       filter (search equals, not equals, etc)
         */
        int searchMethod = SeFilter.METHOD_IDENTICAL;
        int nFids = (fids == null) ? 0 : fids.length;

        try
        {
            for (int i = 0; i < nFids; i++)
            {
                objectId = new SeObjectId(getNumericFid(fids[i]));
                idFilter = new SeShapeIdFilter(getLayerName(),
                        sdeLayer.getSpatialColumn(), getLayerName(), objectId,
                        searchMethod);

                sdeSpatialFilters.add(idFilter);
            }
        }
        catch (Exception ex)
        {
          throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    /**
     * Returns the numeric identifier of a FeatureId, given by the full
     * qualified name of the featureclass prepended to the ArcSDE feature id.
     * ej: SDE.SDE.SOME_LAYER.1
     *
     * @param fid a geotools FeatureID
     *
     * @return an ArcSDE feature ID
     *
     * @throws IllegalArgumentException DOCUMENT ME!
     */
    private long getNumericFid(String fid) throws IllegalArgumentException
    {
        int dotIndex = fid.lastIndexOf('.');

        try
        {
            return Long.parseLong(fid.substring(++dotIndex));
        }
        catch (Exception ex)
        {
            throw new IllegalArgumentException("FeatureID " + fid
                + " does not contains a valid ArcSDE FID");
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param filter DOCUMENT ME!
     *
     * @throws RuntimeException DOCUMENT ME!
     */
    private void addBBoxFilter(GeometryFilter filter)
    {
        log.finer("exporting GeometryFilter");

        DefaultExpression left = (DefaultExpression) filter.getLeftGeometry();
        DefaultExpression right = (DefaultExpression) filter.getRightGeometry();

        Geometry bbox = null;
        String spatialCol = sdeLayer.getSpatialColumn();

        if ((left != null)
                && (left.getType() == DefaultExpression.LITERAL_GEOMETRY))
        {
            bbox = (Geometry) ((BBoxExpression) left).getLiteral();
        }
        else if ((right != null)
                && (right.getType() == DefaultExpression.LITERAL_GEOMETRY))
        {
            bbox = (Geometry) ((BBoxExpression) right).getLiteral();
        }

        if (bbox != null)
        {
            try
            {
                GeometryBuilder gb = GeometryBuilder.builderFor(Polygon.class);
                //obtain a valid bbox by intersecting the parameter one with
                //the layer's extent, because SDE doesn't like coordinate filters
                //that exceeds the valid layer's range
                SeExtent seExtent = sdeLayer.getExtent();
                SeShape extent = new SeShape(sdeLayer.getCoordRef());
                extent.generateRectangle(seExtent);

                Geometry layerEnv = gb.construct(extent);
                bbox = bbox.intersection(layerEnv);

                SeShape envShape = gb.construct(bbox, sdeLayer.getCoordRef());

                SeFilter bboxFilter = new SeShapeFilter(getLayerName(),
                        spatialCol, envShape, SeFilter.METHOD_ENVP);

                sdeSpatialFilters.add(bboxFilter);
            }
            catch (Exception ex)
            {
                String errMsg = "can't create an envelope sde shape from "
                    + bbox + ". cause: " + ex.getMessage();

                log.warning(errMsg);

                throw new RuntimeException(errMsg, ex);
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param filter DOCUMENT ME!
     *
     * @throws RuntimeException DOCUMENT ME!
     */
    private void addIntersectsFilter(GeometryFilter filter)
    {
        log.finer("exporting GeometryFilter");

        DefaultExpression left = (DefaultExpression) filter.getLeftGeometry();

        DefaultExpression right = (DefaultExpression) filter.getRightGeometry();

        Geometry compareGeometry = null;

        String spatialCol = sdeLayer.getSpatialColumn();

        if ((left != null)
                && (left.getType() == DefaultExpression.LITERAL_GEOMETRY))
        {
            compareGeometry = (Geometry) ((BBoxExpression) left).getLiteral();
        }
        else if ((right != null)
                && (right.getType() == DefaultExpression.LITERAL_GEOMETRY))
        {
            compareGeometry = (Geometry) ((BBoxExpression) right).getLiteral();
        }

        if (compareGeometry != null)
        {
            try
            {
                GeometryBuilder bg = GeometryBuilder.builderFor(Polygon.class);

                SeShape envShape = bg.construct(compareGeometry,
                        sdeLayer.getCoordRef());

                SeFilter bboxFilter = new SeShapeFilter(getLayerName(),
                        spatialCol, envShape, SeFilter.METHOD_ENVP);

                sdeSpatialFilters.add(bboxFilter);
            }
            catch (GeometryBuildingException ex)
            {
                String errMsg = "can't create an envelope sde shape from "
                    + compareGeometry + ". cause: " + ex.getMessage();

                log.warning(errMsg);

                throw new RuntimeException(errMsg, ex);
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param filter DOCUMENT ME!
     */
    public void visit(Filter filter)
    {
    }

    /**
     * DOCUMENT ME!
     *
     * @param filter DOCUMENT ME!
     */
    public void visit(BetweenFilter filter)
    {
    }

    /**
     * DOCUMENT ME!
     *
     * @param filter DOCUMENT ME!
     */
    public void visit(CompareFilter filter)
    {
    }

    /**
     * DOCUMENT ME!
     *
     * @param filter DOCUMENT ME!
     */
    public void visit(LikeFilter filter)
    {
    }

    /**
     * DOCUMENT ME!
     *
     * @param filter DOCUMENT ME!
     */
    public void visit(LogicFilter filter)
    {
    }

    /**
     * DOCUMENT ME!
     *
     * @param filter DOCUMENT ME!
     */
    public void visit(NullFilter filter)
    {
    }

    /**
     * DOCUMENT ME!
     *
     * @param expression DOCUMENT ME!
     */
    public void visit(AttributeExpression expression)
    {
    }

    /**
     * DOCUMENT ME!
     *
     * @param expression DOCUMENT ME!
     */
    public void visit(Expression expression)
    {
    }

    /**
     * DOCUMENT ME!
     *
     * @param expression DOCUMENT ME!
     */
    public void visit(LiteralExpression expression)
    {
    }

    /**
     * DOCUMENT ME!
     *
     * @param expression DOCUMENT ME!
     */
    public void visit(MathExpression expression)
    {
    }

    /**
     * DOCUMENT ME!
     *
     * @param expression DOCUMENT ME!
     */
    public void visit(FunctionExpression expression)
    {
    }
}
