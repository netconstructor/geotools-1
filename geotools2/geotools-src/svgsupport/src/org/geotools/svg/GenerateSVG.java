/*
 * GenerateSVG.java
 *
 * Created on 07 June 2002, 23:57
 */



package org.geotools.svg;


import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Color;
import java.io.Writer;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.svggen.SVGGeneratorContext;
import org.apache.batik.dom.GenericDOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DOMImplementation;

import org.geotools.map.Map;
import org.geotools.renderer.Java2DRenderer;

import com.vividsolutions.jts.geom.Envelope;

/**
 *
 * @author  James
 */
public class GenerateSVG {
    
    /** Creates a new instance of GenerateSVG */
    public GenerateSVG() {
    }
    
    public void go(Map map,OutputStream out) throws IOException{
        // Get a DOMImplementation
        DOMImplementation domImpl =
        GenericDOMImplementation.getDOMImplementation();
        
        // Create an instance of org.w3c.dom.Document
        Document document = domImpl.createDocument(null, "svg", null);
        
        // Set up the context
        SVGGeneratorContext ctx = SVGGeneratorContext.createDefault(document);
        ctx.setComment("Generated by GeoTools2 with Batik SVG Generator");
        
        // Create an instance of the SVG Generator
        SVGGraphics2D g2d = new SVGGraphics2D(ctx,true);
        g2d.setSVGCanvasSize(new Dimension(100,100));
        
        
        Java2DRenderer renderer = new Java2DRenderer();
        renderer.setOutput(g2d,new Rectangle(g2d.getSVGCanvasSize()));
        
        map.render(renderer,new Envelope(0,0,100,100));
        
        g2d.stream(new OutputStreamWriter(out));
        
    }
    
}
