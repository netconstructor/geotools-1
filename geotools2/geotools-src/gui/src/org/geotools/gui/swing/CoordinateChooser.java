/*
 * Geotools - OpenSource mapping toolkit
 * (C) 2002, Centre for Computational Geography
 * (C) 2001, Institut de Recherche pour le D�veloppement
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public
 *    License along with this library; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 * Contacts:
 *     UNITED KINGDOM: James Macgill
 *             mailto:j.macgill@geog.leeds.ac.uk
 *
 *     FRANCE: Surveillance de l'Environnement Assist�e par Satellite
 *             Institut de Recherche pour le D�veloppement / US-Espace
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package org.geotools.gui.swing;

// Time
import java.util.Date;
import java.util.TimeZone;
import java.util.Calendar;

// Geometry and coordinates
import java.awt.Dimension;
import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;

// User interface (Swing)
import java.awt.Insets;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.JOptionPane;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;
import javax.swing.BorderFactory;
import javax.swing.AbstractButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.AbstractSpinnerModel;
import javax.swing.JFormattedTextField;
import javax.swing.text.InternationalFormatter;

// Events
import java.awt.EventQueue;
import java.util.EventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

// Parsing and formating
import java.text.Format;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;

// Miscellaneous
import java.util.Arrays;
import java.util.Locale;

// Geotools dependencies
import org.geotools.pt.Angle;
import org.geotools.pt.Latitude;
import org.geotools.pt.Longitude;
import org.geotools.pt.AngleFormat;

// Resources
import org.geotools.resources.XDimension2D;
import org.geotools.resources.XRectangle2D;
import org.geotools.resources.SwingUtilities;
import org.geotools.resources.gui.Resources;
import org.geotools.resources.gui.ResourceKeys;


/**
 * A pane of controls designed to allow a user to select spatio-temporal coordinates.
 * Current implementation use geographic coordinates (longitudes/latitudes) and dates
 * according some locale calendar. Future version may allow the use of user-specified
 * coordinate system.
 *
 * <p>&nbsp;</p>
 * <p align="center"><img src="doc-files/CoordinateChooser.png"></p>
 * <p>&nbsp;</p>
 *
 * @version $Id: CoordinateChooser.java,v 1.1 2002/08/07 13:26:59 desruisseaux Exp $
 * @author Martin Desruisseaux
 */
public class CoordinateChooser extends JPanel {
    /**
     * Liste de choix dans laquelle l'utilisateur
     * choisira le fuseau horaire de ses dates.
     */
    private final JComboBox timezone;

    /**
     * Dates de d�but et de fin de la plage de temps demand�e par l'utilisateur.
     * Ces dates sont g�r�es par un mod�le {@link SpinnerDateModel}.
     */
    private final JSpinner tmin, tmax;

    /**
     * Longitudes et latitudes minimales et maximales demand�es par l'utilisateur.
     * Ces coordonn�es sont g�r�es par un mod�le {@link SpinnerNumberModel}.
     */
    private final JSpinner xmin, xmax, ymin, ymax;

    /**
     * R�solution (en minutes de longitudes et de latitudes) demand�e par l'utilisateur.
     * Ces r�solution sont g�r�es par un mod�le {@link SpinnerNumberModel}.
     */
    private final JSpinner xres, yres;

    /**
     * Bouton radio pour s�lectioner la meilleure r�solution possible.
     */
    private final AbstractButton radioBestRes;

    /**
     * Bouton radio pour s�lectioner la r�solution sp�cifi�e.
     */
    private final AbstractButton radioPrefRes;

    /**
     * Composante facultative � afficher � la droite
     * du paneau <code>CoordinateChooser</code>.
     */
    private JComponent accessory;

    /**
     * Class encompassing various listeners for users selections.
     *
     * @version $Id: CoordinateChooser.java,v 1.1 2002/08/07 13:26:59 desruisseaux Exp $
     * @author Martin Desruisseaux
     */
    private final class Listeners implements ActionListener, ChangeListener {
        /**
         * List of components to toggle.
         */
        private final JComponent[] toggle;

        /**
         * Construct a <code>Listeners</code> object.
         */
        public Listeners(final JComponent[] toggle) {
            this.toggle=toggle;
        }

        /**
         * Invoked when user select a new timezone.
         */
        public void actionPerformed(final ActionEvent event) {
            update(getTimeZone());
        }

