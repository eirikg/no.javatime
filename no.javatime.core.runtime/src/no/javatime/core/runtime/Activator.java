package no.javatime.core.runtime;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import no.javatime.inplace.extender.intface.Extender;

public class Activator implements BundleActivator {

	private static ExtenderTracker extenderTracker;
	private ExtenderServiceListener<?> extenderListener;
	private static BundleContext context;
	
	@Override
	public void start(BundleContext context) throws Exception {
		System.out.println("Start Run");
		Activator.context = context;

		extenderListener = new ExtenderServiceListener<>();
		context.addServiceListener(extenderListener, Extender.EXTENDER_FILTER);
		
		extenderTracker = new ExtenderTracker(context, Bundle.STARTING | Bundle.ACTIVE, null);
		extenderTracker.open();
		// Extenders.register(context.getBundle(), Simulator.class.getName(), new SimulatorImpl(), null);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		System.out.println("Stop Run");
		extenderTracker.close();
		extenderTracker = null;
		context.removeServiceListener(extenderListener);
		Activator.context = null;
	}
	
	public static ExtenderTracker getExtenderTracker() {
		return extenderTracker;
	}

	public static BundleContext getContext() {
		return context;
	}
}
