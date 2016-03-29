package no.javatime.core.runtime.simulator;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import no.javatime.core.model.annotations.Model;
import no.javatime.core.model.annotations.ModelElement;
import no.javatime.core.runtime.Activator;
import no.javatime.core.runtime.ExtenderTracker;
import no.javatime.core.runtime.util.AnnotationUtil;
import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.extender.intface.Extenders;
import no.javatime.inplace.log.intface.BundleLog;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;

/**
 * Defines a model, sorts its model elements in execution order and inject model elements declared
 * as fields in model elements.
 * <p>
 * A model is an acyclic graph of model elements and their relationships. A model element is a class
 * with an optional interface annotated with {@link ModelElement} and a type of
 * {@link ModelElement#type()}. Model elements are automatically registered as services when the
 * bundle hosting model elements is activated. Each service is tracked and wrapped in an
 * {@link Extender}.
 * <p>
 * <ol type="1">
 * <li>The model element sorter takes a random model element optionally annotated with {@link Model}
 * as input and collects the model elements of the model by traversing the graph of dependent model
 * elements
 * <li>Performs a topological sort of all the model element types where the output is the execution
 * order of the model elements constituting the model
 * <li>Injects references to model element services for all fields declared as model elements
 * </ol>
 * <p>
 * Model elements are further grouped according to model element types and cyclic references within
 * groups of model elements are logged as warnings.
 * <p>
 * For model, model element types and group of model element types:
 * 
 * @see Model
 * @see ModelElement
 */
public class ClassModelElementSorter {

	private Collection<Extender<?>> models = new LinkedHashSet<>();

	/**
	 * A unique list of all model elements in a model sorted in execution order where all model
	 * element fields are injected
	 */
	private Collection<Class<?>> classExecOrder = new LinkedHashSet<>();

	/** If cycles between model elements of specific groups should be logged */
	private boolean cycles;

	public Collection<Extender<?>> getModels() {
		return models;
	}

	/**
	 * Get the execution order of model elements with all model elements fields injected
	 * <p>
	 * To return an ordered set {@link #sort(Collection)} must be invoked on beforehand
	 * 
	 * @return An ordered collection of model elements representing the execution order of a model or
	 * an empty collection
	 */
	public Collection<Class<?>> getClassExecOrder() {
		return classExecOrder;
	}

	public Collection<Extender<?>> sort(Bundle bundle) throws ExtenderException {

		Collection<Extender<?>> extenders = getTrackedModelElements(bundle);
		return sort(extenders);
	}

	/**
	 * Inject and sort model elements with the specified set start extender services.
	 * <p>
	 * See {@link #visitElement(Class, Class, EnumSet, List)} for a specification of the sort method
	 * <p>
	 * 
	 * @param extenders Extender services to sort
	 * @return Sorted collection of model element extender services
	 * @throws ExtenderException If the service id is null for the registered service
	 */
	public Collection<Extender<?>> sort(Collection<Extender<?>> extenders) throws ExtenderException {

		Collection<Class<?>> modelElementClasses = new LinkedHashSet<>();
		for (Extender<?> extender : extenders) {
			Class<?> serviceClass = extender.getServiceClass();
			// TODO Restricted models is not implemented
			if (serviceClass.isAnnotationPresent(Model.class)) {
				models.add(extender);
			}
			modelElementClasses.add(serviceClass);
		}
		// Model elements of type system are always included
		EnumSet<ModelElement.Type> modelTypes = EnumSet.of(ModelElement.Type.SYSTEM); 
		modelElementClasses.addAll(getTrackedModelElementTypes(modelTypes));

		// Phase one collects all independent (providing) model elements down the dependency chain
		// This will add model elements from other bundles
		boolean allowCyclesTmp = this.cycles;
		setAllowCycles(true);
		classExecOrder.clear();
		modelTypes = EnumSet.allOf(ModelElement.Type.class);
		sortModelElements(modelElementClasses, modelTypes);
		// Add the complete model as input to phase two sort
		modelElementClasses.addAll(classExecOrder);
		logSortedModelElements(modelElementClasses);
		this.cycles = allowCyclesTmp;
		// Sort according to groups of model element types
		sortModel(modelElementClasses);
		Collection<Extender<?>> sortedExtenders = getModelElementExtenders(classExecOrder);
		AnnotationUtil.injectModelElements(sortedExtenders);
		return sortedExtenders;
	}

