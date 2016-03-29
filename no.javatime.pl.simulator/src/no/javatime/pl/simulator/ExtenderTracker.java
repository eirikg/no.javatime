package no.javatime.pl.simulator;

import java.util.Collection;
import java.util.Dictionary;
import java.util.LinkedHashSet;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTrackerCustomizer;

import no.javatime.core.runtime.simulator.Simulator;
import no.javatime.inplace.extender.intface.BundleServiceScopeFactory;
import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.ExtenderBundleTracker;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.log.intface.BundleLog;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;

/**
 * Registers services provided by other bundles
 */

public class ExtenderTracker extends ExtenderBundleTracker {

	private Bundle thisBundle;
	private BundleLog bundleLog;

	public ExtenderTracker(BundleContext context, int stateMask,
			BundleTrackerCustomizer<Collection<Extender<?>>> customizer) {
		super(context, stateMask, customizer);
		thisBundle = context.getBundle();
	}
	Collection<Bundle> registeredBundles = new LinkedHashSet<>();
	@Override
	public Collection<Extender<?>> addingBundle(Bundle bundle, BundleEvent event)
			throws ExtenderException {

		try {
			Dictionary<String, String> headers = bundle.getHeaders();
			String serviceHeaderValue = headers.get(Simulator.MODEL_SIMULATOR_SERVICE);
			if (null != serviceHeaderValue) {
				trackExtender(bundle, Simulator.class.getName(),
						new BundleServiceScopeFactory<>(serviceHeaderValue), null);
			}
			serviceHeaderValue = headers.get(BundleLog.BUNDLE_LOG_SERVICE);
			if (null != serviceHeaderValue) {
				Extender<BundleLog> bundleLogExtender = trackExtender(bundle, BundleLog.class.getName(),
						new BundleServiceScopeFactory<>(serviceHeaderValue), null);
				bundleLog = bundleLogExtender.getService();
			}

		} catch (ExtenderException | IllegalStateException e) {
			if (null != bundleLog) {
				bundleLog.log(StatusCode.EXCEPTION, bundle, e, e.getMessage());
			} else {
				e.printStackTrace();
			}
		}
		return super.addingBundle(bundle, event);
	}

	@Override
	public void unregistering(Extender<?> extender) {
		super.unregistering(extender);
	}
	
	@Override
	public void removedBundle(Bundle bundle, BundleEvent event, Collection<Extender<?>> object) {

		super.removedBundle(bundle, event, object);
	}
}