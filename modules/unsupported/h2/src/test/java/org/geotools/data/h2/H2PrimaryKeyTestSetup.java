package org.geotools.data.h2;

import org.geotools.jdbc.JDBCPrimaryKeyTestSetup;

public class H2PrimaryKeyTestSetup extends JDBCPrimaryKeyTestSetup {

    protected H2PrimaryKeyTestSetup() {
        super(new H2TestSetup());
    }

    @Override
    protected void createAutoGeneratedPrimaryKeyTable() throws Exception {
        run( "CREATE TABLE \"auto\" ( \"key\" int AUTO_INCREMENT(1) PRIMARY KEY, " 
            + "\"name\" VARCHAR, \"geom\" BLOB)" );
        
        run( "INSERT INTO \"auto\" (\"name\",\"geom\" ) VALUES ('one',NULL)");
        run( "INSERT INTO \"auto\" (\"name\",\"geom\" ) VALUES ('two',NULL)");
        run( "INSERT INTO \"auto\" (\"name\",\"geom\" ) VALUES ('three',NULL)");
    }
    
    @Override
    protected void createNonIncrementingPrimaryKeyTable() throws Exception {
        run( "CREATE TABLE \"noninc\" ( \"key\" int PRIMARY KEY, " 
                + "\"name\" VARCHAR, \"geom\" BLOB)" );
            
        run( "INSERT INTO \"noninc\" VALUES (1, 'one', NULL)");
        run( "INSERT INTO \"noninc\" VALUES (2, 'two', NULL)");
        run( "INSERT INTO \"noninc\" VALUES (3, 'three', NULL)");
    }
    
    @Override
    protected void createMultiColumnPrimaryKeyTable() throws Exception {
        run( "CREATE TABLE \"multi\" ( \"key1\" int NOT NULL, \"key2\" VARCHAR NOT NULL, " 
                + "\"name\" VARCHAR, \"geom\" BLOB)" );
        run( "ALTER TABLE \"multi\" ADD PRIMARY KEY (\"key1\",\"key2\")" );
            
        run( "INSERT INTO \"multi\" VALUES (1, 'x', 'one', NULL)");
        run( "INSERT INTO \"multi\" VALUES (2, 'y', 'two', NULL)");
        run( "INSERT INTO \"multi\" VALUES (3, 'z', 'three', NULL)");
    }

    @Override
    protected void dropAutoGeneratedPrimaryKeyTable() throws Exception {
        run( "DROP TABLE \"auto\"" );
    }

    @Override
    protected void dropNonIncrementingPrimaryKeyTable() throws Exception {
        run( "DROP TABLE \"noninc\"" );
    }
    
    @Override
    protected void dropMultiColumnPrimaryKeyTable() throws Exception {
        run( "DROP TABLE \"multi\"" );
    }

    

}