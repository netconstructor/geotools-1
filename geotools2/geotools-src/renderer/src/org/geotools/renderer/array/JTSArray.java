/*
 * Geotools - OpenSource mapping toolkit
 * (C) 2003, Institut de Recherche pour le D�veloppement
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
 *
 *    You should have received a copy of the GNU Lesser General Public
 *    License along with this library; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 * Contacts:
 *     UNITED KINGDOM: James Macgill
 *             mailto:j.macgill@geog.leeds.ac.uk
 *
 *     FRANCE: Surveillance de l'Environnement Assist�e par Satellite
 *             Institut de Recherche pour le D�veloppement / US-Espace
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package org.geotools.renderer.array;

// J2SE dependencies
import java.awt.geom.Point2D;

// JTS dependencies
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Coordinate;

// Geotools dependencies
import org.geotools.resources.XArray;


/**
 * A wrapper around an array of JTS {@link Coordinate}s. This array is
 * usually a reference to the internal array of a {@link LineString} object.
 *
 * @version $Id: JTSArray.java,v 1.2 2003/02/20 11:18:08 desruisseaux Exp $
 * @author Martin Desruisseaux
 */
public final class JTSArray extends PointArray {
    /**
     * Num�ro de s�rie (pour compatibilit� avec des versions ant�rieures).
     */
    private static final long serialVersionUID = 5944964058006239460L;

    /**
     * The coordinates. This is usually a reference to an internal
     * array of {@link LineString}. <strong>Do not modify</strong>.
     */
    private final Coordinate[] coords;

    /**
     * Range of valid coordinates in {@link #coords}. This range goes from <code>lower</code>
     * inclusive to <code>upper</code> exclusive. <strong>Note:</strong> Methods {@link #lower()}
     * and {@link #upper()} returns twice those values, because of {@link PointArray} specification.
     */
    private final int lower, upper;

    /**
     * Construct a new <code>JTSArray</code> for the specified coordinate points.
     */
    public JTSArray(final Coordinate[] coords) {
        this.coords = coords;
        this.lower  = 0;
        this.upper  = coords.length;
    }

    /**
     * Construct a new <code>JTSArray</code> which is a sub-array of the specified coordinates.
     */
    public JTSArray(final Coordinate[] coords, final int lower, final int upper) {
        this.coords = coords;
        this.lower  = lower;
        this.upper  = upper;
    }

    /**
     * Returns 2&times;{@link #lower}.
     */
    final int lower() {
        return lower << 1;
    }

    /**
     * Returns 2&times;{@link #upper}.
     */
    final int upper() {
        return upper << 1;
    }

    /**
     * Returns an estimation of memory usage in bytes. This method count 32 bytes for each
     * {@link Coordinate} object plus 12 bytes for internal fields (the {@link #array}
     * reference plus {@link #lower} and {@link #upper} values).
     */
    public final long getMemoryUsage() {
        return count()*32 + 12;
    }

    /**
     * Returns the number of points in this geometry.
     */
    public final int count() {
        return upper-lower;
    }
    
    /**
     * Returns the first point in the specified object.
     */
    public final Point2D getFirstPoint(final Point2D point) {
        final Coordinate coord = coords[lower];
        if (point != null) {
            point.setLocation(coord.x, coord.y);
            return point;
        } else {
            return new Point2D.Double(coord.x, coord.y);
        }
    }
    
    /**
     * Returns the list point in the specified object.
     */
    public final Point2D getLastPoint(final Point2D point) {
        final Coordinate coord = coords[upper-1];
        if (point != null) {
            point.setLocation(coord.x, coord.y);
            return point;
        } else {
            return new Point2D.Double(coord.x, coord.y);
        }
    }

    /**
     * Retourne un it�rateur qui balaiera les points partir de l'index sp�cifi�.
     */
    public final PointIterator iterator(final int index) {
        return new JTSIterator(coords, index+lower, upper);
    }
    
    /**
     * Retourne un tableau enveloppant les m�mes points que le tableau courant,
     * mais des index <code>lower</code> inclusivement jusqu'� <code>upper</code>
     * exclusivement. Si le sous-tableau ne contient aucun point (c'est-�-dire si
     * <code>lower==upper</code>), alors cette m�thode retourne <code>null</code>.
     *
     * @param lower Index du premier point � prendre en compte.
     * @param upper Index suivant celui du dernier point � prendre en compte.
     */
    public final PointArray subarray(int lower, int upper) {
        lower += this.lower;
        upper += this.lower;
        if (lower             == upper            ) return null;
        if (lower==this.lower && upper==this.upper) return this;
        return new JTSArray(coords, lower, upper);
    }

    /**
     * Ins�re les donn�es (<var>x</var>,<var>y</var>) du tableau <code>toMerge</code> sp�cifi�.
     */
    public PointArray insertAt(int index, float[] toMerge, int lower, int upper, boolean reverse) {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Renverse l'ordre de tous les points compris dans ce tableau.
     */
    public PointArray reverse() {
        throw new UnsupportedOperationException();
    }

    /**
     * Retourne un tableau immutable qui contient les m�mes donn�es que celui-ci.
     *
     * @param  compress <code>true</code> si l'on souhaite aussi comprimer les donn�es.
     * @return Tableau immutable et �ventuellement compress�, <code>this</code>
     *         si ce tableau r�pondait d�j� aux conditions ou <code>null</code>
     *         si ce tableau ne contient aucune donn�e.
     */
    public final PointArray getFinal(final boolean compress) {
        if (compress && count() >= 8) {
            return new DefaultArray(toArray());
        }
        return super.getFinal(compress);
    }
    
    /**
     * Append (<var>x</var>,<var>y</var>) coordinates to the specified destination array.
     * The destination array will be filled starting at index {@link ArrayData#length}.
     * If <code>resolution2</code> is greater than 0, then points that are closer than
     * <code>sqrt(resolution2)</code> from previous one will be skiped.
     *
     * @param  The destination array. The coordinates will be filled in
     *         {@link ArrayData#array}, which will be expanded if needed.
     *         After this method completed, {@link ArrayData#length} will
     *         contains the index after the <code>array</code>'s element
     *         filled with the last <var>y</var> ordinate.
     * @param  resolution2 The minimum squared distance desired between points.
     */
    public final void toArray(final ArrayData dest, final float resolution2) {
        if (!(resolution2 >= 0)) {
            throw new IllegalArgumentException(String.valueOf(resolution2));
        }
        final int offset = dest.length;
        float[]   copy   = dest.array;
        final int upper  = upper();
        int       src    = lower();
        int       dst    = offset;
        if (src < upper) {
            if (copy.length <= dst) {
                dest.array = copy = XArray.resize(copy, capacity(src, dst, offset));
            }
            double lastX, lastY;
            Coordinate coord = coords[src >> 1];
            copy[dst++] = (float)(lastX = coord.x);
            copy[dst++] = (float)(lastY = coord.y);
            while (src < upper) {
                coord = coords[(src+=2) >> 1];
                final double dx = coord.x - lastX;
                final double dy = coord.y - lastY;
                if ((dx*dx + dy*dy) >= resolution2) {
                    if (copy.length <= dst) {
                        dest.array = copy = XArray.resize(copy, capacity(src, dst, offset));
                    }
                    copy[dst++] = (float)(lastX = coord.x);
                    copy[dst++] = (float)(lastY = coord.y);
                }
            }
        }
        dest.length = dst;
        assert dest.length <= dest.array.length;
    }    
}
