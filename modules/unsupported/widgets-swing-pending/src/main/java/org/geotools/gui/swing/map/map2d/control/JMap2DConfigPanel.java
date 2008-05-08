/*
 *    GeoTools - OpenSource mapping toolkit
 *    http://geotools.org
 *    (C) 2006, GeoTools Project Managment Committee (PMC)
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
package org.geotools.gui.swing.map.map2d.control;


import org.geotools.gui.swing.map.map2d.Map2D;
import org.geotools.gui.swing.map.map2d.strategy.MergeBufferedImageStrategy;
import org.geotools.gui.swing.map.map2d.strategy.RenderingStrategy;
import org.geotools.gui.swing.map.map2d.strategy.SingleBufferedImageStrategy;
import org.geotools.gui.swing.map.map2d.strategy.SingleVolatileImageStrategy;

/**
 * JMap2DConfigPanel is a JPanel to edit the GTRenderer and Rendering Strategy of the Map2D
 * @author  johann Sorel
 */
public class JMap2DConfigPanel extends javax.swing.JPanel {

    private Map2D map = null;

    /** 
     * Creates new form JOptimizeMap2DPanel 
     */
    public JMap2DConfigPanel() {
        initComponents();
    }

    
    private void setRendering(RenderingStrategy type) {
        if (map != null) {
            map.setRenderingStrategy(type);
        }
    }

    /**
     * set the related Map2D
     * @param map2d : related Map2D
     */
    public void setMap(Map2D map2d) {
        this.map = map2d;

        if (map2d != null ) {
            RenderingStrategy cl = map2d.getRenderingStrategy();
            
            if(cl instanceof SingleBufferedImageStrategy){
                jrb_rendering_single_buffer.setSelected(true);
            }else if(cl instanceof MergeBufferedImageStrategy){
                jrb_rendering_merge_buffer.setSelected(true);
            }else if(cl instanceof SingleBufferedImageStrategy){
                jrb_rendering_single_volatile.setSelected(true);
            }                       
            
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        bg_rendering = new javax.swing.ButtonGroup();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel8 = new javax.swing.JLabel();
        jrb_rendering_single_buffer = new javax.swing.JRadioButton();
        jrb_rendering_merge_buffer = new javax.swing.JRadioButton();
        jrb_rendering_single_volatile = new javax.swing.JRadioButton();

        jLabel7.setText("Software solutions :"); // NOI18N

        jSeparator1.setOrientation(javax.swing.SwingConstants.VERTICAL);

        jLabel8.setText("Hardware solutions :"); // NOI18N

        bg_rendering.add(jrb_rendering_single_buffer);
        jrb_rendering_single_buffer.setText("Single buffered image"); // NOI18N
        jrb_rendering_single_buffer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jrb_rendering_single_bufferActionPerformed(evt);
            }
        });

        bg_rendering.add(jrb_rendering_merge_buffer);
        jrb_rendering_merge_buffer.setText("Merge buffered images"); // NOI18N
        jrb_rendering_merge_buffer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jrb_rendering_merge_bufferActionPerformed(evt);
            }
        });

        bg_rendering.add(jrb_rendering_single_volatile);
        jrb_rendering_single_volatile.setSelected(true);
        jrb_rendering_single_volatile.setText("Volatile image"); // NOI18N
        jrb_rendering_single_volatile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jrb_rendering_single_volatileActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(jLabel7)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 103, Short.MAX_VALUE))
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(10, 10, 10)
                        .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jrb_rendering_merge_buffer, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 198, Short.MAX_VALUE)
                            .add(jPanel1Layout.createSequentialGroup()
                                .add(jrb_rendering_single_buffer, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 170, Short.MAX_VALUE)
                                .add(28, 28, 28)))))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jSeparator1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 24, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel8)
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(10, 10, 10)
                        .add(jrb_rendering_single_volatile)))
                .add(158, 158, 158))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(jLabel8)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jrb_rendering_single_volatile))
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(jLabel7)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jrb_rendering_single_buffer)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jrb_rendering_merge_buffer))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jSeparator1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 279, Short.MAX_VALUE))
                .addContainerGap())
        );

        jTabbedPane1.addTab("Rendering", jPanel1);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jTabbedPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 529, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jTabbedPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 329, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jrb_rendering_single_bufferActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jrb_rendering_single_bufferActionPerformed
        setRendering(new SingleBufferedImageStrategy());
}//GEN-LAST:event_jrb_rendering_single_bufferActionPerformed

    private void jrb_rendering_merge_bufferActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jrb_rendering_merge_bufferActionPerformed
        setRendering(new MergeBufferedImageStrategy());
    }//GEN-LAST:event_jrb_rendering_merge_bufferActionPerformed

    private void jrb_rendering_single_volatileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jrb_rendering_single_volatileActionPerformed
        setRendering(new SingleVolatileImageStrategy());
    }//GEN-LAST:event_jrb_rendering_single_volatileActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup bg_rendering;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JRadioButton jrb_rendering_merge_buffer;
    private javax.swing.JRadioButton jrb_rendering_single_buffer;
    private javax.swing.JRadioButton jrb_rendering_single_volatile;
    // End of variables declaration//GEN-END:variables
}
