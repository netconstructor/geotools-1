/*
 * CustomGlyphRenderer.java
 *
 * Created on April 6, 2004, 3:58 PM
 */

package org.geotools.renderer.lite;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Vector;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.filter.Expression;
import org.geotools.filter.FilterFactory;
import org.geotools.styling.ExternalGraphic;
import org.geotools.styling.Graphic;

/**
 *
 * @author  jfc173
 */
public class CustomGlyphRenderer implements GlyphRenderer {
    
    private GlyphPropertiesList list = new GlyphPropertiesList();
    private boolean maxFound = false;
    private int maxBarHeight = 0;
    
    /** Creates a new instance of CustomGlyphRenderer */
    public CustomGlyphRenderer() {
        FilterFactory factory = FilterFactory.createFilterFactory();
        list.addProperty("radius", Expression.class, factory.createLiteralExpression(50));
        list.addProperty("circle color", Expression.class, factory.createLiteralExpression("#000066"));
        list.addProperty("bar height", Expression.class, factory.createLiteralExpression(150));
        list.addProperty("bar color", Expression.class, factory.createLiteralExpression("#000000"));
        list.addProperty("bar uncertainty", Expression.class, factory.createLiteralExpression(50));
        list.addProperty("bar uncertainty width", Expression.class, factory.createLiteralExpression(5));
        list.addProperty("bar uncertainty color", Expression.class, factory.createLiteralExpression("#999999"));
        list.addProperty("pointer length", Expression.class, factory.createLiteralExpression(100));
        list.addProperty("pointer color", Expression.class, factory.createLiteralExpression("#FF0000"));
        list.addProperty("pointer direction", Expression.class, factory.createLiteralExpression(21));
        list.addProperty("wedge width", Expression.class, factory.createLiteralExpression(25));
        list.addProperty("wedge color", Expression.class, factory.createLiteralExpression("#9999FF"));
    }
    
    public boolean canRender(String format) {
        return format.equalsIgnoreCase("image/hack");
    }
    
    public List getFormats() {
        Vector ret = new Vector();
        ret.add("image/hack");
        return ret;
    }
    
    public String getGlyphName(){
        return "exploded clock";  //I think Alan called it this once, so here it sticks.
    }
    
    public GlyphPropertiesList getGlyphProperties(){
        return list;
    }
    
    //Is this really necessary, since the values in the properties list can be set?
    public void setGlyphProperties(GlyphPropertiesList gpl){
        list = gpl;
    }
    
