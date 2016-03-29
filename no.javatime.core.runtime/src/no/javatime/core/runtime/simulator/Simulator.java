package no.javatime.core.runtime.simulator;

import java.util.Collection;

import org.osgi.framework.Bundle;

import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.ExtenderException;

public interface Simulator {

	/**
	 * Manifest header for accessing the default service implementation class name of the simulator
	 */
	public final static String MODEL_SIMULATOR_SERVICE = "Model-Simulator-Service";

	Collection<Extender<?>> getModels();
	
	Collection<Bundle> getBundleModels();
	
	void simulate(Bundle bundle) throws ExtenderException;

	// void simulate(Collection<Extender<?>> models) throws ExtenderException;
	
}
