package no.javatime.core.model.elements;

import no.javatime.core.model.annotations.ModelElement;
import no.javatime.core.model.annotations.ModelElement.Type;

@ModelElement (type = Type.ENDOGENOUS)
public interface IEndogenous extends IElement {

	Double startValue();
	Double getValue();
	void setValue(Double value);
	Numeral getAsNumeral();
	void setAsNumeral(Numeral value);
}