    public BufferedImage render(Graphic graphic, ExternalGraphic eg, Feature feature) {
        //Change this to get values from list.  
        int radius = 50;
        Expression e = (Expression) list.getPropertyValue("radius");
        if (e != null){
            radius = ((Number) e.getValue(feature)).intValue();
        }
        
        Color circleColor = Color.BLUE.darker();
        e = (Expression) list.getPropertyValue("circle color");
        if (e != null){
            circleColor = Color.decode((String) e.getValue(feature));
        }        
        
        int barHeight = 150;
        e = (Expression) list.getPropertyValue("bar height");
        if (e != null){
            barHeight = ((Number) e.getValue(feature)).intValue();
        }       
        
        Color barColor = Color.BLACK;
        e = (Expression) list.getPropertyValue("bar color");
        if (e != null){
            barColor = Color.decode((String) e.getValue(feature));
        }        
        
        int barUncertainty = 50;
        e = (Expression) list.getPropertyValue("bar uncertainty");
        if (e != null){
            barUncertainty = ((Number) e.getValue(feature)).intValue();
        } 
        
        int barUncWidth = 5;
        e = (Expression) list.getPropertyValue("bar uncertainty width");
        if (e != null){
            barUncWidth = ((Number) e.getValue(feature)).intValue();
        } 
        
        Color barUncColor = Color.GRAY;
        e = (Expression) list.getPropertyValue("bar uncertainty color");
        if (e != null){
            barUncColor = Color.decode((String) e.getValue(feature));
        }
        
        int pointerDirection = 21;
        e = (Expression) list.getPropertyValue("pointer direction");
        if (e != null){
            pointerDirection = ((Number) e.getValue(feature)).intValue();
        } 
        
        Color pointerColor = Color.RED;
        e = (Expression) list.getPropertyValue("pointer color");
        if (e != null){
            pointerColor = Color.decode((String) e.getValue(feature));
        }        
        
        int pointerLength = 100;
        e = (Expression) list.getPropertyValue("pointer length");
        if (e != null){
            pointerLength = ((Number) e.getValue(feature)).intValue();
        } 
        
        int wedgeWidth = 25;
        e = (Expression) list.getPropertyValue("wedge width");
        if (e != null){
            wedgeWidth = ((Number) e.getValue(feature)).intValue();
        } 
        
        Color wedgeColor = Color.BLUE;
        e = (Expression) list.getPropertyValue("wedge color");
        if (e != null){
            wedgeColor = Color.decode((String) e.getValue(feature));
        }        
        
        int circleCenterX, circleCenterY, imageHeight, imageWidth;


        BufferedImage image;
        Graphics2D imageGraphic;

        //calculate maximum value of barHeight + barUncertainty & use that instead of "barHeight + barUncertainty"
        if (!maxFound){
            maxFound = true;
            FeatureCollection fc = feature.getParent();
            FeatureIterator features = fc.features();
            while (features.hasNext()){
                Feature next = features.next();
                Expression tempExp = (Expression) list.getPropertyValue("bar height");
                int temp1 = 0;
                if (tempExp != null){
                    temp1 = ((Number) tempExp.getValue(next)).intValue();
                }
                tempExp = (Expression) list.getPropertyValue("bar uncertainty");
                int temp2 = 0;
                if (tempExp != null){
                    temp2 = ((Number) tempExp.getValue(next)).intValue();
                }            
                if (temp1 + temp2 > maxBarHeight){
                    maxBarHeight = temp1 + temp2;
                }            
            }
        }   
        
        circleCenterX = Math.max(pointerLength, radius);
        circleCenterY = Math.max(maxBarHeight, Math.max(pointerLength, radius));     
        
        imageHeight = Math.max(radius * 2, Math.max(radius + pointerLength, Math.max(radius + maxBarHeight, pointerLength + maxBarHeight)));
        imageWidth = Math.max(radius * 2, pointerLength * 2);
        image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        pointerLength = Math.max(pointerLength, radius);
        imageGraphic = image.createGraphics();
        imageGraphic.setColor(circleColor);
        imageGraphic.fillOval(circleCenterX - radius, circleCenterY - radius, radius * 2, radius * 2);
        imageGraphic.setColor(wedgeColor); 
        imageGraphic.fillArc(circleCenterX - radius,
                             circleCenterY - radius,
                             radius * 2,
                             radius * 2, 
                             calculateWedgeAngle(pointerDirection, wedgeWidth),
                             wedgeWidth * 2);
        imageGraphic.setColor(barUncColor);
        imageGraphic.fillRect(circleCenterX - barUncWidth, 
                              circleCenterY - barHeight - barUncertainty, 
                              barUncWidth * 2, 
                              barUncertainty * 2);
         //pointer
        int[] endPoint = calculateEndOfPointer(circleCenterX, circleCenterY, pointerLength, pointerDirection);
        imageGraphic.setStroke(new java.awt.BasicStroke(3));
        imageGraphic.setColor(pointerColor);
        imageGraphic.draw(new java.awt.geom.Line2D.Double(circleCenterX, circleCenterY, endPoint[0], endPoint[1]));
        //bar
        imageGraphic.setStroke(new java.awt.BasicStroke(3));
        imageGraphic.setColor(barColor);
        imageGraphic.draw(new java.awt.geom.Line2D.Double(circleCenterX, circleCenterY, circleCenterX, circleCenterY - barHeight));

        imageGraphic.dispose();
        return image;
    }

    private int calculateWedgeAngle(int pointerDirection, int wedgeWidth){
        return 450 - (pointerDirection + wedgeWidth);
    }
    
    private int[] calculateEndOfPointer(int circleCenterX, int circleCenterY, int pointerLength, int pointerDirection){
        int x = circleCenterX + (int) Math.round(pointerLength * Math.cos(Math.toRadians(pointerDirection - 90)));
        int y = circleCenterY + (int) Math.round(pointerLength * Math.sin(Math.toRadians(pointerDirection - 90)));
        return new int[]{x, y};
    }    
    
}
