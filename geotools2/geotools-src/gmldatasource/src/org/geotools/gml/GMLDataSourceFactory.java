/*
 * GMLDataSourceFactory.java
 *
 * Created on March 4, 2003, 3:44 PM
 */

package org.geotools.gml;

import java.net.URL;
import java.util.HashMap;
import org.geotools.data.DataSource;
import org.geotools.data.DataSourceException;

/**
 *
 * @author  jamesm
 */
public class GMLDataSourceFactory implements org.geotools.data.DataSourceFactorySpi {
    
    /** Creates a new instance of GMLDataSourceFactory */
    public GMLDataSourceFactory() {
    }
    
    public boolean canProcess(HashMap params) {
        if(!params.containsKey("url")){
            return false;
        }
        String url =  (String)params.get("url");
        if(!url.toUpperCase().endsWith("GML")){
            return false;
        }
        return true;
    }
    
    public DataSource createDataSource(HashMap params)  throws DataSourceException {      
        if(!canProcess(params)){
            return null;
        }
        String location = (String)params.get("url");
        GMLDataSource ds = new GMLDataSource(location);
        return ds;
    }
    
    public String getDescription() {
        return "Geographic Markup Language (GML) files version 2.x";
    }
    
}