	/**
	 * There is one service registered for each element in the model. Elements of the same type are
	 * grouped based on the {@link ModelElement} annotation type of an element. One type may consist
	 * of multiple annotation types. This has no semantic significance beyond that alternative
	 * annotation type names may be used for the same model element type. Model elements of one type
	 * (one or more annotation types) is independent of the execution order of other types, but are
	 * always dependent on their own internal order.
	 * <p>
	 * As a consequence of this grouping, the ordering is split into separate phases - one for each
	 * type -, where the result of each phase is added to the execution chain. Using a common super
	 * class for elements is an alternative grouping technique but not applied here. The algorithm has
	 * many valid topological sorts, each with a valid execution order.
	 * <p>
	 * The following general rules applies for the execution order of model elements:
	 * <ol>
	 * <li>Exogenous model elements are executed first and may be referenced from all types of model
	 * elements, but an exogenous model element may not reference other (endogenous) model elements,
	 * except for system elements and elements of their own type. Exogenous class elements are not
	 * dependent on other endogenous class elements, and has no requirement on the execution order
	 * except for their internal order.</li>
	 * <li>States are calculated at time t, based on their value from the previous step (t-1), other
	 * elements at time t-1 and model elements (e.g. transitions) in the time interval from the
	 * previous step to the current step (t-1 -> t). Hence states are executed after exogenous
	 * elements and are not dependent on other types of model elements.
	 * <p>
	 * The internal execution order of states are determined by their internal direct and indirect
	 * dependencies. That is if Class A, declares a member field of Class B, Class A is dependent on
	 * Class B, and hence Class B must be executed before Class A, both directly and indirectly.</li>
	 * <li>Transitions are calculated at time t for the interval (t -> t+1) from states and
	 * auxiliaries at time t and occasionally from transitions in the (t-1 -> t) interval.
	 * <p>
	 * The internal execution order of transitions are determined by their internal dependencies. The
	 * same internal dependency rules applies to transitions as for states.</li>
	 * <li>Auxiliaries are calculated at time t from levels and other auxiliaries at time t and
	 * occasionally from transitions in the (t-1, t) interval. By their nature they can be eliminated
	 * by substitution into the transition equations
	 * <p>
	 * The internal execution order of auxiliaries are determined by their internal dependencies.</li>
	 * </ol>
	 * 
	 * @param model Collection of model elements to sort
	 * @return true if the elements where sorted without errors
	 * @throws ExtenderException If the service id is null for the registered service
	 */
	public void sortModel(Collection<Class<?>> model) throws ExtenderException {

		classExecOrder.clear();
		// Order internal dependencies of elements of same type group
		EnumSet<ModelElement.Type> modelTypes = EnumSet.of(ModelElement.Type.SYSTEM);
		sortModelElements(model, modelTypes);
		modelTypes = EnumSet.of(ModelElement.Type.EXOGENOUS);
		sortModelElements(model, modelTypes);
		modelTypes = EnumSet.of(ModelElement.Type.INPUT);
		sortModelElements(model, modelTypes);
		modelTypes = EnumSet.of(ModelElement.Type.AUXILIARY, ModelElement.Type.ENDOGENOUS);
		sortModelElements(model, modelTypes);
		modelTypes = EnumSet.of(ModelElement.Type.STATE, ModelElement.Type.INTEGRAL,
				ModelElement.Type.LEVEL);
		sortModelElements(model, modelTypes);
		modelTypes = EnumSet.of(ModelElement.Type.TRANSITION, ModelElement.Type.RATE,
				ModelElement.Type.DERIVATIVE);
		sortModelElements(model, modelTypes);
		logSortedModelElements(classExecOrder);
	}

