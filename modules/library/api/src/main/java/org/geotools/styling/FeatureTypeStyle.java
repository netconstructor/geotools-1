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
 */
package org.geotools.styling;

import java.util.List;


/**
 * How to style a feature type.  This is introduced as a convenient package
 * that can be used independently for feature types, for example in
 * GML Default Styling.  The "layer" concept is discarded inside of this
 * element and all processing is relative to feature types.
 * The FeatureTypeName is allowed to be optional, but only one feature
 * type may be in context and it must match the syntax and semantics of all
 * attribute references inside of the FeatureTypeStyle.
 * <p>
 * The details of this object are taken from the
 * <a href="https://portal.opengeospatial.org/files/?artifact_id=1188">
 * OGC Styled-Layer Descriptor Report (OGC 02-070) version 1.0.0.</a>:
 * <pre><code>
 * &lt;xsd:element name="FeatureTypeStyle"&gt;
 * &lt;xsd:annotation&gt;
 *   &lt;xsd:documentation&gt;
 *     A FeatureTypeStyle contains styling information specific to one
 *    feature type.  This is the SLD level that separates the 'layer'
 *     handling from the 'feature' handling.
 *   &lt;/xsd:documentation&gt;
 *   &lt;/xsd:annotation&gt;
 *   &lt;xsd:complexType&gt;
 *     &lt;xsd:sequence&gt;
 *       &lt;xsd:element ref="sld:Name" minOccurs="0"/&gt;
 *       &lt;xsd:element ref="sld:Title" minOccurs="0"/&gt;
 *       &lt;xsd:element ref="sld:Abstract" minOccurs="0"/&gt;
 *       &lt;xsd:element ref="sld:FeatureTypeName" minOccurs="0"/&gt;
 *       &lt;xsd:element ref="sld:SemanticTypeIdentifier" minOccurs="0"
 *                   maxOccurs="unbounded"/&gt;
 *       &lt;xsd:element ref="sld:Rule" maxOccurs="unbounded"/&gt;
 *     &lt;/xsd:sequence&gt;
 *   &lt;/xsd:complexType&gt;
 * &lt;/xsd:element&gt;
 * </code></pre>
 *
 * @source $URL$
 * @version $Id$
 * @author James Macgill, CCG
 */
public interface FeatureTypeStyle {
    public String getName();

    void setName(String name);

    public String getTitle();

    void setTitle(String title);

    public String getAbstract();

    void setAbstract(String abstractStr);

    /**
     * Only features with the type name returned by this method should
     * be styled by this feature type styler.
     * @return The name of types that this styler applies to
     */
    String getFeatureTypeName();

    /**
     * Sets the type name of the features that this styler should be
     * applied to.
     * @task REVISIT: should a set method be declared in this interface at all?
     * @param name The TypeName of the features to be styled by this instance.
     */
    void setFeatureTypeName(String name);

    /**
     * The SemanticTypeIdentifiers is experimental and is intended to be used
     * to identify, using a community-controlled name(s), what the style is
     * suitable to be used for.
     * For example, a single style may be suitable to use with many
     * different feature types.  The syntax of the SemanticTypeIdentifiers
     * string is undefined, but the strings "generic:line_string",
     * "generic:polygon", "generic:point", "generic:text",
     * "generic:raster", and "generic:any" are reserved to indicate
     * that a FeatureTypeStyle may be used with any feature type
     * with the corresponding default geometry type (i.e., no feature
     * properties are referenced in the feature type style).
     *
     *
     * @return An array of strings representing systematic types which
     *         could be styled by this instance.
     **/
    String[] getSemanticTypeIdentifiers();

    /**
     * The SemanticTypeIdentifiers is experimental and is intended to be used
     * to identify, using a community-controlled name(s), what the style is
     * suitable to be used for.
     * For example, a single style may be suitable to use with many
     * different feature types.  The syntax of the SemanticTypeIdentifiers
     * string is undefined, but the strings "generic:line_string",
     * "generic:polygon", "generic:point", "generic:text",
     * "generic:raster", and "generic:any" are reserved to indicate
     * that a FeatureTypeStyle may be used with any feature type
     * with the corresponding default geometry type (i.e., no feature
     * properties are referenced in the feature type style).
     *
     *
     * @param types An array of strings representing systematic types which
     *         could be styled by this instance.
     **/
    void setSemanticTypeIdentifiers(String[] types);

    /**
     * Rules govern the appearance of any given feature to be styled by
     * this styler.  Each rule contains conditions based on scale and
     * feature attribute values.  In addition, rules contain the symbolizers
     * which should be applied when the rule holds true.
     *
     * @version SLD 1.0
     * @version SLD 1.0.20 TODO: GeoAPI getRules(): List<Rule>
     * @return The full set of rules contained in this styler.
     */
    Rule[] getRules();

    /**
     * Rules govern the appearance of any given feature to be styled by
     * this styler.  Each rule contains conditions based on scale and
     * feature attribute values.  In addition, rules contain the symbolizers
     * which should be applied when the rule holds true.
     *
     * @param rules The set of rules to be set for this styler.
     */
    void setRules(Rule[] rules);

    void addRule(Rule rule);

    /**
     * Rules govern the appearance of any given feature to be styled by
     * this styler.
     * <p>
     * This is *the* list being used to mange the rules!
     * </p>
     * @since GeoTools 2.2.M3, GeoAPI 2.0
     */
    List<Rule> rules();

    void accept(StyleVisitor visitor);
}
