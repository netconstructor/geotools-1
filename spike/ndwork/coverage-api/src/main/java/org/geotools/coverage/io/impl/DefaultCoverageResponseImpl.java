/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2008, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.coverage.io.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.geotools.coverage.io.CoverageRequest;
import org.geotools.coverage.io.CoverageResponse;
import org.geotools.util.NullProgressListener;
import org.opengis.coverage.Coverage;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.util.ProgressListener;
/**
 * 
 * @author Simone Giannecchini, GeoSolutions
 *
 */
@SuppressWarnings("unchecked")
public class DefaultCoverageResponseImpl implements CoverageResponse {
	
	private List exceptions=Collections.synchronizedList(new ArrayList<Exception>());
	
	private String handle=null;
	
	private CoverageRequest originatingRequest=null;
	
	private List results= Collections.synchronizedList(new ArrayList<GridCoverage>());
	
	private Status status= Status.UNAVAILABLE;
	

	public Collection<? extends Exception> getExceptions() {
		synchronized (this.exceptions) {
			return new ArrayList<Exception>(this.exceptions);
		}
	}
	
	public void addExceptions(final Collection<? extends Exception> exceptions ){
		synchronized (this.exceptions) {
			this.exceptions.add(exceptions);
		}	
	}
	
	public void addException(Exception exception ){
		synchronized (this.exceptions) {
			this.exceptions.add(exception);
		}	
	}

	public String getHandle() {
		return this.handle;
	}
	
	public void setHandle(final String handle){
		this.handle=handle;
	}

	public CoverageRequest getRequest() {
		return this.originatingRequest;
	}
	
	public void setRequest(final CoverageRequest request){
		this.originatingRequest=request;
	}

	public Collection<? extends Coverage> getResults(ProgressListener listener) {
		if( listener == null ) listener = new NullProgressListener();
		listener.started();
		try {
			synchronized (this.results) {
				return new ArrayList<GridCoverage>(this.results);
			}
		}
		finally {
			listener.complete();
		}
	}
	
	public void addResults(final Collection<? extends GridCoverage> results) {
		synchronized (this.results) {
			this.results.add(results);
		}
	}	
	
	public void addResult(GridCoverage grid ) {
		synchronized (this.results) {
			this.results.add(grid);
		}
	}		

	public Status getStatus() {
		return status;
	}
	
	public void setStatus(final Status status){
		this.status= status;
	}

}