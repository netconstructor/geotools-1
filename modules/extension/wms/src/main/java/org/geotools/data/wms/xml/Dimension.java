package org.geotools.data.wms.xml;


/**
 * Property class for holding and handling of property values declared in 
 * Dimension-element of a layer
 * 
 * http://schemas.opengis.net/wms/1.1.1/WMS_MS_Capabilities.dtd
 * <!-- The Dimension element declares the _existence_ of a dimension. -->
 * <!ELEMENT Dimension EMPTY >
 * <!ATTLIST Dimension
 *           name CDATA #REQUIRED
 *           units CDATA #REQUIRED
 *           unitSymbol CDATA #IMPLIED>
 *
 * http://schemas.opengis.net/wms/1.3.0/capabilities_1_3_0.xsd
 * <element name="Dimension">
 *	<complexType>
 *		<simpleContent>
 *			<extension base="string">
 *				<attribute name="name" type="string" use="required"/>
 *				<attribute name="units" type="string" use="required"/>
 *				<attribute name="unitSymbol" type="string"/>
 *				<attribute name="default" type="string"/>
 *				<attribute name="multipleValues" type="boolean"/>
 *				<attribute name="nearestValue" type="boolean"/>
 *				<attribute name="current" type="boolean"/>
 *			</extension>
 *		</simpleContent>
 *	</complexType>
 * </element>
 * 
 * @version SVN $Id$
 * @author Per Engstrom, Curalia AB, pereng@gmail.com
 *
 */
public class Dimension {

	protected String name;
	protected String units;
	protected String unitSymbol;
	protected boolean current;
	protected Extent extent;
	
	public Dimension(String name, String units, String unitSymbol) {
		if (name == null || name.length() == 0) {
			throw new IllegalArgumentException("Error creating Extent: parameter name must not be null!");
		}
		if (units == null || units.length() == 0) {
			throw new IllegalArgumentException("Error creating Extent: parameter units must not be null!");
		}
		
		this.name = name;
		this.units = units;
		this.unitSymbol = unitSymbol;
	}
	
	public Dimension(String name, String units) {
		if (name == null || name.length() == 0) {
			throw new IllegalArgumentException("Error creating Extent: parameter name must not be null!");
		}
		if (units == null || units.length() == 0) {
			throw new IllegalArgumentException("Error creating Extent: parameter units must not be null!");
		}

		this.name = name;
		this.units = units;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUnits() {
		return units;
	}

	public void setUnits(String units) {
		this.units = units;
	}

	public String getUnitSymbol() {
		return unitSymbol;
	}

	public void setUnitSymbol(String unitSymbol) {
		this.unitSymbol = unitSymbol;
	}
	
	public boolean isCurrent() {
		return current;
	}

	public void setCurrent(boolean current) {
		this.current = current;
	}

	public Extent getExtent() {
		return extent;
	}

	public void setExtent(Extent extent) {
		this.extent = extent;
	}

	public String toString() {
		return name + ", " + units + "(" + unitSymbol +")";
	}
	
}
