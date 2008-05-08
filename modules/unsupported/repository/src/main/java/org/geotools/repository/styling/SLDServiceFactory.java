package org.geotools.repository.styling;

import java.io.File;
import java.io.FileFilter;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.geotools.repository.Catalog;
import org.geotools.repository.Service;
import org.geotools.repository.ServiceFactory;


public class SLDServiceFactory implements ServiceFactory {

	static String SLD_NAMESPACE = "http://www.opengis.net/sld" ;
	
	static String KEY = "sldFile";
	
	public Service createService( Catalog parent, URI id, Map params ) {
		return new SLDService( parent, new File( id ) );
	}

	public boolean canProcess(URI uri) {
		File file = new File( uri );
		if ( !file.exists() )
			return false;
	
		if ( file.isDirectory() ) {
			File[] files = file.listFiles( 
				new FileFilter() {

					public boolean accept(File pathname) {
						return isSLDFile( pathname );
					}
					
				}
			);
			return files.length > 0;
		}
		else { 
			return isSLDFile( file );
		}
		
	}

	public Map createParams(URI uri) {
		if ( canProcess( uri ) ) {
			HashMap map = new HashMap();
			map.put( KEY, uri );
			return map;
		}
		
		return null;
	}
	
	static boolean isSLDFile( File file ) {
		String filename = file.getName();
		
		if ( filename.length() > 3 ) {
			String ext = filename.substring( filename.length() - 4 );
			return ext.equalsIgnoreCase( ".sld" ) || 
				ext.equalsIgnoreCase( ".xml" );
		}
		
		return false;
	}

}
