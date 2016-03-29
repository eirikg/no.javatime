package no.javatime.core.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.LinkedList;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTrackerCustomizer;

import no.javatime.core.model.annotations.ModelElement;
import no.javatime.core.model.elements.Events;
import no.javatime.core.model.elements.Numeral;
import no.javatime.core.model.elements.TimeSeriesProvider;
import no.javatime.core.runtime.util.AnnotationUtil;
import no.javatime.core.runtime.util.BundleUtil;
import no.javatime.inplace.extender.intface.BundleServiceScopeFactory;
import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.ExtenderBundleTracker;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.extender.intface.Extenders;
import no.javatime.inplace.extender.intface.Introspector;
import no.javatime.inplace.extender.intface.PrototypeServiceScopeFactory;
import no.javatime.inplace.log.intface.BundleLog;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;

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

	/** This is the Xtext language bundle. Should not be added as a model
	 	Must change this if the name of the language bundle is changed
	 	Move this to the language bundle in question
	*/
	public static String DSL_LANG_SYMBOLIC_NAME = "no.javatime.lang";

	private Bundle thisBundle;
	private BundleLog bundleLog;
	private Bundle timeElementBundle;
	Collection<Bundle> modelBundles = new LinkedHashSet<>();

	public Collection<Bundle> getModelBundles() {
		return modelBundles;
	}

	public ExtenderTracker(BundleContext context, int stateMask,
			BundleTrackerCustomizer<Collection<Extender<?>>> customizer) {
		super(context, stateMask, customizer);
		thisBundle = context.getBundle();
	}

	@Override
	public Collection<Extender<?>> addingBundle(Bundle bundle, BundleEvent event)
			throws ExtenderException {

		try {
			Dictionary<String, String> headers = bundle.getHeaders();
			String timeServiceHeaderValue = headers.get(Events.EVENTS_SERVICE);
			// Register the events service
			if (null != timeServiceHeaderValue) {
				registerTimeSystemElement(bundle, timeServiceHeaderValue);
				// Register the time series provider
				String timeSeriesServiceHeaderValue = headers.get(TimeSeriesProvider.TIME_SERIES_SERVICE);
				if (null != timeSeriesServiceHeaderValue) {
					Collection<String> classPaths = new ArrayList<>();
					classPaths.add(timeSeriesServiceHeaderValue);
					registerModelElementClasses(bundle, classPaths);
					// Extender<TimeSeriesProvider> seriesExtender = Extenders
					// .getExtender(TimeSeriesProvider.class.getName());
					// if (null == seriesExtender) {
					// seriesExtender = Extenders.register(bundle, thisBundle,
					// TimeSeriesProvider.class.getName(), timeSeriesServiceHeaderValue, null);
					// }
					// logExdenderInfo(bundle, seriesExtender, null);
				}
				// Register the numeral service
				String valueServiceHeaderValue = headers.get(Numeral.NUMERAL_SERVICE);
				if (null != valueServiceHeaderValue) {
					Extender<Numeral> valueExtender = Extenders.getExtender(Numeral.class.getName());
					if (null == valueExtender) {
						valueExtender = Extenders.register(bundle, thisBundle, Numeral.class.getName(),
								new PrototypeServiceScopeFactory<Numeral>(valueServiceHeaderValue), null);
					}
					// logExdenderInfo(bundle, valueExtender, null);
				}
			}
			if (null != timeElementBundle) {
				// TODO May be used instead or as part of BundleTracker
				// TODO May also be used to filter timeElementBundle elements to include in a simulation run
				// Extender<Events> extender = Extenders.getExtender(Events.class.getName());
				// if (null != extender) {
				if (!bundle.getSymbolicName().endsWith(DSL_LANG_SYMBOLIC_NAME)) {
					Collection<Bundle> provBundles = BundleUtil.getDirectProvidingBundles(bundle);

					// If this bundle use the no.javatime.core.model bundle it is simulation model
					if (provBundles.contains(timeElementBundle)) {
						registerModel(bundle);						
					}
				}
				// Collection<Bundle> provBundles =
				// extenderTracker.getDirectProvidingBundles(extender.getOwner());
			}
			// }
			// if (isModelBundle(headers)) {
			// System.out.println("Import/Require Bundle header: " + bundle.getSymbolicName()
			// + " require capabilities from no.javatime.core.model");
			// Collection<String> classPaths = getBundleClasses(bundle);
			// registerModelElementClasses(bundle, classPaths);
			// }
			String serviceHeaderValue = headers.get(BundleLog.BUNDLE_LOG_SERVICE);
			if (null != serviceHeaderValue) {
				Extender<BundleLog> bundleLogExtender = trackExtender(bundle, BundleLog.class.getName(),
						new BundleServiceScopeFactory<>(serviceHeaderValue), null);
				bundleLog = bundleLogExtender.getService();
				// logExdenderInfo(thisBundle, bundleLogExtender, null);
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

	private boolean registerTimeSystemElement(Bundle bundle, String timeServiceInterfaceName) {

		boolean isRegistered = true;
		try {
			Extender<Events> eventsExtender = Extenders.getExtender(Events.class.getName());
			if (null == eventsExtender) {
				eventsExtender = Extenders.register(bundle, thisBundle, Events.class.getName(),
						timeServiceInterfaceName, null);
			}
			// logExdenderInfo(bundle, eventsExtender, null);
			timeElementBundle = bundle;
		} catch (ExtenderException e) {
			isRegistered = false;
		}
		return isRegistered;

	}

	/**
	 * Register and track classes annotated with <code>@ModelElement</code> as model element services and add
	 * the specified bundle as a model if it at least contains a model elements of type state or
	 * transition(and their derivates)
	 * <p>
	 * If failing to load or register a class a message is sent to the bundle log
	 * 
	 * @param bundle A bundle with model element classes
	 */
	private <S> void registerModel(Bundle bundle) {

		Collection<Bundle> provBundles = BundleUtil.getDirectProvidingBundles(bundle);
		// If this bundle use the no.javatime.core.model bundle it is simulation model
		if (provBundles.contains(timeElementBundle)) {
			Collection<String> classPaths = BundleUtil.getBundleClasses(bundle);
			Collection<Extender<S>> extenders = registerModelElementClasses(bundle, classPaths);
			if (extenders.size() > 0) {
				for (Extender<?> extender : extenders) {
					Class<?> serviceClass = extender.getServiceClass();
					ModelElement modelElement = serviceClass.getAnnotation(ModelElement.class);
					if (BundleUtil.isModel(modelElement)) {
						modelBundles.add(bundle);
						break;
					}
				}
			}
		}
	}

	/**
	 * Register and track classes annotated with <code>@ModelElement</code> as model element services
	 * <p>
	 * If an annotated class has no declared interfaces, the class is both the implementation and
	 * interface class.
	 * <p>
	 * If failing to load or register a class a message is sent to the bundle log
	 * 
	 * @param bundle A bundle with model element classes
	 * @param classPaths A list of all classes in the class path of the specified bundle
	 * @return A collection of registered services, each wrapped in an extender
	 */
	private <S> Collection<Extender<S>> registerModelElementClasses(Bundle bundle,
			Collection<String> classPaths) {

		Collection<Extender<S>> extenders = new LinkedList<>();
		// Only register once in cases where the service interface is the same as the service class
		Collection<Class<?>> registeredClasses = new HashSet<>();

		for (String classPath : classPaths) {
			try {

				Class<?> serviceClass = Introspector.loadClass(bundle, classPath);
				ModelElement modelElement = serviceClass.getAnnotation(ModelElement.class);
				// Do not register when interface, wait for the service implementation in class paths
				if (null != modelElement && !serviceClass.isInterface()
						&& !registeredClasses.contains(serviceClass)) {
					Dictionary<String, String> properties = new Hashtable<>();
					properties.put("key", modelElement.key());
					// Use implementation class if no declared interface
					String[] interfaces = AnnotationUtil.getInterfaces(serviceClass);
					Extender<S> extender = track(bundle, interfaces, serviceClass.getName(), properties);
					// Extender<S> extender = registerAndTrack(bundle, interfaces, serviceClass.getName(),
					// properties);
					extenders.add(extender);
					registeredClasses.add(serviceClass);
					// logExdenderInfo(thisBundle, extender, interfaces);
				}
			} catch (ExtenderException e) {
				BundleLog bundleLog = Extenders.getService(BundleLog.class);
				if (null != bundleLog) {
					bundleLog.add(StatusCode.WARNING, bundle, null,
							"Failed to load and/or register " + classPath);
				}

			}
		}
		return extenders;
	}

	public <S> Extender<S> track(Bundle owner, String[] serviceInterfaceNames, String service,
			Dictionary<String, ?> properties) throws ExtenderException {
		// TODO Works but need a rewrite
		Extender<S> extender = null;
		for (int i = 0; i < serviceInterfaceNames.length; i++) {
			extender = Extenders.getExtender(serviceInterfaceNames[i], owner);
			if (null == extender) {
				break;
			}
		}
		if (null == extender) {
			extender = registerAndTrack(owner, serviceInterfaceNames, service, properties);
		} else {
			trackExtender(extender);
		}
		return extender;
	}

	public void logExdenderInfo(Bundle bundle, Extender<?> extender, String[] interfaces) {
		BundleLog bundleLog = Extenders.getService(BundleLog.class);
		if (null != bundleLog) {
			if (null != interfaces && interfaces.length > 0) {
				bundleLog.add(StatusCode.INFO, bundle, null,
						"Registered service interface: " + interfaces[0]);
				for (int i = 1; i < interfaces.length; i++) {
					bundleLog.addToRoot(StatusCode.INFO, bundle, null,
							"plus service interface: " + interfaces[i]);
				}
			} else {
				bundleLog.add(StatusCode.INFO, bundle, null,
						"Registered service interface: " + extender.getServiceInterfaceName());
			}
			bundleLog.addToRoot(StatusCode.INFO, bundle, null,
					"Registrar bundle: " + extender.getRegistrar());
			bundleLog.addToRoot(StatusCode.INFO, bundle, null, "Owner bundle: " + extender.getOwner());
			bundleLog.addToRoot(StatusCode.INFO, bundle, null,
					"Implementation service: " + extender.getServiceClass().getName());
			bundleLog.addToRoot(StatusCode.INFO, bundle, null, "Key: " + extender.getProperty("key"));
			bundleLog.log();
		}
	}
}