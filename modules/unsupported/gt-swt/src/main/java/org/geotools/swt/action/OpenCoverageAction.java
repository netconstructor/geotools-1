/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.geotools.swt.action;

import java.io.File;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.map.MapContext;
import org.geotools.map.MapLayer;
import org.geotools.styling.Style;
import org.geotools.swt.control.JFileImageChooser;
import org.geotools.swt.utils.ImageCache;
import org.geotools.swt.utils.Utils;

/**
 * Action to open image files.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class OpenCoverageAction extends MapAction implements ISelectionChangedListener {

    public OpenCoverageAction() {
        super("Open Image", "Load an image into the viewer.", ImageCache.getInstance().getImage(ImageCache.OPEN));
    }

    public void run() {
        Display display = Display.getCurrent();
        Shell shell = new Shell(display);
        File openFile = JFileImageChooser.showOpenFile(shell);

        if (openFile != null && openFile.exists()) {
            AbstractGridFormat format = GridFormatFinder.findFormat(openFile);
            AbstractGridCoverage2DReader tiffReader = format.getReader(openFile);
            Style rgbStyle = Utils.createRGBStyle(tiffReader);

            MapContext mapContext = mapPane.getMapContext();
            MapLayer layer = new MapLayer(tiffReader, rgbStyle);
            layer.setTitle(openFile.getName());
            mapContext.addLayer(layer);
            mapPane.redraw();
        }
    }

    public void selectionChanged( SelectionChangedEvent arg0 ) {

    }

}
