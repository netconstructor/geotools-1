/*
 * Geotools 2 - OpenSource mapping toolkit
 * (C) 2003, Geotools Project Management Committee (PMC)
 * (C) 2002, Institut de Recherche pour le Développement
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
 *     FRANCE: Surveillance de l'Environnement Assistée par Satellite
 *             Institut de Recherche pour le Développement / US-Espace
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package org.geotools;

// JUnit dependencies
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

// Test units
import org.geotools.cs.*;
import org.geotools.ct.*;
import org.geotools.pt.*;


/**
 * Performs all tests for the Coordinate Transformations Services implementation.
 *
 * @version $Id: CTS_Suite.java,v 1.9 2003/05/13 10:58:50 desruisseaux Exp $
 * @author Martin Desruisseaux
 */
public class CTS_Suite extends TestCase {
    /**
     * Construct a suite.
     */
    public CTS_Suite(final String name) {
        super(name);
    }        

    /**
     * Run the suite from the command line.
     */
    public static void main(final String[] args) {
        TestRunner.run(suite());
    }

    /**
     * Returns all suites.
     */
    public static Test suite() {
        final TestSuite suite = new TestSuite("All CTS tests");
        suite.addTest(CoordinateFormatTest     .suite());
        suite.addTest(WKTParserTest            .suite());
        suite.addTest(SerializationTest        .suite());
        suite.addTest(LinearTransformTest      .suite());
        suite.addTest(ConcatenatedTransformTest.suite());
        suite.addTest(ExponentialTransformTest .suite());
        suite.addTest(GeocentricTransformTest  .suite());
        suite.addTest(PassthroughTransformTest .suite());
        suite.addTest(ScriptTest               .suite());
        return suite;
    }
}
