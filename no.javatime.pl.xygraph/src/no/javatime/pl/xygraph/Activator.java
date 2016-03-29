package no.javatime.pl.xygraph;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.internal.workbench.E4Workbench;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class Activator extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "no.javatime.pl.xygraph"; //$NON-NLS-1$

	private static Activator plugin;
	private static BundleContext context;
	private static ExtenderTracker extenderTracker;

	public Activator() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		Activator.context = context;
		extenderTracker = new ExtenderTracker(context, Bundle.STARTING | Bundle.ACTIVE, null);
		extenderTracker.open();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		extenderTracker.close();
		extenderTracker = null;
		plugin = null;
		super.stop(context);
		Activator.context = null;
	}

	public static Activator getDefault() {
		return plugin;
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
