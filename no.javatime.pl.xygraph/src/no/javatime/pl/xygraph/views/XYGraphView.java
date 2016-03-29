package no.javatime.pl.xygraph.views;

import java.text.DecimalFormat;
import java.util.Collection;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.menu.MDirectMenuItem;
import org.eclipse.e4.ui.model.application.ui.menu.MDirectToolItem;
import org.eclipse.e4.ui.model.application.ui.menu.MMenu;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuFactory;
import org.eclipse.e4.ui.model.application.ui.menu.MToolBar;
import org.eclipse.e4.ui.services.EMenuService;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.nebula.visualization.xygraph.dataprovider.CircularBufferDataProvider;
import org.eclipse.nebula.visualization.xygraph.figures.IXYGraph;
import org.eclipse.nebula.visualization.xygraph.figures.ToolbarArmedXYGraph;
import org.eclipse.nebula.visualization.xygraph.figures.Trace;
import org.eclipse.nebula.visualization.xygraph.figures.Trace.PointStyle;
import org.eclipse.nebula.visualization.xygraph.figures.XYGraph;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import no.javatime.core.model.elements.TimeSeriesProvider;
import no.javatime.inplace.extender.intface.Extender;

/**
 * View displaying and updating XY graphs with time series
 */
public class XYGraphView {

	/** The identifier of this part */
	public final static String VIEW_ID = XYGraphView.class.getName();
	
	/* Hosts the graph on a SWT canvas*/
	private LightweightSystem lightweightSystem;

	/**
	 * Create an empty XY graph with a tool bar in a containing part
	 * 
	 * @param parent Parent control to a canvas used to display a XY graph  
	 */
	@PostConstruct
	public void createXYGraph(Composite parent, EModelService service, MPart mPart, EMenuService menuService) {

		// Handler for tool item and menu item
		RunHandler runHandler = new RunHandler();

		// Create a tool item with a drop down menu and place it on the part (1-4)
		// (1) Create a tool bar and a tool item and add the tool item to the tool bar
		MToolBar toolbar = MMenuFactory.INSTANCE.createToolBar();
		MDirectToolItem directToolitem = MMenuFactory.INSTANCE.createDirectToolItem();
		directToolitem.setElementId(XYGraphView.VIEW_ID);
		directToolitem.setIconURI("platform:/plugin/no.javatime.pl.xygraph/icons/sample.gif");
		// directToolitem.setContributionURI("bundleclass://no.javatime.pl.xygraph/no.javatime.pl.xygraph.views.RunHandler");
		directToolitem.setObject(runHandler);
		directToolitem.setVisible(true);
		directToolitem.setEnabled(true);		
		toolbar.getChildren().add(directToolitem);
				
		// (2) Create a menu and a menu item and add the menu item to the menu 
		MMenu menu = MMenuFactory.INSTANCE.createMenu();
		MDirectMenuItem directMenuitem = MMenuFactory.INSTANCE.createDirectMenuItem();
		directMenuitem.setLabel("Direct Menu Item");
		//directMenuitem.setContributionURI("bundleclass://no.javatime.pl.xygraph/no.javatime.pl.xygraph.views.RunHandler");
		directMenuitem.setObject(runHandler);
		directMenuitem.setVisible(true);
		directMenuitem.setEnabled(true);
		directMenuitem.setElementId(XYGraphView.VIEW_ID);
		menu.getChildren().add(directMenuitem);		
		
		// (3) Add the menu to the tool item
		directToolitem.setMenu(menu);
		
		// (4) Add the tool bar to the part
		mPart.setToolbar(toolbar);
		
		// Create canvas to hold the graph
		Canvas canvas = new Canvas(parent, SWT.NONE);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = 116;
		gd.horizontalSpan = 3;
		canvas.setLayoutData(gd);
		lightweightSystem = new LightweightSystem(canvas);		
		IXYGraph xyGraph = new XYGraph();
		xyGraph.setTitle("Waiting for Time Series Data ...");
		ToolbarArmedXYGraph toolbarArmedXYGraph = new ToolbarArmedXYGraph(xyGraph);
		lightweightSystem.setContents(toolbarArmedXYGraph);		
	}
	
	@PreDestroy
	public void removeXYGraph(Composite parent, EModelService service, MPart part, EPartService partService) {
		// partService.hidePart(part);
		System.out.println("Hiding view");
	}
	private final static DecimalFormat defaultFormat = new DecimalFormat("###,###.###");

	/**
	 * Updates the graph and its properties with the specified time series
	 * 
	 * @param timeSeriesDataProvider A set of time series with a common time span 
	 */
	public void updateXYGraph(TimeSeriesProvider timeSeriesDataProvider) {

		// create a new XY Graph.
		IXYGraph xyGraph = new XYGraph();
		ToolbarArmedXYGraph toolbarArmedXYGraph = new ToolbarArmedXYGraph(xyGraph);
		xyGraph.setTitle(timeSeriesDataProvider.getModelName());
		xyGraph.getPrimaryYAxis().setTitle("$");
		xyGraph.getPrimaryXAxis().setTitle("Year");
		// xyGraph.primaryYAxis.setRange(new Range(y[0], Math.ceil(y[y.length-1])));
		xyGraph.getPrimaryYAxis().setAutoScale(true);
		xyGraph.getPrimaryYAxis().setFormatPattern(defaultFormat.toPattern());
		// Add time series
		double[] steps = timeSeriesDataProvider.getRawSteps();
		// xyGraph.primaryXAxis.setRange(new Range(steps[0]-1, steps[steps.length-1]+1));
		xyGraph.getPrimaryXAxis().setAutoScale(true);
		if (steps.length > 0) {
			Collection<Extender<?>> modelElements = timeSeriesDataProvider.getModelElements();
			for (Extender<?> modelElement : modelElements) {				
				double[] timeSeries = timeSeriesDataProvider.getTimeSeries(modelElement);
				CircularBufferDataProvider traceDataProvider = new CircularBufferDataProvider(false);
				traceDataProvider.setBufferSize(steps.length);
				traceDataProvider.setCurrentYDataArray(timeSeries);
				traceDataProvider.setCurrentXDataArray(steps);
				Trace trace = new Trace(modelElement.getServiceClass().getName(), 
						xyGraph.getPrimaryXAxis(), xyGraph.getPrimaryYAxis(), traceDataProvider);
				trace.setPointStyle(PointStyle.XCROSS);
				xyGraph.addTrace(trace);			
			}
		}
		// Update graph with new time series
		lightweightSystem.setContents(toolbarArmedXYGraph);
	}	
}