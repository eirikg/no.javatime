package no.javatime.core.runtime.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;

import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

import no.javatime.core.model.annotations.ModelElement;

public class BundleUtil {

	/**
	 * Get the nearest bundles that provides capabilities to the specified bundle
	 * 
	 * @param bundle which other bundles provide capabilities to
	 * @return the list of direct bundles who provide capabilities to this bundle
	 */
	public static Collection<Bundle> getDirectProvidingBundles(Bundle bundle) {

		if (null != bundle) {
			BundleWiring wiredProvBundle = bundle.adapt(BundleWiring.class);
			if (null != wiredProvBundle && wiredProvBundle.isInUse()) {
				Collection<Bundle> providedBundles = new LinkedHashSet<Bundle>();
				for (BundleWire wire : wiredProvBundle.getRequiredWires(BundleRevision.PACKAGE_NAMESPACE)) {
					Bundle reqBundle = wire.getProviderWiring().getBundle();
					if (null != reqBundle) {
						providedBundles.add(reqBundle);
					}
				}
				for (BundleWire wire : wiredProvBundle.getRequiredWires(BundleRevision.BUNDLE_NAMESPACE)) {
					Bundle reqBundle = wire.getProviderWiring().getBundle();
					if (null != reqBundle) {
						providedBundles.add(reqBundle);
					}
				}
				return providedBundles;
			}
		}
		return Collections.emptyList();
	}

	/**
	 * Get the nearest bundles that requires capabilities from the specified bundle
	 * 
	 * @param bundle which other bundles require capabilities from
	 * @return the list of direct bundles who require capabilities from this bundle
	 */
	public static Collection<Bundle> getDirectRequiringBundles(Bundle bundle) {

		if (null != bundle) {
			BundleWiring wiredReqBundle = bundle.adapt(BundleWiring.class);
			if (null != wiredReqBundle && wiredReqBundle.isInUse()) {
				Collection<Bundle> requiredBundles = new LinkedHashSet<Bundle>();
				for (BundleWire wire : wiredReqBundle.getProvidedWires(BundleRevision.PACKAGE_NAMESPACE)) {
					Bundle reqBundle = wire.getRequirerWiring().getBundle();
					if (null != reqBundle) {
						requiredBundles.add(reqBundle);
					}
				}
				for (BundleWire wire : wiredReqBundle.getProvidedWires(BundleRevision.BUNDLE_NAMESPACE)) {
					Bundle reqBundle = wire.getRequirerWiring().getBundle();
					if (null != reqBundle) {
						requiredBundles.add(reqBundle);
					}
				}
				return requiredBundles;
			}
		}
		return Collections.emptyList();
	}

	/**
	 * Locate an return all local class (including interfaces) names in the specified bundle
	 * <p>
	 * The returned class names are on a form ready to be loaded by the class loader
	 * 
	 * @param bundle Bundle containing a set of classes
	 * @return List of class names
	 */
	public static Collection<String> getBundleClasses(Bundle bundle) {

		// TODO Only investigate exported packages
		Collection<String> classes = new LinkedList<>();
		BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
		if (null == bundleWiring) {
			return classes; // Wiring not in use
		}
		Collection<String> resources = bundleWiring.listResources("/", "*.class",
				BundleWiring.LISTRESOURCES_LOCAL | BundleWiring.LISTRESOURCES_RECURSE);
		for (String resource : resources) {
			// Remove ".class" at end of string
			int idx = resource.lastIndexOf('.');
			String classPath = (idx == -1) ? "" : resource.substring(0, idx);
			if (classPath.length() > 0) {
				classPath = classPath.replaceAll("/", ".");
				classes.add(classPath);
			}
		}
		return classes;
	}

	/**
	 * Model elements that are explicit provided and made available by a bundle for other bundles to
	 * load are found with the "Model-Element" header name in the manifest of the providing bundle.
	 * The header value is a list of fully qualified class names implementing a service where each
	 * class is delimited by a
	 * 
	 * @param modelElementHeaderValues String of fully qualified class names separated by semicolon
	 * @return List of fully qualified class names
	 * @throws BundleException If the header value is invalid
	 */
	public static Collection<String> getModelElementNames(String modelElementHeaderValues)
			throws BundleException {

		Collection<String> modelElementNames = new LinkedList<>();
		ManifestElement[] manifestElements = ManifestElement.parseHeader("Model-Element",
				modelElementHeaderValues);
		if (null != manifestElements) {
			for (int i = 0; i < manifestElements.length; i++) {
				String[] serviceNames = manifestElements[i].getValueComponents();
				for (int j = 0; j < serviceNames.length; j++) {
					modelElementNames.add(serviceNames[j]);
				}
			}
		}
		return modelElementNames;
	}

	/** Defines a set of model types where one of them is sufficient to define a model */
	private static EnumSet<ModelElement.Type> modelType = EnumSet.of(ModelElement.Type.STATE,
			ModelElement.Type.LEVEL, ModelElement.Type.INTEGRAL, ModelElement.Type.TRANSITION,
			ModelElement.Type.RATE, ModelElement.Type.DERIVATIVE);

	/**
	 * A bundle is a model if it at least contains one model element of type state or transition(and
	 * their derivates). Returns true if the specified model element is on of the required types.
	 * 
	 * @param modelElement Model element of any type or null
	 * @return True if the model element is an endogenous element of type state or transition (and
	 * their derivates). Otherwise false.
	 */
	public static boolean isModel(ModelElement modelElement) {

		return modelElement != null && modelType.contains(modelElement.type()) ? true : false;
	}

	/**
	 * Check if the specified headers of a manifest file has dependencies on the "no.javatime.core.model"
	 * bundle. If such dependency exist the bundle may or may not contain model elements
	 * <p>
	 * It is the import package and require bundle headers that are examined for dependencies.
	 * 
	 * @param manifestHeaders Manifest headers and values
	 * @return True if there are any dependencies on the on the model bundle. Otherwise false
	 * @throws BundleException If the header values is invalid
	 */
	public static boolean isModelElementBundle(Dictionary<String, String> manifestHeaders)
			throws BundleException {

		if (hasComponentValue("no.javatime.core.model.annotations", Constants.IMPORT_PACKAGE,
				manifestHeaders.get(Constants.IMPORT_PACKAGE))) {
			return true;
		}
		if (hasComponentValue("no.javatime.core.model", Constants.REQUIRE_BUNDLE,
				manifestHeaders.get(Constants.REQUIRE_BUNDLE))) {
			return true;
		}
		return false;
	}

	/**
	 * Search for the given component value in the given header value and return true if a match is
	 * found.
	 * <p>
	 * The specified header attribute key is the key for the specified header value. The attribute key
	 * is only specified to provide error messages when the header value is invalid.
	 * 
	 * @param componentValue The value to search for in the specified header value
	 * @param headerAttributeKey The attribute key of the specified header value
	 * @param headerValue A string of component values for the given header attribute key
	 * @return True if the given component value exist in the specified header value. Otherwise false
	 * @throws BundleException If the header value is invalid
	 */
	private static boolean hasComponentValue(String componentValue, String headerAttributeKey,
			String headerValue) throws BundleException {

		if (null == headerValue) {
			return false;
		}
		ManifestElement[] manifestElements = ManifestElement.parseHeader(headerAttributeKey,
				headerValue);
		if (null != manifestElements) {
			for (int i = 0; i < manifestElements.length; i++) {
				String[] componentValues = manifestElements[i].getValueComponents();
				for (int j = 0; j < componentValues.length; j++) {
					if (componentValue.equals(componentValues[j])) {
						return true;
					}
				}
			}
		}
		return false;
	}

}
