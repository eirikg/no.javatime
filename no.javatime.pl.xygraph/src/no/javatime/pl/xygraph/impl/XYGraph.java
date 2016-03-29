package no.javatime.pl.xygraph.impl;

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

import no.javatime.core.model.annotations.ModelElement;
import no.javatime.core.model.annotations.Stop;
import no.javatime.core.model.annotations.ModelElement.Type;
import no.javatime.core.model.elements.Events;
import no.javatime.core.model.elements.TimeSeriesProvider;
import no.javatime.pl.xygraph.Activator;
import no.javatime.pl.xygraph.intface.IXYGraph;
import no.javatime.pl.xygraph.views.XYGraphView;

@ModelElement (type = Type.SYSTEM)
public class XYGraph implements IXYGraph { 
	
	private MPart mPart = null;

	@Override
	@Stop
	public Boolean showView(Events time, TimeSeriesProvider provider) {

		if (time.isStop()) {
			if (null == getView()) {
				showView();
			}
			updateGraph(provider);
			return true;
		}
		return false;
	}

	@Override
	public void updateGraph(TimeSeriesProvider provider) {

		XYGraphView view = getView(); 
		if (null != view) {
			Activator.getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					System.out.println("Updating view");
					view.updateXYGraph(provider);
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
				mPart.setLabel("XY Graph");
				mPart.setElementId(XYGraphView.VIEW_ID);
				mPart.setContributionURI("bundleclass://no.javatime.pl.xygraph/"
						+ XYGraphView.VIEW_ID);
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

	@Override
	public void showView() {

		IEclipseContext eclipseContext = Activator.getServiceContext();		
		final EPartService partService = eclipseContext.get(EPartService.class);
//		if (null == mPart) {
//			testCreateMenu();
//		}
		createView();
		Activator.getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				mPart.getTags().add(EPartService.REMOVE_ON_HIDE_TAG);				
				mPart.setIconURI("platform:/plugin/no.javatime.pl.xygraph/icons/sample.gif");
				mPart = partService.showPart(mPart, PartState.VISIBLE);
			}
		});
	}
	
	public XYGraphView getView() {

		if (null != mPart) {
			Object object = mPart.getObject();
			if (object instanceof XYGraphView)
				return (XYGraphView) object;
		}
		return null;
	}

	@Override
	public void hideView() {

		IEclipseContext eclipseContext = Activator.getServiceContext();		
		final EPartService partService = eclipseContext.get(EPartService.class);

		Activator.getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				if (null != mPart) {
					partService.hidePart(mPart, true);
				}
			}
		});
	}	
	

	@Override
	public MPart findPart() {

		IEclipseContext eclipseContext = Activator.getServiceContext();		
		final EPartService partService = eclipseContext.get(EPartService.class);
		return partService.findPart(XYGraphView.VIEW_ID);
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
		window.setLabel("MyWindow");
		MMenu menu = MMenuFactory.INSTANCE.createMenu();
		window.setMainMenu(menu);

		menu.setLabel("menuLabel");
		mPart = modelService.createModelElement(MPart.class);
		mPart.setLabel("XY Graph");
		mPart.setElementId(XYGraphView.VIEW_ID);
		mPart.setContributionURI("bundleclass://no.javatime.pl.xygraph/"
				+ XYGraphView.VIEW_ID);
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
				.setContributionURI("bundleclass://no.javatime.pl.xygraph/"
						+ XYGraphView.VIEW_ID);

		return window;
	}
}