	/**
	 * Sort all specified model elements of the specified model types in execution order
	 * <p>
	 * The sorted model elements may be obtained from {@link #getClassExecOrder()}
	 * 
	 * @param modelElements Collection of model elements to sort based on the specified type
	 * @param modelTypes Types of model elements annotated with {@link ModelElement}
	 * @throws ExtenderException If the service id is null for the registered service
	 */
	protected <A extends Annotation> void sortModelElements(Collection<Class<?>> modelElements,
			EnumSet<ModelElement.Type> modelTypes) throws ExtenderException {

		if (null != modelElements) {
			for (Class<?> element : modelElements) {
				// Skip non model elements and type of elements not included in this phase
				ModelElement annotation = element.getAnnotation(ModelElement.class);
				if (null != annotation && modelTypes.contains(annotation.type())) {
					visitElement(element, null, modelTypes, new LinkedList<Class<?>>());
				}
			}
		}
	}

	/**
	 * Dependencies between class elements in a simulation model can be viewed as a directed acyclic
	 * graph (DAG), and the strategy applied here is a depth-first search (DFS), finding a final class
	 * element by following a path from an initial class element until it is not possible to extend
	 * the path. The DFS traverse the class elements, and stores them as an ordered set of elements in
	 * execution order. The ordered execution chain can be obtained from {@link #getClassExecOrder()}.
	 * Duplicate start elements in input are rejected without notice. There are no restrictions on the
	 * type of class elements that may participate in the ordering. This is controlled from the
	 * outside by the <code>modelTypes</code> parameter. To succeed, the algorithm presumes a DAG, and
	 * if cycles are detected, they are reported, and the execution order is undefined.
	 * <p>
	 * Super classes of model elements are not added to the execution chain. They are an inherent part
	 * of their sub types and the containing classes in a super type is part of the dependency graph
	 * for which the sub type is dependent on.
	 * 
	 * @param child the independent element declared or referenced in parent
	 * @param parent the dependent element containing declared and referenced children
	 * @param instructionType a common super class for all class types included in the ordering
	 * @param visited is a list of class elements in the current call stack (used for detecting
	 * cycles)
	 * @throws ExtenderException If the service id is null for the registered service
	 */
	private <A extends Annotation> void visitElement(Class<?> child, Class<?> parent,
			EnumSet<ModelElement.Type> modelTypes, List<Class<?>> visited) throws ExtenderException {
		// Has this start class element been visited before (not through recursion)
		if (!classExecOrder.contains(child)) {
			// If this class element has been visited before during this nested
			// sequence of recursive calls, it is a cycle
			if (visited.contains(child)) {
				logCircularReference(child, parent);
				return;
			}
			visited.add(child);
			// Get direct domain classes that child depends on
			Collection<Class<?>> containingElements = new LinkedHashSet<>();
			getDirectInjectedClasses(child, containingElements);
			if (containingElements.size() > 0) {
				for (Class<?> containingElement : containingElements) {
					// Is this an element of a type to be added to the execution chain
					ModelElement annotation = containingElement.getAnnotation(ModelElement.class);
					if (null != annotation && modelTypes.contains(annotation.type())) {
						// Traverse one step further down the tree of injections
						visitElement(containingElement, child, modelTypes, visited);
					}
				}
			}
			// Do not add super class elements to the execution chain
			// When parent is null, child is a start input class having no parent and is not a super class
			if (parent == null || !child.isAssignableFrom(parent)) {
				// Add this class element at the last (correct) position in the execution chain
				classExecOrder.add(child);
			}
		}
	}

	/**
	 * Get all model elements that are injected into the specified requiring model element and its
	 * super model elements. The result is in the specified collection of providing (injected)
	 * elements. Model elements in sub classes are injected before model elements in super classes
	 * <p>
	 * Note that the content of the specified collection of providing elements are modified.
	 * <p>
	 * If a providing element to inject is not registered as a service the providing element is not
	 * added to the result set and a warning message is sent to the bundle log
	 * 
	 * @param requiringElement Model element with injected elements
	 * @param providingElements all model elements that are injected into the specified requiring
	 * element and its super elements
	 * @throws ExtenderException If the service id is null for the registered service
	 */
	private void getDirectInjectedClasses(Class<?> requiringElement,
			Collection<Class<?>> providingElements) throws ExtenderException {

		if (null == requiringElement) {
			return;
		} else {
			getDirectInjectedClasses(requiringElement.getSuperclass(), providingElements);
			providingElements.addAll(AnnotationUtil.getInjectFieldServices(requiringElement));
			providingElements.addAll(AnnotationUtil.getInjectMethodParameterServices(requiringElement,
					AnnotationUtil.implicitInject));
		}
	}

