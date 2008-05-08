/*
 *    GeoTools - OpenSource mapping toolkit
 *    http://geotools.org
 *    (C) 2002-2006, GeoTools Project Managment Committee (PMC)
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

// Geotools dependencies
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.geotools.filter.expression.PropertyAccessor;
import org.geotools.filter.expression.PropertyAccessors;
import org.geotools.filter.expression.SimpleFeaturePropertyAccessorFactory;
import org.opengis.filter.FilterVisitor;
import org.opengis.filter.identity.Identifier;

/**
 * Defines a ID filter, which holds a list of IDs ( usually feature id;s ). This
 * filter stores a series of IDs, which are used to distinguish features
 * uniquely.
 * <p>
 * Please note that addAllFids( Collection ) may be a performance hog; uDig
 * makes use of its own implementation of FidFilter in order to reuse the
 * internal set of fids between uses.
 * </p>
 * 
 * @author Rob Hranac, TOPP
 * @author Justin Deoliveira, TOPP
 * 
 * TODO: this class shoul be renamed to IdFilterImpl
 * 
 * @source $URL:
 *         http://svn.geotools.org/geotools/trunk/gt/modules/library/main/src/main/java/org/geotools/filter/FidFilterImpl.java $
 * @version $Id$
 */
public class FidFilterImpl extends AbstractFilterImpl implements FidFilter {
	/** Logger for the default core module. */
	private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geotools.core");

	/** List of the Identifer. */
	private Set fids = new HashSet();

	/**
	 * Empty constructor.
	 * 
	 * @deprecated use {@link #FidFilterImpl(Set)}
	 */
	protected FidFilterImpl() {
		super(FilterFactoryFinder.createFilterFactory());
		filterType = AbstractFilter.FID;
	}

	/**
	 * Constructor with first fid set
	 * 
	 * @param initialFid
	 *            The type of comparison.
	 * @deprecated use {@link #FidFilterImpl(Set)}
	 */
	protected FidFilterImpl(String initialFid) {
		super(FilterFactoryFinder.createFilterFactory());
		filterType = AbstractFilter.FID;
		addFid(initialFid);
	}

	/**
	 * Constructor which takes {@link org.opengis.filter.identity.Identifier},
	 * not String.
	 * 
	 */
	protected FidFilterImpl(Set/* <Identiger> */fids) {
		super(FilterFactoryFinder.createFilterFactory());
		filterType = AbstractFilter.FID;
		// check these are really identifiers
		for (Iterator it = fids.iterator(); it.hasNext();) {
			Object next = it.next();
			if (!(next instanceof Identifier))
				throw new ClassCastException("Fids must implement Identifier, "
						+ next.getClass() + " does not");
		}
		this.fids = fids;
	}

	/**
	 * Returns all the fids in this filter.
	 * 
	 * @return An array of all the fids in this filter.
	 * 
	 * @deprecated use {@link #getIDs()}
	 */
	public final String[] getFids() {
		return (String[]) fids().toArray(new String[0]);
	}

	/**
	 * @see org.opengis.filter.Id#getIDs()
	 */
	public Set getIDs() {
		return getFidsSet();
	}

	/**
	 * @see org.opengis.filter.Id#getIdentifiers()
	 */
	public Set getIdentifiers() {
		return fids;
	}

	/**
	 * @see org.opengis.filter.identity.FeatureId#setIDs(Set)
	 */
	public void setIDs(Set ids) {
		fids = new HashSet();
		addAllFids(ids);
	}

	/**
	 * Accessor method for fid set as Strings.
	 * 
	 * @return the internally stored fids.
	 */
	public Set getFidsSet() {
		return fids();
	}

	/**
	 * Helper method to pull out strings from featureId set.
	 * 
	 * @return
	 */
	private Set fids() {
		Set set = new TreeSet();
		for (Iterator i = fids.iterator(); i.hasNext();) {
			Identifier id = (Identifier) i.next();
			set.add(id.toString());
		}

		return set;
	}

	/**
	 * Adds a feature ID to the filter.
	 * 
	 * @param fid
	 *            A single feature ID.
	 * @deprecated
	 */
	public final void addFid(String fid) {
		LOGGER.finest("got fid: " + fid);
		fids.add(factory.featureId(fid));
	}

