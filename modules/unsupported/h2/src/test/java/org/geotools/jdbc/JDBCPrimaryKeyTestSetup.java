package org.geotools.jdbc;

import java.sql.SQLException;

import javax.sql.DataSource;

public abstract class JDBCPrimaryKeyTestSetup extends JDBCTestSetup {

    JDBCTestSetup delegate;

    protected JDBCPrimaryKeyTestSetup(JDBCTestSetup delegate) {
        this.delegate = delegate;
    }
    
    public void setUp() throws Exception {
        super.setUp();
        
        delegate.setUp();
    }
    
    protected final void initializeDatabase() throws Exception {
        delegate.initializeDatabase();
    }

    protected final DataSource createDataSource() {
        return delegate.createDataSource();
    }

    protected final SQLDialect createSQLDialect(JDBCDataStore dataStore) {
        return delegate.createSQLDialect(dataStore);
    }

    protected final void setUpData() throws Exception {
        //kill all the data
        try {
            dropAutoGeneratedPrimaryKeyTable();
        } catch (SQLException e) {
        }
        try {
            dropNonIncrementingPrimaryKeyTable();
        } catch (SQLException e) {
        }
        try {
            dropMultiColumnPrimaryKeyTable();
        } catch (SQLException e) {
        }
        //create all the data
        createAutoGeneratedPrimaryKeyTable();
        createNonIncrementingPrimaryKeyTable();
        createMultiColumnPrimaryKeyTable();
    }

    /**
     * Drops the "auto" table.
     */
    protected abstract void dropAutoGeneratedPrimaryKeyTable() throws Exception;

    /**
     * Drops the "noninc" table.
     */
    protected abstract void dropNonIncrementingPrimaryKeyTable() throws Exception;
    
    /**
     * Drops the "multi" table.
     */
    protected abstract void dropMultiColumnPrimaryKeyTable() throws Exception;

    /**
     * Creates a table with auto-incrementing primary key column, which has the 
     * following schema:
     * <p>
     * auto( name:String; geom:Geometry; ) 
     * </p>
     * <p>
     * The table should be populated with the following data:
     *  "one" | NULL ; pkey = 1
     *  "two" | NULL ; pkey = 2
     *  "three" | NULL ; pkey = 3
     * </p>
     */
    protected abstract void createAutoGeneratedPrimaryKeyTable() throws Exception;
    
    /**
     * Creates a table with a non incrementing primary key column, which has the 
     * following schema:
     * <p>
     * noninc( name:String; geom:Geometry; ) 
     * </p>
     * <p>
     * The table should be populated with the following data:
     *  "one" | NULL ; pkey = 1
     *  "two" | NULL ; pkey = 2
     *  "three" | NULL ; pkey = 3
     * </p>
     */
    protected abstract void createNonIncrementingPrimaryKeyTable() throws Exception;
    
    /**
     * Creates a table with a primary key which is made up of multiple columns, which has the 
     * following schema:
     * <p>
     * multi( name:String, geom: Geometry );
     *  </p>
     *  <p>
     *  The table should be populated with the following data:
     *  "one" | NULL ; pkey1 = 1, pkey2 = 'x'
     *  "two" | NULL ; pkey1 = 1, pkey2 = 'y'
     *  "three" | NULL ; pkey1 = 1, pkey2 = 'z'
     *  </p>
     */
    protected abstract void createMultiColumnPrimaryKeyTable() throws Exception;

   
}