        /**
         * Invoked when user change the button radio state
         * ("use best resolution" / "set resolution").
         */
        public void stateChanged(final ChangeEvent event) {
            setEnabled(radioPrefRes.isSelected());
        }

        /**
         * Enable or disable {@link #toggle} components.
         */
        final void setEnabled(final boolean state) {
            for (int i=0; i<toggle.length; i++) {
                toggle[i].setEnabled(state);
            }
        }
    }

    /**
     * Construct a default coordinate chooser.
     */
    public CoordinateChooser() {
        this(new Date(0), new Date());
    }

    /**
     * Construct a coordinate chooser.
     *
     * @param minTime The minimal date allowed.
     * @param maxTime the maximal date allowed.
     */
    public CoordinateChooser(final Date minTime, final Date maxTime) {
        super(new GridBagLayout());
        final Locale locale = getDefaultLocale();
        final int timeField = Calendar.DAY_OF_YEAR;
        final Resources resources = Resources.getResources(locale);

        radioBestRes=new JRadioButton(resources.getString(ResourceKeys.USE_BEST_RESOLUTION), true);
        radioPrefRes=new JRadioButton(resources.getString(ResourceKeys.SET_PREFERRED_RESOLUTION));

        tmin = new JSpinner(new SpinnerDateModel(minTime, minTime, maxTime, timeField));
        tmax = new JSpinner(new SpinnerDateModel(maxTime, minTime, maxTime, timeField));
        xmin = new JSpinner(new AngleSpinnerModel(new Longitude(Longitude.MIN_VALUE)));
        xmax = new JSpinner(new AngleSpinnerModel(new Longitude(Longitude.MAX_VALUE)));
        ymin = new JSpinner(new AngleSpinnerModel(new  Latitude( Latitude.MIN_VALUE)));
        ymax = new JSpinner(new AngleSpinnerModel(new  Latitude( Latitude.MAX_VALUE)));
        xres = new JSpinner(new SpinnerNumberModel(1, 0, 360*60, 1));
        yres = new JSpinner(new SpinnerNumberModel(1, 0, 180*60, 1));

        final AngleFormat   angleFormat = new AngleFormat("D�MM.m'", locale);
        final DateFormat     dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale);
        final NumberFormat numberFormat = NumberFormat.getNumberInstance(locale);
        xmin.setEditor(new AngleSpinnerModel.Editor(xmin, angleFormat));
        xmax.setEditor(new AngleSpinnerModel.Editor(xmax, angleFormat));
        ymin.setEditor(new AngleSpinnerModel.Editor(ymin, angleFormat));
        ymax.setEditor(new AngleSpinnerModel.Editor(ymax, angleFormat));

        setup(tmin, 10,   dateFormat);
        setup(tmax, 10,   dateFormat);
        setup(xmin,  7,         null);
        setup(xmax,  7,         null);
        setup(ymin,  7,         null);
        setup(ymax,  7,         null);
        setup(xres,  3, numberFormat);
        setup(yres,  3, numberFormat);

        final String[] timezones=TimeZone.getAvailableIDs();
        Arrays.sort(timezones);
        timezone=new JComboBox(timezones);
        timezone.setSelectedItem(dateFormat.getTimeZone().getID());

        final JLabel labelSize1=new JLabel(resources.getLabel(ResourceKeys.SIZE_IN_MINUTES));
        final JLabel labelSize2=new JLabel("\u00D7"  /* Multiplication symbol */);
        final ButtonGroup group=new ButtonGroup();
        group.add(radioBestRes);
        group.add(radioPrefRes);

        final Listeners listeners=new Listeners(new JComponent[] {labelSize1, labelSize2, xres, yres});
        listeners   .setEnabled(false);
        timezone    .addActionListener(listeners);
        radioPrefRes.addChangeListener(listeners);

        final JPanel p1=getPanel(resources.getString(ResourceKeys.GEOGRAPHIC_COORDINATES));
        final JPanel p2=getPanel(resources.getString(ResourceKeys.TIME_RANGE            ));
        final JPanel p3=getPanel(resources.getString(ResourceKeys.PREFERRED_RESOLUTION  ));
        final GridBagConstraints c=new GridBagConstraints();

