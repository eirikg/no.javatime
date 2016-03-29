package no.javatime.core.runtime.simulator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import no.javatime.core.model.annotations.Action;
import no.javatime.core.model.annotations.ModelElement;
import no.javatime.core.model.annotations.SetSeriesValue;
import no.javatime.core.model.annotations.Start;
import no.javatime.core.model.annotations.StartValue;
import no.javatime.core.model.annotations.Stop;
import no.javatime.core.runtime.Activator;
import no.javatime.core.runtime.util.AnnotationUtil;
import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.extender.intface.Extenders;
import no.javatime.inplace.log.intface.BundleLog;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;

public class MethodModelElementSorter {

	private LinkedHashMap<Extender<?>, LinkedHashSet<MethodModelElement>> initialMethodExecMapChain = new LinkedHashMap<>();
	private LinkedHashMap<Extender<?>, LinkedHashSet<MethodModelElement>> runMethodExecMapChain = new LinkedHashMap<>();

	public LinkedHashMap<Extender<?>, LinkedHashSet<MethodModelElement>> getInitialModelElements() {
		return initialMethodExecMapChain;
	}

	public LinkedHashSet<MethodModelElement> getInitalMethodOrdering(Extender<?> modelElement) {
		return initialMethodExecMapChain.get(modelElement);
	}

	public LinkedHashMap<Extender<?>, LinkedHashSet<MethodModelElement>> sortInitalMethods(
			Collection<Extender<?>> extenders) throws ExtenderException {

		for (Extender<?> extenderElement : extenders) {
			LinkedHashSet<MethodModelElement> methodExecChain = new LinkedHashSet<MethodModelElement>();
			Class<?> serviceClass = extenderElement.getServiceClass();
			Object object = extenderElement.getService();
			ModelElement annotation = serviceClass.getAnnotation(ModelElement.class);
			if (null != annotation) {
				MethodModelElement methodModelElement = null;
				// All model element types may receive the start annotation
				methodModelElement = addAnnotedInjectMethod(serviceClass, object, Start.class);
				if (null != methodModelElement) {
					methodExecChain.add(methodModelElement);
				}
				switch (annotation.type()) {
				case EXOGENOUS:
					break;
				case ENDOGENOUS:
				case AUXILIARY:
					break;
				case TRANSITION:
				case RATE:
				case DERIVATIVE:
				case STATE:
				case LEVEL:
				case INTEGRAL:
					methodModelElement = addAnnotedInjectMethod(serviceClass, object, StartValue.class);
					if (null != methodModelElement) {
						methodExecChain.add(methodModelElement);
					}
					methodModelElement = addAnnotedSetMethod(serviceClass, object, SetSeriesValue.class);
					if (null != methodModelElement) {
						methodExecChain.add(methodModelElement);
					}
					break;
				default:
					break;
				}
			}
			if (methodExecChain.size() > 0) {
				initialMethodExecMapChain.put(extenderElement, methodExecChain);
			}
		}
		return initialMethodExecMapChain;
	}

	public void logExecInitChain() {
		BundleLog bundleLog = Extenders.getService(BundleLog.class);
		bundleLog.add(StatusCode.INFO, Activator.getContext().getBundle(), null,
				"Simulation execution order: Step 0");
		for (Map.Entry<Extender<?>, LinkedHashSet<MethodModelElement>> entry : initialMethodExecMapChain
				.entrySet()) {
			Extender<?> sortedExtender = entry.getKey();
			bundleLog.addToRoot(StatusCode.INFO, sortedExtender.getOwner(), null,
							sortedExtender.getServiceClass().getName());
			LinkedHashSet<MethodModelElement> methodElements = entry.getValue();
			for (MethodModelElement methodElement : methodElements) {
				bundleLog.addToParent(StatusCode.INFO, sortedExtender.getOwner(), null,
						methodElement.method.getDeclaringClass().getSimpleName() + "."
								+ methodElement.method.getName());
			}
		}
		bundleLog.log();
	}

	public LinkedHashMap<Extender<?>, LinkedHashSet<MethodModelElement>> getRunModelElements() {
		return runMethodExecMapChain;
	}

	public LinkedHashSet<MethodModelElement> getRunMethodOrdering(Extender<?> modelElement) {
		return runMethodExecMapChain.get(modelElement);
	}

