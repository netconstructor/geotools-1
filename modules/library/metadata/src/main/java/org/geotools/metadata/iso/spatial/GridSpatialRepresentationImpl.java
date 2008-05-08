/*
 *    GeoTools - OpenSource mapping toolkit
 *    http://geotools.org
 *    (C) 2004-2006, GeoTools Project Managment Committee (PMC)
 *    (C) 2004, Institut de Recherche pour le Développement
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
 *    This package contains documentation from OpenGIS specifications.
 *    OpenGIS consortium's work is fully acknowledged here.
 */
package org.geotools.metadata.iso.spatial;

import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import org.opengis.metadata.spatial.Dimension;
import org.opengis.metadata.spatial.CellGeometry;
import org.opengis.metadata.spatial.GridSpatialRepresentation;


/**
 * Basic information required to uniquely identify a resource or resources.
 *
 * @source $URL$
 * @version $Id$
 * @author Martin Desruisseaux
 * @author Touraïvane
 *
 * @since 2.1
 */
@XmlType(name = "MD_GridSpatialRepresentation", propOrder={"numberOfDimensions",
    "axisDimensionsProperties", "cellGeometry", "transformationParameterAvailable"
})
@XmlSeeAlso({GeorectifiedImpl.class, GeoreferenceableImpl.class})
@XmlRootElement(name = "MD_GridSpatialRepresentation")
public class GridSpatialRepresentationImpl extends SpatialRepresentationImpl
        implements GridSpatialRepresentation
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -8400572307442433979L;

    /**
     * Number of independent spatial-temporal axes.
     */
    private Integer numberOfDimensions;

    /**
     * Information about spatial-temporal axis properties.
     */
    private List<Dimension> axisDimensionsProperties;

    /**
     * Identification of grid data as point or cell.
     */
    private CellGeometry cellGeometry;

    /**
     * Indication of whether or not parameters for transformation exists.
     */
    private boolean transformationParameterAvailable;

    /**
     * Constructs an initially empty grid spatial representation.
     */
    public GridSpatialRepresentationImpl() {
    }

    /**
     * Constructs a metadata entity initialized with the values from the specified metadata.
     *
     * @since 2.4
     */
    public GridSpatialRepresentationImpl(final GridSpatialRepresentation source) {
        super(source);
    }

    /**
     * Creates a grid spatial representation initialized to the given values.
     * <p>
     * <b>Note:</b> this is a convenience constructor. The argument types don't need to
     * match exactly the types expected by getters and setters.
     */
    public GridSpatialRepresentationImpl(final int numberOfDimensions,
                                         final List<? extends Dimension> axisDimensionsProperties,
                                         final CellGeometry cellGeometry,
                                         final boolean transformationParameterAvailable)
    {
        setNumberOfDimensions               (numberOfDimensions);
        setAxisDimensionsProperties         (axisDimensionsProperties);
        setCellGeometry                     (cellGeometry);
        setTransformationParameterAvailable (transformationParameterAvailable);
    }

    /**
     * Number of independent spatial-temporal axes.
     */
    @XmlElement(name = "numberOfDimensions", required = true, 
                namespace = "http://www.isotc211.org/2005/gmd")
    public Integer getNumberOfDimensions() {
        return numberOfDimensions;
    }

    /**
     * Set the number of independent spatial-temporal axes.
     */
    public synchronized void setNumberOfDimensions(final Integer newValue) {
        checkWritePermission();
        numberOfDimensions = newValue;
    }

    /**
     * Information about spatial-temporal axis properties.
     */
    @XmlElement(name = "axisDimensionProperties", required = true,
                namespace = "http://www.isotc211.org/2005/gmd")
    public synchronized List<Dimension> getAxisDimensionsProperties() {
        return axisDimensionsProperties = nonNullList(axisDimensionsProperties, Dimension.class);
    }

    /**
     * Set information about spatial-temporal axis properties.
     */
    public synchronized void setAxisDimensionsProperties(final List<? extends Dimension> newValues) {
        checkWritePermission();
        axisDimensionsProperties = (List<Dimension>)
                copyCollection(newValues, axisDimensionsProperties, Dimension.class);
    }

    /**
     * Identification of grid data as point or cell.
     */
    @XmlElement(name = "cellGeometry", required = true,
                namespace = "http://www.isotc211.org/2005/gmd")
    public CellGeometry getCellGeometry() {
        return cellGeometry;
    }

    /**
     * Set identification of grid data as point or cell.
     */
    public synchronized void setCellGeometry(final CellGeometry newValue) {
        checkWritePermission();
        cellGeometry = newValue;
    }

    /**
     * Indication of whether or not parameters for transformation exists.
     */
    @XmlElement(name = "transformationParameterAvailability", required = true,
                namespace = "http://www.isotc211.org/2005/gmd")
    public boolean isTransformationParameterAvailable() {
        return transformationParameterAvailable;
    }

    /**
     * Set indication of whether or not parameters for transformation exists.
     */
    public synchronized void setTransformationParameterAvailable(final boolean newValue) {
        checkWritePermission();
        transformationParameterAvailable = newValue;
    }
}
