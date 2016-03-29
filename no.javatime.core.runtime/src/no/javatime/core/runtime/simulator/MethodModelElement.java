package no.javatime.core.runtime.simulator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import no.javatime.inplace.extender.intface.ExtenderException;

/**
 * Define runtime information about a method to be executed.
 * <p>
 * Compile time information is the method, declaring class of the method, parameter types and
 * possible an annotation. Methods with multiple annotations should be split in one method model
 * element instance for each annotation.
 * <p>
 * The method must be filled with the following runtime information before it can be executed:
 * <ol>
 * <li>The method invoked from the specified object with the specified parameter values
 * <li>The object as defined by the declaring class from which the underlying method is invoked from
 * <li>A parameter value for each method parameter type
 * </ol>
 */
public class MethodModelElement {

	Method method; // Invoked from the defined object with the defined parameter values
	Object object; // The object from which the underlying method is invoked from
	Object[] parameterValue; // Actual parameters
	Class<?> annotationClass; // The annotation this method is annotated with

	/**
	 * Creates a method element model with a defined method, object from which the specified method is invoked and the 
	 * parameter values corresponding to the parameter types of the specified method.
	 * <p>
	 * All parameters may be <code>null</code>, but must be specified before the method is executed
	 *  
	 * @param method A method instance invoked from the specified object with the specified parameter
	 * values
	 * @param object The object from which the underlying method is invoked from
	 * @param parameterValue Actual parameter values
	 */
	public MethodModelElement(Method method, Object object, Object[] parameterValue) {

		this.method = method;
		this.object = object;
		this.parameterValue = parameterValue;
	}

	/**
	 * Get the annotation this method is annotated with
	 * 
	 * @return the annotation this method is annotated with or null if no annotation has been
	 * specified
	 */
	public Class<?> getAnnotationClass() {
		return annotationClass;
	}

	/**
	 * One of possible multiple annotations for the method.
	 * 
	 * @param annotationClass An annotation this method is annotated with. May be null.
	 */
	public void setAnnotationClass(Class<?> annotationClass) {
		this.annotationClass = annotationClass;
	}

	/**
	 * Invokes the underlying method of this method model element
	 * 
	 * It is a prerequisite that the object executing the method, the method itself and its parameter
	 * values are specified for this method model element
	 * <p>
	 * No validation of the definition of the method to execute is performed.
	 * 
	 * @return Return value or null if return value is void
	 * @throws ExtenderException exception bounded to the underlying reflection exceptions. Adds some
	 * limited additional information about the cause in context of the executed method
	 */
	public Object execute() throws ExtenderException {

		Object returnValue = invoke(method, object, parameterValue);
		return returnValue;
	}

	/**
	 * Execute this method model element defined with a <code>Double</code> parameter type. The
	 * parameter value of the defined parameter type is the specified parameter value
	 * <p>
	 * It is a prerequisite that the object executing the method, the method itself and only one
	 * parameter of type <code>Double</code> is defined for this method model element.
	 * <p>
	 * No validation of the definition of the method to execute is performed.
	 * 
	 * @param doubleValue A parameter value of type <code>Double</code>
	 * @return The return value form executing this method model element or null if the executed
	 * method returns void
	 * @throws ExtenderException exception bounded to the underlying reflection exceptions. Adds some
	 * limited additional information about the cause in context of the executed method
	 */
	public Object executeSet(Double doubleValue) throws ExtenderException {
		parameterValue[0] = doubleValue;
		return invoke(method, object, parameterValue);
	}

	/**
	 * Executes an arbitrary class member method given its object and actual parameters
	 * <p>
	 * Requires that that the declaring class and the formal parameters are defined for the method.
	 * See {@link #getMethod(String, Class, Class[])
	 * 
	 * @param T type of class
	 * @param methodName method name to invoke
	 * @param obj the object from which underlying method is invoked from
	 * @param parameterValue the actual parameter values used in the method call
	 * @return the return value of the invoked method or null if the signature of the method return
	 * value is void
	 * @throws ExtenderException exception bounded to the underlying reflection exceptions. Adds
	 * some limited additional information about the cause in context of this method
	 */
	private Object invoke(Method method, Object obj, Object[] parameterValue)
			throws ExtenderException {

		try {
			return method.invoke(obj, parameterValue);
		} catch (IllegalArgumentException e) {
			throw new ExtenderException(e,
					"Encountered an illegal argument while trying to execute method {0}", method.getName());
		} catch (IllegalAccessException e) {
			throw new ExtenderException(e, "Failed to access method {0}", method.getName());
		} catch (InvocationTargetException e) {
			throw new ExtenderException(e, "Failed to execute method: {0}", method.getName());
		} catch (ExceptionInInitializerError e) {
			throw new ExtenderException(e, "Exception in a static initializer provoked by method {0}",
					method.getName());
		} catch (NullPointerException e) {
			throw new ExtenderException(e);
		}
	}

	/**
	 * Executes an arbitrary class member method given its class, object, formal and actual parameters
	 * 
	 * @param T type of class
	 * @param methodName method name to invoke
	 * @param cls class in which the method is the member method to invoke
	 * @param parameterType an array defining the formal parameter types of the method
	 * @param obj the object from which underlying method is invoked from
	 * @param parameterValue the actual parameter values used in the method call
	 * @return the return value of the invoked method or null if the signature of the method return
	 * value is void
	 * @throws ExtenderException exception bounded to the underlying reflection exceptions. Adds
	 * some limited additional information about the cause in context of this method
	 */
	@SuppressWarnings("unused")
	private Object invoke(String methodName, Class<?> cls, Class<?>[] parameterType, Object obj,
			Object[] parameterValue) throws ExtenderException {

		/* The method to invoke */
		Method method = null;

		try {
			method = getMethod(methodName, cls, parameterType);
			return method.invoke(obj, parameterValue);
		} catch (IllegalArgumentException e) {
			throw new ExtenderException(e,
					"Encountered an illegal argument while trying to execute method {0} in Class {1}",
					methodName, cls.getSimpleName());
		} catch (IllegalAccessException e) {
			throw new ExtenderException(e, "Failed to access method {0} in class {1}", methodName,
					cls.getSimpleName());
		} catch (InvocationTargetException e) {
			throw new ExtenderException(e, "Execution failed in Class: {0} and Method: {1}",
					cls.getSimpleName(), methodName);
		} catch (ExceptionInInitializerError e) {
			throw new ExtenderException(e,
					"Exception in a static initializer provoked by method {1} in class {0}",
					cls.getSimpleName(), methodName);
		} catch (NullPointerException e) {
			throw new ExtenderException(e);
		}
	}
	
	/**
	 * Get a method instance based on its name, declaring class and parameter types
	 * 
	 * @param methodName The name of the method declared in the specified class and parameter types
	 * @param declaringClass The class declaring a method with the specified name and parameter types
	 * @param parameterTypes The set of parameter types defined for the methods with the specified name and declaring class
	 * @return The method instance with the specified name and parameter types
	 * @throws ExtenderException If no method with the specified name exist or the name is null
	 */
	private Method getMethod(String methodName, Class<?> declaringClass, Class<?>[] parameterTypes)
			throws ExtenderException {

		try {
			return declaringClass.getMethod(methodName, parameterTypes);
		} catch (SecurityException e) {
			throw new ExtenderException(e, "Security violation while executing method {0} in class {1}",
					methodName, declaringClass.getSimpleName());
		} catch (NoSuchMethodException e) {
			throw new ExtenderException(e, "Method {0} in class {1} could not be found", methodName,
					declaringClass.getSimpleName());
		}
	}
}