	/**
	 * Get all extenders with its service annotated with {@link Model}
	 * 
	 * @return extenders annotated with {@link Model}
	 */
	public static Collection<Extender<?>> getTrackedModels() {

		Collection<Extender<?>> extenderElements = new LinkedHashSet<>();

		ExtenderTracker tracker = Activator.getExtenderTracker();
		Collection<Extender<?>> extenders = tracker.getTrackedExtenders();
		for (Extender<?> extender : extenders) {
			Class<?> serviceClass = extender.getServiceClass();
			Model model = serviceClass.getAnnotation(Model.class);
			if (null != model) {
				extenderElements.add(extender);
			}
		}
		return extenderElements;
	}

	/**
	 * Get all classes annotated with {@link ModelElement} of the specified model types
	 * 
	 * @param modelTypes a set of model types
	 * @return classes annotated with one of the types in the specified set of model types or an empty
	 * collection
	 */
	public static Collection<Class<?>> getTrackedModelElementTypes(
			EnumSet<ModelElement.Type> modelTypes) {

		Collection<Class<?>> classElements = new LinkedHashSet<>();

		ExtenderTracker tracker = Activator.getExtenderTracker();
		Collection<Extender<?>> extenders = tracker.getTrackedExtenders();
		for (Extender<?> extender : extenders) {
			Class<?> serviceClass = extender.getServiceClass();
			ModelElement modelElement = serviceClass.getAnnotation(ModelElement.class);
			if (null != modelElement && modelTypes.contains(modelElement.type())) {
				classElements.add(serviceClass);
			}
		}
		return classElements;
	}

	/**
	 * Get all registered and tracked model element services (extenders) annotated with
	 * <code>@ModelElement</code> in the specified bundle
	 * <p>
	 * Not tracked extenders (e.g. the Events model element) are not included. Not tracked model
	 * elements are system services
	 * @param bundle Only model elements from this bundle
	 * 
	 * @return An unordered collection of all registered and tracked model element services
	 */
	public static Collection<Extender<?>> getTrackedModelElements(Bundle bundle) {

		Collection<Extender<?>> extenderElements = new LinkedHashSet<>();
		if (null != bundle) {
			ExtenderTracker tracker = Activator.getExtenderTracker();
			Collection<Extender<?>> extenders = tracker.getTrackedExtenders();
			for (Extender<?> extender : extenders) {
				Class<?> serviceClass = extender.getServiceClass();
				ModelElement modelElement = serviceClass.getAnnotation(ModelElement.class);
				if (null != modelElement && bundle.equals(extender.getOwner())) {
					extenderElements.add(extender);
				}
			}
		} else {
			Bundle thisBundle = FrameworkUtil.getBundle(ClassModelElementSorter.class);
			BundleLog bundleLog = Extenders.getService(BundleLog.class, thisBundle);
			bundleLog.log(StatusCode.WARNING, thisBundle, null, "Missing bundle when sorting model");
		}
		return extenderElements;
	}

	/**
	 * Get all extenders from the specified collection of service classes.
	 * <p>
	 * If a specified service class is not registered as a service it is ignored and a warning is sent
	 * to the bundle log
	 * 
	 * @param serviceClasses A collection of service classes registered as extenders
	 * @return A collection of extenders for the specified service classes or an empty collection
	 * @throws ExtenderException If the service id is null for the registered service
	 */
	public static Collection<Extender<?>> getModelElementExtenders(
			Collection<Class<?>> serviceClasses) throws ExtenderException {

		Collection<Extender<?>> extenders = new LinkedList<>();

		Extender<?> extender = null;
		for (Class<?> serviceClass : serviceClasses) {
			if (serviceClass.isInterface()) {
				extender = Extenders.getExtender(serviceClass.getName());
			} else {
				String serviceInterfaceClassName = null;
				Class<?> serviceInterfaceClass = AnnotationUtil.getAnnotatedInterface(serviceClass);
				if (null == serviceInterfaceClass) {
					serviceInterfaceClassName = serviceClass.getName();
				} else {
					serviceInterfaceClassName = serviceInterfaceClass.getName();
				}
				extender = Extenders.getExtender(serviceInterfaceClassName);
			}
			if (null != extender) {
				extenders.add(extender);
			} else {
				BundleLog bundleLog = Extenders.getService(BundleLog.class);
				bundleLog.log(StatusCode.WARNING, Activator.getContext().getBundle(), null,
						serviceClass.getName() + " model element has not been registered as a service");
			}
		}
		return extenders;
	}

