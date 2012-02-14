/**
 * 
 */
package rinde.sim.ui;

import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.DeviceData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;


import rinde.sim.core.RoadModel;
import rinde.sim.core.Simulator;
import rinde.sim.core.TickListener;
import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.Point;
import rinde.sim.ui.renderers.Renderer;
import rinde.sim.ui.utils.Sleak;
import rinde.sim.util.TimeFormatter;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be> - color resource management
 */
public class View implements PaintListener, SelectionListener, ControlListener, TickListener, Listener {

	public static final String COLOR_WHITE = "white";
	public static final String COLOR_GREEN = "green";
	public static final String COLOR_BLACK = "black";
	
	public static final String DEFAULT_COLOR = "default_color"; 
	
	public static final String ICO_PKG = "package";
	
	protected final Canvas canvas;
	protected Image image;
	protected org.eclipse.swt.graphics.Point origin;
	protected org.eclipse.swt.graphics.Point size;
	protected final Display display;
	protected final Simulator<? extends RoadModel> simulator;
	protected final Renderer[] renderers;
	protected final ScrollBar hBar;
	protected final ScrollBar vBar;
	protected double m;//multiplier
	protected double minX;
	protected double minY;
	protected final long sleepInterval;
	
	protected static boolean testingMode = false;
	
	private ColorRegistry colorRegistry;
	

	double deltaX;
	double deltaY;

	protected Label timeLabel;

	private View(Composite parent, Simulator<? extends RoadModel> simulator, long sleepInterval, Renderer... renderers) {
		this.simulator = simulator;
		this.simulator.addAfterTickListener(this);
		this.sleepInterval = sleepInterval;
		display = parent.getDisplay();
		this.renderers = renderers;
		initColors();
		canvas = new Canvas(parent, SWT.DOUBLE_BUFFERED | SWT.NONE | SWT.NO_REDRAW_RESIZE | SWT.V_SCROLL | SWT.H_SCROLL);
		//canvas.setBounds(0, 0, 800, 500);
		canvas.setBackground(colorRegistry.get(COLOR_WHITE));
		origin = new org.eclipse.swt.graphics.Point(0, 0);
		size = new org.eclipse.swt.graphics.Point(800, 500);
		canvas.addPaintListener(this);
		canvas.addControlListener(this);
		canvas.redraw();

		timeLabel = new Label(canvas, SWT.NONE);
		timeLabel.setText("hello world");
		timeLabel.setBounds(20, 20, 200, 20);
		timeLabel.setBackground(colorRegistry.get(COLOR_WHITE));

		hBar = canvas.getHorizontalBar();
		hBar.addSelectionListener(this);
		vBar = canvas.getVerticalBar();
		vBar.addSelectionListener(this);

	}

	/**
	 * Initializes color registry and passes it to all renderers
	 */
	private void initColors() {
		assert display != null : "should be called after display is initialized";
		assert renderers != null : "should be called after renderers are initialized";
		colorRegistry = new ColorRegistry(display);
		colorRegistry.put(COLOR_WHITE, new RGB(0xFF,0xFF,0xFF));
		colorRegistry.put(COLOR_BLACK, new RGB(0x00,0x00,0x00));
		colorRegistry.put(COLOR_GREEN, new RGB(0x00,0xFF,0x00));
		
	}

	boolean firstTime = true;

	@Override
	public void paintControl(final PaintEvent e) {
		final GC gc = e.gc;

		if (firstTime) {
			calculateSizes();
			firstTime = false;
		}

		if (image == null) {
			image = drawRoads();
			updateScrollbars(false);
		}

		gc.drawImage(image, origin.x, origin.y);

		final Rectangle rect = image.getBounds();
		final Rectangle client = canvas.getClientArea();
		final int marginWidth = client.width - rect.width;
		if (marginWidth > 0) {
			gc.fillRectangle(rect.width, 0, marginWidth, client.height);
		}
		final int marginHeight = client.height - rect.height;
		if (marginHeight > 0) {
			gc.fillRectangle(0, rect.height, client.width, marginHeight);
		}

		for (Renderer renderer : renderers) {
			renderer.render(gc, origin.x, origin.y, minX, minY, m);
		}

	}

