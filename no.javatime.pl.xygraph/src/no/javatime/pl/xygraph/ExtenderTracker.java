package no.javatime.pl.xygraph;

import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTrackerCustomizer;

import no.javatime.core.model.annotations.ModelElement;
import no.javatime.core.model.elements.Events;
import no.javatime.core.model.elements.Numeral;
import no.javatime.core.model.elements.TimeSeriesProvider;
import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.ExtenderBundleTracker;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.pl.xygraph.impl.XYGraph;
import no.javatime.pl.xygraph.intface.IXYGraph;

/**
 * Create services for all model element classes provided by other bundles:
 * <ol>
 * <li>Create and track services for all classes in user bundles annotated with {@link ModelElement}
 * <li>Register but do not track services for model elements provided by the system model bundle:
 * <ol type="a">
 * <li>{@link Events}
 * <li>{@link TimeSeriesProvider}
 * <li>{@link Numeral}
 * </ol>
 * </ol>
 * Tracked user bundles are models to simulate while system model element services are injected or
 * referenced from model elements in user bundles.
 * <p>
 * The set of tracked model elements will always contain one or more complete simulation models. If
 * a model element is required from another bundle the providing bundle will be activated by the
 * InPlace Activator and tracked by this bundle due to bundle dependencies.
 */
public class ExtenderTracker extends ExtenderBundleTracker {

	public ExtenderTracker(BundleContext context, int stateMask,
			BundleTrackerCustomizer<Collection<Extender<?>>> customizer) {
		super(context, stateMask, customizer);
	}

	@Override
	public Collection<Extender<?>> addingBundle(Bundle bundle, BundleEvent event)
			throws ExtenderException {

		try {
			// Register our own graph service.
			// When registered here, the same service will be used by the runtime bundle
			Dictionary<String, String> headers = bundle.getHeaders();
			String graphHeaderValue = headers.get(IXYGraph.XYGRAPH_MODEL_SERVICE);
			if (null != graphHeaderValue) {
				ModelElement modelElement = XYGraph.class.getAnnotation(ModelElement.class);
				Dictionary<String, String> properties = null;
				if (null != modelElement) {
					properties = new Hashtable<>();
					properties.put("key", modelElement.key());
				}
				trackExtender(bundle, IXYGraph.class.getName(), graphHeaderValue, null);
			}

		} catch (ExtenderException | IllegalStateException e) {
		}
		return super.addingBundle(bundle, event);
	}

	@Override
	public void removedBundle(Bundle bundle, BundleEvent event, Collection<Extender<?>> object) {

		// Remove the graph view when bundle is going down
		for (Object eObject : object) {
			if (eObject instanceof Extender<?>) {
				Extender<?> extender = (Extender<?>) eObject;
				if (extender.getServiceClass().equals(XYGraph.class)) {
					IXYGraph xyGraph = (IXYGraph) extender.getService();
					if (null != xyGraph) {
						xyGraph.hideView();
					}
				}
			}
		}
		super.removedBundle(bundle, event, object);
	}
}