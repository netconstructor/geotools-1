/*
 * ProjectionFactoryTest.java
 * JUnit based test
 *
 * Created on 21 February 2002, 17:32
 */                

package org.geotools.proj4j;

import junit.framework.*;

/**
 *
 * @author ian
 */                                
public class ProjectionFactoryTest extends TestCase {
    
    public ProjectionFactoryTest(java.lang.String testName) {
        super(testName);
    }        
    
    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite(ProjectionFactoryTest.class);
        
        return suite;
    }
    
    /** Test of createProjection method, of class org.geotools.proj4j.ProjectionFactory. */
    public void testCreateProjection() {
        System.out.println("testCreateProjection");
        try{
            Projection p = ProjectionFactory.createProjection(new String[]{"proj=tmerc"});
        }catch(ProjectionException e){
            fail(e.toString());
        }
           
    }
    
    // Add test methods here, they have to start with 'test' name.
    // for example: 
    // public void testHello() {}


}
