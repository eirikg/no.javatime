/*
 * generated by Xtext 2.9.1
 */
package no.javatime.lang.ui.tests;

import com.google.inject.Injector;
import no.javatime.lang.ui.internal.LangActivator;
import org.eclipse.xtext.junit4.IInjectorProvider;

public class ModelUiInjectorProvider implements IInjectorProvider {

	@Override
	public Injector getInjector() {
		return LangActivator.getInstance().getInjector("no.javatime.lang.Model");
	}

}
