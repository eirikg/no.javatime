/*
 * generated by Xtext 2.9.1
 */
package no.javatime.lang.ui;

import com.google.inject.Injector;
import no.javatime.lang.ui.internal.LangActivator;
import org.eclipse.xtext.ui.guice.AbstractGuiceAwareExecutableExtensionFactory;
import org.osgi.framework.Bundle;

/**
 * This class was generated. Customizations should only happen in a newly
 * introduced subclass. 
 */
public class ModelExecutableExtensionFactory extends AbstractGuiceAwareExecutableExtensionFactory {

	@Override
	protected Bundle getBundle() {
		return LangActivator.getInstance().getBundle();
	}
	
	@Override
	protected Injector getInjector() {
		return LangActivator.getInstance().getInjector(LangActivator.NO_JAVATIME_LANG_MODEL);
	}
	
}
