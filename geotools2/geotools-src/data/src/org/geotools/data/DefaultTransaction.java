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
package org.geotools.data;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * Description
 * 
 * <p>
 * Details
 * </p>
 *
 * @author Jody Garnett, Refractions Research
 */
public class DefaultTransaction implements Transaction {
    /** Records State by key */
    Map stateLookup = new HashMap();

    /** Records current Authorizations */
    Set authorizations = new HashSet();

    public DefaultTransaction() {
    }

    /**
     * Remembers Externalized State for a DataSource.
     * 
     * <p>
     * This is the GOF Momento pattern: a FeatureSource is able to externalize
     * its internal State required for Transaction support and have this class
     * manage it. It may retrieve this State with getState( key ).
     * </p>
     * 
     * <p>
     * In addition several FeatureSource implementations may share State, a
     * common example is JDBCDataSources keeping a shared JDBC connection
     * using the JDBC URL as a key.
     * </p>
     *
     * @param key Key used to externalize State
     * @param state Externalized State (Momeneto)
     *
     * @throws IllegalArgumentException When Transaction already using key
     *
     * @see org.geotools.data.Transaction#putState(java.lang.Object,
     *      org.geotools.data.Transaction.State)
     */
    public void putState(Object key, State state) {
        if (stateLookup.containsKey(key)) {
            State current = (State) stateLookup.get(key);

            if (state == current) {
                throw new IllegalArgumentException(
                    "Transaction already has an this State for key: " + key
                    + ". Please check for existing State before creating your own.");
            } else {
                throw new IllegalArgumentException(
                    "Transaction already has an entry for key:" + key
                    + ". Please check for existing State before creating your own.");
            }
        } else {
            stateLookup.put(key, state);

            // allow configuration
            state.setTransaction(this);
        }
    }

    /**
     * Removes state from DefaultTransaction's care.
     * 
     * <p>
     * Currently does not complain if there is no State associated with key to
     * remove - this may change in the future.
     * </p>
     *
     * @param key
     *
     * @throws IllegalArgumentException If no State was maintained for supplied
     *         <code>key</code>
     *
     * @see org.geotools.data.Transaction#removeState(java.lang.Object)
     */
    public void removeState(Object key) {
        if (stateLookup.containsKey(key)) {
            State state = (State) stateLookup.remove(key);
            state.setTransaction(null);
        } else {
            throw new IllegalArgumentException(
                "Transaction does not no anything about key:" + key
                + ". Has this key already been removed?");
        }
    }

    /**
     * Returns externalized state or <code>null</code> if not available.
     * 
     * <p>
     * Used by DataStore implementations to externalize information required
     * for Transaction support using the GOF Momento pattern.
     * </p>
     *
     * @param key
     *
     * @return Previously externalized State.
     *
     * @see org.geotools.data.Transaction#getState(java.lang.Object)
     */
    public State getState(Object key) {
        return (State) stateLookup.get(key);
    }

    /**
     * Commits all modifications against this Transaction.
     * 
     * <p>
     * This implementation will call commit() on all State managed by this
     * Transaction. This allows DataStores to provide their own implementation
     * of commit().
     * </p>
     *
     * @throws IOException Encountered problem maintaining transaction state
     * @throws DataSourceException See IOException
     *
     * @see org.geotools.data.Transaction#commit()
     */
    public void commit() throws IOException {
        State state;
        int problemCount = 0;
        IOException io = null;

        for (Iterator i = stateLookup.values().iterator(); i.hasNext();) {
            state = (State) i.next();

            try {
                state.commit();
            } catch (IOException e) {
                problemCount++;
                io = e;
            }
        }

        if (io != null) {
            if (problemCount == 1) {
                throw io;
            }

            throw new DataSourceException("Commit encountered " + problemCount
                + " problems - the first was", io);
        }
    }

    /**
     * Rollsback all modifications against this Transaction.
     * 
     * <p>
     * This implementation will call rollback() on all State managed by this
     * Transaction. This allows DataStores to provide their own implementation
     * of rollback().
     * </p>
     *
     * @throws IOException Encountered problem maintaining transaction State
     * @throws DataSourceException IOException
     *
     * @see org.geotools.data.Transaction#rollback()
     */
    public void rollback() throws IOException {
        int problemCount = 0;
        IOException io = null;
        State state;

        for (Iterator i = stateLookup.values().iterator(); i.hasNext();) {
            state = (State) i.next();

            try {
                state.rollback();
            } catch (IOException e) {
                problemCount++;
                io = e;
            }
        }

        if (io != null) {
            if (problemCount == 1) {
                throw io;
            }

            throw new DataSourceException("Rollback encountered "
                + problemCount + " problems - the first was", io);
        }
    }

    /**
     * The current set of Authorization IDs held by this Transaction.
     * 
     * <p>
     * This set is reset by the next call to commit or rollback.
     * </p>
     *
     * @return Set of Authorization IDs
     */
    public Set getAuthorizations() {
        return Collections.unmodifiableSet(authorizations);
    }

    /**
     * Provides an authorization ID allowing access to locked Features.
     * 
     * <p>
     * Details
     * </p>
     *
     * @param authID Provided Authorization ID
     *
     * @throws IOException Encountered problems maintaing Transaction State
     * @throws DataSourceException See IOException
     *
     * @see org.geotools.data.Transaction#setAuthorization(java.lang.String)
     */
    public void addAuthorization(String authID) throws IOException {
        int problemCount = 0;
        IOException io = null;
        State state;
        authorizations.add(authID);

        for (Iterator i = stateLookup.values().iterator(); i.hasNext();) {
            state = (State) i.next();

            try {
                state.addAuthorization(authID);
            } catch (IOException e) {
                problemCount++;
                io = e;
            }
        }

        if (io != null) {
            if (problemCount == 1) {
                throw io;
            }

            throw new DataSourceException("setAuthorization encountered "
                + problemCount + " problems - the first was", io);
        }
    }
}
