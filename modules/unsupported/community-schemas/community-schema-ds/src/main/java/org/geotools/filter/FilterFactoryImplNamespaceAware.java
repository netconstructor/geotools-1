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

package org.geotools.filter;

import org.geotools.factory.Hints;
import org.geotools.factory.Hints.Key;
import org.opengis.filter.expression.PropertyName;
import org.xml.sax.helpers.NamespaceSupport;

public class FilterFactoryImplNamespaceAware extends FilterFactoryImpl {

    public static final Key NAMESPACE_CONTEXT = new Hints.Key(
            org.xml.sax.helpers.NamespaceSupport.class);

    private Hints namespaceHints;

    /**
     * Empty constructor, no namespace context received, behaves exactly like
     * {@link FilterFactoryImpl}
     */
    public FilterFactoryImplNamespaceAware() {
        super();
    }

    public FilterFactoryImplNamespaceAware(NamespaceSupport namespaces) {
        setNamepaceContext(namespaces);
    }

    // @Override
    public PropertyName property(String name) {
        return new AttributeExpressionImpl(name, namespaceHints);
    }

    public void setNamepaceContext(NamespaceSupport namespaces) {
        namespaceHints = new Hints(NAMESPACE_CONTEXT, namespaces);
    }
}