	private void calculateSizes() {
		Set<Point> nodes = simulator.model.getNodes();

		minX = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		minY = Double.POSITIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;

		for (Point p : nodes) {
			minX = Math.min(minX, p.x);
			maxX = Math.max(maxX, p.x);
			minY = Math.min(minY, p.y);
			maxY = Math.max(maxY, p.y);
		}

		deltaX = maxX - minX;
		deltaY = maxY - minY;

		//		System.out.println(deltaX + " " + deltaY);

		Rectangle area = canvas.getClientArea();
		if (deltaX > deltaY) {
			m = area.width / deltaX;
		} else {
			m = area.height / deltaY;
		}

		//m *= 2;

	}

	public Image drawRoads() {
		size = new org.eclipse.swt.graphics.Point((int) (m * deltaX), (int) (m * deltaY));
		final Image img = new Image(display, size.x + 10, size.y + 10);
		final GC gc = new GC(img);

		Graph graph = simulator.model.getGraph();
		for (Entry<Point, Point> e : graph.getConnections()) {
			int x1 = (int) ((e.getKey().x - minX) * m);
			int y1 = (int) ((e.getKey().y - minY) * m);
			gc.setForeground(colorRegistry.get(COLOR_GREEN));
			gc.drawOval(x1 - 2, y1 - 2, 4, 4);

			int x2 = (int) ((e.getValue().x - minX) * m);
			int y2 = (int) ((e.getValue().y - minY) * m);
			gc.setForeground(colorRegistry.get(COLOR_BLACK));
			gc.drawLine(x1, y1, x2, y2);
		}
		gc.dispose();

		return img;
	}
	
	/**
	 * Define the SWT handles tracing mode. Disabled by default
	 * @param testingMode
	 */
	public static void setTestingMode(boolean testingMode) {
		View.testingMode = testingMode;
	}

