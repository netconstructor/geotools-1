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
package org.geotools.data.spatialite;

import java.sql.Connection;
import java.util.Properties;

import org.apache.commons.dbcp.BasicDataSource;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.geotools.jdbc.JDBCTestSetup;

public class SpatiaLiteTestSetup extends JDBCTestSetup {
    
    @Override
    protected JDBCDataStoreFactory createDataStoreFactory() {
        return new SpatiaLiteDataStoreFactory();
    }
    
    @Override
    protected void setUpDataStore(JDBCDataStore dataStore) {
        super.setUpDataStore(dataStore);
        dataStore.setDatabaseSchema(null);
    }
    
    @Override
    protected void initializeDataSource(BasicDataSource ds, Properties db) {
        super.initializeDataSource(ds, db);
        ds.addConnectionProperty("enable_load_extension", "true");
        ds.addConnectionProperty("shared_cache", "true");
        ds.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
    }
    
    @Override
    protected void setUpData() throws Exception {
        //drop old data
        runSafe("DROP TABLE ft1");
        runSafe("DROP TABLE ft2");
        runSafe("DELETE FROM geometry_columns where f_table_name in ('ft1','ft2')");
        
        //create some data
        String sql = "CREATE TABLE ft1 (id INTEGER PRIMARY KEY)";
        run( sql );
        
        sql = "SELECT AddGeometryColumn('ft1','geometry',4326,'POINT',2)";
        run( sql );
        
        sql = "ALTER TABLE ft1 add intProperty INTEGER";
        run( sql );
        
        sql = "ALTER TABLE ft1 add doubleProperty DOUBLE";
        run( sql );
        
        sql = "ALTER TABLE ft1 add stringProperty VARCHAR(255)";
        run( sql );

        sql = "INSERT INTO ft1 VALUES ("
            + "0,GeomFromText('POINT(0 0)',4326), 0, 0.0,'zero');";
        run(sql);

        sql = "INSERT INTO ft1 VALUES ("
            + "1,GeomFromText('POINT(1 1)',4326), 1, 1.1,'one');";
        run(sql);

        sql = "INSERT INTO ft1 VALUES ("
            + "2,GeomFromText('POINT(2 2)',4326), 2, 2.2,'two');";
        run(sql);
    }

}
