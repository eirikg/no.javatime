/**
 * 
 */
package no.javatime.core.model.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use this annotation to receive initial and calculated simulation values after they are
 * initialized and calculated and before they are saved by the simulator.
 * <P>
 * The {@link no.javatime.core.model.elements.IEndogenous#setValue(Double) setValue(Double)} method is
 * called for each model element at each simulation step (including step 0) by the simulator:
 * <ol type="A">
 * <li>To set the initial value before the simulation starts (
 * {@link no.javatime.core.model.elements.IElement#getStep() step == 0L}). The initial value is provided
 * in the only parameter to this method
 * <li>Right after a new value is calculated. The calculated value is provided in the parameter.
 * </ol>
 * <p>
 * Create a method in an endogenous model element and use this annotation to receive the start value
 * and calculated values at each step. When annotated, the
 * {@link no.javatime.core.model.elements.Endogenous#setValue(Double) super.SetValue()} must be called
 * from the annotated method to store the initial and calculated values
 * <p>
 * Only one method can be annotated for each model element class, and the first one found,
 * traversing the super-class chain, is used.
 * <p>
 * The annotated method must have one parameter of type <code>Double</code>. It is not possible to
 * add additional parameters to the annotated method.
 * <p>
 * An example of a  model element (e.g. an Account) annotated with <code>@SetSeriesValue</code> is shown below:
 * 
 * <blockquote>
 * 
 * <pre>
 * <code>@SetSeriesValue</code>
 * public void setBalance(Double value) {
 * 	if (getStep() == OL) {
 *		// Do any checks/modifications to the start value
 * 	} else {
 *		// Do any additional calculations before storing the value at this step
 * 	}
 * 	super.setValue(value);
 * }
 * </pre>
 * 
 * </blockquote>
 * <p>
 * Note that the start value is provided by the method annotated with <code>@StartValue</code> and
 * the calculated value at each step is provided by the <code>@Action</code> annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Inherited
@Documented
public @interface SetSeriesValue {
}
