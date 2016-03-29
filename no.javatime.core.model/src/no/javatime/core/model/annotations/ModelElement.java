/**
 * 
 */
package no.javatime.core.model.annotations;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Documented
public @interface ModelElement {
	
	public enum Type { ELEMENT, ENDOGENOUS, EXOGENOUS, INPUT, STATE, LEVEL, INTEGRAL, TRANSITION, RATE, DERIVATIVE, AUXILIARY, CONSTANT, SYSTEM }
	
	public enum Scope {SINGLETON, BUNDLE, PROTOTYPE}
	
	public Type type() default Type.ENDOGENOUS;
	
	public String key() default "model.element";
	
	public Scope scope() default Scope.SINGLETON;
	
	public boolean isActive() default true;
}