	public static void startGui(final Simulator<? extends RoadModel> simulator, long sleepInterval, Renderer... renderers) {
		Display.setAppName("RinSim");
		
		Display display;
		if(testingMode) {
			DeviceData data = new DeviceData();
			data.tracking = true;
			display = new Display(data);
			Sleak sleak = new Sleak();
			sleak.open();			
		} else {
			display =  new Display();
		}

		final Shell shell = new Shell(display, SWT.TITLE | SWT.CLOSE | SWT.RESIZE);
		shell.setText("RinSim - Simulator");
		shell.setLayout(new FillLayout());

		Menu bar = new Menu(shell, SWT.BAR);

		shell.setMenuBar(bar);
		MenuItem fileItem = new MenuItem(bar, SWT.CASCADE);
		fileItem.setText("Control");

		Menu submenu = new Menu(shell, SWT.DROP_DOWN);
		fileItem.setMenu(submenu);

		final MenuItem item = new MenuItem(submenu, SWT.PUSH);
		Listener playPauseListener = new Listener() {
			@Override
			public void handleEvent(Event e) {
				if (simulator.isPlaying()) {
					item.setText("&Play");
				} else {
					item.setText("&Pause");
				}
				new Thread() {
					@Override
					public void run() {
						simulator.togglePlayPause();
					}
				}.start();
			}
		};
		item.setText("&Play");
		item.setAccelerator(SWT.MOD1 + 'P');
		item.addListener(SWT.Selection, playPauseListener);

		new MenuItem(submenu, SWT.SEPARATOR);
		final MenuItem nextItem = new MenuItem(submenu, SWT.PUSH);
		nextItem.setText("Next tick");
		nextItem.setAccelerator(SWT.MOD1 + ']');
		nextItem.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (simulator.isPlaying()) {
					simulator.stop();
				}
				simulator.tick();
			}
		});

		MenuItem viewItem = new MenuItem(bar, SWT.CASCADE);
		viewItem.setText("View");

		Menu viewMenu = new Menu(shell, SWT.DROP_DOWN);
		viewItem.setMenu(viewMenu);

		final MenuItem zoomInItem = new MenuItem(viewMenu, SWT.PUSH);
		zoomInItem.setText("Zoom in");
		zoomInItem.setAccelerator(SWT.MOD1 + '+');
		zoomInItem.setData("in");

		final MenuItem zoomOutItem = new MenuItem(viewMenu, SWT.PUSH);
		zoomOutItem.setText("Zoom out");
		zoomOutItem.setAccelerator(SWT.MOD1 + '-');
		zoomOutItem.setData("out");

		shell.setSize(new org.eclipse.swt.graphics.Point(1024, 768));
		//shell.setMaximized(true);
		shell.setMinimumSize(400, 300);

		View v = new View(shell, simulator, sleepInterval, renderers);
		zoomInItem.addListener(SWT.Selection, v);
		zoomOutItem.addListener(SWT.Selection, v);

		shell.open();

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		if (shell.isDisposed()) {
			simulator.stop();
		}
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		// not needed
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		if (e.widget == vBar) {
			final int vSelection = vBar.getSelection();
			final int destY = -vSelection - origin.y;
			final Rectangle rect = image.getBounds();
			canvas.scroll(0, destY, 0, 0, rect.width, rect.height, false);
			origin.y = -vSelection;
		} else {
			final int hSelection = hBar.getSelection();
			final int destX = -hSelection - origin.x;
			final Rectangle rect = image.getBounds();
			canvas.scroll(destX, 0, 0, 0, rect.width, rect.height, false);
			origin.x = -hSelection;
		}

	}

	protected void updateScrollbars(boolean adaptToScrollbar) {
		final Rectangle rect = image.getBounds();
		final Rectangle client = canvas.getClientArea();
		hBar.setMaximum(rect.width);
		vBar.setMaximum(rect.height);
		hBar.setThumb(Math.min(rect.width, client.width));
		vBar.setThumb(Math.min(rect.height, client.height));
		final int hPage = rect.width - client.width;
		final int vPage = rect.height - client.height;
		int hSelection = hBar.getSelection();
		int vSelection = vBar.getSelection();
		if (adaptToScrollbar) {
			if (hSelection >= hPage) {
				if (hPage <= 0) {
					hSelection = 0;
				}
				origin.x = -hSelection;
			}
			if (vSelection >= vPage) {
				if (vPage <= 0) {
					vSelection = 0;
				}
				origin.y = -vSelection;
			}
		} else {
			hBar.setSelection(-origin.x);
			vBar.setSelection(-origin.y);
		}
	}

	@Override
	public void controlResized(final ControlEvent pE) {
		if (image != null) {
			updateScrollbars(true);
			canvas.redraw();
		}
	}

	@Override
	public void controlMoved(ControlEvent e) {//not needed
	}

	@Override
	public void tick(long currentTime, long timeStep) {
		if (display.isDisposed()) {
			return;
		}

		display.asyncExec(new Runnable() {
			@Override
			public void run() {
				if (!canvas.isDisposed()) {
					timeLabel.setText(TimeFormatter.format(simulator.getCurrentTime()));
					canvas.redraw();
				}
			}
		});
		if (sleepInterval > 0) {
			try {
				Thread.sleep(sleepInterval);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void handleEvent(Event event) {
		if (event.widget.getData().equals("in")) {
			origin.x -= m * deltaX / 2;
			origin.y -= m * deltaY / 2;
			m *= 2;

		} else {
			m /= 2;
			origin.x += m * deltaX / 2;
			origin.y += m * deltaY / 2;
		}
		if(image != null)
			image.dispose();
		image = null;// this forces a redraw
		canvas.redraw();
	}
}