        c.weightx=1;
        c.gridx=1; c.gridy=0; p1.add(ymax, c);
        c.gridx=0; c.gridy=1; p1.add(xmin, c);
        c.gridx=2; c.gridy=1; p1.add(xmax, c);
        c.gridx=1; c.gridy=2; p1.add(ymin, c);

        JLabel label;
        c.gridx=0; c.anchor=c.WEST; c.insets.right=3; c.weightx=0;
        c.gridy=0; p2.add(label=new JLabel(resources.getLabel(ResourceKeys.START_TIME)), c); label.setLabelFor(tmin);
        c.gridy=1; p2.add(label=new JLabel(resources.getLabel(ResourceKeys.END_TIME  )), c); label.setLabelFor(tmax);
        c.gridy=2; p2.add(label=new JLabel(resources.getLabel(ResourceKeys.TIME_ZONE )), c); label.setLabelFor(timezone); c.gridwidth=4;
        c.gridy=0; p3.add(radioBestRes,  c);
        c.gridy=1; p3.add(radioPrefRes,  c);
        c.gridy=2; c.gridwidth=1; c.anchor=c.EAST; c.insets.right=c.insets.left=1; c.weightx=1;
        c.gridx=0; p3.add(labelSize1, c); labelSize1.setLabelFor(xres);  c.weightx=0;
        c.gridx=1; p3.add(xres,       c);
        c.gridx=2; p3.add(labelSize2, c); labelSize2.setLabelFor(yres);
        c.gridx=3; p3.add(yres,       c);

        c.gridx=1; c.fill=c.HORIZONTAL; c.insets.right=c.insets.left=0; c.weightx=1;
        c.gridy=0; p2.add(tmin,     c);
        c.gridy=1; p2.add(tmax,     c);
        c.gridy=2; p2.add(timezone, c);