	public LinkedHashMap<Extender<?>, LinkedHashSet<MethodModelElement>> sortRunMethods(
			Collection<Extender<?>> extenders) throws ExtenderException {

		for (Extender<?> extenderElement : extenders) {

			LinkedHashSet<MethodModelElement> methodExecChain = new LinkedHashSet<MethodModelElement>();
			Class<?> serviceClass = extenderElement.getServiceClass();
			Object object = extenderElement.getService();
			ModelElement annotation = serviceClass.getAnnotation(ModelElement.class);
			if (null != annotation) {
				MethodModelElement methodModelElement = null;
				switch (annotation.type()) {
				case EXOGENOUS:
					// No method injection in exogenous yet
					break;
				case ENDOGENOUS:
				case AUXILIARY:
					break;
				case TRANSITION:
				case RATE:
				case DERIVATIVE:
				case STATE:
				case LEVEL:
				case INTEGRAL:
					methodModelElement = addAnnotedInjectMethod(serviceClass, object, Action.class);
					if (null != methodModelElement) {
						methodExecChain.add(methodModelElement);
					}
					methodModelElement = addAnnotedSetMethod(serviceClass, object, SetSeriesValue.class);
					if (null != methodModelElement) {
						methodExecChain.add(methodModelElement);
					}
					break;
				default:
					break;
				}
				// All model element types may receive the stop annotation
				methodModelElement = addAnnotedInjectMethod(serviceClass, object, Stop.class);
				if (null != methodModelElement) {
					methodExecChain.add(methodModelElement);
				}
			}
			if (methodExecChain.size() > 0) {
				runMethodExecMapChain.put(extenderElement, methodExecChain);
			}
		}
		return runMethodExecMapChain;
	}

	public void logExecRunChain() {
		BundleLog bundleLog = Extenders.getService(BundleLog.class);
		bundleLog.add(StatusCode.INFO, Activator.getContext().getBundle(), null,
				"Simulation execution order: Stepping");
		for (Map.Entry<Extender<?>, LinkedHashSet<MethodModelElement>> entry : runMethodExecMapChain
				.entrySet()) {
			Extender<?> sortedExtender = entry.getKey();
			bundleLog.addToRoot(StatusCode.INFO, sortedExtender.getOwner(), null,
					sortedExtender.getServiceClass().getName());
			LinkedHashSet<MethodModelElement> methodElements = entry.getValue();
			for (MethodModelElement methodElement : methodElements) {
				bundleLog.addToParent(StatusCode.INFO, sortedExtender.getOwner(), null,
						methodElement.method.getDeclaringClass().getSimpleName() + "."
								+ methodElement.method.getName());
			}
		}
		bundleLog.log();
	}

	private <A extends Annotation> MethodModelElement addAnnotedInjectMethod(Class<?> serviceClass,
			Object object, Class<A> annotationClass) {

		MethodModelElement methodElement = null;
		Method method = AnnotationUtil.getAnnotatedMethodModelElement(serviceClass, annotationClass);
		if (null != method) {
			methodElement = injectMethodParameters(method, serviceClass, object);
			methodElement.setAnnotationClass(annotationClass);
		}
		return methodElement;
	}

	private <A extends Annotation> MethodModelElement addAnnotedSetMethod(Class<?> serviceClass,
			Object object, Class<A> annotationClass) {

		MethodModelElement methodElement = null;
		// Default value
		Object[] parameterValue = { 0d };

		Method method = AnnotationUtil.getAnnotatedMethodModelElement(serviceClass, annotationClass);
		if (null != method) {
			Class<?>[] parameterTypes = method.getParameterTypes();
			for (int i = 0; i < parameterTypes.length; i++) {
				Class<?> parameterType = parameterTypes[i];
				if (parameterType.isAssignableFrom(Double.class)) {
					parameterValue[0] = 0d;
				} else if (parameterType.isAssignableFrom(Boolean.class)) {
					parameterValue[0] = false;
				}
			}
			methodElement = new MethodModelElement(method, object, parameterValue);
			methodElement.setAnnotationClass(annotationClass);
		}
		return methodElement;
	}

	// private <A extends Annotation> MethodModelElement addAnnotedSetBooleanMethod(Class<?>
	// serviceClass,
	// Object object, Class<A> annotationClass) {
	//
	// MethodModelElement methodElement = null;
	//
	// Method method = AnnotationUtil.getAnnotatedMethodModelElement(serviceClass, annotationClass);
	// if (null != method) {
	// // Default value
	// Object[] parameterValue = {false};
	// methodElement = new MethodModelElement(method, object, parameterValue);
	// methodElement.setAnnotationClass(annotationClass);
	// }
	// return methodElement;
	// }

	private MethodModelElement injectMethodParameters(Method method, Class<?> serviceClass,
			Object object) throws ExtenderException {

		Object[] parameterValues = AnnotationUtil.injectMethodParameters(method, serviceClass);
		return new MethodModelElement(method, object, parameterValues);
	}
}
