package no.javatime.pl.simulator.views;

import java.util.Collection;
import java.util.List;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.di.AboutToShow;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.model.application.ui.menu.MDirectMenuItem;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuElement;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import no.javatime.core.runtime.simulator.Simulator;
import no.javatime.inplace.extender.intface.Extenders;
import no.javatime.inplace.log.intface.BundleLog;
import no.javatime.inplace.region.status.IBundleStatus.StatusCode;

/**
 * Handler for running simulation models and dynamic menu contributors each containing a model to
 * run
 *
 */
public class SimulatorMenuHandler {

	private static Bundle plugin = FrameworkUtil.getBundle(SimulatorMenuHandler.class);

	public static String handlerContributionUri = "bundleclass://" + plugin.getSymbolicName() + "/"
			+ SimulatorMenuHandler.class.getName();

	/**
	 * Add all activated bundle models to the tool bar drop down menu
	 * 
	 * @param menuItems An empty list to fill with menu items
	 * @param modelService Model service to create menu items
	 */
	@AboutToShow
	public void aboutToShow(List<MMenuElement> menuItems, EModelService modelService) {

		Simulator simulator = Extenders.getService(Simulator.class, plugin);
		Collection<Bundle> bundles = simulator.getBundleModels();
		// One dynamic menu contribution applies for all direct menu items
		for (Bundle bundle : bundles) {
			MDirectMenuItem directMenuItem = modelService.createModelElement(MDirectMenuItem.class);
			directMenuItem.setLabel(bundle.getSymbolicName());
			directMenuItem.setTooltip("Simulate the " + bundle.getSymbolicName() + " model");
			directMenuItem.setContributorURI("platform:/plugin/" + plugin.getSymbolicName());
			// Using set object as an alternative to setContributionURI
			// directMenuItem.setContributionURI(handlerContributionUri);
			directMenuItem.setObject(this);
			directMenuItem.setVisible(true);
			directMenuItem.setEnabled(true);
			directMenuItem.setElementId(bundle.getLocation());
			directMenuItem.getTags().add(bundle.getSymbolicName());
			menuItems.add(directMenuItem);
		}
	}

	/**
	 * Run the simulator on the selected menu item
	 * 
	 * @param part not in use
	 * @param window not in use
	 * @param menuItem The selected menu item from the tool bar drop down menu
	 */
	@Execute
	public void execute(@Active MPart part, @Active MWindow window, MDirectMenuItem menuItem) {

		Simulator simulator = Extenders.getService(Simulator.class, plugin);
		// The bundle location is in the element id of the selected menu item
		Bundle bundle = plugin.getBundleContext().getBundle(menuItem.getElementId());
		if (null != bundle) {
			simulator.simulate(bundle);
		} else {
			BundleLog bundleLog = Extenders.getService(BundleLog.class, plugin);
			bundleLog.log(StatusCode.WARNING, bundle, null, "Missing bundle model to simulate");
		}
	}
}
