package no.javatime.core.model.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use this annotation to signal that a simulation run should stop or continue
 * <p>
 * For a model element, model element methods annotated with the <code>@Stop</code> annotation is
 * called for each simulation step after other model element methods are executed for the model
 * element at that step and once after a simulation run has stopped.
 * <p>
 * The annotation may be used in any {@link ModelElement model element type}. The annotated method
 * may have a variable number of model element parameters and must return <code>Boolean</code> where
 * <code>true</code> signals stop and <code>false</code> signals that the simulation should
 * continue.
 * <p>
 * If a method annotated with <code>@Stop</code> returns <code>true</code> at any step:
 * <ol>
 * <li>The simulation will continue finish executing the
 * {@link no.javatime.core.model.elements.Events#getStep() current step}
 * <li>Remaining model elements annotated with <code>@Stop</code> will not be invoked during the
 * current step
 * <li>End the simulation run and call all model elements annotated with <code>@Stop</code> in
 * execution order - including the method signaling stop - before terminating.
 * </ol>
 * This implies that it will not be possible for other model elements annotated with
 * <code>@Stop</code> to override - resume the simulation - the stop condition.
 * <p>
 * To determine if a stop condition has occurred use
 * {@link no.javatime.core.model.elements.Events#isStop() isStop()}. To stop a simulation run relative to
 * the {@link no.javatime.core.model.elements.Events#getMax() max number of steps} defined at start up of
 * a simulation run see {@link no.javatime.core.model.elements.Events#setStep(Double) setStep(Long)} and
 * {@link no.javatime.core.model.elements.Events#setMax(Double) setMax(Long)}
 * <p>
 * An example is shown below where a method in a Deposit model element is annotated with
 * <code>@Stop</code>, stopping the simulation if the balance exceeds a certain amount. In the
 * sample, Account is a state model element and Limit is an exogenous model element
 * 
 * <blockquote>
 * 
 * <pre>
 * <code>@Stop</code>
 * public Boolean checkBalance(Account account, Limit limit) {
 * 
 * 	return (account.getBalance() >= limit.getLimit()) ? true : false;
 * }
 * </pre>
 * 
 * </blockquote>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Inherited
@Documented
public @interface Stop {

}
