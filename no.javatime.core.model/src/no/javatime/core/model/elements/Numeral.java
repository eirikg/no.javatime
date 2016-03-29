package no.javatime.core.model.elements;

import no.javatime.core.model.annotations.ModelElement;
import no.javatime.core.model.annotations.ModelElement.Type;

@ModelElement (type = Type.EXOGENOUS)
public interface Numeral {

	/**
	 * Manifest header for accessing the default service implementation class name of the numeral service
	 */
	public final static String NUMERAL_SERVICE = "NUMERAL-Model-Element-Service";

	
	Double get();

	void set(Double value);

}