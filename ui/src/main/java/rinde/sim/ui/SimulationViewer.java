package rinde.sim.ui;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newLinkedHashSet;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import rinde.sim.core.Simulator;
import rinde.sim.core.TickListener;
import rinde.sim.core.TimeLapse;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.ModelReceiver;
import rinde.sim.ui.renderers.CanvasRenderer;
import rinde.sim.ui.renderers.PanelRenderer;
import rinde.sim.ui.renderers.Renderer;
import rinde.sim.ui.renderers.ViewPort;
import rinde.sim.ui.renderers.ViewRect;
import rinde.sim.util.TimeFormatter;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

/**
 * Simulation viewer.
 * 
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class SimulationViewer extends Composite implements TickListener,
    ControlListener, PaintListener, SelectionListener {

  public static final String ICO_PKG = "package";

  protected Canvas canvas;
  protected org.eclipse.swt.graphics.Point origin;
  protected org.eclipse.swt.graphics.Point size;

  protected Image image;
  protected final List<CanvasRenderer> renderers;

  protected final Set<ModelReceiver> modelRenderers;

  protected final boolean autoPlay;

  protected MenuItem playPauseMenuItem;
  protected double m; // multiplier

  boolean firstTime = true;
  final Simulator simulator;
  ViewRect viewRect;
  Label timeLabel;

  // private Map<String, Color> colorRegistry;

  private ScrollBar hBar;
  private ScrollBar vBar;

  // rendering frequency related
  private int speedUp;
  private long lastRefresh;

  private int zoomRatio;
  private final Display display;

  public SimulationViewer(Shell shell, final Simulator sim, int pSpeedUp,
      boolean pAutoPlay, Renderer... pRenderers) {
    super(shell, SWT.NONE);

    autoPlay = pAutoPlay;

    final Multimap<Integer, PanelRenderer> panels = LinkedHashMultimap.create();
    renderers = newArrayList();
    modelRenderers = newLinkedHashSet();
    for (final Renderer r : pRenderers) {
      if (r instanceof ModelReceiver) {
        modelRenderers.add((ModelReceiver) r);
      }
      boolean valid = false;
      if (r instanceof PanelRenderer) {
        panels.put(((PanelRenderer) r).getPreferredPosition(),
            (PanelRenderer) r);
        valid = true;
      }

      if (r instanceof CanvasRenderer) {
        renderers.add((CanvasRenderer) r);
        valid = true;
      }

      checkState(valid, "A renderer was not of a recognized subtype: %s", r);

      if (r instanceof TickListener) {
        sim.addTickListener((TickListener) r);
      }

    }
    simulator = sim;
    simulator.addTickListener(this);

    speedUp = pSpeedUp;
    shell.setLayout(new FillLayout());
    display = shell.getDisplay();
    setLayout(new FillLayout());

    createMenu(shell);
    shell.addListener(SWT.Close, new Listener() {
      @SuppressWarnings("synthetic-access")
      @Override
      public void handleEvent(Event event) {
        simulator.stop();
        while (simulator.isPlaying()) {
          // wait until simulator acutally stops (it finishes its
          // current tick first).
        }

        if (!display.isDisposed()) {
          display.dispose();
        }
      }
    });

    if (panels.isEmpty()) {

      createContent(this);
    } else {

      final SashForm vertical = new SashForm(this, SWT.VERTICAL | SWT.SMOOTH);
      vertical.setLayout(new FillLayout());

      final int topHeight = configurePanels(vertical, panels.removeAll(SWT.TOP));

      final SashForm horizontal = new SashForm(vertical, SWT.HORIZONTAL
          | SWT.SMOOTH);
      horizontal.setLayout(new FillLayout());

      final int leftWidth = configurePanels(horizontal,
          panels.removeAll(SWT.LEFT));

      // create canvas
      createContent(horizontal);

      final int rightWidth = configurePanels(horizontal,
          panels.removeAll(SWT.RIGHT));
      final int bottomHeight = configurePanels(vertical,
          panels.removeAll(SWT.BOTTOM));

      final int canvasHeight = (size.y - topHeight) - bottomHeight;
      if (topHeight > 0 && bottomHeight > 0) {
        vertical.setWeights(varargs(topHeight, canvasHeight, bottomHeight));
      } else if (topHeight > 0) {
        vertical.setWeights(varargs(topHeight, canvasHeight));
      } else if (bottomHeight > 0) {
        vertical.setWeights(varargs(canvasHeight, bottomHeight));
      }

      final int canvasWidth = (size.x - leftWidth) - rightWidth;
      if (leftWidth > 0 && rightWidth > 0) {
        horizontal.setWeights(varargs(leftWidth, canvasWidth, rightWidth));
      } else if (leftWidth > 0) {
        horizontal.setWeights(varargs(leftWidth, canvasWidth));
      } else if (rightWidth > 0) {
        horizontal.setWeights(varargs(canvasWidth, rightWidth));
      }

      checkState(panels.isEmpty(),
          "Invalid preferred position set for panels: %s", panels.values());
    }

  }

  static int[] varargs(int... ints) {
    return ints;
  }

  protected int configurePanels(SashForm parent,
      Collection<PanelRenderer> panels) {
    if (panels.isEmpty()) {
      return 0;
    }

    int prefSize = 0;
    for (final PanelRenderer p : panels) {
      prefSize = Math.max(p.preferredSize(), prefSize);
    }
    if (panels.size() == 1) {
      final PanelRenderer p = panels.iterator().next();
      final Group g = new Group(parent, SWT.SHADOW_NONE);
      p.initializePanel(g);
    } else {
      final TabFolder tab = new TabFolder(parent, SWT.NONE);

      for (final PanelRenderer p : panels) {
        final TabItem ti = new TabItem(tab, SWT.NONE);
        ti.setText(p.getName());
        final Composite comp = new Composite(tab, SWT.NONE);
        ti.setControl(comp);
        p.initializePanel(comp);
      }
    }
    return prefSize;
  }

  protected void configureModelRenderers() {
    for (final ModelReceiver mr : modelRenderers) {
      mr.registerModelProvider(simulator.getModelProvider());
    }
  }

  /**
   * Configure shell.
   */
  protected Canvas createContent(Composite parent) {
    canvas = new Canvas(parent, SWT.DOUBLE_BUFFERED | SWT.NONE
        | SWT.NO_REDRAW_RESIZE | SWT.V_SCROLL | SWT.H_SCROLL);
    canvas.setBackground(display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

    origin = new org.eclipse.swt.graphics.Point(0, 0);
    size = new org.eclipse.swt.graphics.Point(800, 500);
    canvas.addPaintListener(this);
    canvas.addControlListener(this);
    // canvas.redraw();
    this.layout();

    timeLabel = new Label(canvas, SWT.NONE);
    timeLabel.setText("hello world");
    timeLabel.pack();
    timeLabel.setLocation(50, 10);
    timeLabel
        .setBackground(canvas.getDisplay().getSystemColor(SWT.COLOR_WHITE));

    hBar = canvas.getHorizontalBar();
    hBar.addSelectionListener(this);
    vBar = canvas.getVerticalBar();
    vBar.addSelectionListener(this);

    return canvas;
  }

  @SuppressWarnings("unused")
  protected void createMenu(Shell shell) {
    final Menu bar = new Menu(shell, SWT.BAR);
    shell.setMenuBar(bar);

    final MenuItem fileItem = new MenuItem(bar, SWT.CASCADE);
    fileItem.setText("&Control");

    final Menu submenu = new Menu(shell, SWT.DROP_DOWN);
    fileItem.setMenu(submenu);

    // play switch
    playPauseMenuItem = new MenuItem(submenu, SWT.PUSH);
    playPauseMenuItem.setText("&Play\tCtrl+P");
    playPauseMenuItem.setAccelerator(SWT.MOD1 + 'P');
    playPauseMenuItem.addListener(SWT.Selection, new Listener() {

      @Override
      public void handleEvent(Event e) {
        onToglePlay((MenuItem) e.widget);
      }
    });

    new MenuItem(submenu, SWT.SEPARATOR);
    // step execution switch
    final MenuItem nextItem = new MenuItem(submenu, SWT.PUSH);
    nextItem.setText("Next tick\tCtrl+Shift+]");
    nextItem.setAccelerator(SWT.MOD1 + SWT.SHIFT + ']');
    nextItem.addListener(SWT.Selection, new Listener() {
      @Override
      public void handleEvent(Event e) {
        onTick((MenuItem) e.widget);
      }
    });

    // view options

    final MenuItem viewItem = new MenuItem(bar, SWT.CASCADE);
    viewItem.setText("&View");

    final Menu viewMenu = new Menu(shell, SWT.DROP_DOWN);
    viewItem.setMenu(viewMenu);

    // zooming
    final MenuItem zoomInItem = new MenuItem(viewMenu, SWT.PUSH);
    zoomInItem.setText("Zoom &in\tCtrl++");
    zoomInItem.setAccelerator(SWT.MOD1 + '+');
    zoomInItem.setData("in");

    final MenuItem zoomOutItem = new MenuItem(viewMenu, SWT.PUSH);
    zoomOutItem.setText("Zoom &out\tCtrl+-");
    zoomOutItem.setAccelerator(SWT.MOD1 + '-');
    zoomOutItem.setData("out");

    final Listener zoomingListener = new Listener() {
      @Override
      public void handleEvent(Event e) {
        onZooming((MenuItem) e.widget);
      }
    };
    zoomInItem.addListener(SWT.Selection, zoomingListener);
    zoomOutItem.addListener(SWT.Selection, zoomingListener);

    // speedUp

    final Listener speedUpListener = new Listener() {

      @Override
      public void handleEvent(Event e) {
        onSpeedChange((MenuItem) e.widget);
      }
    };

    final MenuItem increaseSpeedItem = new MenuItem(submenu, SWT.PUSH);
    increaseSpeedItem.setAccelerator(SWT.MOD1 + ']');
    increaseSpeedItem.setText("Speed &up\tCtrl+]");
    increaseSpeedItem.setData(">");
    increaseSpeedItem.addListener(SWT.Selection, speedUpListener);
    //
    final MenuItem decreaseSpeed = new MenuItem(submenu, SWT.PUSH);
    decreaseSpeed.setAccelerator(SWT.MOD1 + '[');
    decreaseSpeed.setText("Slow &down\tCtrl+[");
    decreaseSpeed.setData("<");
    decreaseSpeed.addListener(SWT.Selection, speedUpListener);

  }

  /**
   * Default implementation of the play/pause action. Can be overridden if
   * needed.
   * 
   * @param source
   */
  protected void onToglePlay(MenuItem source) {
    if (simulator.isPlaying()) {
      source.setText("&Play\tCtrl+P");
    } else {
      source.setText("&Pause\tCtrl+P");
    }
    new Thread() {
      @Override
      public void run() {
        simulator.togglePlayPause();
      }
    }.start();
  }

  /**
   * Default implementation of step execution action. Can be overridden if
   * needed.
   * 
   * @param source
   */
  protected void onTick(MenuItem source) {
    if (simulator.isPlaying()) {
      simulator.stop();
    }
    simulator.tick();
  }

  protected void onZooming(MenuItem source) {
    if ("in".equals(source.getData())) {
      if (zoomRatio == 16) {
        return;
      }
      origin.x -= m * viewRect.width / 2;
      origin.y -= m * viewRect.height / 2;
      m *= 2;
      zoomRatio <<= 1;
    } else {
      if (zoomRatio < 2) {
        return;
      }
      m /= 2;
      origin.x += m * viewRect.width / 2;
      origin.y += m * viewRect.height / 2;
      zoomRatio >>= 1;
    }
    if (image != null) {
      image.dispose();
    }
    image = null; // this forces a redraw
    canvas.redraw();
  }

  protected void onSpeedChange(MenuItem source) {
    if (">".equals(source.getData())) {
      speedUp <<= 1;
    } else {
      if (speedUp > 1) {
        speedUp >>= 1;
      }
    }
  }

  public Image drawRoads() {
    size = new org.eclipse.swt.graphics.Point((int) (m * viewRect.width),
        (int) (m * viewRect.height));
    final Image img = new Image(getDisplay(), size.x, size.y);
    final GC gc = new GC(img);

    for (final CanvasRenderer r : renderers) {
      r.renderStatic(gc, new ViewPort(new Point(0, 0)/*
                                                      * new Point(origin.x,
                                                      * origin.y)
                                                      */, viewRect, m));
    }

    //
    // Graph<? extends EdgeData> graph = roadModel.getGraph();
    //
    // for (Connection<? extends EdgeData> e : graph.getConnections()) {
    // int x1 = (int) ((e.from.x - minX) * m);
    // int y1 = (int) ((e.from.y - minY) * m);
    // // gc.setForeground(colorRegistry.get(COLOR_GREEN));
    // // gc.drawOval(x1 - 2, y1 - 2, 4, 4);
    //
    // int x2 = (int) ((e.to.x - minX) * m);
    // int y2 = (int) ((e.to.y - minY) * m);
    // gc.setForeground(colorRegistry.get(COLOR_BLACK));
    // gc.drawLine(x1, y1, x2, y2);
    //
    // // gc.setBackground(colorRegistry.get(COLOR_WHITE));
    // // gc.drawText(Math.round(e.edgeData.getLength() * 10.0) / 10.0 +
    // // "m", (x1 + x2) / 2, (y1 + y2) / 2, false);
    // }
    gc.dispose();

    return img;
  }

  @Override
  public void paintControl(PaintEvent e) {
    final GC gc = e.gc;

    final boolean wasFirstTime = firstTime;
    if (firstTime) {
      configureModelRenderers();
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

    for (final CanvasRenderer renderer : renderers) {
      renderer.renderDynamic(gc, new ViewPort(new Point(origin.x, origin.y),
          viewRect, m), simulator.getCurrentTime());
      // renderer.render(gc, origin.x, origin.y, minX, minY, m);
    }

    // auto play sim if required
    if (wasFirstTime && autoPlay) {
      onToglePlay(playPauseMenuItem);
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

  private void calculateSizes() {
    // System.out.println("SIM IS CONFIGURED: " + simulator.isConfigured());
    if (!simulator.isConfigured()) {
      return;
    }

    double minX = Double.POSITIVE_INFINITY;
    double maxX = Double.NEGATIVE_INFINITY;
    double minY = Double.POSITIVE_INFINITY;
    double maxY = Double.NEGATIVE_INFINITY;

    boolean isDefined = false;
    for (final CanvasRenderer r : renderers) {
      final ViewRect rect = r.getViewRect();
      if (rect != null) {
        minX = Math.min(minX, rect.min.x);
        maxX = Math.max(maxX, rect.max.x);
        minY = Math.min(minY, rect.min.y);
        maxY = Math.max(maxY, rect.max.y);
        isDefined = true;
      }
    }

    checkState(
        isDefined,
        "none of the available renderers implements getViewRect(), known renderers: %s",
        renderers);

    viewRect = new ViewRect(new Point(minX, minY), new Point(maxX, maxY));

    // deltaX = maxX - minX;
    // deltaY = maxY - minY;

    // System.out.println(deltaX + " " + deltaY);

    final Rectangle area = canvas.getClientArea();
    if (viewRect.width > viewRect.height) {
      m = area.width / viewRect.width;
    } else {
      m = area.height / viewRect.height;
    }
    zoomRatio = 1;

    // m *= 2;

  }

  @Override
  public void controlMoved(ControlEvent e) {
    // not needed
  }

  @Override
  public void controlResized(ControlEvent e) {
    if (image != null) {
      updateScrollbars(true);
      canvas.redraw();
    }
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

  @Override
  public void widgetDefaultSelected(SelectionEvent e) {
    // not needed

  }

  @Override
  public void tick(TimeLapse timeLapse) {}

  @Override
  public void afterTick(TimeLapse timeLapse) {
    if (simulator.isPlaying()
        && lastRefresh + timeLapse.getTimeStep() * speedUp > timeLapse
            .getStartTime()) {
      return;
    }
    lastRefresh = timeLapse.getStartTime();
    try {
      Thread.sleep(30);
    } catch (final InterruptedException e) {
      throw new RuntimeException(e);
    }
    if (display.isDisposed()) {
      return;
    }
    display.syncExec(new Runnable() {
      @Override
      public void run() {
        if (!canvas.isDisposed()) {
          if (simulator.getTimeStep() > 500) {
            timeLabel.setText(TimeFormatter.format(simulator.getCurrentTime()));
          } else {
            timeLabel.setText("" + simulator.getCurrentTime());
          }
          timeLabel.pack();
          canvas.redraw();
        }
      }
    });
  }
}
