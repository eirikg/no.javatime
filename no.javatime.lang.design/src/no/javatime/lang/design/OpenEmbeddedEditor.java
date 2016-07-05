package no.javatime.lang.design;

import org.obeonetwork.dsl.viewpoint.xtext.support.action.OpenXtextEmbeddedEditor;

import com.google.inject.Injector;

import no.javatime.lang.ui.internal.LangActivator;

public class OpenEmbeddedEditor extends OpenXtextEmbeddedEditor {

	@Override
	protected Injector getInjector() {
		 return  LangActivator.getInstance().getInjector("no.javatime.lang.Model");
	}

}