	/**
	 * Return whether cycles among model elements are ignored
	 * 
	 * @return true if cycles are allowed and false otherwise.
	 * @see ModelElement
	 * @see #setAllowCycles(boolean)
	 */
	public boolean isAllowCycles() {
		return cycles;
	}

	/**
	 * If cycles are allowed no warnings are sent to the log. Otherwise cycles are sent to the bundle
	 * log. The model is sorted independent of this setting
	 * 
	 * @param cycles true if cycles are allowed and false if not
	 * @see ModelElement
	 * @see #isAllowCycles()
	 */
	public void setAllowCycles(boolean allowCycles) {
		this.cycles = allowCycles;
	}

	/**
	 * Construct a log message about a circular reference between the specified model elements
	 * <p>
	 * No log message is sent to the bundle log if {@link #isAllowCycles() cycles} are allowed
	 * 
	 * @param child Model element referencing the specified parent model element
	 * @param parent Model element referencing the specified child model element
	 */
	private void logCircularReference(Class<?> child, Class<?> parent) {

		if (!cycles) {
			BundleLog bundleLog = Extenders.getService(BundleLog.class);
			String directRecursion = parent == child ? " (self reference)" : "";
			String ofType = " of type ";
			ModelElement annotation = child.getAnnotation(ModelElement.class);
			String childAnnotationName = null != annotation ? ofType + annotation.type().name() : "";
			annotation = parent.getAnnotation(ModelElement.class);
			String parentAnnotationName = null != annotation ? ofType + annotation.type().name() : "";
			StatusCode statusCode = parent == child ? StatusCode.INFO : StatusCode.WARNING;
			bundleLog.add(statusCode, Activator.getContext().getBundle(), null,
					" Circular reference between model element " + parent.getName() + childAnnotationName
							+ " and " + child.getName() + parentAnnotationName + directRecursion);
			if (parent == child) {
				bundleLog.addToParent(StatusCode.INFO, Activator.getContext().getBundle(), null,
						"A self refence is unambigous but superfluous, referencing the same instance (the singleton: "
								+ parent.getSimpleName() + ")");
			} else {
				bundleLog.addToParent(StatusCode.INFO, Activator.getContext().getBundle(), null,
						"The execution order between instances of circular referenced model elements is undefined");
				bundleLog.addToParent(StatusCode.WARNING, Activator.getContext().getBundle(), null,
						"In this run " + parent.getSimpleName() + " is executed before "
								+ child.getSimpleName());
				bundleLog.addToParent(StatusCode.INFO, Activator.getContext().getBundle(), null,
						"Note that a circular dependecy between different model elements may be direct or indirect (transitive)");
			}
			bundleLog.log();
		}
	}

	/**
	 * Log model elements
	 * 
	 * @param modelElements Model elements to log
	 */
	public void logSortedModelElements(Collection<Class<?>> modelElements) {

		BundleLog bundleLog = Extenders.getService(BundleLog.class);
		bundleLog.add(StatusCode.INFO, Activator.getContext().getBundle(), null,
				"Sorted Model Element Classes in execution order");
		for (Class<?> clazz : modelElements) {
			Bundle bundle = FrameworkUtil.getBundle(clazz);
			ModelElement annotation = clazz.getAnnotation(ModelElement.class);
			bundleLog.addToRoot(StatusCode.INFO, bundle, null,
					annotation.type() + ": " + clazz.getName());
		}
		bundleLog.log();
	}
}