        c.insets.right=c.insets.left=c.insets.top=c.insets.bottom=3;
        c.gridx=0; c.anchor=c.CENTER; c.fill=c.BOTH; c.weighty=1;
        c.gridy=0; add(p1, c);
        c.gridy=1; add(p2, c);
        c.gridy=2; add(p3, c);
    }

    /**
     * Retourne un panneau avec une bordure titr�e.
     */
    private static JPanel getPanel(final String title) {
        final JPanel panel=new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createTitledBorder(title),
                        BorderFactory.createEmptyBorder(6,6,6,6)));
        return panel;
    }

    /**
     * D�finit la largeur (en nombre de colonnes) d'un champ.
     * Eventuellement, cette m�thode peut aussi red�finir le
     * format.
     */
    private static void setup(final JSpinner spinner, final int width, final Format format) {
        final JFormattedTextField field=((JSpinner.DefaultEditor)spinner.getEditor()).getTextField();
        field.setMargin(new Insets(/*top*/0, /*left*/6, /*bottom*/0, /*right*/3));
        field.setColumns(width);
        if (format!=null) {
            ((InternationalFormatter)field.getFormatter()).setFormat(format);
        }
    }

    /**
     * Returns the value for the specified number,
     * or NaN if <code>value</code> is not a number.
     */
    private static double doubleValue(final JSpinner spinner) {
        final Object value = spinner.getValue();
        return (value instanceof Number) ? ((Number)value).doubleValue() : Double.NaN;
    }

    /**
     * Returns the value for the specified angle,
     * or NaN if <code>value</code> is not an angle.
     */
    private static double degrees(final JSpinner spinner, final boolean expectLatitude) {
        final Object value = spinner.getValue();
        if (value instanceof Angle) {
            if (expectLatitude ? (value instanceof Longitude) : (value instanceof Latitude)) {
                return Double.NaN;
            }
            return ((Angle) value).degrees();
        }
        return Double.NaN;
    }

    /**
     * Gets the geographic area, in latitude and longitude degrees.
     */
    public Rectangle2D getGeographicArea() {
        final double xmin = degrees(this.xmin, false);
        final double ymin = degrees(this.ymin,  true);
        final double xmax = degrees(this.xmax, false);
        final double ymax = degrees(this.ymax,  true);
        return new XRectangle2D.Double(Math.min(xmin,xmax), Math.min(ymin,ymax),
                                       Math.abs(xmax-xmin), Math.abs(ymax-ymin));
    }

    /**
     * Sets the geographic area, in latitude and longitude degrees.
     */
    public void setGeographicArea(final Rectangle2D area) {
        xmin.setValue(new Longitude(area.getMinX()));
        xmax.setValue(new Longitude(area.getMaxX()));
        ymin.setValue(new  Latitude(area.getMinY()));
        ymax.setValue(new  Latitude(area.getMaxY()));
    }

    /**
     * Returns the preferred resolution. A <code>null</code>
     * value means that the best available resolution should
     * be used.
     */
    public Dimension2D getPreferredResolution() {
        if (radioPrefRes.isSelected()) {
            return new XDimension2D.Double(doubleValue(xres), doubleValue(yres));
        }
        return null;
    }

    /**
     * Sets the preferred resolution. A <code>null</code>
     * value means that the best available resolution should
     * be used.
     */
    public void setPreferredResolution(final Dimension2D resolution) {
        if (resolution!=null) {
            xres.setValue(new Double(resolution.getWidth ()*60));
            yres.setValue(new Double(resolution.getHeight()*60));
            radioPrefRes.setSelected(true);
        }  else {
            radioBestRes.setSelected(true);
        }
    }

    /**
     * Returns the time zone used for displaying dates.
     */
    public TimeZone getTimeZone() {
        return TimeZone.getTimeZone(timezone.getSelectedItem().toString());
    }

    /**
     * Sets the time zone. This method change the control's display.
     * It doesn't change the date values, i.e. it have no effect
     * on previous or future call to {@link #setTimeRange}.
     */
    public void setTimeZone(final TimeZone timezone) {
        this.timezone.setSelectedItem(timezone.getID());
    }

    /**
     * Update the time zone in text fields. This method is automatically invoked
     * by {@link JComboBox} on user's selection. It is also (indirectly) invoked
     * on {@link #setTimeZone} call.
     */
    private void update(final TimeZone timezone) {
        boolean refresh=true;
        try {
            tmin.commitEdit();
            tmax.commitEdit();
        } catch (ParseException exception) {
            refresh = false;
        }
        ((JSpinner.DateEditor)tmin.getEditor()).getFormat().setTimeZone(timezone);
        ((JSpinner.DateEditor)tmax.getEditor()).getFormat().setTimeZone(timezone);
        if (refresh) {
            // TODO: If a "JSpinner.reformat()" method was available, we would use it here.
            fireStateChanged((AbstractSpinnerModel)tmin.getModel());
            fireStateChanged((AbstractSpinnerModel)tmax.getModel());
        }
    }

    /**
     * Run each ChangeListeners stateChanged()
     * method for the specified spinner model.
     */
    private static void fireStateChanged(final AbstractSpinnerModel model) {
        final ChangeEvent   changeEvent = new ChangeEvent(model);
        final EventListener[] listeners = model.getListeners(ChangeListener.class);
        for (int i=listeners.length; --i>=0;) {
            ((ChangeListener)listeners[i]).stateChanged(changeEvent);
        }
    }

    /**
     * Returns the start time, or <code>null</code> if there is none.
     */
    public Date getStartTime() {
        return (Date) tmin.getValue();
    }

    /**
     * Returns the end time, or <code>null</code> if there is none.
     */
    public Date getEndTime() {
        return (Date) tmax.getValue();
    }

    /**
     * Sets the time range.
     *
     * @param startTime The start time.
     * @param   endTime The end time.
     *
     * @see #getStartTime
     * @see #getEndTime
     */
    public void setTimeRange(final Date startTime, final Date endTime) {
        tmin.setValue(startTime);
        tmax.setValue(  endTime);
    }

    /**
     * Returns the accessory component.
     *
     * @return The accessory component, or <code>null</code> if there is none.
     */
    public JComponent getAccessory() {
        return accessory;
    }

    /**
     * Sets the accessory component. An accessory is often used to show available data.
     * However, it can be used for anything that the programmer wishes, such as extra
     * custom coordinate chooser controls.
     * <br><br>
     * Note: If there was a previous accessory, you should unregister
     * any listeners that the accessory might have registered with the
     * coordinate chooser.
     *
     * @param The accessory component, or <code>null</code> to remove any previous accessory.
     */
    public void setAccessory(final JComponent accessory) {
        synchronized (getTreeLock()) {
            if (this.accessory!=null) {
                remove(this.accessory);
            }
            this.accessory = accessory;
            if (accessory!=null) {
                final GridBagConstraints c=new GridBagConstraints();
                c.insets.right=c.insets.left=c.insets.top=c.insets.bottom=3;
                c.gridx=1; c.weightx=1; c.gridwidth=1;
                c.gridy=0; c.weighty=1; c.gridheight=3;
                c.anchor=c.CENTER; c.fill=c.BOTH;
                add(accessory, c);
            }
            validate();
        }
    }

    /**
     * Returns the resources.
     */
    private Resources getResources() {
        return Resources.getResources(getLocale());
    }

    /**
     * Check if an angle is of expected
     * type (latitude or longitude).
     */
    private void checkAngle(final JSpinner field, final boolean expectLatitude) throws ParseException {
        final Object angle=field.getValue();
        if (expectLatitude ? (angle instanceof Longitude) : (angle instanceof Latitude)) {
            throw new ParseException(getResources().getString(
                    ResourceKeys.ERROR_BAD_COORDINATE_$1, angle), 0);
        }
    }

    /**
     * Commits the currently edited values. If commit
     * fails, focus will be set on the offending field.
     *
     * @throws ParseException If at least one of currently
     *         edited value couldn't be commited.
     */
    public void commitEdit() throws ParseException {
        JSpinner focus=null;
        try {
            (focus=tmin).commitEdit();
            (focus=tmax).commitEdit();
            (focus=xmin).commitEdit();
            (focus=xmax).commitEdit();
            (focus=ymin).commitEdit();
            (focus=ymax).commitEdit();
            (focus=xres).commitEdit();
            (focus=yres).commitEdit();

            checkAngle(focus=xmin, false);
            checkAngle(focus=xmax, false);
            checkAngle(focus=ymin,  true);
            checkAngle(focus=ymax,  true);
        } catch (ParseException exception) {
            focus.requestFocus();
            throw exception;
        }
    }

    /**
     * Prend en compte les valeurs des champs �dit�s par l'utilisateur.
     * Si les entr�s ne sont pas valide, affiche un message d'erreur en
     * utilisant la fen�tre parente <code>owner</code> sp�cifi�e.
     *
     * @param  owner Fen�tre dans laquelle faire appara�tre d'eventuels messages d'erreur.
     * @return <code>true</code> si la prise en compte des param�tres � r�ussie.
     */
    private boolean commitEdit(final Component owner) {
        try {
            commitEdit();
        } catch (ParseException exception) {
            SwingUtilities.showMessageDialog(owner, exception.getLocalizedMessage(),
                                             getResources().getString(ResourceKeys.ERROR_BAD_ENTRY),
                                             JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    /**
     * Shows a dialog box requesting input from the user. The dialog box will be
     * parented to <code>owner</code>. If <code>owner</code> is contained into a
     * {@link javax.swing.JDesktopPane}, the dialog box will appears as an internal
     * frame. This method can be invoked from any thread (may or may not be the
     * <i>Swing</i> thread).
     *
     * @param  owner The parent component for the dialog box,
     *         or <code>null</code> if there is no parent.
     * @return <code>true</code> if user pressed the "Ok" button, or
     *         <code>false</code> otherwise (e.g. pressing "Cancel"
     *         or closing the dialog box from the title bar).
     */
    public boolean showDialog(final Component owner) {
        return showDialog(owner, getResources().format(ResourceKeys.COORDINATES_SELECTION));
    }

    /**
     * Shows a dialog box requesting input from the user. The dialog box will be
     * parented to <code>owner</code>. If <code>owner</code> is contained into a
     * {@link javax.swing.JDesktopPane}, the dialog box will appears as an internal
     * frame. This method can be invoked from any thread (may or may not be the
     * <i>Swing</i> thread).
     *
     * @param  owner The parent component for the dialog box,
     *         or <code>null</code> if there is no parent.
     * @param  title The dialog box title.
     * @return <code>true</code> if user pressed the "Ok" button, or
     *         <code>false</code> otherwise (e.g. pressing "Cancel"
     *         or closing the dialog box from the title bar).
     */
    public boolean showDialog(final Component owner, final String title) {
        while (SwingUtilities.showOptionDialog(owner, this, title)) {
            if (commitEdit(owner)) {
                return true;
            }
        }
        return false;
    }
}
