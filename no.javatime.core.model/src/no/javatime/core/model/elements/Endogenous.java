/**
 * 
 */
package no.javatime.core.model.elements;

import com.google.inject.Inject;

import no.javatime.core.model.annotations.GetSeriesValue;
import no.javatime.core.model.annotations.ModelElement;
import no.javatime.core.model.annotations.ModelElement.Type;
import no.javatime.core.model.annotations.SetSeriesValue;
import no.javatime.core.model.annotations.StartValue;
import no.javatime.inplace.extender.intface.Extenders;

/**
 *
 */
@ModelElement(type = Type.ENDOGENOUS)
public abstract class Endogenous extends Element implements IEndogenous {

	/**
	 * The mutable <code>value</code> field is used to store simulated values calculated by state and
	 * transition model elements and their sub model elements. For other endogenous model elements
	 * referenced from states and transitions the field is not used by the simulator and may be used
	 * as a temporary storage.
	 * <p>
	 * For state type model elements the <code>value</code> field represents the state of a model
	 * element. At step zero it is given an initial value and for all other steps the value is
	 * calculated form the state value at the previous step and referenced transition (and other model
	 * element types) values calculated in the transition from the previous step to the current step.
	 * Technically, transitions are than calculated after states at each step, including the initial
	 * step.
	 * <p>
	 * Model elements of type transition, and all model elements derived from transition, are
	 * stateless, and their value can be calculated repeatedly, producing the same result each time,
	 * within a step (including the initial step (step zero)). Therefore, there is no need for an
	 * initial value, at step zero. The transition value is calculated from the initial value of its
	 * referenced state model element(s) (and other element types) and represents the calculated value
	 * from one step to the next step. As it is a derived value, there is no requirement to store the
	 * calculated value in the <code>value</code> field.
	 * <p>
	 * Due to this dependency between states and transitions state model elements are calculated
	 * before transition model elements at each step.
	 * <p>
	 * The calculated value of a transition is stored in the <code>value</code> field to simplify the
	 * interpretation of the model, and to save computation time. Accessing or calculating a
	 * transition value at a given step will produce the same result.
	 * <p>
	 * The relationship and ordering of all model element types are specified by the model element
	 * annotation.
	 * 
	 * @see ModelElement
	 */
	private Double value;

	@Override
	@StartValue
	public Double startValue() {
		return 0d;
	}

	/**
	 * Get the calculated value of a class element at the current step
	 * 
	 * @return the calculated value of a class element at the current step
	 * @see #calculate()
	 */
	@Override
	@GetSeriesValue
	public Double getValue() {
		return value;
	}

	/**
	 * Sets the calculated value of a model element at the current step.
	 * <p>
	 * 
	 * @param value is the calculated value of a model element at the current step
	 * @see #calculate()
	 */
	@Override
	@SetSeriesValue
	public void setValue(Double value) {
		this.value = value;
	}

	@Override
	@Inject
	public Numeral getAsNumeral() {
		Numeral numeral = Extenders.getService(Numeral.class);
		numeral.set(new Double(value));
		return numeral;
	}

	@Override
	@Inject
	public void setAsNumeral(Numeral value) {
		this.value = new Double(value.get());
	}

}
