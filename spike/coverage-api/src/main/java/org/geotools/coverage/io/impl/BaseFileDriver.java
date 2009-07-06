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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.coverage.io.service.FileBasedRasterService;
import org.geotools.coverage.io.service.RasterService;
import org.geotools.data.Parameter;
import org.geotools.factory.Hints;
import org.geotools.util.SimpleInternationalString;
import org.opengis.util.ProgressListener;

/**
 * Base class extending {@link BaseRasterService} leveraging on URLs.
 */
public abstract class BaseFileDriver<T> extends BaseRasterService<T> implements FileBasedRasterService<T> {
	/**
	 * Parameter "url" used to indicate to a local file or remote resource being
	 * accessed as a coverage.
	 */
	public final static Parameter<URL> URL = new Parameter<URL>("url",
			java.net.URL.class, new SimpleInternationalString("URL"),
			new SimpleInternationalString(
					"Url to a local file or remote location"));

	/**
	 * Parameter "file" used to indicate to indicate a local file.
	 */
	public final static Parameter<File> FILE = new Parameter<File>("file",
			File.class, new SimpleInternationalString("File"),
			new SimpleInternationalString( "Local file"));

	/**
	 * Utility method to convert a URL to a file; or return null
	 * if not possible.
	 * @param url
	 * @return File or null if not available
	 */
	public static File toFile( URL url ){
		if( url == null ) return null;
		if (url.getProtocol().equalsIgnoreCase("file")) {
			try {
				return new File(URLDecoder.decode(url.getFile(), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				return null;
			}
		}
		return null;				
	}
	
	/**
	 * Utility method to help convert a URL to a file if possible.
	 * 
	 * @param url
	 * @return File
	 */
	public static File urlToFile(URL url) {
		URI uri;
		try {
			// this is the step that can fail, and so
			// it should be this step that should be fixed
			uri = url.toURI();
		} catch (URISyntaxException e) {
			// OK if we are here, then obviously the URL did
			// not comply with RFC 2396. This can only
			// happen if we have illegal unescaped characters.
			// If we have one unescaped character, then
			// the only automated fix we can apply, is to assume
			// all characters are unescaped.
			// If we want to construct a URI from unescaped
			// characters, then we have to use the component
			// constructors:
			try {
				uri = new URI(url.getProtocol(), url.getUserInfo(), url
						.getHost(), url.getPort(), url.getPath(), url
						.getQuery(), url.getRef());
			} catch (URISyntaxException e1) {
				// The URL is broken beyond automatic repair
				throw new IllegalArgumentException("broken URL: " + url);
			}
		}
		return new File(uri);
	}

	private List<String> fileExtensions;

	protected BaseFileDriver(final String name, final String description,
			final String title, final Hints implementationHints,
			final List<String> fileExtensions) {
		super(name, description, title, implementationHints);
		this.fileExtensions = new ArrayList<String>(fileExtensions);
	}

	public List<String> getFileExtensions() {
		return new ArrayList<String>(fileExtensions);
	}

	/**
	 * Test to see if this driver is suitable for connecting to the coverage
	 * storage pointed to by the specified {@link URL} source.
	 * 
	 * @param source
	 *            URL a {@link URL} to a real file (may not be local)
	 * 
	 * @return True when this driver can resolve and read a coverage storage
	 *         specified by the {@link URL}.
	 */
	public abstract boolean canConnect(URL source);

	/**
	 * Subclass can override to define required parameters.
	 * <p>
	 * Default implementation expects a single URL indicating the
	 * location.
	 * 
	 * @return
	 */
	protected Map<String, Parameter<?>> defineParameterInfo(){
		HashMap<String, Parameter<?>> info = new HashMap<String, Parameter<?>>();
		info.put(URL.key, URL);
		return info;
	}
	/**
	 * Create a {@link RasterStorage}.
	 * 
	 * The {@link RasterService} will attempt to create the named
	 * {@link RasterStorage} in a format specific fashion. Full featured
	 * drivers will create all associated files, database objects, or whatever
	 * is appropriate.
	 * 
	 * 
	 * @param params
	 *            Map of <key,value> pairs used to specify how to create the
	 *            target {@link RasterStorage}.
	 * @param hints
	 *            map of <key,value> pairs which can be used to control the
	 *            behaviour of the entire library.
	 * @param listener
	 *            which can be used to listen for progresses on this operation.
	 *            It can be <code>null</code>.
	 * @return a {@link RasterStorage} instance which is connected to the newly
	 *         created coverage storage.
	 * @throws IOException
	 *             in case something bad happens.
	 */
	public RasterService<T> createInstance(URL source, Map<String, Serializable> params,
			Hints hints, final ProgressListener listener) throws IOException {
		throw new UnsupportedOperationException(getTitle()
					+ " does not support create operation");
		
	}
}
