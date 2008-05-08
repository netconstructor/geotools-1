/* $Id$
 *
 * Created on 4/08/2003
 */
package org.geotools.data.oracle;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.jdbc.ConnectionPoolManager;
import org.geotools.test.OnlineTestCase;

/**
 * Test the datastore factories
 * 
 * @author Andrea Aime
 */
public class OracleDataStoreFactoryOnlineTest extends OnlineTestCase {
    /** The Oracle driver class name */
    private static final String JDBC_DRIVER = "oracle.jdbc.driver.OracleDriver";
    boolean GO = true;
    /**
     * Creates a new OracleDataStoreFactoryTest Test.
     * 
     * @throws ClassNotFoundException If the driver cannot be found
     */
    public OracleDataStoreFactoryOnlineTest() throws ClassNotFoundException {
        super();
        try {
        	Class.forName(JDBC_DRIVER);
        }
        catch( Throwable t ){
        	GO = false;
        	// must be running off dummy jar!
        }
    }

    /**
     * Removes the properties
     * 
     * @throws Exception
     * @see junit.framework.TestCase#tearDown()
     */
    protected void disconnect() throws Exception {
        super.disconnect();
        ConnectionPoolManager manager = ConnectionPoolManager.getInstance();
        manager.closeAll();
    }
    
    public void testDataStoreFactory() throws Exception {
        if( !GO ) return;
        OracleDataStoreFactory factory = new OracleDataStoreFactory();
        checkFactoryNamespace(factory);
    }
    
    public void testOciDataStoreFactory() throws Exception {
        if( !GO ) return;
        OracleOCIDataStoreFactory factory = new OracleOCIDataStoreFactory();
        checkFactoryNamespace(factory);
    }

    public void checkFactoryNamespace(DataStoreFactorySpi factory) throws Exception {
    	Map map = new HashMap();
        map.put("host", fixture.getProperty("host"));
        map.put("port", fixture.getProperty("port"));
        map.put("instance", fixture.getProperty("instance"));
        map.put("user", fixture.getProperty("user"));
        map.put("passwd", fixture.getProperty("passwd"));
        map.put("dbtype", "oracle");
        map.put("alias", fixture.getProperty("instance"));
        map.put("namespace", null);
        
        assertTrue(factory.canProcess(map));
        OracleDataStore store = (OracleDataStore) factory.createDataStore(map); 
        assertNull(store.getNameSpace());
        
        map.put("schema", fixture.getProperty("user").toUpperCase());
        store = (OracleDataStore) factory.createDataStore(map); 
        assertNull(store.getNameSpace());
        
        map.put("namespace", "topp");
        store = (OracleDataStore) factory.createDataStore(map); 
        assertEquals(new URI("topp"), store.getNameSpace());
    }

    protected String getFixtureId() {
        return "oracle.test";
    }
}
