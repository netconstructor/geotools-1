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
package org.geotools.data.jdbc;

import com.vividsolutions.jts.geom.Envelope;
import org.geotools.data.AttributeReader;
import org.geotools.data.AttributeWriter;
import org.geotools.data.DataSourceException;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultQuery;
import org.geotools.data.EmptyFeatureWriter;
import org.geotools.data.FIDFeatureReader;
import org.geotools.data.FIDReader;
import org.geotools.data.FeatureListenerManager;
import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureWriter;
import org.geotools.data.FilteringFeatureReader;
import org.geotools.data.FilteringFeatureWriter;
import org.geotools.data.InProcessLockingManager;
import org.geotools.data.JoiningAttributeReader;
import org.geotools.data.JoiningAttributeWriter;
import org.geotools.data.LockingManager;
import org.geotools.data.Query;
import org.geotools.data.ReTypeFeatureReader;
import org.geotools.data.SchemaNotFoundException;
import org.geotools.data.Transaction;
import org.geotools.factory.FactoryConfigurationError;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.geotools.filter.Filter;
import org.geotools.filter.SQLEncoderException;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Abstract helper class for JDBC DataStore implementations.
 * 
 * <p>
 * This class provides a default implementation of a JDBC data store. Support
 * for vendor specific JDBC data stores can be easily added to Geotools by
 * subclassing this class and overriding the hooks provided.
 * </p>
 * 
 * <p>
 * At a minimum subclasses should implement the following methods:
 * 
 * <ul>
 * <li>
 * {@link #buildAttributeType(ResultSet) buildAttributeType(ResultSet)} - This
 * should be overriden to construct an attribute type that represents any
 * column types not supported by the default implementation, such as geometry
 * columns.
 * </li>
 * <li>
 * {@link #createGeometryAttributeReader(AttributeType,QueryData,int)
 * createGeometryAttributeReader} - Should be overriden to provide an
 * AttributeReader that is capable of reading geometries in the format of the
 * database.
 * </li>
 * </ul>
 * </p>
 * 
 * <p>
 * Additionally subclasses can optionally override the following:
 * 
 * <ul>
 * <li>
 * {@link #determineFidColumnName(String) determindeFidColumnName} - Used to
 * determine the FID column name.  The default uses the primary key, but
 * subclasses can alter this as needed.
 * </li>
 * <li>
 * {@link #allowTable(String) allowTable} - Used to determine whether a table
 * name should be exposed asa  feature type.
 * </li>
 * <li>
 * {@link #determineSRID(String,String) determineSRID} - Used to determine the
 * SpatialReference ID of a geometry column in a table.
 * </li>
 * <li>
 * {@link #buildSQLQuery(String,AttributeType[],Filter,boolean)
 * buildSQLQuery()} - Sub classes can override this to build a custom SQL
 * query.
 * </li>
 * </ul>
 * </p>
 *
 * @author Sean  Geoghegan, Defence Science and Technology Organisation
 * @author Chris Holmes, TOPP
 */
public abstract class JDBCDataStore implements DataStore {
    /** The logger for the filter module. */
    private static final Logger LOGGER = Logger.getLogger(
            "org.geotools.data.jdbc");

    /**
     * Maps SQL types to Java classes. This might need to be fleshed out more
     * later, Ive ignored complex types such as ARRAY, BLOB and CLOB. It is
     * protected so subclasses can override it I guess.
     * 
     * <p>
     * These mappings were taken from
     * http://java.sun.com/j2se/1.3/docs/guide/jdbc/getstart/mapping.html#997737
     * </p>
     */
    protected static final Map TYPE_MAPPINGS = new HashMap();

    static {
        TYPE_MAPPINGS.put(new Integer(Types.VARCHAR), String.class);
        TYPE_MAPPINGS.put(new Integer(Types.CHAR), String.class);
        TYPE_MAPPINGS.put(new Integer(Types.LONGVARCHAR), String.class);

        TYPE_MAPPINGS.put(new Integer(Types.BIT), Boolean.class);
        TYPE_MAPPINGS.put(new Integer(Types.BOOLEAN), Boolean.class);

        TYPE_MAPPINGS.put(new Integer(Types.TINYINT), Short.class);
        TYPE_MAPPINGS.put(new Integer(Types.SMALLINT), Short.class);

        TYPE_MAPPINGS.put(new Integer(Types.INTEGER), Integer.class);
        TYPE_MAPPINGS.put(new Integer(Types.BIGINT), Long.class);

        TYPE_MAPPINGS.put(new Integer(Types.REAL), Float.class);
        TYPE_MAPPINGS.put(new Integer(Types.FLOAT), Double.class);
        TYPE_MAPPINGS.put(new Integer(Types.DOUBLE), Double.class);

        TYPE_MAPPINGS.put(new Integer(Types.DECIMAL), BigDecimal.class);
        TYPE_MAPPINGS.put(new Integer(Types.NUMERIC), BigDecimal.class);

        TYPE_MAPPINGS.put(new Integer(Types.DATE), java.sql.Date.class);
        TYPE_MAPPINGS.put(new Integer(Types.TIME), java.sql.Time.class);
        TYPE_MAPPINGS.put(new Integer(Types.TIMESTAMP), java.sql.Timestamp.class);
    }

    /** Manages listener lists for FeatureSource implementations */
    public FeatureListenerManager listenerManager = new FeatureListenerManager();
    private LockingManager lockingManager = createLockingManager();

    /** The ConnectionPool */
    private ConnectionPool connectionPool;

    /**
     * The namespace of this DataStore.  This is assuming we want all feature
     * types from a single data store to have the same namespace.
     */
    private String namespace;

    /**
     * This allows subclasses to define a schema name to narrow down the tables
     * represented by the data store.  The default is null which means the
     * schema will not be used to narrow down available tables.
     */
    private String databaseSchemaName = null;

    /** A map of FeatureTypes with their names as the key. */
    private Map featureTypeMap = null;

    public JDBCDataStore(ConnectionPool connectionPool)
        throws IOException {
        this(connectionPool, null, "");
    }

    public JDBCDataStore(ConnectionPool connectionPool,
        String databaseSchemaName) throws IOException {
        this(connectionPool, databaseSchemaName, databaseSchemaName);
    }

    public JDBCDataStore(ConnectionPool connectionPool,
        String databaseSchemaName, String namespace) throws IOException {
        this.connectionPool = connectionPool;
        this.namespace = namespace;
        this.databaseSchemaName = databaseSchemaName;
        this.featureTypeMap = createFeatureTypeMap();
    }

    /**
     * Allows subclass to create LockingManager to support their needs.
     *
     * @return
     */
    protected LockingManager createLockingManager() {
        return new InProcessLockingManager();
    }

    /* (non-Javadoc)
     * @see org.geotools.data.DataStore#getFeatureTypes()
     */
    public String[] getTypeNames() {
        return (String[]) featureTypeMap.keySet().toArray(new String[featureTypeMap.keySet()
                                                                                   .size()]);
    }

    /* (non-Javadoc)
     * @see org.geotools.data.DataStore#getSchema(java.lang.String)
     */
    public FeatureType getSchema(String typeName) throws IOException {
        if (featureTypeMap.containsKey(typeName)) {
            FeatureTypeInfo info = getFeatureTypeInfo(typeName);
            FeatureTypeInfo holder = (FeatureTypeInfo) featureTypeMap.get(typeName);

            return holder.schema;
        } else {
            throw new SchemaNotFoundException(typeName);
        }
    }

    /**
     * Create a new featureType.
     * 
     * <p>
     * Not currently supported - subclass may implement.
     * </p>
     *
     * @param featureType
     *
     * @throws IOException
     * @throws UnsupportedOperationException DOCUMENT ME!
     *
     * @see org.geotools.data.DataStore#createSchema(org.geotools.feature.FeatureType)
     */
    public void createSchema(FeatureType featureType) throws IOException {
        throw new UnsupportedOperationException(
            "Table creation not implemented");
    }

    /**
     * Default implementation based on getFeatureReader and getFeatureWriter.
     * 
     * <p>
     * We should be able to optimize this to only get the RowSet once
     * </p>
     *
     * @see org.geotools.data.DataStore#getFeatureSource(java.lang.String)
     */
    public FeatureSource getFeatureSource(String typeName)
        throws IOException {
        if (getLockingManager() != null) {
            // Use default JDBCFeatureLocking that delegates all locking
            // the getLockingManager
            // 
            return new JDBCFeatureLocking(this, getSchema(typeName));
        } else {
            // subclass should provide a FeatureLocking implementation
            // but for now we will simply forgo all locking
            return new JDBCFeatureStore(this, getSchema(typeName));
        }
    }

    /**
     * This is a public entry point to the DataStore.
     * 
     * <p>
     * We have given some though to changing this api to be based on query.
     * </p>
     * 
     * <p>
     * Currently the is is the only way to retype your features to different
     * name spaces.
     * </p>
     * (non-Javadoc)
     *
     * @see org.geotools.data.DataStore#getFeatureReader(org.geotools.feature.FeatureType,
     *      org.geotools.filter.Filter, org.geotools.data.Transaction)
     */
    public FeatureReader getFeatureReader(final FeatureType featureType,
        final Filter filter, final Transaction transaction)
        throws IOException {
        String typeName = featureType.getTypeName();
        int compare = DataUtilities.compare(featureType, getSchema(typeName));
        Query query;

        if (compare == 0) {
            // they are the same type
            query = new DefaultQuery(featureType.getTypeName(), filter);
        } else if (compare == 1) {
            // featureType is a proper subset and will require reTyping
            String[] names = attributeNames(featureType, filter);
            query = new DefaultQuery(featureType.getTypeName(), filter,
                    Query.DEFAULT_MAX, names, "getFeatureReader");
        } else {
            // featureType is not compatiable
            throw new IOException("Type " + typeName + " does not your request");
        }

        FeatureReader reader = getFeatureReader(query, transaction);

        if (compare == 1) {
            reader = new ReTypeFeatureReader(reader, featureType);
        }

        return reader;
    }

    /**
     * Gets the list of attribute names required for both featureType and
     * filter
     *
     * @param featureType DOCUMENT ME!
     * @param filter DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     *
     * @throws IOException DOCUMENT ME!
     */
    protected String[] attributeNames(FeatureType featureType, Filter filter)
        throws IOException {
        String typeName = featureType.getTypeName();
        FeatureType origional = getSchema(typeName);
        SQLBuilder sqlBuilder = getSqlBuilder(typeName);

        if (featureType.getAttributeCount() == origional.getAttributeCount()) {
            // featureType is complete (so filter must require subset
            return DataUtilities.attributeNames(featureType);
        }

        String[] typeAttributes = DataUtilities.attributeNames(featureType);
        String[] filterAttributes = DataUtilities.attributeNames(sqlBuilder
                .getPostQueryFilter(filter));

        if ((filterAttributes == null) || (filterAttributes.length == 0)) {
            // no filter attributes required            
            return typeAttributes;
        }

        // need to combine results               
        Set set = new HashSet();
        int i;

        for (i = 0; i < typeAttributes.length; i++) {
            set.add(typeAttributes[i]);
        }

        for (i = 0; i < filterAttributes.length; i++) {
            set.add(filterAttributes[i]);
        }

        if (set.size() == typeAttributes.length) {
            // filter required a subset of featureType attributes
            return typeAttributes;
        } else {
            String[] attributeNames = new String[set.size()];
            i = 0;

            for (Iterator names = set.iterator(); names.hasNext(); i++) {
                attributeNames[i] = (String) names.next();
            }

            return attributeNames;
        }
    }

    /**
     * The top level method for getting a FeatureReader.
     * 
     * <p>
     * Chris- I've gone with the Query object aswell.  It just seems to make
     * more sense.  This is pretty well split up across methods. The hooks for
     * DB specific AttributeReaders are createResultSetReader and
     * createGeometryReader.
     * </p>
     * 
     * <p>
     * JG- I have implemented getFeatureReader( FeatureType, Filter,
     * Transasction) ontop of this method, it will Retype as required
     * </p>
     *
     * @param query
     * @param trans
     *
     * @return
     *
     * @throws IOException DOCUMENT ME!
     * @throws DataSourceException
     */
    public FeatureReader getFeatureReader(Query query, Transaction trans)
        throws IOException {
        String typeName = query.getTypeName();
        AttributeType[] attrTypes = getAttTypes(query);
        SQLBuilder sqlBuilder = getSqlBuilder(typeName);
        Filter preFilter = sqlBuilder.getPreQueryFilter(query.getFilter());
        Filter postFilter = sqlBuilder.getPostQueryFilter(query.getFilter());

        String sqlQuery = constructQuery(query, attrTypes);

        QueryData queryData = executeQuery(typeName, sqlQuery, trans,
                ResultSet.CONCUR_READ_ONLY);
        FeatureType schema;

        try {
            schema = FeatureTypeFactory.newFeatureType(attrTypes, typeName,
                    getNameSpace());
        } catch (FactoryConfigurationError e) {
            throw new DataSourceException("Schema Factory Error when creating schema for FeatureReader",
                e);
        } catch (SchemaException e) {
            throw new DataSourceException("Schema Error when creating schema for FeatureReader",
                e);
        }

        return createFeatureReader(schema, postFilter, queryData);
    }

    /** Used internally to call the subclass hooks that construct the SQL query.
     * 
     * @param query
     * @param attrTypes
     * @return
     * @throws IOException
     * @throws DataSourceException
     */ 
    private String constructQuery(Query query, AttributeType[] attrTypes) throws IOException, DataSourceException {
        String typeName = query.getTypeName();
        SQLBuilder sqlBuilder = getSqlBuilder(query.getTypeName());
        Filter preFilter = sqlBuilder.getPreQueryFilter(query.getFilter());
        Filter postFilter = sqlBuilder.getPostQueryFilter(query.getFilter());
        
        String sqlQuery;
        FeatureTypeInfo info = getFeatureTypeInfo(typeName);
        boolean useMax = (postFilter == null); // not used yet

        try {
            LOGGER.fine("calling sql builder with filter " + preFilter);
            if (query.getFilter() == Filter.ALL) {
                StringBuffer buf = new StringBuffer("SELECT ");
                sqlBuilder.sqlColumns(buf, info.fidColumnName, attrTypes);
                sqlBuilder.sqlFrom(buf, typeName);
                buf.append(" WHERE '1' = '0'"); // NO-OP it
                sqlQuery = buf.toString();
            } else {
                sqlQuery = sqlBuilder.buildSQLQuery(typeName, info.fidColumnName,
                                        attrTypes, preFilter);
            }
            LOGGER.info("sql is " + sqlQuery);
        } catch (SQLEncoderException e) {
            throw new DataSourceException("Error building SQL Query", e);
        }
        return sqlQuery;
    }

    /**
     * Create a new FeatureReader based on attributeReaders.
     * 
     * <p>
     * The provided <code>schema</code> describes the attributes in the
     * queryData ResultSet. This schema should cover the requirements of
     * <code>filter</code>.
     * </p>
     * 
     * <p>
     * Retyping to the users requested Schema will not happen in this method.
     * </p>
     *
     * @param schema
     * @param postFilter Filter for post processing, or <code>null</code> if
     *        not requried.
     * @param queryData Holds a ResultSet for attribute Readers
     *
     * @return
     *
     * @throws IOException
     * @throws DataSourceException DOCUMENT ME!
     */
    protected FeatureReader createFeatureReader(FeatureType schema,
        Filter postFilter, QueryData queryData) throws IOException {
        AttributeReader[] attrReaders = buildAttributeReaders(schema
                .getAttributeTypes(), queryData);
        AttributeReader aReader = new JoiningAttributeReader(attrReaders);

        FeatureReader fReader;

        try {
            FIDReader fidReader = new ResultSetFIDReader(queryData,
                    schema.getTypeName(), 1);
            fReader = new FIDFeatureReader(aReader, fidReader, schema);
        } catch (SchemaException e) {
            throw new DataSourceException("Error creating schema", e);
        }

        if ((postFilter != null) && postFilter != Filter.ALL) {
            fReader = new FilteringFeatureReader(fReader, postFilter);
        }

        return fReader;
    }

    /**
     * Builds the AttributeReaders from the QueryData and the array of
     * attribute types. This is private since the default implementation shoud
     * not need to be overriden.
     * 
     * <p>
     * Subclasses can provide custom attribute readers for  the basic types by
     * overriding createResultSetReader().  createResultSetReader has
     * parameters that define a range of columns in the Result to read. The
     * default implementation of this method creates a
     * RangedResultSetAttributeReader instance.
     * </p>
     * 
     * <p>
     * Subclasses must provide a geometry attribute reader by implementing
     * createGeometryReader().  This must provide an AttributeReader capable
     * of  reading a geometry at a single column.  This will be called when
     * isGeometry() on a attribute returns true.
     * </p>
     * TODO: Should this be final??
     *
     * @param attrTypes The attribute types to create a list of
     *        AttributeReaders for.
     * @param queryData The Query result resources for the readers.
     *
     * @return The list of attribute readers.
     *
     * @throws IOException If an error occurs building the readers.  It seems
     *         to make sense that if we can't get readers for all the
     *         attribute we shoudl bomb out. (??)
     */
    protected final AttributeReader[] buildAttributeReaders(
        AttributeType[] attrTypes, QueryData queryData)
        throws IOException {
        List attrReaders = new ArrayList();
        List basicAttrTypes = new ArrayList();

        for (int i = 0; i < attrTypes.length; i++) {
            if (attrTypes[i].isGeometry()) {
                // create a reader for any previous attribute types
                if (basicAttrTypes.size() > 0) {
                    AttributeType[] basicTypes = (AttributeType[]) basicAttrTypes
                        .toArray(new AttributeType[basicAttrTypes.size()]);

                    // startIndex is 1 based and need to add 1 to get past the fid column.
                    int startIndex = i - basicAttrTypes.size() + 2;
                    attrReaders.add(createResultSetReader(basicTypes,
                            queryData, startIndex, i + 2));
                    basicAttrTypes.clear();
                }

                attrReaders.add(createGeometryReader(attrTypes[i], queryData,
                        i + 2));
            } else {
                basicAttrTypes.add(attrTypes[i]);
            }
        }

        // check for left over columns
        if (basicAttrTypes.size() > 0) {
            AttributeType[] basicTypes = (AttributeType[]) basicAttrTypes
                .toArray(new AttributeType[basicAttrTypes.size()]);
            int startIndex = attrTypes.length - basicAttrTypes.size() + 2;

            // + 2 to get past fid and 1 based index           
            attrReaders.add(createResultSetReader(basicTypes, queryData,
                    startIndex, attrTypes.length + 2));
        }

        return (AttributeReader[]) attrReaders.toArray(new AttributeReader[attrReaders
            .size()]);
    }

    /**
     * Executes the SQL Query.
     * 
     * <p>
     * This is private in the expectation that subclasses should not need to
     * change this behaviour.
     * </p>
     * 
     * <p>
     * Jody with a question here - I have stopped this method from closing
     * connection shared by a Transaction. It sill seems like we are leaving
     * connections open by using this method. I have also stopped QueryData
     * from doing the same thing.
     * </p>
     * 
     * <p>
     * Answer from Sean:  Resources for successful queries are closed when
     * close is called on the AttributeReaders constructed with the QueryData.
     * We can't close them here since they need to be open to read from the
     * ResultSet.
     * </p>
     * 
     * <p>
     * Jody AttributeReader question: I looked at the code and Attribute
     * Readers do not close with respect to Transactions (they need to as we
     * can issue a Reader against a Transaction. I have changed the
     * JDBCDataStore.close method to force us to keep track of these things.
     * </p>
     * 
     * <p>
     * SG: I've marked this as final since I don't think it shoudl be
     * overriden, but Im not sure
     * </p>
     *
     * @param tableName DOCUMENT ME!
     * @param sqlQuery The SQL query to execute.
     * @param transaction The Transaction is included here for handling
     *        transaction connections at a later stage.  It is not currently
     *        used.
     * @param concurrency DOCUMENT ME!
     *
     * @return The QueryData object that contains the resources for the query.
     *
     * @throws IOException DOCUMENT ME!
     * @throws DataSourceException If an error occurs performing the query.
     *
     * @task HACK: This is just protected for postgis FeatureWriter purposes.
     *       Should move back to private when that stuff moves more abstract
     *       here.
     */
    protected final QueryData executeQuery(String tableName, String sqlQuery,
        Transaction transaction, int concurrency) throws IOException {
        LOGGER.info("About to execure query: " + sqlQuery);

        Connection conn = null;
        Statement statement = null;
        ResultSet rs = null;

        try {
            conn = getConnection(transaction);
            statement = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
                    concurrency);
            rs = statement.executeQuery(sqlQuery);

            FeatureTypeInfo info = getFeatureTypeInfo(tableName);
            QueryData queryData = new QueryData(info, conn, statement, rs,
                    transaction);

            return queryData;
        } catch (SQLException e) {
            // if an error occurred we close the resources
            String msg = "Error Performing SQL query";
            LOGGER.log(Level.SEVERE, msg, e);
            close(rs);
            close(statement);
            close(conn, transaction, e);
            throw new DataSourceException(msg, e);
        }
    }

    /**
     * Hook for subclass to return a different sql builder.
     *
     * @param typeName The typename for the sql builder.
     *
     * @return A new sql builder.
     *
     * @throws IOException if anything goes wrong.
     */
    public SQLBuilder getSqlBuilder(String typeName) throws IOException {
        return new DefaultSQLBuilder();
    }

    protected AttributeReader createResultSetReader(AttributeType[] attrType,
        QueryData queryData, int startIndex, int endIndex) {
        return new ResultSetAttributeIO(attrType, queryData, startIndex,
            endIndex);
    }

    protected AttributeWriter createResultSetWriter(AttributeType[] attrType,
        QueryData queryData, int startIndex, int endIndex) {
        return new ResultSetAttributeIO(attrType, queryData, startIndex,
            endIndex);
    }

    /**
     * Hook to create the geometry reader for a vendor specific data source.
     *
     * @param attrType The AttributeType to read.
     * @param queryData The data containing the result of the query.
     * @param index The index within the result set to read the data from.
     *
     * @return The AttributeReader that will read the geometry from the
     *         results.
     *
     * @throws DataSourceException If an error occurs building the
     *         AttributeReader.
     */
    protected abstract AttributeReader createGeometryReader(
        AttributeType attrType, QueryData queryData, int index)
        throws IOException;

    protected abstract AttributeWriter createGeometryWriter(
        AttributeType attrType, QueryData queryData, int index)
        throws IOException;

    /**
     * Creates a map of feature types names to feature types.
     *
     * @return
     *
     * @throws IOException DOCUMENT ME!
     * @throws DataSourceException
     */
    private final Map createFeatureTypeMap() throws IOException {
        final int TABLE_NAME_COL = 3;

        Connection conn = null;

        try {
            Map featureTypeMap = new HashMap();
            conn = getConnection(Transaction.AUTO_COMMIT);

            DatabaseMetaData meta = conn.getMetaData();
            String[] tableType = { "TABLE" };
            ResultSet tables = meta.getTables(null, databaseSchemaName, "%",
                    tableType);

            while (tables.next()) {
                String tableName = tables.getString(TABLE_NAME_COL);

                if (allowTable(tableName)) {
                    featureTypeMap.put(tableName, null);
                }
            }

            return featureTypeMap;
        } catch (SQLException sqlException) {
            close(conn, Transaction.AUTO_COMMIT, sqlException);
            conn = null;

            String message = "Error querying database for list of tables:"
                + sqlException.getMessage();
            throw new DataSourceException(message, sqlException);
        } finally {
            close(conn, Transaction.AUTO_COMMIT, null);
        }
    }

    /**
     * Gets a connection from the connection pool.
     *
     * @param transaction DOCUMENT ME!
     *
     * @return A single use connection.
     *
     * @throws IOException DOCUMENT ME!
     * @throws DataSourceException If the connection is not an
     *         OracleConnection.
     */
    protected final Connection getConnection(Transaction transaction)
        throws IOException {
        if (transaction != Transaction.AUTO_COMMIT) {
            // we will need to save a JDBC connenction is
            // transaction.putState( connectionPool, JDBCState )
            //throw new UnsupportedOperationException("Transactions not supported yet");
            JDBCTransactionState state = (JDBCTransactionState) transaction
                .getState(connectionPool);

            if (state == null) {
                state = new JDBCTransactionState(connectionPool);
                transaction.putState(connectionPool, state);
            }

            return state.getConnection();
        }

        try {
            return connectionPool.getConnection();
        } catch (SQLException sqle) {
            throw new DataSourceException("Could not get connection", sqle);
        }
    }

    /**
     * A utility method for closing a Connection. Wraps and logs any exceptions
     * thrown by the close method.
     * 
     * <p>
     * Connections are maintained by a Transaction and we will need to manage
     * them with respect to their Transaction.
     * </p>
     * 
     * <p>
     * Jody here - I am forcing this to be explicit, by requiring you give the
     * Transaction context when you close a connection seems to be the only
     * way to hunt all the cases down. AttributeReaders based on QueryData
     * rely on
     * </p>
     * 
     * <p>
     * I considered accepting an error flag to control Transaction rollback,
     * but I really only want to capture SQLException that force transaction
     * rollback.
     * </p>
     *
     * @param conn The Connection to close. This can be null since it makes it
     *        easy to close connections in a finally block.
     * @param transaction Context for the connection, we will only close the
     *        connection for Transaction.AUTO_COMMIT
     * @param sqlException Error status, <code>null</code> for no error
     */
    protected static void close(Connection conn, Transaction transaction,
        SQLException sqlException) {
        if (conn == null) {
            // Assume we have already closed the connection
            // (allows use of method in a finally block)
            return;
        }

        if (transaction != Transaction.AUTO_COMMIT) {
            // we should not close Transaction connections
            // they will do this themselves when they are finished
            // with the connection.
            if (sqlException != null) {
                // we are closing due to an SQLException                
                try {
                    transaction.rollback();
                } catch (IOException e) {
                    String msg = "Error rolling back transaction in response"
                        + "to connection error. We are in an inconsistent state";
                    LOGGER.log(Level.SEVERE, msg, e);

                    // TODO: this is a bad place to be should we completely gut the transaction 
                    // to prevent damage                                                            
                    // transaction.close();
                }
            }

            return;
        }

        try {
            if (!conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            String msg = "Error closing JDBC Connection";
            LOGGER.log(Level.WARNING, msg, e);
        }
    }

    /**
     * A utility method for closing a ResultSet. Wraps and logs any exceptions
     * thrown by the close method.
     *
     * @param rs The ResultSet to close. This can be null since it makes it
     *        easy to close result sets in a finally block.
     */
    protected static void close(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                String msg = "Error closing JDBC ResultSet";
                LOGGER.log(Level.WARNING, msg, e);
            } catch (Exception e) { // oracle drivers are crapping out

                String msg = "Error closing JDBC ResultSet";

                //LOGGER.log(Level.WARNING, msg, e);
            }
        }
    }

    /**
     * A utility method for closing a Statement. Wraps and logs any exceptions
     * thrown by the close method.
     *
     * @param statement The statement to close. This can be null since it makes
     *        it easy to close statements in a finally block.
     */
    protected static void close(Statement statement) {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                String msg = "Error closing JDBC Statement";
                LOGGER.log(Level.WARNING, msg, e);
            }
        }
    }

    /**
     * Provides a hook for sub classes to filter out specific tables in the
     * data store that are not to be used as geospatial tables.  The default
     * implementation of this method is to allow all tables.
     *
     * @param tablename A table name to check.
     *
     * @return True if the table should be exposed as a FeatureType, false if
     *         it should be ignored.
     */
    protected boolean allowTable(String tablename) {
        return true;
    }

    /**
     * Builds the schema for a table in the database.
     * 
     * <p>
     * This works by retrieving the column information for the table from the
     * DatabaseMetaData object.  It then iterates over the information for
     * each column, calling buildAttributeType(ResultSet) to construct an
     * AttributeType for each column.  The list of attribute types is then
     * turned into a FeatureType that defines the schema.
     * </p>
     * 
     * <p>
     * It is not intended that this method is overriden.  It should provide the
     * required functionality for most sub-classes.  To add AttributeType
     * construction for vendor specific SQL types, such as geometries,
     * override the buildAttributeType(ResultSet) method.
     * </p>
     * 
     * <p>
     * This may become final later.  In fact Ill make it private because I
     * don't think It will need to be overriden.
     * </p>
     *
     * @param typeName The name of the table to construct a feature type for.
     * @param fidColumn The name of the column holding the fid.
     *
     * @return The FeatureType for the table.
     *
     * @throws IOException DOCUMENT ME!
     * @throws DataSourceException This can occur if there is an SQL error or
     *         an error constructing the FeatureType.
     *
     * @see JDBCDataStore#buildAttributeType(ResultSet)
     */
    private FeatureType buildSchema(String typeName, String fidColumn)
        throws IOException {
        final int NAME_COLUMN = 4;
        Connection conn = null;
        ResultSet tableInfo = null;

        try {
            conn = getConnection(Transaction.AUTO_COMMIT);

            DatabaseMetaData dbMetaData = conn.getMetaData();
            tableInfo = dbMetaData.getColumns(null, null, typeName, "%");

            List attributeTypes = new ArrayList();

            while (tableInfo.next()) {
                try {
                    // If an FID column is provided and this is it, don't make an AttributeType for it
                    if ((fidColumn != null)
                            && fidColumn.equals(tableInfo.getString(NAME_COLUMN))) {
                        continue;
                    }

                    AttributeType attributeType = buildAttributeType(tableInfo);
                    attributeTypes.add(attributeType);
                } catch (DataSourceException dse) {
                    String msg = "Error building attribute type. The column will be ignored";
                    LOGGER.log(Level.WARNING, msg, dse);
                }
            }

            AttributeType[] types = (AttributeType[]) attributeTypes.toArray(new AttributeType[0]);

            return FeatureTypeFactory.newFeatureType(types, typeName,
                getNameSpace());
        } catch (SQLException sqlException) {
            close(conn, Transaction.AUTO_COMMIT, sqlException);
            conn = null; // prevent finally block from reclosing
            throw new DataSourceException("SQL Error building FeatureType for "
                + typeName, sqlException);
        } catch (FactoryConfigurationError e) {
            throw new DataSourceException("Error creating FeatureType "
                + typeName, e);
        } catch (SchemaException e) {
            throw new DataSourceException("Error creating FeatureType for "
                + typeName, e);
        } finally {
            close(tableInfo);
            close(conn, Transaction.AUTO_COMMIT, null);
        }
    }

    /**
     * Constructs an AttributeType from a row in a ResultSet. The ResultSet
     * contains the information retrieved by a call to  getColumns() on the
     * DatabaseMetaData object.  This information  can be used to construct an
     * Attribute Type.
     * 
     * <p>
     * The default implementation construct an AttributeType using the default
     * JDBC type mappings defined in JDBCDataStore.  These type mappings only
     * handle native Java classes and SQL standard column types, so to handle
     * Geometry columns, sub classes should override this to check if a column
     * is a geometry column, if it is a geometry column the appropriate
     * determination of the geometry type can be performed. Otherwise,
     * overriding methods should call super.buildAttributeType.
     * </p>
     * 
     * <p>
     * Note: Overriding methods must never move the current row pointer in the
     * result set.
     * </p>
     *
     * @param rs The ResultSet containing the result of a
     *        DatabaseMetaData.getColumns call.
     *
     * @return The AttributeType built from the ResultSet.
     *
     * @throws SQLException If an error occurs processing the ResultSet.
     * @throws DataSourceException Provided for overriding classes to wrap
     *         exceptions caused by other operations they may perform to
     *         determine additional types.  This will only be thrown by the
     *         default implementation if a type is present that is not present
     *         in the TYPE_MAPPINGS.
     */
    protected AttributeType buildAttributeType(ResultSet rs)
        throws SQLException, DataSourceException {
        final int COLUMN_NAME = 4;
        final int DATA_TYPE = 5;
        final int TYPE_NAME = 6;

        String columnName = rs.getString(COLUMN_NAME);
        int dataType = rs.getInt(DATA_TYPE);
        Class type = (Class) TYPE_MAPPINGS.get(new Integer(dataType));

        if (type == null) {
            throw new DataSourceException("Unknown SQL Type: "
                + rs.getString(TYPE_NAME));
        }

        return AttributeTypeFactory.newAttributeType(columnName, type);
    }

    /**
     * Provides a hook for subclasses to determine the SRID of a geometry
     * column.
     * 
     * <p>
     * This allows SRIDs to be determined in a Vendor specific way and to be
     * cached by the default implementation.  To retreive these srids, get the
     * FeatureTypeInfo object for the table and call
     * getSRID(geometryColumnName).  This will allow storage of SRIDs for
     * multiple geometry columns in each table.
     * </p>
     * 
     * <p>
     * If no SRID can be found, subclasses should return -1.  The default
     * implementation always returns -1.
     * </p>
     *
     * @param tableName The name of the table to get the SRID for.
     * @param geometryColumnName The name of the geometry column within the
     *        table to get SRID for.
     *
     * @return The SRID for the geometry column in the table or -1.
     *
     * @throws IOException DOCUMENT ME!
     */
    protected int determineSRID(String tableName, String geometryColumnName)
        throws IOException {
        return -1;
    }

    /**
     * Provides the default implementation of determining the FID column.
     * 
     * <p>
     * The default implementation of determining the FID column name is to use
     * the primary key as the FID column.  If no primary key is present, null
     * will be returned.  Sub classes can override this behaviour to define
     * primary keys for vendor specific cases.
     * </p>
     * 
     * <p>
     * There is an unresolved issue as to what to do when there are multiple
     * primary keys.  Maybe a restriction that table much have a single column
     * primary key is appropriate.
     * </p>
     * 
     * <p>
     * This should not be called by subclasses to retreive the FID column name.
     * Instead, subclasses should call getFeatureTypeInfo(String) to get the
     * FeatureTypeInfo for a feature type and get the fidColumn name from the
     * fidColumn name memeber.
     * </p>
     *
     * @param typeName The name of the table to get a primary key for.
     *
     * @return The name of the primay key column or null if one does not exist.
     *
     * @throws IOException This will only occur if there is an error getting a
     *         connection to the Database.
     */
    protected String determineFidColumnName(String typeName)
        throws IOException {
        final int NAME_COLUMN = 4;
        String fidColumnName = null;
        ResultSet rs = null;
        Connection conn = null;

        try {
            conn = getConnection(Transaction.AUTO_COMMIT);

            DatabaseMetaData dbMetadata = conn.getMetaData();
            rs = dbMetadata.getPrimaryKeys(null, null, typeName);

            if (rs.next()) {
                fidColumnName = rs.getString(NAME_COLUMN);
            }
        } catch (SQLException sqlException) {
            close(conn, Transaction.AUTO_COMMIT, sqlException);
            conn = null; // prevent finally block from reclosing          
            LOGGER.warning("Could not find the primary key - using the default");
        } finally {
            close(rs);
            close(conn, Transaction.AUTO_COMMIT, null);
        }

        return fidColumnName;
    }

    /**
     * Gets the namespace of the data store.
     *
     * @return The namespace.
     */
    public String getNameSpace() {
        return namespace;
    }

    /**
     * Retreives the FeatureTypeInfo object for a FeatureType.
     * 
     * <p>
     * This allows subclasses to get access to the information about a feature
     * type, this includes the schema and the fidColumnName.
     * </p>
     *
     * @param featureTypeName The name of the feature type to get the info for.
     *
     * @return The FeatureTypeInfo object for the named feature type or null if
     *         the feature type does not exist.
     *
     * @throws IOException If an error occurs creating the FeatureTypeInfo.
     */
    protected final FeatureTypeInfo getFeatureTypeInfo(String featureTypeName)
        throws IOException {
        FeatureTypeInfo info = (FeatureTypeInfo) featureTypeMap.get(featureTypeName);

        if (info == null) {
            String fidColumnName = determineFidColumnName(featureTypeName);
            FeatureType schema = buildSchema(featureTypeName, fidColumnName);
            info = new FeatureTypeInfo(featureTypeName, fidColumnName, schema);

            AttributeType[] types = schema.getAttributeTypes();

            for (int i = 0; i < types.length; i++) {
                if (types[i].isGeometry()) {
                    int srid = determineSRID(featureTypeName, types[i].getName());
                    info.putSRID(types[i].getName(), srid);
                }
            }

            featureTypeMap.put(featureTypeName, info);
        }

        return info;
    }

    /**
     * Retrieve a FeatureWriter over entire dataset.
     * <p>
     * Quick notes: This FeatureWriter is often used to add new content, or
     * perform summary calculations over the entire dataset.
     * </p>
     * 
     * <p>
     * Subclass may wish to implement an optimized featureWriter for these
     * operations.
     * </p>
     * <p>
     * It should provide Feature for next() even when hasNext() is
     * <code>false</code>.
     * </p>
     * 
     * <p>
     * Subclasses are responsible for checking with the lockingManger unless
     * they are providing their own locking support.
     * </p>
     *
     * @param typeName
     * @param append
     * @param transaction
     *
     * @return
     *
     * @throws IOException
     * @throws UnsupportedOperationException DOCUMENT ME!
     *
     * @see org.geotools.data.DataStore#getFeatureWriter(java.lang.String,
     *      boolean, org.geotools.data.Transaction)
     */
    public FeatureWriter getFeatureWriter(String typeName, Transaction transaction) throws IOException {
        return getFeatureWriter(typeName,Filter.NONE,transaction);        
    }
    /**
     * Retrieve a FeatureWriter for creating new content.
     * 
     * <p>
     * Subclass may wish to implement an optimized featureWriter for this
     * operation. One based on prepaired statemnts is a possibility, as we
     * do not require a ResultSet.
     * </p>
     * <p>
     * To allow new content the FeatureWriter should provide Feature for next()
     * even when hasNext() is <code>false</code>.
     * </p>
     * 
     * <p>
     * Subclasses are responsible for checking with the lockingManger unless
     * they are providing their own locking support.
     * </p>
     *
     * @param typeName
     * @param append
     * @param transaction
     *
     * @return
     *
     * @throws IOException
     * @throws UnsupportedOperationException DOCUMENT ME!
     *
     * @see org.geotools.data.DataStore#getFeatureWriter(java.lang.String,
     *      boolean, org.geotools.data.Transaction)
     */    
    public FeatureWriter getFeatureWriterAppend(String typeName,
                                              Transaction transaction) throws IOException {
        FeatureWriter writer = getFeatureWriter( typeName, transaction );
        while( writer.hasNext()){
            writer.next(); // this would be a use for skip then :-)
        }
        return writer;
    }    

    /**
     * Aquire FetureWriter for modification of contents specifed by filter.
     * 
     * <p>
     * Quick notes: This FeatureWriter is often used to remove contents
     * specified by the provided filter, or perform summary calculations.
     * </p>
     * 
     * <p>
     * It is not used to provide new content and should return <code>null</code>
     * for next() when hasNext() returns <code>false</code>.
     * </p>
     * 
     * <p>
     * Subclasses are responsible for checking with the lockingManger unless
     * they are providing their own locking support.
     * </p>
     *
     * @param typeName
     * @param filter
     * @param transaction
     *
     * @return
     *
     * @throws IOException If typeName could not be located
     * @throws NullPointerException If the provided filter is null
     * @throws DataSourceException See IOException
     *
     * @see org.geotools.data.DataStore#getFeatureWriter(java.lang.String,
     *      org.geotools.filter.Filter, org.geotools.data.Transaction)
     */
    public FeatureWriter getFeatureWriter(String typeName, Filter filter,
        Transaction transaction) throws IOException {
        if (filter == null) {
            throw new NullPointerException("getFeatureReader requires Filter: "
                + "did you mean Filter.NONE?");
        }
        
        if (transaction == null) {
            throw new NullPointerException("getFeatureReader requires Transaction: " +
                    "did you mean Transaction.AUTO_COMMIT");
        }
        FeatureType schema = getSchema(typeName);
        if( filter.equals( Filter.ALL )){
            return new EmptyFeatureWriter( schema ); 
        }

        FeatureTypeInfo info = getFeatureTypeInfo(typeName);
        LOGGER.fine("getting feature writer for " + typeName + ": " + info);

        SQLBuilder sqlBuilder = getSqlBuilder(typeName);
        Filter preFilter = sqlBuilder.getPreQueryFilter(filter);
        Filter postFilter = sqlBuilder.getPostQueryFilter(filter);
        Query query = new DefaultQuery(typeName, filter);
        String sqlQuery = constructQuery(query, getAttTypes(query));

        QueryData queryData = executeQuery(typeName, sqlQuery, transaction,
                ResultSet.CONCUR_UPDATABLE);
        FeatureReader reader = createFeatureReader(info.getSchema(),
                postFilter, queryData);
        AttributeWriter[] writers = buildAttributeWriters(info.getSchema()
                                                              .getAttributeTypes(),
                queryData);
        AttributeWriter joinedW = new JoiningAttributeWriter(writers);
        FeatureWriter writer = createFeatureWriter(reader, joinedW, queryData);

        if( getLockingManager() != null && getLockingManager() instanceof InProcessLockingManager ){
            InProcessLockingManager inProcess = (InProcessLockingManager) getLockingManager();
            writer = inProcess.checkedWriter( writer, transaction );
        }
        
        if (postFilter != null && postFilter != Filter.ALL) {
            writer = new FilteringFeatureWriter(writer, postFilter);
        }

        return writer;
    }
    
    protected JDBCFeatureWriter createFeatureWriter(FeatureReader fReader,
        AttributeWriter writer, QueryData queryData) throws IOException {
        LOGGER.fine("returning jdbc feature writer");

        return new JDBCFeatureWriter(fReader, writer, queryData);
    }

    protected final AttributeWriter[] buildAttributeWriters(
        AttributeType[] attrTypes, QueryData queryData)
        throws IOException {
        List attrWriters = new ArrayList();
        List basicAttrTypes = new ArrayList();

        for (int i = 0; i < attrTypes.length; i++) {
            if (attrTypes[i].isGeometry()) {
                // create a reader for any previous attribute types
                if (basicAttrTypes.size() > 0) {
                    AttributeType[] basicTypes = (AttributeType[]) basicAttrTypes
                        .toArray(new AttributeType[basicAttrTypes.size()]);

                    // startIndex is 1 based and need to add 1 to get past the fid column.
                    int startIndex = i - basicAttrTypes.size() + 2;
                    attrWriters.add(createResultSetWriter(basicTypes,
                            queryData, startIndex, i + 2));
                    basicAttrTypes.clear();
                }

                attrWriters.add(createGeometryWriter(attrTypes[i], queryData,
                        i + 2));
            } else {
                basicAttrTypes.add(attrTypes[i]);
            }
        }

        // check for left over columns
        if (basicAttrTypes.size() > 0) {
            AttributeType[] basicTypes = (AttributeType[]) basicAttrTypes
                .toArray(new AttributeType[basicAttrTypes.size()]);
            int startIndex = attrTypes.length - basicAttrTypes.size() + 2;

            // + 2 to get past fid and 1 based index           
            attrWriters.add(createResultSetWriter(basicTypes, queryData,
                    startIndex, attrTypes.length + 2));
        }

        return (AttributeWriter[]) attrWriters.toArray(new AttributeWriter[attrWriters
            .size()]);
    }

    /**
     * Gets the attribute types from the query.  If all are requested then
     * returns all attribute types of this query.  If only certain
     * propertyNames are requested then this returns the correct attribute
     * types, throwing an exception is they can not be found.
     * 
     * <p>
     * This should be in an abstract class.
     * </p>
     *
     * @param attNames contains the propertyNames.
     * @param schema DOCUMENT ME!
     *
     * @return the array of attribute types to be returned by getFeature.
     *
     * @throws IOException DOCUMENT ME!
     * @throws DataSourceException if query contains a propertyName that is not
     *         a part of this type's schema.
     *
     * @task REVISIT: consider returning a FeatureType.  This should completely
     *       clone the schema passed in, must get the namespace and typename
     *       right, with the abbreviated attributes.  Also the inheritance
     *       needs to be figured out.
     */
    private AttributeType[] getAttTypes(List attNames, FeatureType schema)
        throws IOException {
        AttributeType[] schemaTypes = schema.getAttributeTypes();

        AttributeType[] retAttTypes = new AttributeType[attNames.size()];
        int retPos = 0;

        for (int i = 0, n = schemaTypes.length; i < n; i++) {
            String schemaTypeName = schemaTypes[i].getName();

            if (attNames.contains(schemaTypeName)) {
                retAttTypes[retPos++] = schemaTypes[i];
            }
        }

        //TODO: better error reporting, and completely test this method.
        if (attNames.size() != retPos) {
            //REVISIT: not to clear what's going on here.  Basically
            //all names should be found in the schema.
            String msg = "attempted to request a property, "
                + " that is not part of the schema ";
            throw new DataSourceException(msg);
        }

        return retAttTypes;
    }

    private AttributeType[] getAttTypes(Query query) throws IOException {
        FeatureType schema = getSchema(query.getTypeName());

        if (query.retrieveAllProperties()) {
            return schema.getAttributeTypes();
        } else {
            return getAttTypes(Arrays.asList(query.getPropertyNames()), schema);
        }
    }

    /**
     * Locking manager used for this DataStore.
     * 
     * <p>
     * By default AbstractDataStore makes use of InProcessLockingManager.
     * </p>
     *
     * @return
     *
     * @see org.geotools.data.DataStore#getLockingManager()
     */
    public LockingManager getLockingManager() {
        return lockingManager;
    }

    /**
     * Stores information about known FeatureTypes.
     *
     * @author Sean Geoghegan, Defence Science and Technology Organisation.
     */
    public static class FeatureTypeInfo {
        private String featureTypeName;
        private String fidColumnName;
        private FeatureType schema;
        private Map sridMap = new HashMap();

        public FeatureTypeInfo(String typeName, String fidColumn,
            FeatureType schema) {
            this.featureTypeName = typeName;
            this.fidColumnName = fidColumn;
            this.schema = schema;
        }

        /**
         * DOCUMENT ME!
         *
         * @return
         */
        public String getFeatureTypeName() {
            return featureTypeName;
        }

        /**
         * DOCUMENT ME!
         *
         * @return
         */
        public String getFidColumnName() {
            return fidColumnName;
        }

        /**
         * DOCUMENT ME!
         *
         * @return
         */
        public FeatureType getSchema() {
            return schema;
        }

        /**
         * Get the DataStore specific SRID for a geometry column
         *
         * @param geometryAttributeName The name of the Geometry column to get
         *        the srid for.
         *
         * @return The srid of the geometry column.  This will only be present
         *         if determineSRID(String) of JDBCDataStore has been
         *         overridden.  If there is no SRID registered -1 will be
         *         returned.
         */
        public int getSRID(String geometryAttributeName) {
            int srid = -1;

            Integer integer = (Integer) sridMap.get(geometryAttributeName);

            if (integer != null) {
                srid = integer.intValue();
            }

            return srid;
        }

        public Map getSRIDs() {
            return Collections.unmodifiableMap(sridMap);
        }

        /**
         * Puts the srid for a geometry column in the internal map.
         *
         * @param geometryColumnName The geometry column name.
         * @param srid The SRID of the geometry column.
         */
        void putSRID(String geometryColumnName, int srid) {
            sridMap.put(geometryColumnName, new Integer(srid));
        }

        public String toString() {
            return "typeName = " + featureTypeName + ", fidCol = "
            + fidColumnName + ", schema: " + schema + "srids: " + sridMap;
        }
    }

    /**
     * Provides an encapsulation of the connection, statement and result set of
     * a JDBC query.  This class solves the problem of "Where do we close JDBC
     * resources when they are being used by multiple AttributeReaders?".
     * 
     * <p>
     * An alternative solution would be to have the FeatureReader manage the
     * resources, however this will pose problems when combining JDBC
     * Attribute Readers with other AttributeReaders and FeatureReaders.  The
     * QueryData solution works by holding all the needed resources and
     * providing a close method that closes all the resources.  When the close
     * method is called any readers that are registered as QueryDataListeners
     * for the query data will be notified.  This will allow one
     * AttributeReader to close the resources and any other AttributeReaders
     * using the same resources will find out about it.
     * </p>
     *
     * @author Sean Geoghegan, Defence Science and Technology Organisation.
     */
    public static class QueryData {
        private FeatureTypeInfo featureTypeInfo;
        private Connection conn;
        private ResultSet resultSet;
        private Statement statement;
        private ArrayList listeners = new ArrayList();
        private Transaction transaction;
        private boolean isInserting = false;

        /**
         * The constructor for the QueryData object.
         *
         * @param featureTypeInfo DOCUMENT ME!
         * @param conn The connection to the DB.
         * @param statement The statement used to execute the query.
         * @param resultSet The result set.
         * @param transaction DOCUMENT ME!
         */
        public QueryData(FeatureTypeInfo featureTypeInfo, Connection conn,
            Statement statement, ResultSet resultSet, Transaction transaction) {
            this.featureTypeInfo = featureTypeInfo;
            this.conn = conn;
            this.resultSet = resultSet;
            this.statement = statement;
            this.transaction = transaction;
        }

        /**
         * Gets the result set.
         *
         * @return The result of the query.
         */
        public ResultSet getResultSet() {
            return resultSet;
        }

        /**
         * Closes all the resources.
         * 
         * <p>
         * All resources are closed the QueryDataListener.queryDataClosed() is
         * called on all QueryDataListeners.
         * </p>
         * 
         * <p>
         * The Connection is handled differently depending on if this is an
         * AUTO_COMMIT Transaction or not.
         * 
         * <ul>
         * <li>
         * AUTO_COMMIT connections are closed
         * </li>
         * <li>
         * Transaction connection are left open if <code>error</code> is false
         * </li>
         * <li>
         * Transaction connectino are left open and the Transaction is rolled
         * back if error is <code>true</code>.
         * </li>
         * </ul>
         * </p>
         * 
         * <p>
         * Jody Here: I have forced this method to handle maintaining
         * conneciton status as it knows about Transactions and
         * AttributeReaders do not.
         * </p>
         * 
         * <p>
         * <b>USEAGE GUIDELINES:</b>
         * </p>
         * 
         * <ul>
         * <li>
         * sqlException != null: When ever we have an SQLException we will need
         * to force the any Transaction associated with this conneciton to
         * rollback.
         * </li>
         * <li>
         * sqlException == null: When we are finished with resources we will
         * call close( null) which will return AUTO_COMMIT connections to the
         * pool and leave Transaction connections open.
         * </li>
         * </ul>
         *
         * @param sqlException DOCUMENT ME!
         */
        public void close(SQLException sqlException) {
            System.err.println("Close called on query data");
            JDBCDataStore.close(resultSet);
            JDBCDataStore.close(statement);
            JDBCDataStore.close(conn, transaction, sqlException);
            resultSet = null;
            //transaction = null;
            statement = null;
            conn = null;
            fireCloseEvent();
        }

        /**
         * Adds a QueryDataListener to the list of listeners.
         *
         * @param l The Listener to the add.
         */
        public void addQueryDataListener(QueryDataListener l) {
            listeners.add(l);
        }

        /**
         * Removes a QueryDataListener for the list of listeners.
         *
         * @param l The Listener to remove.
         */
        public void removeQueryDataListener(QueryDataListener l) {
            listeners.remove(l);
        }

        /**
         * Fires the close event.
         */
        protected void fireCloseEvent() {
            List clone = (List) listeners.clone();

            for (Iterator iter = clone.iterator(); iter.hasNext();) {
                QueryDataListener l = (QueryDataListener) iter.next();
                l.queryDataClosed(this);
            }
        }

        /**
         * Returns transaction, this Query data is opperating against.
         * 
         * <p>
         * Please note that if transacstion is not Transaction.AUTO_COMMIT you
         * will need to call transaction.rollback() in the event of an
         * SQLException.
         * </p>
         *
         * @return The current Transaction
         */
        public Transaction getTransaction() {
            return transaction;
        }

        /**
         * A convience method that ensures we handle rollback on error
         * correctly.
         * 
         * <p>
         * Returns an IOException encapsulating the sqlException after
         * correctly rolling back the current Transaction. Rollback only
         * occurs if we are not using Transacstion.AUTO_COMMIT.
         * </p>
         * TODO: chris is this a good idea?
         *
         * @param message DOCUMENT ME!
         * @param sqlException DOCUMENT ME!
         *
         * @return DOCUMENT ME!
         */
        public IOException cast(String message, SQLException sqlException) {
            if (transaction != Transaction.AUTO_COMMIT) {
                try {
                    transaction.rollback();
                } catch (IOException e) {
                    // problem with rollback
                }

                return new DataSourceException(message
                    + "(transaction rolled back)", sqlException);
            } else {
                return new DataSourceException(message, sqlException);
            }
        }

        /**
         *
         */
        public Connection getConnection() {
            return conn;
        }

        /**
         * DOCUMENT ME!
         *
         * @return
         */
        public FeatureTypeInfo getFeatureTypeInfo() {
            return featureTypeInfo;
        }
        
        /**
         * @return Returns the isInserting.
         */
        public boolean isInserting() {
            return isInserting;
        }

        /**
         * @param isInserting The isInserting to set.
         */
        public void setInserting(boolean isInserting) {
            this.isInserting = isInserting;
        }

    }

    protected class JDBCFeatureWriter implements FeatureWriter {
        protected QueryData queryData;
        protected AttributeWriter writer;
        protected Feature live = null; // current for FeatureWriter 
        protected Feature current = null; // copy of live returned to user    
        protected FeatureReader fReader;

        /**
         * <p>
         * Details
         * </p>
         *
         * @param fReader DOCUMENT ME!
         * @param writer DOCUMENT ME!
         * @param queryData
         *
         * @throws IOException
         */
        public JDBCFeatureWriter(FeatureReader fReader, AttributeWriter writer,
            QueryData queryData) throws IOException {
            this.queryData = queryData;
            this.fReader = fReader;
            this.writer = writer;
        }

        public FeatureType getFeatureType() {
            return queryData.getFeatureTypeInfo().getSchema();
        }

        public Feature next() throws IOException {
            if (queryData == null) {
                throw new IOException("FeatureWriter has been closed");
            }

            FeatureType featureType = queryData.getFeatureTypeInfo().getSchema();

            if (hasNext()) {
                try {
                    // existing content
                    live = fReader.next();
                    current = featureType.duplicate(live);
                    LOGGER.info("Calling next on writer");
                    writer.next();
                } catch (IllegalAttributeException e) {
                    throw new DataSourceException("Unable to edit "
                        + live.getID() + " of " + featureType.getTypeName(), e);
                }
            } else {
                // new content 
                live = null;

                try {
                    current = DataUtilities.template(featureType);
                    queryData.getResultSet().moveToInsertRow();
                    writer.next();
                } catch (IllegalAttributeException e) {
                    throw new DataSourceException(
                        "Unable to add additional Features of "
                        + featureType.getTypeName(), e);
                } catch (SQLException e) {
                    throw new DataSourceException("Unable to move to insert row.",
                        e);
                }
            }

            return current;
        }

        public void remove() throws IOException {
            if (queryData == null) {
                throw new IOException("FeatureWriter has been closed");
            }

            if (current == null) {
                throw new IOException("No feature available to remove");
            }

            if (live != null) {
                Envelope bounds = live.getBounds();
                live = null;
                current = null;

                try {
                    queryData.getResultSet().deleteRow();
                    listenerManager.fireFeaturesRemoved(queryData.getFeatureTypeInfo()
                                                                 .getFeatureTypeName(),
                        queryData.getTransaction(), bounds);
                } catch (SQLException sqle) {
                    String message = "problem deleting row";

                    if (queryData.getTransaction() != Transaction.AUTO_COMMIT) {
                        queryData.getTransaction().rollback();
                        message += "(transaction canceled)";
                    }

                    throw new DataSourceException(message, sqle);
                }
            } else {
                // cancel add new content
                current = null;
            }
        }

        /**
         * What to do with inserts and FIDS???
         *
         * @throws IOException DOCUMENT ME!
         * @throws DataSourceException DOCUMENT ME!
         */
        public void write() throws IOException {
            if (queryData == null) {
                throw new IOException("FeatureWriter has been closed");
            }

            if (current == null) {
                throw new IOException("No feature available to write");
            }

            LOGGER.fine("write called, live is " + live + " and cur is "
                + current);

            if (live != null) {
                if (live.equals(current)) {
                    // no modifications made to current
                    live = null;
                    current = null;
                } else {
                    ResultSet rs = queryData.getResultSet();
                    doUpdate(live, current);

                    try {
                        rs.updateRow();
                    } catch (SQLException sqlException) {
                        // This is a serious problem when working against
                        // a transaction connection, queryData knows how to
                        // handle it though
                        queryData.close(sqlException);
                        throw new DataSourceException("Error updating row",
                            sqlException);
                    }

                    Envelope bounds = new Envelope();
                    bounds.expandToInclude(live.getBounds());
                    bounds.expandToInclude(current.getBounds());
                    listenerManager.fireFeaturesChanged(queryData.getFeatureTypeInfo()
                                                                 .getFeatureTypeName(),
                        queryData.getTransaction(), bounds);
                    live = null;
                    current = null;
                }
            } else {
                // Do an insert - TODO not yet sure how to handle new FIDs, any ideas???
                LOGGER.fine("doing insert in jdbc featurewriter");

                ResultSet rs = queryData.getResultSet();

                try {
                    doInsert(current);
                } catch (SQLException e) {
                    throw new DataSourceException("Row adding failed.", e);
                }

                listenerManager.fireFeaturesAdded(queryData.getFeatureTypeInfo()
                                                           .getFeatureTypeName(),
                    queryData.getTransaction(), current.getBounds());
                current = null;
            }
        }

        /**
         * Protected method to perform an insert. Postgis needs to do this
         * seperately.  With updates it can just override the geometry stuff,
         * using a direct sql update statement, but for inserts it can't
         * update a row that doesn't exist yet.
         *
         * @param current DOCUMENT ME!
         *
         * @throws IOException DOCUMENT ME!
         * @throws SQLException DOCUMENT ME!
         * @throws DataSourceException DOCUMENT ME!
         */
        protected void doInsert(Feature current)
            throws IOException, SQLException {
            ResultSet rs = queryData.getResultSet();
            //rs.moveToInsertRow();  This gets done in the result writer.  Might need to revisit this.

            try {
                System.out.println(current.getID());
                doUpdate(DataUtilities.template(current.getFeatureType()),current);
                // TODO This is a bit of a hack
                String fid = current.getID();
                fid = fid.substring(fid.lastIndexOf("-")+1);
                rs.updateObject(1, Integer.valueOf(fid));
            } catch (IllegalAttributeException e) {
                throw new DataSourceException("Unable to do insert", e);
            }

            rs.insertRow();
            rs.moveToCurrentRow();
        }

        private void doUpdate(Feature live, Feature current)
            throws IOException {
            try {
                //Can we create for array getAttributes more efficiently?
                for (int i = 0; i < current.getNumberOfAttributes(); i++) {
                    Object curAtt = current.getAttribute(i);
                    Object liveAtt = live.getAttribute(i);

                    if ((live == null)
                            || !DataUtilities.attributesEqual(curAtt, liveAtt)) {
                        LOGGER.info("modifying att# " + i + " to " + curAtt);
                        writer.write(i, curAtt);
                    }
                }
            } catch (IOException ioe) {
                String message = "problem modifying row";
                if (queryData.getTransaction() != Transaction.AUTO_COMMIT) {
                    queryData.getTransaction().rollback();
                    message += "(transaction canceled)";
                }

                throw ioe;
            }
        }

        public boolean hasNext() throws IOException {
            if (queryData == null) {
                throw new IOException("FeatureWriter has been closed");
            }

            // I think this is right, some should sanity check me though.
            // Not sure - having || highlighted another problem for me,
            // with resultsetfidreader, that I fixed.  But if fReader says
            // it doesn't have next, but the writer does, then the JDBCWriter
            // will return true and choke when next is called, since it will
            // call featureReader.next().  Of course if the writer actually
            // does have next, then that's wrong too, since it will be
            // modifying a feature when the user thinks he's making a new one.
            // Perhaps we should throw an exception if they're not the same
            // length?
            return fReader.hasNext() && writer.hasNext();
        }

        public void close() throws IOException {
            if (fReader != null) {
                fReader.close();
            }

            if (writer != null) {
                writer.close();
                writer = null;
            }

            if (queryData != null) {
                queryData.close(null);
                queryData = null;
            }

            current = null;
            live = null;
        }
    }
}
