/*
 *    GeoTools - The Open Source Java GIS Tookit
 *    http://geotools.org
 * 
 *    (C) 2002-2008, Open Source Geospatial Foundation (OSGeo)
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
 * Contacts:
 *     UNITED KINGDOM: James Macgill.  j.macgill@geog.leeds.ac.uk
 */
package org.geotools.styling;


/**
 * A symbolizer describes how a feature should appear on a map.
 *
 * <p>
 * The symbolizer defines not just the shape that should appear but also  such
 * graphical properties as color and opacity.
 * </p>
 *
 * <p>
 * A symbolizer is obtained by specifying one of a small number of different
 * types of symbolizer and then supplying parameters to overide its default
 * behaviour.
 * </p>
 *
 * <p>
 * The details of this object are taken from the <a
 * href="https://portal.opengeospatial.org/files/?artifact_id=1188"> OGC
 * Styled-Layer Descriptor Report (OGC 02-070) version 1.0.0.</a>:
 * <pre><code>
 * &lt;xsd:element name="PointSymbolizer" substitutionGroup="sld:Symbolizer">
 *   &lt;xsd:annotation>
 *     &lt;xsd:documentation>
 *       A "PointSymbolizer" specifies the rendering of a "graphic symbol"
 *       at a point.
 *     &lt;/xsd:documentation>
 *   &lt;/xsd:annotation>
 *   &lt;xsd:complexType>
 *     &lt;xsd:complexContent>
 *       &lt;xsd:extension base="sld:SymbolizerType">
 *         &lt;xsd:sequence>
 *           &lt;xsd:element ref="sld:Geometry" minOccurs="0"/>
 *           &lt;xsd:element ref="sld:Graphic" minOccurs="0"/>
 *         &lt;/xsd:sequence>
 *       &lt;/xsd:extension>
 *     &lt;/xsd:complexContent>
 *   &lt;/xsd:complexType>
 * &lt;/xsd:element>
 * </code></pre>
 * </p>
 *
 * <p>
 * Renderers can use this information when displaying styled features, though
 * it must be remembered that not all renderers will be able to fully
 * represent strokes as set out by this interface.  For example, opacity may
 * not be supported.
 * </p>
 *
 * <p>
 * Notes:
 *
 * <ul>
 * <li>
 * The graphical parameters and their values are derived from SVG/CSS2
 * standards with names and semantics which are as close as possible.
 * </li>
 * </ul>
 * </p>
 *
 * @author James Macgill
 * @source $URL$
 * @version $Id$
 */
public interface PointSymbolizer extends Symbolizer {
    /**
     * Provides the graphical-symbolization parameter to use for the  point
     * geometry.
     *
     * @return The Graphic to be used when drawing a point.
     */
    Graphic getGraphic();

    /**
     * Provides the graphical-symbolization parameter to use for the  point
     * geometry.
     *
     * @param graphic DOCUMENT ME!
     */
    void setGraphic(Graphic graphic);

    /**
     * This property defines the geometry to be used for styling.<br>
     * The property is optional and if it is absent (null) then the "default"
     * geometry property of the feature should be used.  Geometry types other
     * than inherently point types can be used.  The geometryPropertyName is
     * the name of a geometry property in the Feature being styled.
     * Typically, features only have one geometry so, in general, the need to
     * select one is not required. Note: this moves a little away from the SLD
     * spec which provides an XPath reference to a Geometry object, but does
     * follow it in spirit.
     *
     * @return String The name of the attribute in the feature being styled
     *         that should be used.  If null then the default geometry should
     *         be used.
     */
    String getGeometryPropertyName();

    /**
     * This property defines the geometry to be used for styling.<br>
     * The property is optional and if it is absent (null) then the "default"
     * geometry property of the feature should be used.  Geometry types other
     * than inherently point types can be used.  The geometryPropertyName is
     * the name of a geometry property in the Feature being styled.
     * Typically, features only have one geometry so, in general, the need to
     * select one is not required. Note: this moves a little away from the SLD
     * spec which provides an XPath reference to a Geometry object, but does
     * follow it in spirit.
     *
     * @param geometryPropertyName DOCUMENT ME!
     */
    void setGeometryPropertyName(String geometryPropertyName);
}
