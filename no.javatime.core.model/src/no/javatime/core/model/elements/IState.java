/**
 * 
 */
package no.javatime.core.model.elements;

import no.javatime.core.model.annotations.ModelElement;
import no.javatime.core.model.annotations.ModelElement.Type;

/**
 * @author eg
 *
 */
@ModelElement (type = Type.STATE)
public interface IState extends IEndogenous {
	
	public Double inFlow();

	public Double outFlow();

	public Double netFlow();

	public Double calculate();
	
}
