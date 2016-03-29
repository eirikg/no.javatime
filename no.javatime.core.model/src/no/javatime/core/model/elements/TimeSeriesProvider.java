package no.javatime.core.model.elements;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.osgi.framework.Bundle;

import no.javatime.inplace.extender.intface.Extender;

public interface TimeSeriesProvider {
	/**
	 * Manifest header for accessing the default service implementation class name of the time service
	 */
	public final static String TIME_SERIES_SERVICE = "Time-Series-Model-Element-Service";

	String getModelName();

	public Bundle getModelBundle();

	public void setModelBundle(Bundle modelBundle);

	Boolean initializeTimeSeries();

	Map<Extender<?>, ArrayList<Double>> getTimeSeries();

	Collection<Extender<?>> getModelElements();

	double[] getTimeSeries(Extender<?> extender);

	Collection<Double> getTimeseries(Long step);

	Collection<Double> getSteps();
	
	double[] getRawSteps();

	void addStep(Double step);

	void addTimeSeriesValue(Extender<?> extender, Double value);

}