/*
 *    GeoTools - OpenSource mapping toolkit
 *    http://geotools.org
 *    (C) 2003-2006, Geotools Project Managment Committee (PMC)
 *    (C) 2001, Institut de Recherche pour le Développement
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.gui.swing.tree;


/**
 * General-purpose node in a tree data structure. This default implementation implements
 * Geotools {@link MutableTreeNode} interface, which inherits a {@code getUserObject()}
 * method. This method is provided in Swing {@link javax.swing.tree.DefaultMutableTreeNode}
 * implementation but seems to have been forgotten in all Swing interfaces.
 *
 * @since 2.0
 * @source $URL$
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class DefaultMutableTreeNode extends javax.swing.tree.DefaultMutableTreeNode
                                 implements MutableTreeNode
{
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = -8782548896062360341L;

    /**
     * Creates a tree node that has no parent and no children, but which allows children.
     */
    public DefaultMutableTreeNode() {
        super();
    }

    /**
     * Creates a tree node with no parent, no children, but which allows
     * children, and initializes it with the specified user object.
     *
     * @param userObject an Object provided by the user that constitutes the node's data
     */
    public DefaultMutableTreeNode(Object userObject) {
        super(userObject);
    }

    /**
     * Creates a tree node with no parent, no children, initialized with
     * the specified user object, and that allows children only if specified.
     *
     * @param userObject an Object provided by the user that constitutes the node's data
     * @param allowsChildren if true, the node is allowed to have child nodes -- otherwise,
     *        it is always a leaf node
     */
    public DefaultMutableTreeNode(Object userObject, boolean allowsChildren) {
        super(userObject, allowsChildren);
    }
}
