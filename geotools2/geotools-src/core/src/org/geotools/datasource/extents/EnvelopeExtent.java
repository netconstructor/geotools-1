package org.geotools.datasource.extents;

import org.geotools.data.Extent;
import org.geotools.feature.Feature;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Envelope;


/** A geographic filter class
 * @author James Macgill, CCG, University of Leeds
 * @author Ian Turton, CCG, University of Leeds
 * $Id: EnvelopeExtent.java,v 1.10 2002/05/14 23:49:50 robhranac Exp $
 */
public class EnvelopeExtent implements Extent {
    /** internal representation of the bounding box
     */
    private Envelope bounds = new Envelope();
    /** null constructor
     */
    public EnvelopeExtent(){}
    
    /** construct from a jts envelope
     * @param e the jts envelope to use
     */
    public EnvelopeExtent(Envelope e){
        setBounds(e);
    }
    
    /** construct from the corners of the area required
     * @param minx the left hand side of the area
     * @param maxx the right hand side of the area
     * @param miny the lower side of the area
     * @param maxy the upper side of the area
     */
    public EnvelopeExtent(double minx, double maxx, double miny, double maxy){
        Envelope e = new Envelope(minx, maxx, miny, maxy);
        setBounds(e);
    }
    
    /** sets the bounds of the extent
     * @param r the new bounding envelope
     */
    public void setBounds(Envelope r) {
        if (r.getWidth() <= 0 || r.getHeight() <= 0){
            /* this is almost certainly an error in this context
             * but technically its a valid envelope
             */
            System.err.println("Negative or zero envelope set" + 
                " in EnvelopeExtent");
        }
        bounds = r;
    }
    
    /** get the bounds of this extent
     * @return a jts envelope represtenting the bounds of this extent
     */
    public Envelope getBounds() {
        return bounds;
    }
    
    /** Gets the Extent which represents the intersection between 
     * this and another
     * @param other The extent to test against
     * @return An Extent representing the intersecting area 
     * between two Extents, null if there is no overlap.
     */
    public Extent intersection(Extent other) {
        EnvelopeExtent newExtent = new EnvelopeExtent();
        EnvelopeExtent otherExtent = (EnvelopeExtent) other;
        Envelope otherGeo = otherExtent.getBounds();
        newExtent.setBounds(createIntersect(bounds, otherGeo));
        return newExtent;
    }
    
    /** Gets the difference, represented by another Extent, 
     * between this Extent and other.
     * @param other The extent to test against
     * @return An array of Extents making up the total 
     * area of other not taken up by this Extent.
     * If there is no overlap between the two, this 
     * returns the same extent as other.
     */
    public Extent [] difference(Extent other) {
        EnvelopeExtent newExtent = new EnvelopeExtent();
        EnvelopeExtent otherExtent = (EnvelopeExtent) other;
        Envelope otherGeo = otherExtent.getBounds();
        Envelope[] side = remainder(bounds, otherGeo);
        java.util.Vector v = new java.util.Vector();
        if (side != null){
            for (int i = 0; i < side.length; i++){
                if (side[i] != null){
                    v.addElement(new EnvelopeExtent(side[i]));
                }
            }
        }
        return (Extent[]) v.toArray(new Extent[v.size()]);
    }
    
    /** Tests whether the given Feature is within this Extent. 
     * This Extent implementation must be
     * able to read the contents of the Feature.
     * @return True is the Feature is within this Extent, false if otherwise.
     * @param feature the feature to test
     */
    public boolean containsFeature(Feature feature) {
        // Assume the Feature contains a Geometry
        if (feature == null /*|| feature.row==null*/){
            return false;
        }
        Geometry s = (Geometry) feature.getDefaultGeometry();
        return bounds.overlaps(s.getEnvelopeInternal());
    }
    /** creates an interface
     * @param one one of the input envelopes
     * @param two the other input envelope
     * @return the intersection between the two envelopes
     */
    private Envelope createIntersect(Envelope one, Envelope two){
        if (!one.overlaps(two)){
            return null;
        }
        double b1x1, b1y1, b1x2, b1y2; // coords of the first box
        double b2x1, b2y1, b2x2, b2y2; // coords of the second box
        double nx1, ny1, nx2, ny2; // coords of the new box
        
        b1x1 = one.getMinX();
        b1y1 = one.getMinY(); // bottom left
        b1x2 = one.getMaxX();
        b1y2 = one.getMaxY(); // top right
        
        b2x1 = two.getMinX();
        b2y1 = two.getMinY(); // bottom left
        b2x2 = two.getMaxX();
        b2y2 = two.getMaxY(); // top right
        
        
        // find the left edge
        if (b1x1 < b2x1){
            nx1 = b2x1;
        } else {
            nx1 = b1x1;
        }
        // find the right edge
        if (b1x2 > b2x2){
            nx2 = b2x2;
        } else {
            nx2 = b1x2;
        }
        // find the top edge
        if (b1y2 > b2y2){
            ny2 = b2y2;
        } else {
            ny2 = b1y2;
        }
        // find the bottom edge
        if (b1y1 > b2y1){
            ny1 = b1y1;
        } else {
            ny1 = b2y1;
        }
        
        return (new Envelope(nx1, nx2, ny1, ny2));
        
    }
    /** returns the 4 rectangles that are left after two is 
     * removed from one.
     * Some or all of the rectangles returned may be null.
     * the side boxes are from base to top of this rectangle, the top/bottom
     * boxes are from the edge to edge of the intersect <br>
     * the return order is left,top,right,bottom
     * @param one the first envelope
     * @param two another envelope
     * @return an array of envelopes - some or all may be null
     */
    private Envelope[] remainder(Envelope one, Envelope two){
        Envelope [] ret = new Envelope[4];
        //if they don't intersect there is no remainder? or we could return r?
        Envelope inter = createIntersect(one, two);
        System.out.println("inter " + inter);
        if (inter == null){
            return ret;
        }
        // left hand edge
        if ((inter.getMinX() - one.getMinX()) > 0){
            ret[0] = new Envelope(one.getMinX(), inter.getMinX(),
                one.getMinY(), one.getMaxY());
        }
        // top edge
        if ((inter.getWidth() > 0) && ((one.getMaxY() - inter.getMaxY()) > 0)){
            ret[1] = new Envelope(inter.getMinX(), inter.getMaxX(), 
                inter.getMaxY(), one.getMaxY());
        }
        // right hand edge
        if ((one.getMaxX() - inter.getMaxX()) > 0){
            ret[2] = new Envelope(inter.getMaxX(), one.getMaxX(), one.getMinY(),
            one.getMaxY());
        }
        // bottom edge
        if (inter.getWidth() > 0 && (inter.getMinY() - one.getMinY()) > 0){
            ret[3] = new Envelope(inter.getMinX(), inter.getMaxX(), 
                one.getMinY(), inter.getMaxY());
        }
        return ret;
        
        
    }
    /** toString method
     * @return string representation of the exent
     */
    public String toString(){
        return new String(bounds.getMinX() + "," + bounds.getMinY() +
        " " + bounds.getMaxX() + "," + bounds.getMaxY());
    }
    
    /**
     * Produces the smallest extent that will hold 
     * both the existing extent and that of the extent pased in
     * TODO: Think about implecation of combining, 
     * new extent may contain areas which were in neither.
     * @param other The extent to combine with this extent
     * @return The new, larger, extent.
     **/
    public Extent combine(Extent other) {
        Envelope total = new Envelope();
        total.expandToInclude(this.getBounds());
        total.expandToInclude(((EnvelopeExtent) other).getBounds());
        return new EnvelopeExtent(total);
    }
}

