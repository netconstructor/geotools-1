/*
 *    GeoTools - The Open Source Java GIS Toolkit
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
package org.geotools.data.directory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.geotools.data.DataStore;
import org.geotools.data.DefaultServiceInfo;
import org.geotools.data.FeatureLocking;
import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.FeatureWriter;
import org.geotools.data.LockingManager;
import org.geotools.data.Query;
import org.geotools.data.ServiceInfo;
import org.geotools.data.Transaction;
import org.geotools.feature.FeatureTypes;
import org.geotools.feature.NameImpl;
import org.geotools.feature.SchemaException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;

public class DirectoryDataStore implements DataStore {
    
    DirectoryTypeCache cache;
    DirectoryLockingManager lm;
    
    public DirectoryDataStore(File directory, URI namespaceURI) throws IOException {
        cache = new DirectoryTypeCache(directory, namespaceURI);
    }

    public FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(
            Query query, Transaction transaction) throws IOException {
        String typeName = query.getTypeName();
        return getDataStore(typeName).getFeatureReader(query, transaction);
    }

    public FeatureSource<SimpleFeatureType, SimpleFeature> getFeatureSource(
            String typeName) throws IOException {
        FeatureSource fs = getDataStore(typeName).getFeatureSource(typeName);
        if(fs instanceof FeatureLocking) {
            return new DirectoryFeatureLocking((FeatureLocking) fs, this);
        } else if(fs instanceof FeatureStore) {
            return new DirectoryFeatureStore((FeatureStore) fs, this);
        } else {
            return new DirectoryFeatureSource((FeatureSource) fs, this);
        }
    }

    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter(
            String typeName, Filter filter, Transaction transaction)
            throws IOException {
        return getDataStore(typeName).getFeatureWriter(typeName, filter, transaction);
    }

    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter(
            String typeName, Transaction transaction) throws IOException {
        return getDataStore(typeName).getFeatureWriter(typeName, transaction);
    }

    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriterAppend(
            String typeName, Transaction transaction) throws IOException {
        return getDataStore(typeName).getFeatureWriterAppend(typeName, transaction);
    }

    public LockingManager getLockingManager() {
        if(lm == null) {
            lm = new DirectoryLockingManager(cache);
        }
        return lm;
    }

    public SimpleFeatureType getSchema(String typeName) throws IOException {
        return getDataStore(typeName).getSchema(typeName);
    }

    public String[] getTypeNames() throws IOException {
        Set<String> typeNames = cache.getTypeNames();
        return typeNames.toArray(new String[typeNames.size()]);
    }

    public FeatureSource<SimpleFeatureType, SimpleFeature> getView(Query query)
            throws IOException, SchemaException {
        String typeName = query.getTypeName();
        return getDataStore(typeName).getView(query);
    }

    public void updateSchema(String typeName, SimpleFeatureType featureType)
            throws IOException {
        getDataStore(typeName).updateSchema(typeName, featureType);
    }

    public void createSchema(SimpleFeatureType featureType) throws IOException {
        throw new UnsupportedOperationException("This datastore does not support type creation");
    }

    public void dispose() {
        cache.dispose();
    }

    public FeatureSource<SimpleFeatureType, SimpleFeature> getFeatureSource(
            Name typeName) throws IOException {
        return getFeatureSource(typeName.getLocalPart());
    }

    public ServiceInfo getInfo() {
        DefaultServiceInfo info = new DefaultServiceInfo();
        info.setDescription("Features from Directory " + cache.directory );
        info.setSchema( FeatureTypes.DEFAULT_NAMESPACE );
        info.setSource( cache.directory.toURI() );
        try {
            info.setPublisher( new URI(System.getenv("user.name")) );
        } catch (URISyntaxException e) {
        }
        return info;
    }

    public List<Name> getNames() throws IOException {
        String[] typeNames = getTypeNames();
        List<Name> names = new ArrayList<Name>(typeNames.length);
        for (String typeName : typeNames) {
            names.add(new NameImpl(typeName));
        }
        return names;
    }

    public SimpleFeatureType getSchema(Name name) throws IOException {
        return getSchema(name.getLocalPart());
    }

    public void updateSchema(Name typeName, SimpleFeatureType featureType)
            throws IOException {
        updateSchema(typeName.getLocalPart(), featureType);
    }
    
    DataStore getDataStore(String typeName) throws IOException {
        // grab the store for a specific feature type, making sure it's actually there
        DataStore store = cache.getDataStore(typeName, true);
        if(store == null)
            throw new IOException("Feature type " + typeName + " is unknown");
        return store;
    }

}