	/**
	 * Adds a collection of feature IDs to the filter.
	 * 
	 * @param fidsToAdd
	 *            A collection of feature IDs as strings.
	 */
	public void addAllFids(Collection fidsToAdd) {
		if (fidsToAdd == null)
			return;

		for (Iterator i = fidsToAdd.iterator(); i.hasNext();) {
			String fid = (String) i.next();
			addFid(fid);
		}
	}

	/**
	 * Removes a feature ID from the filter.
	 * 
	 * @param fid
	 *            A single feature ID.
	 */
	public final void removeFid(String fid) {
		if (fid == null) {
			return;
		}

		for (Iterator f = fids.iterator(); f.hasNext();) {
			Identifier featureId = (Identifier) f.next();
			if (fid.equals(featureId.toString())) {
				f.remove();
			}
		}

	}

	/**
	 * Removes a collection of feature IDs from the filter.
	 * 
	 * @param fidsToRemove
	 *            A collection of feature IDs.
	 */
	public void removeAllFids(Collection fidsToRemove) {
		if (fidsToRemove == null)
			return;

		for (Iterator f = fidsToRemove.iterator(); f.hasNext();) {
			String fid = (String) f.next();
			removeFid(fid);
		}
	}

	/**
	 * Determines whether or not the given feature's ID matches this filter.
	 * <p>
	 * In order to get the object's ID, the {@link PropertyAccessor} capable of
	 * dealing with <code>feature</code> has to support the request of the
	 * expression <code>"@id"</code>
	 * </p>
	 * 
	 * @param feature
	 *            Specified feature to examine.
	 * 
	 * @return <tt>true</tt> if the feature's ID matches an fid held by this
	 *         filter, <tt>false</tt> otherwise.
	 * @see SimpleFeaturePropertyAccessorFactory
	 */
	public boolean evaluate(Object feature) {
		if (feature == null) {
			return false;
		}

		final Set fids = fids();
		final String attPath = "@id";
		
		PropertyAccessor accessor = PropertyAccessors.findPropertyAccessor(feature, attPath, null, null);

		if (accessor == null) {
			return false;
		}
		Object id = accessor.get(feature, attPath, null);
		return fids.contains(id);
	}

	/**
	 * Returns a string representation of this filter.
	 * 
	 * @return String representation of the compare filter.
	 */
	public String toString() {
		StringBuffer fidFilter = new StringBuffer();

		Iterator fidIterator = fids.iterator();

		while (fidIterator.hasNext()) {
			fidFilter.append(fidIterator.next().toString());

			if (fidIterator.hasNext()) {
				fidFilter.append(", ");
			}
		}

		return "[ " + fidFilter.toString() + " ]";
	}

	/**
	 * Used by FilterVisitors to perform some action on this filter instance.
	 * Typicaly used by Filter decoders, but may also be used by any thing which
	 * needs infomration from filter structure. Implementations should always
	 * call: visitor.visit(this); It is importatant that this is not left to a
	 * parent class unless the parents API is identical.
	 * 
	 * @param visitor
	 *            The visitor which requires access to this filter, the method
	 *            must call visitor.visit(this);
	 */
	public Object accept(FilterVisitor visitor, Object extraData) {
		return visitor.visit(this, extraData);
	}

	/**
	 * Returns a flag indicating object equality.
	 * 
	 * @param filter
	 *            the filter to test equality on.
	 * 
	 * @return String representation of the compare filter.
	 */
	public boolean equals(Object filter) {
		LOGGER.finest("condition: " + filter);

		if ((filter != null) && (filter.getClass() == this.getClass())) {
			LOGGER.finest("condition: " + ((FidFilterImpl) filter).filterType);

			if (((FidFilterImpl) filter).filterType == AbstractFilter.FID) {
				return fids.equals(((FidFilterImpl) filter).fids);
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	/**
	 * Override of hashCode method.
	 * 
	 * @return a hash code value for this fid filter object.
	 */
	public int hashCode() {
		return fids.hashCode();
	}
}
