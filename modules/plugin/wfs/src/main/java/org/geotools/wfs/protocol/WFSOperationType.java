/*
 *    GeoTools - OpenSource mapping toolkit
 *    http://geotools.org
 *    (C) 2004-2008, Geotools Project Managment Committee (PMC)
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
 */
package org.geotools.wfs.protocol;

/**
 * Enumeration for the possible operations a WFS may implement.
 * 
 * @author Gabriel Roldan
 * @version $Id$
 * @since 2.5.x
 * @source $URL$
 */
public enum WFSOperationType {
    GET_CAPABILITIES("GetCapabilities"), 
    DESCRIBE_FEATURETYPE("DescribeFeatureType"),
    GET_FEATURE("GetFeature"), 
    GET_GML_OBJECT("GetGmlObject"), 
    LOCK_FEATURE("LockFeature"), 
    GET_FEATURE_WITH_LOCK("GetFeatureWithLock"), 
    TRANSACTION("Transaction");

    private String operationName;

    private WFSOperationType(String operationName){
        this.operationName = operationName;
    }
    
    public String getName(){
        return operationName;
    }
}
