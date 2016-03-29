package no.javatime.core.runtime.simulator;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NoSuchElementException;

import org.osgi.framework.Bundle;

import no.javatime.core.model.annotations.Action;
import no.javatime.core.model.annotations.StartValue;
import no.javatime.core.model.annotations.Stop;
import no.javatime.core.model.elements.Events;
import no.javatime.core.model.elements.TimeSeriesProvider;
import no.javatime.core.runtime.Activator;
import no.javatime.core.runtime.ExtenderTracker;
import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.extender.intface.Extenders;
import no.javatime.inplace.log.intface.BundleLog;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;

/**
 * Run a simulation
 * <ol>
 * <li>Get the events service and set upper bound of simulation steps. The events service is a
 * singleton that keeps information about delta time and track the simulation steps
 * <li>Inject fields and sort all model element services in execution order.
 * <li>Sort all methods in execution order within each model element service before initializing the
 * model
 * <li>Initialize the model by execution the sorted methods within each model element service
 * <li>Sort all methods in execution order within each model element service to drive the simulation
 * <li>Run the model by execution sorted methods within each model element service
 * </ol>
 * <p>
 * All model element services are sorted first. This ordering is used both when initializing and
 * running the model. For each phase (initialize and run) a separate set of methods are sorted in
 * execution order, and executed once for each ordered model element service.
 *
 */
public class SimulatorImpl implements Simulator {

	private final static DecimalFormat defaultFormat = new DecimalFormat("###,###.###");
	
	
	@Override
	public Collection<Bundle> getBundleModels() {
		ExtenderTracker tracker = Activator.getExtenderTracker();
		return tracker.getModelBundles();
	}

	@Override
	public Collection<Extender<?>> getModels() {
		return ClassModelElementSorter.getTrackedModels();
	}
	
	/**
	 * Sort simulation model and inject fields and method parameters before executing the sorted model
	 * <p>
	 * Sequence of method calls invoked by the simulator for one model element instance:
	 * <ol>
	 * (1) Initialization: (simulation at step 0) 
	 * <li>init(); // Return start value (default is 0)
	 * <li>setStartValue(Double) // Set the start value field (startValue) returned by init() 
	 * <li>set(Double) // At step 0. Same value as set by setStartValue but different field (value)
	 * <li>start() // Check for start condition. If true run the simulation step by step
	 * </ol> 
	 * <ol>
	 * (2) For each simulation step (start at step 1) 
	 * <li>Time.step() // Increment simulation one step 
	 * <li>calculate() // calculate and return simulated value at current step 
	 * <li>set() // Set the returned calculated value (not set by calculate))
	 * <li>stop() // Check for stop condition. If false continue with next step
	 * </ol> 
	 * 
	 */
	@Override
	public void simulate(Bundle bundle) throws ExtenderException {

		// Set step length and delta time
		Events events = Extenders.getService(Events.class);
		events.reset();
		events.setMax(5d);
		events.setDt(1d);
		// Sort model element classes according to execution order
		ClassModelElementSorter sorter = new ClassModelElementSorter();
		Collection<Extender<?>> sortedModelElements = sorter.sort(bundle);			
//		if (null == models) {
//			sortedModelElements = sorter.sort(ClassModelElementSorter.getTrackedModelElements());			
//		} else {
//			sortedModelElements = sorter.sort(models);						
//		}
		// Sort methods in each model element class in execution order
		MethodModelElementSorter methodSorter = new MethodModelElementSorter();
		// Sort methods in initialization execution order to execute step zero
		LinkedHashMap<Extender<?>, LinkedHashSet<MethodModelElement>> initializeExecutionChain = methodSorter
				.sortInitalMethods(sortedModelElements);
		methodSorter.logExecInitChain();
		// Sort methods in execution order to run the simulation (step 1 -> max steps)
		LinkedHashMap<Extender<?>, LinkedHashSet<MethodModelElement>> runExecutionChain = methodSorter
				.sortRunMethods(sortedModelElements);
		methodSorter.logExecRunChain();
		// Execute the sorted model in two phases
		BundleLog bundleLog = Extenders.getService(BundleLog.class);
		bundleLog.add(StatusCode.INFO, Activator.getContext().getBundle(), null, "Simulation run");
		TimeSeriesProvider timeSeriesProvider = Extenders.getService(TimeSeriesProvider.class);
		//timeSeriesProvider.setModels(sorter.getModels());
		timeSeriesProvider.setModelBundle(bundle);
		// Execute the methods initializing the simulation for each model element
		executeInitialStep(events, timeSeriesProvider, initializeExecutionChain, bundleLog);
		// Execute the methods running the simulation for each model element
		executeSteps(events, timeSeriesProvider, runExecutionChain, bundleLog);
		bundleLog.log();
	}

