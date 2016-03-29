/**
 * 
 */
package no.javatime.core.model.elements;

import no.javatime.core.model.annotations.ModelElement;
import no.javatime.core.model.annotations.ModelElement.Type;

/**
 * This represents a unit neutral time variable. 
 */
@ModelElement (type = Type.SYSTEM)
public class Time implements Events {

	private Double step;
	private Double nextStep;
	private Double max;
	private Double dt = Euler;
	private boolean isStop;

	@Override
	public Double getDt() {

		return dt;
	}
	
	@Override
	public void setDt(Double dt) {
		this.dt = dt;
	}

	@Override
	public Double getStep() {
		return step;
	}

	@Override
	public Boolean setStep(Double nextStep) {

		if (nextStep > step + 1 && nextStep <= max) {
			this.nextStep = nextStep;
			return true;
		}
		return false;
	}

	@Override
	public Double step() {
		
		if (nextStep >= step + 1 && nextStep <= max) {
			step = nextStep;
		} else {
			step = step + 1L;
			nextStep = step;
		}
		return step;
	}

	@Override
	public Double reset() {
		
		isStop = false;
		max = 0d;
		return step = nextStep = 0d;
	}

	@Override
	public Boolean setMax (Double maxSteps) {

		if (maxSteps > step) {
			max = maxSteps;
			return true;
		}
		return false;
	}

	@Override
	public Double getMax() {
		return max;
	}
	
	@Override
	public Boolean setStop(Boolean stop) {

		boolean isStopTmp = isStop;
		isStop = stop;
		return isStopTmp;
	}

	@Override
	public Boolean isStop() {

		return isStop || getStep() >= getMax() ? true : false;
	}
}
