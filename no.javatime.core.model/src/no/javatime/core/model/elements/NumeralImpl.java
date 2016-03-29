package no.javatime.core.model.elements;

import no.javatime.core.model.annotations.ModelElement;
import no.javatime.core.model.annotations.ModelElement.Type;

@ModelElement (type = Type.EXOGENOUS)
public class NumeralImpl implements Numeral {

	private Double value = 0d;

	@Override
	public Double get() {
		return value;
	}

	@Override
	public void set(Double value) {
		this.value = value;
	}
}
