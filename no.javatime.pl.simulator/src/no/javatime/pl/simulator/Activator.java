package no.javatime.pl.simulator;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.internal.workbench.E4Workbench;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.extender.intface.Extenders;
import no.javatime.inplace.log.intface.BundleLog;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;
import no.javatime.pl.simulator.intface.SimulatorStarter;

public class Activator extends AbstractUIPlugin {

	private static ExtenderTracker extenderTracker;
	private static BundleContext context;
	
	SimulatorStarter simulationStarter;

	@Override
	public void start(BundleContext context) throws Exception {

		System.out.println("Start Simulate");
		Activator.context = context;
		try {
			extenderTracker = new ExtenderTracker(context, Bundle.STARTING | Bundle.ACTIVE, null);
			extenderTracker.open();
//			Simulator simulator = Extenders.getService(Simulator.class, context.getBundle());
//			Collection<Bundle> bundles = simulator.getModelBundles();
//			Bundle[] arrayBundle = bundles.toArray(new Bundle[bundles.size()]);
//			if (arrayBundle.length > 0) {
//				simulator.simulate(arrayBundle[0]);
//			}
			simulationStarter = new SimulatorStarter();
			simulationStarter.showView();
		} catch (ExtenderException | NullPointerException e) {
			BundleLog bundleLog = Extenders.getService(BundleLog.class);
			bundleLog.log(StatusCode.EXCEPTION, context.getBundle(), e, "Terminating simulation run with errors");
		}
	}
	
	@Override
	public void stop(BundleContext context) throws Exception {
		System.out.println("Stop Simulate");
		simulationStarter.hideView();	
		extenderTracker.close();
		extenderTracker = null;
		Activator.context = null;
	}

	@SuppressWarnings("restriction")
	public static IEclipseContext getServiceContext() {
		return E4Workbench.getServiceContext();
		// return EclipseContextFactory.getServiceContext(getContext());
	}

	public static BundleContext getContext() {
		return context;
	}
	
	/**
	 * Get a valid display
	 * 
	 * @return a display
	 */
	public static Display getDisplay() {
		Display display = Display.getCurrent();
		// May be null if outside the UI thread
		if (display == null) {
			display = Display.getDefault();
		}
		if (display.isDisposed()) {
			return null;
		}
		return display;
	}
}
