package no.javatime.core.model.elements;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.osgi.framework.Bundle;

import no.javatime.core.model.annotations.Start;
import no.javatime.inplace.extender.intface.Extender;

public class TimeSeriesProviderImpl extends System implements TimeSeriesProvider {

	private Bundle modelBundle;
	
	private Map<Extender<?>, ArrayList<Double>> timeSeries = new ConcurrentHashMap<>();

	private Collection<Double> steps = new ArrayList<>();

	@Override
	public String getModelName() {
		
		return null != modelBundle ? modelBundle.getSymbolicName() : "No model bundle name";
	}

	@Override
	@Start
	public Boolean initializeTimeSeries() {
		if (getStep() == 0d) {
			timeSeries = new ConcurrentHashMap<>();
			steps = new ArrayList<>();
		}
		return true;
	}

	@Override
	public void addStep(Double step) {
		this.steps.add(step);
	}

	@Override
	public Collection<Double> getSteps() {
		return steps;
	}

	@Override
	public double[] getRawSteps() {

		Double[] dL = steps.toArray(new Double[steps.size()]);		
		double[] dl = Stream.of(dL).mapToDouble(Double::doubleValue).toArray();
		return dl;
	}
	
	@Override
	public Map<Extender<?>, ArrayList<Double>> getTimeSeries() {
		return timeSeries;
	}

	@Override
	public Collection<Extender<?>> getModelElements() {
		return timeSeries.keySet();
	}
	
	@Override
	public double[] getTimeSeries(Extender<?> extender) {
		
		Collection<Double> timeSeriesElement = timeSeries.get(extender);
		Double[] yD = timeSeriesElement.toArray(new Double[timeSeriesElement.size()]);		
		double[] yd = Stream.of(yD).mapToDouble(Double::doubleValue).toArray();		
		return yd;
	}
	
	@Override
	public Collection<Double> getTimeseries(Long step) {
		
		Collection<Double> timeSeriesStep = new ArrayList<>();
		
		for (Extender<?> extender : timeSeries.keySet()) {
			ArrayList<Double> values = timeSeries.get(extender);
			Double[] arrayvalues = values.toArray(new Double[values.size()]);
			timeSeriesStep.add(arrayvalues[step.intValue()]);				
		}
		return timeSeriesStep;
	}
	
	@Override
	public void addTimeSeriesValue(Extender<?> extender, Double value) {

		ArrayList<Double> values = timeSeries.get(extender);

		if (null != values) {
			values.add(value);
		} else {
			values = new ArrayList<>();
			values.add(value);
			timeSeries.put(extender, values);
		}
	}

	@Override
	public Bundle getModelBundle() {
		return modelBundle;
	}

	@Override
	public void setModelBundle(Bundle modelBundle) {
		this.modelBundle = modelBundle;
	}

}
