/*
 * Geotools - OpenSource mapping toolkit
 * (C) 2002, Centre for Computational Geography
 * (C) 2001, Institut de Recherche pour le Développement
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
 *     FRANCE: Surveillance de l'Environnement Assistée par Satellite
 *             Institut de Recherche pour le Développement / US-Espace
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 *
 *    This package contains documentation from OpenGIS specifications.
 *    OpenGIS consortium's work is fully acknowledged here.
 */
package org.geotools.gp;

// J2SE dependencies
import java.io.Writer;
import java.io.IOException;
import java.io.Serializable;
import java.awt.RenderingHints;
import java.util.Locale;

// JAI dependencies
import javax.media.jai.JAI;
import javax.media.jai.util.Range;
import javax.media.jai.Interpolation;
import javax.media.jai.ParameterList;
import javax.media.jai.ParameterListImpl;
import javax.media.jai.ParameterListDescriptor;

// Geotools Dependencies
import org.geotools.gc.GridCoverage;
import org.geotools.gc.ParameterInfo;
import org.geotools.ct.CoordinateTransformationFactory;

// Resources
import org.geotools.io.TableWriter;
import org.geotools.resources.Utilities;
import org.geotools.resources.gcs.Resources;
import org.geotools.resources.gcs.ResourceKeys;


/**
 * Provides descriptive information for a grid coverage processing
 * operation. The descriptive information includes such information as the
 * name of the operation, operation description, and number of source grid
 * coverages required for the operation.
 *
 * @version $Id: Operation.java,v 1.3 2002/07/27 12:40:49 desruisseaux Exp $
 * @author <a href="www.opengis.org">OpenGIS</a>
 * @author Martin Desruisseaux
 */
public abstract class Operation implements Serializable {
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -1280778129220703728L;

    /**
     * Key for setting a {@link CoordinateTransformationFactory} object other
     * than the default one when coordinate transformations must be performed
     * at rendering time.
     */
    public static final RenderingHints.Key COORDINATE_TRANSFORMATION_FACTORY =
            new Key(0, CoordinateTransformationFactory.class);

    /**
     * Key for setting a {@link JAI} instance other than the default one when
     * JAI operation must be performed at rendering time.
     */
    public static final RenderingHints.Key JAI_INSTANCE =
            new Key(1, JAI.class);
    
    /**
     * List of valid names. Note: the "Optimal" type is not
     * implemented because currently not provided by JAI.
     */
    private static final String[] INTERPOLATION_NAMES= {
        "NearestNeighbor",
        "Bilinear",
        "Bicubic",
        "Bicubic2"
    };
    
    /**
     * Interpolation types (provided by Java Advanced
     * Imaging) for {@link #INTERPOLATION_NAMES}.
     */
    private static final int[] INTERPOLATION_TYPES= {
        Interpolation.INTERP_NEAREST,
        Interpolation.INTERP_BILINEAR,
        Interpolation.INTERP_BICUBIC,
        Interpolation.INTERP_BICUBIC_2
    };
    
    /**
     * The name of the processing operation.
     */
    private final String name;
    
    /**
     * The parameters descriptors.
     */
    private final ParameterListDescriptor descriptor;
    
    /**
     * Construct an operation.
     *
     * @param The name of the processing operation.
     * @param The parameters descriptors.
     */
    public Operation(final String name, final ParameterListDescriptor descriptor) {
        this.name       = name;
        this.descriptor = descriptor;
    }
    
    /**
     * Returns the name of the processing operation.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Returns the description of the processing operation.
     * If no description, returns <code>null</code>.
     * If no description is available in the specified locale,
     * a default one will be used.
     *
     * @param locale The desired locale, or <code>null</code>
     *        for the default locale.
     */
    public String getDescription(final Locale locale) {
        return null;
    }
    
    /**
     * Returns the number of source grid coverages required for the operation.
     */
    public int getNumSources() {
        int count=0;
        final Class[] c = descriptor.getParamClasses();
        if (c!=null) {
            for (int i=0; i<c.length; i++) {
                if (GridCoverage.class.isAssignableFrom(c[i])) {
                    count++;
                }
            }
        }
        return count;
    }
    
    /**
     * Returns the number of parameters for the
     * operation, including source grid coverages.
     */
    public int getNumParameters() {
        return descriptor.getNumParameters();
    }
    
    /**
     * Retrieve the parameter information for a given index.
     * This is mostly a convenience method, since informations
     * are extracted from {@link ParameterListDescriptor}.
     */
    public ParameterInfo getParameterInfo(final int index) {
        return new ParameterInfo(descriptor, index);
    }
    
    /**
     * Retrieve the parameter information for a given name.
     * Search is case-insensitive. This is mostly a convenience
     * method, since informations are extracted from
     * {@link ParameterListDescriptor}.
     */
    public ParameterInfo getParameterInfo(final String name) {
        return new ParameterInfo(descriptor, name);
    }
    