	private void executeInitialStep(Events events, TimeSeriesProvider timeSeriesProvider,
			LinkedHashMap<Extender<?>, LinkedHashSet<MethodModelElement>> execChain, BundleLog bundleLog)
					throws ExtenderException {

		StringBuffer buffer = new StringBuffer();

		for (Map.Entry<Extender<?>, LinkedHashSet<MethodModelElement>> entry : execChain.entrySet()) {
			Extender<?> extenderElement = entry.getKey();
			LinkedHashSet<MethodModelElement> methodElements = entry.getValue();
			// Sorted methods in model element
			for (Iterator<MethodModelElement> iterator = methodElements.iterator(); iterator.hasNext();) {
				MethodModelElement methodModelElement = iterator.next();
				Object returnValue = methodModelElement.execute();
				if (null != returnValue && returnValue instanceof Double
						&& methodModelElement.getAnnotationClass() == StartValue.class) {
					buffer.append(extenderElement.getServiceClass().getSimpleName() + "."
							+ methodModelElement.method.getName() + ": " + defaultFormat.format(returnValue)
							+ " ");
					try {
						methodModelElement = iterator.next();
						// Save the start value at step 0
						methodModelElement.executeSet((Double) returnValue);
					} catch (NoSuchElementException e) {
						throw new ExtenderException(e,
								"Internal error: Missing method for setting start value");
					}
				}
			}
		}
		// Log simulation length and initial values
		bundleLog.addToRoot(StatusCode.INFO, Activator.getContext().getBundle(), null,
				"Initial step value: " + events.getStep() + " Max step value: " + events.getMax());
		bundleLog.addToRoot(StatusCode.INFO, Activator.getContext().getBundle(), null,
				"Step " + events.getStep() + ": " + buffer);
	}

	private void executeSteps(Events events, TimeSeriesProvider timeSeriesProvider, 
			LinkedHashMap<Extender<?>, LinkedHashSet<MethodModelElement>> execChain, BundleLog bundleLog)
					throws ExtenderException {

		StringBuffer buffer = new StringBuffer();

		while (!events.isStop()) {
			events.step();
			timeSeriesProvider.addStep(events.getStep());
			for (Map.Entry<Extender<?>, LinkedHashSet<MethodModelElement>> entry : execChain.entrySet()) {
				Extender<?> extenderElement = entry.getKey();
				LinkedHashSet<MethodModelElement> methodElements = entry.getValue();
				for (Iterator<MethodModelElement> iterator = methodElements.iterator(); iterator
						.hasNext();) {
					MethodModelElement methodModelElement = iterator.next();
					Class<?> annotationClass = methodModelElement.getAnnotationClass(); 
					if (events.isStop() && annotationClass == Stop.class) {
						// Do not stop until all model elements has finished this step
							continue;
					}
					Object returnValue = methodModelElement.execute();
					if (null != returnValue && returnValue instanceof Double
							&& annotationClass == Action.class) {

						buffer.append(extenderElement.getServiceClass().getSimpleName() + "."
								+ methodModelElement.method.getName() + ": " + defaultFormat.format(returnValue)
								+ " ");
						timeSeriesProvider.addTimeSeriesValue(extenderElement, (Double) returnValue);
						try {
							methodModelElement = iterator.next();
							// Save the simulated value at this step
							methodModelElement.executeSet((Double) returnValue);
						} catch (NoSuchElementException e) {
							throw new ExtenderException(e,
									"Internal error: Missing method for setting calculated value");
						}
					} else if (null != returnValue && returnValue instanceof Boolean
							&& annotationClass == Stop.class) {
						events.setStop((Boolean) returnValue); 
					}
				}
			}
			// Log simulated value at the current step
			bundleLog.addToRoot(StatusCode.INFO, Activator.getContext().getBundle(), null,
					"Step " + events.getStep() + ": " + buffer);
			buffer.delete(0, buffer.length());
		}
		// Invoke stop for each model element after terminating the simulation
		executeStop(events, execChain, bundleLog);
	}

	private void executeStop(Events events,
			LinkedHashMap<Extender<?>, LinkedHashSet<MethodModelElement>> execChain, BundleLog bundleLog)
					throws ExtenderException {

		StringBuffer buffer = new StringBuffer();

		for (Map.Entry<Extender<?>, LinkedHashSet<MethodModelElement>> entry : execChain.entrySet()) {
			LinkedHashSet<MethodModelElement> methodElements = entry.getValue();
			for (Iterator<MethodModelElement> iterator = methodElements.iterator(); iterator.hasNext();) {
				MethodModelElement methodModelElement = iterator.next();
				if (methodModelElement.getAnnotationClass() == Stop.class) {
					methodModelElement.execute();
					Extender<?> extenderElement = entry.getKey();
					buffer.append("Execute Stop after step: " + events.getStep().toString() + " for "
							+ extenderElement.getServiceClass().getSimpleName() + "."
							+ methodModelElement.method.getName());
					bundleLog.addToRoot(StatusCode.INFO, Activator.getContext().getBundle(), null,
							buffer.toString());
					buffer.delete(0, buffer.length());
				}
			}
		}
	}
}
