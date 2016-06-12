package no.javatime.core.runtime.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import com.google.inject.Inject;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import no.javatime.core.model.annotations.Action;
import no.javatime.core.model.annotations.ModelElement;
import no.javatime.core.model.annotations.Start;
import no.javatime.core.model.annotations.StartValue;
import no.javatime.core.model.annotations.Stop;
import no.javatime.core.runtime.Activator;
import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.extender.intface.Extenders;
import no.javatime.inplace.log.intface.BundleLog;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;

public class AnnotationUtil {

	public static final Collection<Class<?>> implicitInject = new ArrayList<>();

	static {
		implicitInject.add(Action.class);
		implicitInject.add(StartValue.class);
		implicitInject.add(Start.class);
		implicitInject.add(Stop.class);
	}

	/**
	 * Injects model element objects for all fields annotated with <code>@Inject</code>
	 * <p>
	 * To be injected the model element must have been registered as a service
	 * 
	 * @param extenders Model element services to inject model element objects in
	 */
	public static void injectModelElements(Collection<Extender<?>> extenders) {
		for (Extender<?> extender : extenders) {
			Class<?> serviceClass = extender.getServiceClass();
			Object serviceObject = extender.getService();
			injectModelElement(serviceClass, serviceObject);
		}
	}


	/**
	 * Injects model element objects into the specified object where fields are annotated with inject
	 * in the specified class
	 * <p>
	 * Fields and methods in super classes are injected before those in subclasses
	 * 
	 * @param clazz The class with annotated fields to inject
	 * @param object An object instance of the specified class
	 */
	public static void injectModelElement(Class<?> clazz, Object object) {

		if (null == clazz) {
			return;
		} else {
			injectModelElement(clazz.getSuperclass(), object);
			injectFields(clazz, object);
		}
	}