    /**
     * Returns a default parameter list for this operation.
     */
    public ParameterList getParameterList() {
        return new ParameterListImpl(descriptor);
    }
    
    /**
     * Cast the specified object to an {@link Interpolation object}.
     *
     * @param  type The interpolation type as an {@link Interpolation} or a {@link CharSequence} object.
     * @return The interpolation object for the specified type.
     * @throws IllegalArgumentException if the specified interpolation type is not a know one.
     */
    static Interpolation toInterpolation(final Object type) {
        if (type instanceof Interpolation) {
            return (Interpolation) type;
        } else if (type instanceof CharSequence)
        {
            final String name=type.toString();
            for (int i=0; i<INTERPOLATION_NAMES.length; i++) {
                if (INTERPOLATION_NAMES[i].equalsIgnoreCase(name)) {
                    return Interpolation.getInstance(INTERPOLATION_TYPES[i]);
                }
            }
        }
        throw new IllegalArgumentException(Resources.format(
                ResourceKeys.ERROR_UNKNOW_INTERPOLATION_$1, type));
    }
    
    /**
     * Apply a process operation to a grid coverage. This method is invoked by
     * {@link GridCoverageProcessor}.
     *
     * @param  parameters List of name value pairs for the parameters
     *         required for the operation.
     * @param  A set of rendering hints, or <code>null</code> if none. The
     *         <code>GridCoverageProcessor</code> will usually provides hints
     *         for the following keys: {@link #COORDINATE_TRANSFORMATION_FACTORY}
     *         and {@link #JAI_INSTANCE}.
     * @return The result as a grid coverage.
     */
    protected abstract GridCoverage doOperation(final ParameterList  parameters,
                                                final RenderingHints hints);

    /**
     * Returns a hash value for this operation.
     * This value need not remain consistent between
     * different implementations of the same class.
     */
    public int hashCode() {
        return name.hashCode()*37 + descriptor.hashCode();
    }
    
    /**
     * Compares the specified object with
     * this operation for equality.
     */
    public boolean equals(final Object object) {
        if (object!=null && object.getClass().equals(getClass())) {
            final Operation that = (Operation) object;
            return Utilities.equals(this.name,       that.name) &&
                   Utilities.equals(this.descriptor, that.descriptor);
        }
        return false;
    }
    
    /**
     * Returns a string représentation of this operation.
     * The returned string is implementation dependent. It
     * is usually provided for debugging purposes only.
     */
    public String toString() {
        return Utilities.getShortClassName(this) + '[' + 
               getName() + ": "+descriptor.getNumParameters() + ']';
    }
    
    /**
     * Print a description of this operation to the specified stream.
     * The description include operation name and a list of parameters.
     *
     * @param  out The destination stream.
     * @throws IOException if an error occured will writing to the stream.
     */
    public void print(final Writer out) throws IOException {
        final String lineSeparator = System.getProperty("line.separator", "\n");
        out.write(' ');
        out.write(getName());
        out.write(lineSeparator);
        
        final Resources resources = Resources.getResources(null);
        final TableWriter table = new TableWriter(out, " \u2502 ");
        table.writeHorizontalSeparator();
        table.write(resources.getString(ResourceKeys.NAME));
        table.nextColumn();
        table.write(resources.getString(ResourceKeys.CLASS));
        table.nextColumn();
        table.write(resources.getString(ResourceKeys.DEFAULT_VALUE));
        table.nextLine();
        table.writeHorizontalSeparator();
        
        final String[]    names = descriptor.getParamNames();
        final Class []  classes = descriptor.getParamClasses();
        final Object[] defaults = descriptor.getParamDefaults();
        final int numParameters = descriptor.getNumParameters();
        for (int i=0; i<numParameters; i++) {
            table.write(names[i]);
            table.nextColumn();
            table.write(Utilities.getShortName(classes[i]));
            table.nextColumn();
            if (defaults[i] != ParameterListDescriptor.NO_PARAMETER_DEFAULT) {
                table.write(String.valueOf(defaults[i]));
            }
            table.nextLine();
        }
        table.writeHorizontalSeparator();
        table.flush();
    }

    /**
     * Class of all rendering hint keys defined.
     */
    private static final class Key extends RenderingHints.Key
    {
        /**
         * Base class of all values for this key.
         *
         * @task TODO: We could use only the class name (as a string)
         *             here in order to defer class loading.
         */
        private final Class valueClass;

        /**
         * Construct a new key.
         *
         * @param id An ID. Must be unique for all instances of {@link Key}.
         * @param valueClass Base class of all valid values.
         */
        Key(final int id, final Class valueClass) {
            super(id);
            this.valueClass = valueClass;
        }

        /**
         * Returns <code>true</code> if the specified object is a valid
         * value for this Key.
         *
         * @param  value The object to test for validity.
         * @return <code>true</code> if the value is valid;
         *         <code>false</code> otherwise.
         */
        public boolean isCompatibleValue(final Object value) {
            return (value != null) && valueClass.isAssignableFrom(value.getClass());
        }
    }
}
