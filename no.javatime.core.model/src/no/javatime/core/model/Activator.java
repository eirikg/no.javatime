package no.javatime.core.model;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.extender.intface.Extenders;
import no.javatime.inplace.log.intface.BundleLog;
import no.javatime.inplace.region.intface.InPlaceException;
import no.javatime.inplace.region.status.IBundleStatus;

public class Activator implements BundleActivator {

	private static BundleLog bundleLog;
	private static BundleContext context;

	public static BundleContext getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext bundleContext) throws Exception {
		System.out.println("Start Model");
		Activator.context = bundleContext;
		// Extenders.register(context.getBundle(), Events.class.getName(), new Time(), null);
		bundleLog = Extenders.getService(BundleLog.class);
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		System.out.println("Stop Model");
		Activator.context = null;
	}
	
	/**
	 * Log the specified status object to the bundle log
	 * 
	 * @return the bundle status message
	 * @throws ExtenderException if failing to get the extender service for the bundle log
	 * @throws InPlaceException if the bundle log service returns null
	 */
	public String log(IBundleStatus status) throws InPlaceException, ExtenderException {
		return bundleLog.log(status);
	}
}
