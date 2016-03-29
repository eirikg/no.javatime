package no.javatime.pl.xygraph.intface;

import org.eclipse.e4.ui.model.application.ui.basic.MPart;

import no.javatime.core.model.elements.Events;
import no.javatime.core.model.elements.TimeSeriesProvider;

public interface IXYGraph {

	/**
	 * Manifest header for accessing the default service implementation class name of the XY graph
	 */
	public final static String XYGRAPH_MODEL_SERVICE = "XYGraph-Model-Service";

	/**
	 * Show the graph view
	 * 
	 */
	void showView();

	/**
	 * Show or update the graph with the specified time series if a simulation run is in its end state
	 * as determined by {@link Events#isStop()}
	 * <p>
	 * Called by the simulator at each step, displaying or updating the graph at the end of a simulation
	 * 
	 * @param time The shared time service driving a simulation run 
	 * @param provider Output from a simulation run
	 * @return true if the graph was shown or updated with specified time series, false if not
	 */
	Boolean showView(Events time, TimeSeriesProvider provider);

	/**
	 * Update the graph with the specified time series
	 * @param provider Time series to update the graph with
	 */
	void updateGraph(TimeSeriesProvider provider);


	MPart findPart();

	void hideView();

}