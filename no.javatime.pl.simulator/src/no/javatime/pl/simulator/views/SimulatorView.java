package no.javatime.pl.simulator.views;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.Persist;
import org.eclipse.e4.ui.model.application.ui.MDirtyable;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.menu.MDirectToolItem;
import org.eclipse.e4.ui.model.application.ui.menu.MDynamicMenuContribution;
import org.eclipse.e4.ui.model.application.ui.menu.MMenu;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuFactory;
import org.eclipse.e4.ui.model.application.ui.menu.MToolBar;
import org.eclipse.e4.ui.services.EMenuService;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * View showing and updating the simulator view
 */
public class SimulatorView {

	/** The identifier of this part */
	public final static String VIEW_ID = SimulatorView.class.getName();

	private Bundle plugin = FrameworkUtil.getBundle(SimulatorMenuHandler.class);

	private Text txtInput;
	private TableViewer tableViewer;

	@Inject
	private MDirtyable dirty;

	/**
	 * Do nothing
	 * 
	 * @param mPart this part
	 */
	public void updateSimulatorView(MPart mPart) {

	}

	/**
	 * Create an empty simulator view with a tool bar menu in a containing part
	 * 
	 * @param parent Parent control to for the view
	 */
	@PostConstruct
	public void createSimulatorView(Composite parent, EModelService modelService, MPart mPart,
			EMenuService menuService) {

		// Handler for tool item and menu items
		SimulatorMenuHandler runHandler = new SimulatorMenuHandler();

		// Create a tool item with a drop down menu and place it on the part (1-4)
		// (1) Create a tool bar and a tool item and add the tool item to the tool bar
		MToolBar toolbar = modelService.createModelElement(MToolBar.class);
		MDirectToolItem directToolitem = MMenuFactory.INSTANCE.createDirectToolItem();
		directToolitem.setElementId(SimulatorView.VIEW_ID);
		directToolitem.setIconURI("platform:/plugin/no.javatime.pl.simulator/icons/sample.gif");
		directToolitem.setTooltip("Run last simulated model");
		directToolitem.setContributorURI(
				"platform:/plugin/" + plugin.getSymbolicName());
		// Using set object as an alternative to setContributionURI
		// directToolitem.setContributionURI(SimulatorMenuHandler.handlerContributionUri);
		directToolitem.setObject(runHandler);
		directToolitem.setVisible(true);
		directToolitem.setEnabled(true);
		toolbar.getChildren().add(directToolitem);
		// (2) Create a menu and a dynamic menu item and add the menu item to the menu
		MMenu menu = modelService.createModelElement(MMenu.class);
		// The handler is called once for each created dynamic menu contribution
		MDynamicMenuContribution dynamicMenuContribution = modelService
				.createModelElement(MDynamicMenuContribution.class);
		dynamicMenuContribution.setContributorURI(
				"platform:/plugin/" + plugin.getSymbolicName());
		dynamicMenuContribution.setObject(runHandler);
		dynamicMenuContribution.setVisible(true);
		dynamicMenuContribution.setEnabled(true);
		menu.getChildren().add(dynamicMenuContribution);
		// (3) Add the menu to the tool item
		directToolitem.setMenu(menu);
		// (4) Add the tool bar to the part
		mPart.setToolbar(toolbar);

		// Temporarily
		parent.setLayout(new GridLayout(1, false));

		txtInput = new Text(parent, SWT.BORDER);
		txtInput.setMessage("Enter text to mark part as dirty");
		txtInput.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				dirty.setDirty(true);
			}
		});
		txtInput.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		tableViewer = new TableViewer(parent);
		tableViewer.getTable().setLayoutData(new GridData(GridData.FILL_BOTH));
	}

	@PreDestroy
	public void removeSimulatorView(Composite parent, EModelService service, MPart part,
			EPartService partService) {
		// partService.hidePart(part);
		System.out.println("Hiding view");
	}

	@Focus
	public void setFocus(MPart part) {
		tableViewer.getTable().setFocus();
	}

	@Persist
	public void save() {
		dirty.setDirty(false);
	}

}