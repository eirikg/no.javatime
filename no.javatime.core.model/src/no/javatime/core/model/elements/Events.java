package no.javatime.core.model.elements;

import no.javatime.core.model.annotations.ModelElement;
import no.javatime.core.model.annotations.ModelElement.Type;

@ModelElement(type = Type.SYSTEM)
public interface Events {

	/**
	 * Manifest header for accessing the default service implementation class name of the time service
	 */
	public final static String EVENTS_SERVICE = "Time-Model-Element-Service";

	final Double Euler = 1d;

	/**
	 * @return the dt
	 */
	Double getDt();

	/**
	 * @param dt the dt to set
	 */
	void setDt(Double dt);

	/**
	 * @return the step
	 */
	Double getStep();

	/**
	 * Set the next step value.
	 * <p>
	 * If the value is less or equal next step (<code>getStep() + 1</code>) or larger max
	 * step (<code>getMax()</code> + n where n > 0 the setting is ignored and <code>false</code> is returned.
	 * Otherwise the next simulation step is the specified step.
	 * 
	 * @param nextStep The new next step
	 * @return true if <code>nextStep > getStep() + 1</code> and
	 * <code>nextStep < getMax() Otherwise false
	 */
	Boolean setStep(Double nextStep);

	/**
	 * The simulation start at one for each run and advances with one for each simulation step.
	 * 
	 * @return the next simulation step
	 */
	Double step();

	Double reset();

	Boolean setMax(Double maxSteps);

	Double getMax();

	Boolean setStop(Boolean stop);

	Boolean isStop();

}