	/**
	 * Injects model element objects of type fields into the specified object where fields are
	 * annotated with inject in the specified class
	 * 
	 * @param clazz The class with annotated fields to inject
	 * @param object An object instance of the specified class
	 */
	public static void injectFields(Class<?> clazz, Object object) {

		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			Class<?> type = field.getType();
			Annotation[] annotations = field.getDeclaredAnnotations();
			for (Annotation annotation : annotations) {
				if (annotation instanceof Inject) {
					Object serviceObject = Extenders.getService(type);
					if (null == serviceObject) {
						Class<?> interfaceClass = getServiceInterfaceClass(type);
						if (null != interfaceClass) {
							serviceObject = Extenders.getService(interfaceClass);
						}
					}
					if (null != serviceObject) {
						try {
							final boolean accessible = field.isAccessible();
							field.setAccessible(true);
							field.set(object, serviceObject);
							field.setAccessible(accessible);
						} catch (IllegalArgumentException | IllegalAccessException | ExtenderException e) {
							BundleLog bundleLog = Extenders.getService(BundleLog.class);
							Bundle bundle = FrameworkUtil.getBundle(clazz);
							bundleLog.log(StatusCode.EXCEPTION, bundle, e, e.getMessage());
						}
					} else {
						BundleLog bundleLog = Extenders.getService(BundleLog.class);
						Bundle bundle = FrameworkUtil.getBundle(clazz);
						bundleLog.log(StatusCode.EXCEPTION, bundle, null,
								"Found no registered service for field type " + type.getName() + " to inject");
					}
				}
			}
		}
	}

	/**
	 * Inject service objects into the parameters of the specified method
	 * 
	 * @param method The method with extender service parameters
	 * @param serviceClass Used for logging if errors occurs
	 * @return An array of the injected service parameter objects or null if the method has no
	 * declared parameters
	 * @throws ExtenderException If any of the parameters in the specified is not a registered
	 * extender service
	 */
	public static Object[] injectMethodParameters(Method method, Class<?> serviceClass)
			throws ExtenderException {

		Object[] parameterValues = null;

		Collection<Class<?>> parameterTypes = getInjectMethodParameterServices(method,
				AnnotationUtil.implicitInject);
		if (parameterTypes.size() > 0) {
			parameterValues = new Object[parameterTypes.size()];
			int i = 0;
			for (Class<?> parameterType : parameterTypes) {
				Object serviceObject = Extenders.getService(parameterType);
				if (null == serviceObject) {
					Class<?> interfaceClass = AnnotationUtil.getServiceInterfaceClass(parameterType);
					if (null != interfaceClass) {
						serviceObject = Extenders.getService(interfaceClass);
					}
				}
				if (null != serviceObject) {
					parameterValues[i] = serviceObject;
					i++;
				} else {
					BundleLog bundleLog = Extenders.getService(BundleLog.class);
					Bundle bundle = FrameworkUtil.getBundle(serviceClass);
					bundleLog.log(StatusCode.EXCEPTION, bundle, null,
							"Found no registered service for model element type " + parameterType.getName()
									+ " to inject");
					throw new ExtenderException(
							"Missing service for model element: " + parameterType.getName());
				}
			}
		}
		return parameterValues;
	}

	public static <T extends Annotation> Collection<Class<?>> getInjectMethodParameterServices(
			Class<?> declaringClass, Collection<Class<?>> implicitInjects) {

		Collection<Class<?>> injected = new LinkedHashSet<>();

		Method[] methods = declaringClass.getDeclaredMethods();
		for (Method method : methods) {
			injected.addAll(getInjectMethodParameterServices(method, implicitInjects));
		}

		return injected;
	}

	/**
	 * Find all parameters of the specified method annotated with inject and any of the annotations in
	 * the specified implicit inject parameter and return the service classes representing declared
	 * types of method parameters to inject
	 * <p>
	 * If a service class could not be obtained or does not exist for an annotated method parameter a
	 * warning is sent to the bundle log and the annotation is ignored
	 * <p>
	 * If the same type of a method parameter is injected more than once, all instances of the service
	 * class representing the type of the method parameter is returned
	 * 
	 * @param implicitInject TODO
	 * @param declaringClass Class containing a set of methods to inject
	 * 
	 * @return A collection of unique service classes each representing the type of a method parameter
	 * annotated with inject or an empty collection
	 * @throws ExtenderException If the service representing the type of a field is not registered
	 * (returns null) or is invalid. The cause is specified in the exception
	 */
	public static <T extends Annotation> Collection<Class<?>> getInjectMethodParameterServices(
			Method method, Collection<Class<?>> implicitInjects) {

		Collection<Class<?>> injected = new LinkedHashSet<>();
		Annotation annotation = null;
		annotation = method.getDeclaredAnnotation(Inject.class);
		// Check for implicit annotations
		if (null == annotation) {
			// TODO Works but need a rewrite
			for (Class<?> implicitInject : implicitInjects) {
				annotation = method.getDeclaredAnnotation((Class<T>) implicitInject);
				if (null != annotation) {
					break;
				}
			}
		}
		if (null != annotation) {
//			System.out.println("A: " + annotation.annotationType().getSimpleName() + 
//					" M: " + method.getDeclaringClass().getSimpleName() + "." +  method.getName()  );
			Parameter[] parameters = method.getParameters();
			for (int i = 0; i < parameters.length; i++) {
				Parameter parameter = parameters[i];
				Class<?> type = getServiceClass(parameter.getType());
				if (null != type) {
					injected.add(type);
				}
			}
		}
		return injected;
	}

	/**
	 * Find all methods in the specified declaring class annotated with inject and return the service
	 * classes representing declared types of method parameters to inject
	 * <p>
	 * If a service class could not be obtained or does not exist for an annotated method parameter a
	 * warning is sent to the bundle log and the annotation is ignored
	 * <p>
	 * If the same type of a method parameter is injected more than once, only one instance of the
	 * service class representing the type of the method parameter is returned
	 * 
	 * @param declaringClass Class containing a set of methods to inject
	 * @return A collection of unique service classes each representing the type of a method parameter
	 * annotated with inject or an empty collection
	 * @throws ExtenderException If the service representing the type of a field is not registered
	 * (returns null) or is invalid. The cause is specified in the exception
	 */
	public static Collection<Class<?>> getInjectMethodParameterServices(Class<?> declaringClass)
			throws ExtenderException {

		Collection<Class<?>> injected = new LinkedHashSet<>();

		Method[] methods = declaringClass.getDeclaredMethods();
		for (Method method : methods) {
			Annotation annotation = method.getDeclaredAnnotation(Inject.class);
			if (null != annotation) {
				Parameter[] parameters = method.getParameters();
				for (int i = 0; i < parameters.length; i++) {
					Parameter parameter = parameters[i];
					Class<?> type = getServiceClass(parameter.getType());
					if (null != type) {
						injected.add(type);
					}
				}
			}
		}
		return injected;
	}

	/**
	 * Find all fields in the specified declaring class annotated with inject and return the service
	 * classes representing declared types of fields to inject
	 * <p>
	 * If a service class could not be obtained or does not exist for an annotated field a warning is
	 * sent to the bundle log and the annotation is ignored
	 * <p>
	 * If the same type of a field is injected more than once, only one instance of the service class
	 * representing the type of the field is returned
	 * 
	 * @param declaringClass Class containing a set of fields to inject
	 * @return A collection of unique service classes each representing the type of a declared field
	 * annotated with inject or an empty collection
	 * @throws ExtenderException If the service representing the type of a field is not registered or
	 * is invalid. The cause is specified in the exception
	 */
	public static Collection<Class<?>> getInjectFieldServices(Class<?> declaringClass)
			throws ExtenderException {

		Collection<Class<?>> injected = new LinkedHashSet<>();

		Field[] fields = declaringClass.getDeclaredFields();
		for (Field field : fields) {
			Annotation[] annotations = field.getDeclaredAnnotations();
			for (Annotation annotation : annotations) {
				if (annotation instanceof Inject) {
					Class<?> type = getServiceClass(field.getType());
					if (null != type) {
						injected.add(type);
					}
				}
			}
		}
		return injected;
	}

	/**
	 * Get the service class for the specified class object type. The following variants exist:
	 * <ol>
	 * <li>The given type is a service class
	 * <li>The interface class and the service class is the same
	 * <li>The given type is an interface class
	 * </ol>
	 * <p>
	 * If the specified type is not registered as an extender a warning is sent to the bundle log and
	 * null is returned
	 *
	 * @param type A interface or service class for a registered service
	 * @return The service class of the given type if it exists. Otherwise null is returned and a
	 * message is sent to the bundle log and null is returned
	 */
	public static Class<?> getServiceClass(Class<?> type) {

		Extender<?> extender = null;
		try {
			extender = Extenders.getExtender(type.getName());
		} catch (Exception e) {
		}
		// If type is an interface class return the service class
		if (null != extender) {
			return extender.getServiceClass();
		}
		// If type is a service class, get the service if it exist and return the service class
		Class<?>[] interfaces = type.getInterfaces();
		for (int i = 0; i < interfaces.length; i++) {
			try {
				extender = Extenders.getExtender(interfaces[i].getName());
			} catch (Exception e) {
			}
			if (null != extender) {
				return extender.getServiceClass();
			}
		}
		BundleLog bundleLog = Extenders.getService(BundleLog.class);
		bundleLog.add(StatusCode.WARNING, Activator.getContext().getBundle(), null,
				"Can not find service class " + type.getName() + " for injection");
		bundleLog.log();
		return null;
	}

	/**
	 * Get the interface class for the specified class object type. If any, the preferred interface is
	 * the first interface annotated with {@link ModelElement}. Otherwise the first declared interface
	 * is returned if it exist. If no interface exists the service class is the returned interface
	 * 
	 * The following variants exist:
	 * <ol>
	 * <li>The given type is a service class
	 * <li>The interface class and the service class is the same
	 * <li>The given type is an interface class
	 * </ol>
	 * <p>
	 * If the specified type is not registered as an extender a warning is sent to the bundle log and
	 * null is returned
	 * 
	 * @param type A interface or service class for a registered service
	 * @return The service class of the given type if it exists. Otherwise a message is sent to the
	 * bundle log and null is returned
	 */
	public static Class<?> getServiceInterfaceClass(Class<?> type) {

		Extender<?> extender = null;

		if (!type.isInterface()) {
			Class<?> annotatedInterface = getAnnotatedInterface(type);
			try {
				extender = Extenders.getExtender(annotatedInterface.getName());
			} catch (Exception e) {
			}
			if (null != extender) {
				return extender.getServiceInterfaceClass();
			}
		} else {
			extender = Extenders.getExtender(type.getName());
			if (null != extender) {
				return extender.getServiceInterfaceClass();
			}
		}
		BundleLog bundleLog = Extenders.getService(BundleLog.class);
		bundleLog.add(StatusCode.WARNING, Activator.getContext().getBundle(), null,
				"Can not find interface class " + type.getName() + " for injection");
		bundleLog.log();
		return null;
	}

	/**
	 * Find interface of the specified class. If the specified class implements multiple interfaces,
	 * the first interface annotated with {@link ModelElement} is returned. If no one is annotated the
	 * first declared interface is returned. If the specified class does not implement any interface,
	 * the specified class is assumed to be the interface to be returned.
	 * 
	 * @param implClass Class implementing some interface(s)
	 * @return Interface implemented by the specified class or the class itself of it implements no
	 * interface
	 */
	public static Class<?> getAnnotatedInterface(Class<?> implClass) {

		Class<?>[] interfaces = implClass.getInterfaces();
		for (int i = 0; i < interfaces.length; i++) {
			Class<?> interfaceClass = interfaces[i];
			ModelElement modelElement = interfaceClass.getAnnotation(ModelElement.class);
			if (null != modelElement) {
				return interfaceClass;
			}
		}
		return interfaces.length == 0 ? implClass : interfaces[0];
	}

	/**
	 * Return all interfaces implemented by the specified class. If no interfaces, return the specified
	 * class
	 * 
	 * @param implClass A class implementing interfaces
	 * @return Names of implemented interfaces of the specified class or the class name of the
	 * specified class if no interfaces
	 */
	public static String[] getInterfaces(Class<?> implClass) {

		Class<?>[] interfaces = implClass.getInterfaces();
		if (interfaces.length > 0) {
			List<String> copy = new ArrayList<String>(interfaces.length);
			for (int i = 0; i < interfaces.length; i++) {
				String clazz = interfaces[i].getName();
				if (!copy.contains(clazz)) {
					copy.add(clazz);
				}
			}
			return copy.toArray(new String[copy.size()]);
		} else {
			return new String[] { implClass.getName() };
		}
	}

	/**
	 * Traverse the super-class chain of the specified class to find the first method which is
	 * annotated with the given annotation.
	 * 
	 * @param clazz The class containing zero or more methods annotated with the given annotation
	 * class
	 * @param annotationClass The annotation to find
	 * @return The method annotated with the given annotation class or null if no method found
	 */
	public static <A extends Annotation> Method getAnnotatedMethodModelElement(Class<?> clazz,
			Class<A> annotationClass) {

		Method[] methods = clazz.getMethods();
		for (Method method : methods) {
			Annotation annotation = AnnotationUtil.getInheritedMethodAnnotation(annotationClass, method);
			if (null != annotation) {
				return method;
			}
		}
		return null;
	}

	/**
	 * Traverse the super-class chain to find the first method with the given signature which is
	 * annotated with the given annotation.
	 * 
	 * @param annotationClass The annotation to find
	 * @param annotatedMethod The method possible annotated with the given annotation class
	 * @return
	 */
	public static <A extends Annotation> A getInheritedMethodAnnotation(Class<A> annotationClass,
			AnnotatedElement annotatedMethod) {
		A annotation = annotatedMethod.getAnnotation(annotationClass);
		if (annotation == null && annotatedMethod instanceof Method)
			annotation = getOverriddenMethodAnnotation(annotationClass, (Method) annotatedMethod);
		return annotation;
	}

	private static <A extends Annotation> A getOverriddenMethodAnnotation(Class<A> annotationClass,
			Method method) {
		final Class<?> methodClass = method.getDeclaringClass();
		final String name = method.getName();
		final Class<?>[] params = method.getParameterTypes();

		// prioritize all super classes over all interfaces
		final Class<?> superclass = methodClass.getSuperclass();
		if (superclass != null) {
			final A annotation = getOverriddenMethodAnnotationFrom(annotationClass, superclass, name,
					params);
			if (annotation != null)
				return annotation;
		}

		// depth-first search over interface hierarchy
		for (final Class<?> intf : methodClass.getInterfaces()) {
			final A annotation = getOverriddenMethodAnnotationFrom(annotationClass, intf, name, params);
			if (annotation != null)
				return annotation;
		}

		return null;
	}

	private static <A extends Annotation> A getOverriddenMethodAnnotationFrom(
			Class<A> annotationClass, Class<?> searchClass, String name, Class<?>[] params) {
		try {
			final Method method = searchClass.getMethod(name, params);
			final A annotation = method.getAnnotation(annotationClass);
			if (annotation != null)
				return annotation;
			return getOverriddenMethodAnnotation(annotationClass, method);
		} catch (final NoSuchMethodException e) {
			return null;
		}
	}
}
