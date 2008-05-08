package org.geotools.filter.expression;

import org.geotools.factory.CommonFactoryFinder;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Expression;


import junit.framework.TestCase;

public class AddImplTest extends TestCase {

	AddImpl add; 
	
	protected void setUp() throws Exception {
		FilterFactory ff = CommonFactoryFinder.getFilterFactory( null );
		Expression e1 = ff.literal( 1 );
		Expression e2 = ff.literal( 2 );
		
		add = new AddImpl( e1, e2 );
	}
	
	public void testEvaluate() {
		Object result = add.evaluate( null );
		assertEquals( new Double( 3 ), result );
	}
	
	public void testEvaluateAsInteger() {
		Object result = add.evaluate( null, Integer.class );
		assertEquals( new Integer( 3 ), result );
	}
	
	public void testEvaluateAsString( ) {
		Object result = add.evaluate( null, String.class );
		assertEquals( "3.0", result );
	}
}
