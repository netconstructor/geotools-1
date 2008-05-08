package org.geotools.po.bindings;


import org.geotools.xml.BindingConfiguration;
import org.picocontainer.MutablePicoContainer;

/**
 * Binding configuration for the http://www.geotools.org/po schema.
 *
 * @generated
 */
public final class POBindingConfiguration
	implements BindingConfiguration {


	/**
	 * @generated modifiable
	 */
	public void configure(MutablePicoContainer container) {
	
		//Types
		container.registerComponentImplementation(PO.Items,ItemsBinding.class);
		container.registerComponentImplementation(PO.PurchaseOrderType,PurchaseOrderTypeBinding.class);
		container.registerComponentImplementation(PO.SKU,SKUBinding.class);
		container.registerComponentImplementation(PO.USAddress,USAddressBinding.class);

	}

}