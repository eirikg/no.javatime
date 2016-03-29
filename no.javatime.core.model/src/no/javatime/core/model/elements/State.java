/**
 * 
 */
package no.javatime.core.model.elements;

import java.util.Collection;
import java.util.LinkedHashSet;

import no.javatime.core.model.annotations.Action;
import no.javatime.core.model.annotations.ModelElement;
import no.javatime.core.model.annotations.ModelElement.Type;


/**
 * @author eg
 *
 */
@ModelElement (type = Type.STATE)
public abstract class State extends Endogenous implements IState {

	private Collection<Transition> inFlow;
	private Collection<Transition> outFlow;

	public void addInFlow(Transition... inFlows) {
		if (null == inFlow) {
			inFlow = new LinkedHashSet<>();
		}
		for (Transition e : inFlows) {
			inFlow.add(e);
		}
	}

	public void addOutFlow(Transition... outFlows) {
		if (null == outFlow) {
			outFlow = new LinkedHashSet<>();
		}
		for (Transition e : outFlows) {
			outFlow.add(e);
		}
	}

	@Override
	public Double inFlow() {

		if (null != inFlow) {
			Double value = new Double(0d);
			for (Transition t : inFlow) {
				value = + t.getValue();
			}
			return value;
		}
		return 0d;
	}

	@Override
	public Double outFlow() {

		if (null != outFlow) {
			Double value = new Double(0d);
			for (Transition t : outFlow) {
				value = +t.getValue();
			}
			return value;
		}
		return 0d;
	}

	@Override
	public Double netFlow() {
		return inFlow() - outFlow();
	}

	@Override
	@Action
	public Double calculate() {		
		return getValue() + (netFlow()*time.getDt());
	}
}
