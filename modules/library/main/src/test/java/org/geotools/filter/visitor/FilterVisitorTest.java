package org.geotools.filter.visitor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.visitor.IdCollectorFilterVisitor;
import org.geotools.feature.visitor.IdFinderFilterVisitor;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.FilterVisitor;
import org.opengis.filter.Id;
import org.opengis.filter.expression.PropertyName;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

import junit.framework.TestCase;

/**
 * This test checks that our filter visitor examples on the wiki are in working order.
 * <ul>
 * <li>DefaultFilterVisitor
 * <li>NullFilterVisitor
 * <li>ExtractBoundsFilterVisitor
 * <li>...
 * </ul>
 * 
 * @author Jody Garnett
 */
public class FilterVisitorTest extends TestCase {

    static private FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);
    static private GeometryFactory gf = new GeometryFactory();

    /** Example located on the wiki */
    public void testDefaultFilterVisitorFeatureIdExample() {
        Filter myFilter = ff.id(Collections.singleton(ff.featureId("fred")));
        FilterVisitor allFids = new DefaultFilterVisitor(){
            public Object visit( Id filter, Object data ) {
                Set set = (Set) data;
                set.addAll(filter.getIDs());
                return set;
            }
        };
        Set set = (Set) myFilter.accept(allFids, new HashSet());
        assertEquals(1, set.size());
    }
    /** Example located on the wiki */
    public void testDefaultFilterVisitorPropertyNameExample() {
        Filter myFilter = ff.greater(ff.add(ff.property("foo"), ff.property("bar")), ff.literal(1));

        class FindNames extends DefaultFilterVisitor {
            public Object visit( PropertyName expression, Object data ) {
                Set set = (Set) data;
                set.add(expression.getPropertyName());

                return set;
            }
        }
        Set set = (Set) myFilter.accept(new FindNames(), new HashSet());
        assertTrue(set.contains("foo"));
    }
    public void testNullFilterVisitor() {
        Filter filter = ff.isNull(ff.property("name"));
        assertEquals(new Integer(1), filter.accept(NullFilterVisitor.NULL_VISITOR, 1));

        filter = Filter.INCLUDE;
        assertEquals(new Integer(1), filter.accept(NullFilterVisitor.NULL_VISITOR, 1));

        FilterVisitor allFids = new NullFilterVisitor(){
            public Object visit( Id filter, Object data ) {
                if (data == null)
                    return null;
                Set set = (Set) data;
                set.addAll(filter.getIDs());
                return set;
            }
        };
        Filter myFilter = ff.id(Collections.singleton(ff.featureId("fred")));
        
        Set set = (Set) myFilter.accept(allFids, new HashSet());
        assertNotNull( set );
        Set set2 = (Set) myFilter.accept(allFids, null); // set2 will be null
        assertNull( set2 );
    }
    public void testBoundsFilterVisitor() {
        Filter filter = ff.isNull(ff.property("name"));
        
        assertNull( filter.accept( ExtractBoundsFilterVisitor.BOUNDS_VISITOR, null ) );
        
        ReferencedEnvelope bbox = (ReferencedEnvelope) filter.accept( ExtractBoundsFilterVisitor.BOUNDS_VISITOR, new ReferencedEnvelope() );
        
        assertNotNull( bbox );
        assertTrue( bbox.isNull() );
        
        filter = ff.bbox("name", 0, 0, 10, 10, "EPSG:4326" );
        bbox = (ReferencedEnvelope) filter.accept( ExtractBoundsFilterVisitor.BOUNDS_VISITOR, new ReferencedEnvelope() );
        
        assertNotNull( bbox );
        assertEquals( 10, (int) bbox.getLength(0) );
        assertEquals( 10, (int) bbox.getLength(1) );
        
        Coordinate[] coords = new Coordinate[]{new Coordinate(0,0), new Coordinate(10,10)};
        LineString lineString = gf.createLineString( coords );
        filter = ff.touches( ff.property("name"), ff.literal( lineString ) );
        bbox = (ReferencedEnvelope) filter.accept( ExtractBoundsFilterVisitor.BOUNDS_VISITOR, new ReferencedEnvelope() );
        
        assertNotNull( bbox );
        assertEquals( 10, (int) bbox.getLength(0) );
        assertEquals( 10, (int) bbox.getLength(1) );
    }
    public void testIdFinderFilterVisitor(){
        Filter filter = ff.isNull(ff.property("name"));
        boolean found = (Boolean) filter.accept( new IdFinderFilterVisitor(), null );
        assertFalse( found );
        
        filter = ff.id( Collections.singleton( ff.featureId("eclesia")));
        found = (Boolean) filter.accept( new IdFinderFilterVisitor(), null );
        assertTrue( found );        
    }
    
    public void testIdCollector(){
        Filter filter = ff.isNull(ff.property("name"));
        Set fids = (Set) filter.accept( IdCollectorFilterVisitor.ID_COLLECTOR, new HashSet() );
        assertTrue( fids.isEmpty() );
        assertFalse( fids.contains("eclesia"));
        
        filter = ff.id( Collections.singleton( ff.featureId("eclesia")));
        fids = (Set) filter.accept( IdCollectorFilterVisitor.ID_COLLECTOR, new HashSet() );
        assertFalse( fids.isEmpty() );
        assertTrue( fids.contains("eclesia"));        
    }
}
