package no.javatime.pl.simulator.intface;

import java.util.List;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainer;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.model.application.ui.menu.MDirectMenuItem;
import org.eclipse.e4.ui.model.application.ui.menu.MMenu;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuFactory;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuItem;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;

import no.javatime.pl.simulator.Activator;
import no.javatime.pl.simulator.views.SimulatorView;

public class SimulatorStarter { 
	
	/**
	 * Manifest header for accessing the default service implementation class name of the SimulatorStarter view
	 */
	public final static String SIMULATOR_STARTER_MODEL_SERVICE = "Simulator-Starter-Model-Service";

	private MPart mPart = null;

	public void updateView() {

		SimulatorView view = getView(); 
		if (null != view) {
			Activator.getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					System.out.println("Updating view");
					view.updateSimulatorView(mPart);
				}
			});
		}
	}

	public void createView() {

		if (null == mPart) {
			// This will find the first encountered
			mPart = findPart();
			if (null == mPart) {
				IEclipseContext eclipseContext = Activator.getServiceContext();		
				final IEclipseContext appContext = eclipseContext .getActiveChild();		 
				final EModelService modelService = appContext.get(EModelService.class);
				MApplication application = eclipseContext.get(MApplication.class);
				
				mPart = modelService.createModelElement(MPart.class);
				mPart.setLabel("SimulatorStarter");
				mPart.setElementId(SimulatorView.VIEW_ID);
				mPart.setContributionURI("bundleclass://no.javatime.pl.simulator/"
						+ SimulatorView.VIEW_ID);
				mPart.setCloseable(true);

				List<MPartStack> stacks = modelService.findElements(application, null,
            MPartStack.class, null);
//				for (MPartStack stack : stacks) {
//					System.out.println(stack.toString());
//				}
				stacks.get(0).getChildren().add(mPart);
				
			}
		}
	}

	public void showView() {

		IEclipseContext eclipseContext = Activator.getServiceContext();		
		final EPartService partService = eclipseContext.get(EPartService.class);
//		if (null == mPart) {
			// testCreateMenu();
			// createNewView();
//		}
		if (null == getView()) {
			createView();
		} else {
			updateView();
		}

		Activator.getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				mPart.getTags().add(EPartService.REMOVE_ON_HIDE_TAG);				
				mPart.setIconURI("platform:/plugin/no.javatime.pl.simulator/icons/sample.gif");
				mPart = partService.showPart(mPart, PartState.VISIBLE);
			}
		});
	}
	
	public SimulatorView getView() {

		if (null != mPart) {
			Object object = mPart.getObject();
			if (object instanceof SimulatorView)
				return (SimulatorView) object;
		}
		return null;
	}

	public void hideView() {

		IEclipseContext eclipseContext = Activator.getServiceContext();		
		final EPartService partService = eclipseContext.get(EPartService.class);

		Activator.getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				if (null != partService && null != mPart) {
					partService.hidePart(mPart, true);
				}
			}
		});
	}	
	

	public MPart findPart() {

		IEclipseContext eclipseContext = Activator.getServiceContext();		
		final EPartService partService = eclipseContext.get(EPartService.class);
		return partService.findPart(SimulatorView.VIEW_ID);
	}
	
	// Testing creating new view. Works as expected
	public void createNewView() {
		final MPart mPart;
		IEclipseContext eclipseContext = Activator.getServiceContext();		
		final IEclipseContext appContext = eclipseContext .getActiveChild();		 
		final EModelService modelService = appContext.get(EModelService.class);
		final EPartService partService = eclipseContext.get(EPartService.class);
		MApplication app = appContext.get(MApplication.class);
		final MWindow window = modelService.createModelElement(MWindow.class);
		window.setContributorURI("org.eclipse.e4.legacy.ide.application");
		window.setLabel("MyWindow");
		MMenu menu = MMenuFactory.INSTANCE.createMenu();
		window.setMainMenu(menu);

		menu.setLabel("menuLabel");
		mPart = modelService.createModelElement(MPart.class);
		mPart.setLabel("SimulatorStarter");
		mPart.setElementId(SimulatorView.VIEW_ID);
		mPart.setContributionURI("bundleclass://no.javatime.pl.simulator/" + SimulatorView.VIEW_ID);
		mPart.setCloseable(true);
		window.getChildren().add(mPart);
		
		Activator.getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				// mPart.getTags().add(EPartService.REMOVE_ON_HIDE_TAG);				
				//partService.showPart(mPart, PartState.VISIBLE);
				app.getChildren().add(window);			
			}
		});
	}

	public void testCreateMenu() {
		
		final MWindow window = createWindowWithOneViewAndMenu();
		// IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		 
		IEclipseContext eclipseContext = Activator.getServiceContext();		
		final IEclipseContext appContext = eclipseContext .getActiveChild();		 
		MApplication application = appContext.get(MApplication.class);
	  
			for (MWindow windowElement : application.getChildren()) {
        	System.out.println(windowElement.toString());
        }

		Activator.getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				application.getChildren().add(window);			
			}
		});
	}

	private MPart getContributedPart(MWindow window) {

		MPartSashContainer psc = (MPartSashContainer) window.getChildren().get(
				0);
		MPartStack stack = (MPartStack) psc.getChildren().get(0);
		MPart part = (MPart) stack.getChildren().get(0);
		return part;
	}

	private MWindow createWindowWithOneViewAndMenu() {
		
		IEclipseContext eclipseContext = Activator.getServiceContext();		
		final IEclipseContext appContext = eclipseContext .getActiveChild();		 
		final EModelService ems = appContext.get(EModelService.class);

		final MWindow window = createWindowWithOneView();
		final MMenu menuBar = ems.createModelElement(MMenu.class);
		window.setMainMenu(menuBar);
		final MMenu fileMenu = ems.createModelElement(MMenu.class);
		fileMenu.setLabel("File");
		fileMenu.setElementId("file");
		menuBar.getChildren().add(fileMenu);
		final MMenuItem item1 = ems.createModelElement(MDirectMenuItem.class);
		item1.setElementId("item1");
		item1.setLabel("item1");
		fileMenu.getChildren().add(item1);
		final MMenuItem item2 = ems.createModelElement(MDirectMenuItem.class);
		item2.setElementId("item2");
		item2.setLabel("item2");
		fileMenu.getChildren().add(item2);

		return window;
	}
	
	private MWindow createWindowWithOneView() {

		IEclipseContext eclipseContext = Activator.getServiceContext();		
		final IEclipseContext appContext = eclipseContext .getActiveChild();		 
		final EModelService ems = appContext.get(EModelService.class);

		final MWindow window = ems.createModelElement(MWindow.class);
		window.setHeight(300);
		window.setWidth(400);
		window.setLabel("MyWindow");
		MPartSashContainer sash = ems.createModelElement(MPartSashContainer.class);
		window.getChildren().add(sash);
		MPartStack stack = ems.createModelElement(MPartStack.class);
		sash.getChildren().add(stack);
		MPart contributedPart = ems.createModelElement(MPart.class);
		stack.getChildren().add(contributedPart);
		contributedPart.setLabel("Sample View");
		contributedPart
				.setContributionURI("bundleclass://no.javatime.pl.simulator/"
						+ SimulatorView.VIEW_ID);

		return window;
	}
}
