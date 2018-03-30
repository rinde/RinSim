/*
 * Copyright (C) 2011-2018 Rinde R.S. van Lon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rinde.rinsim.ui;

import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

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
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.Model;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.model.ModelProvider;
import com.github.rinde.rinsim.core.model.ModelReceiver;
import com.github.rinde.rinsim.core.model.time.ClockController;
import com.github.rinde.rinsim.core.model.time.RealtimeClockController;
import com.github.rinde.rinsim.core.model.time.RealtimeClockController.ClockMode;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.View.ViewOption;
import com.github.rinde.rinsim.ui.renderers.CanvasRenderer;
import com.github.rinde.rinsim.ui.renderers.PanelRenderer;
import com.github.rinde.rinsim.ui.renderers.Renderer;
import com.github.rinde.rinsim.ui.renderers.ViewPort;
import com.github.rinde.rinsim.ui.renderers.ViewRect;
import com.google.auto.value.AutoValue;
import com.google.common.base.Optional;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

/**
 * Simulation viewer.
 *
 * @author Bartosz Michalik
 * @author Rinde van Lon
 */
final class SimulationViewer extends Composite implements TickListener,
    ControlListener, PaintListener, SelectionListener, Model<Renderer>,
    ModelReceiver, RenderController {
  static final String SPACE = " ";
  static final org.eclipse.swt.graphics.Point START_SCREEN_SIZE =
    new org.eclipse.swt.graphics.Point(800, 500);
  static final org.eclipse.swt.graphics.Point TIME_LABEL_LOC =
    new org.eclipse.swt.graphics.Point(50, 10);
  static final String PLAY_LABEL = "&Play\tCtrl+P";
  static final long SLEEP_MS = 30;
  static final long TIME_FORMATTER_THRESHOLD = 200;
  static final String TIME_SEPARATOR = ":";
  static final PeriodFormatter FORMATTER = new PeriodFormatterBuilder()
    .appendDays()
    .appendSeparator(SPACE)
    .minimumPrintedDigits(2)
    .printZeroAlways()
    .appendHours()
    .appendLiteral(TIME_SEPARATOR)
    .appendMinutes()
    .appendLiteral(TIME_SEPARATOR)
    .appendSeconds()
    .toFormatter();

  private static final int MIN_SPEED_UP = 1;
  private static final int MAX_SPEED_UP = 512;
  private static final int MAX_ZOOM_LEVEL = 16;

  boolean firstTime = true;
  final ClockController clock;
  @Nullable
  ViewRect viewRect;
  @Nullable
  Label timeLabel;

  final boolean isRealtime;
  final SimulatorAPI simulator;
  ModelProvider modelProvider;

  private Canvas canvas;
  private org.eclipse.swt.graphics.Point origin;
  private org.eclipse.swt.graphics.Point size;

  @Nullable
  private Image image;
  private final List<PanelRenderer> panelRenderers;
  private final List<CanvasRenderer> canvasRenderers;
  private final boolean autoPlay;
  private MenuItem playPauseMenuItem;
  // multiplier
  private double m;

  private boolean requestStaticRenderUpdate;

  @Nullable
  private ScrollBar hBar;
  @Nullable
  private ScrollBar vBar;

  // rendering frequency related
  private int speedUp;
  private long lastRefresh;

  private int zoomRatio;
  private final Display display;
  private final Map<MenuItems, Integer> accelerators;

  SimulationViewer(Shell shell, ClockController cc, SimulatorAPI simapi,
      View.Builder vb) {
    super(shell, SWT.NONE);

    clock = cc;
    isRealtime = clock instanceof RealtimeClockController;

    simulator = simapi;

    accelerators = vb.accelerators();
    autoPlay = vb.viewOptions().contains(ViewOption.AUTO_PLAY);

    canvasRenderers = new ArrayList<>();
    panelRenderers = new ArrayList<>();

    speedUp = vb.speedUp();
    shell.setLayout(new FillLayout());
    display = shell.getDisplay();
    setLayout(new FillLayout());

    createMenu(shell);
  }

  void show() {
    final Multimap<Integer, PanelRenderer> panels = LinkedHashMultimap
      .create();
    for (final PanelRenderer pr : panelRenderers) {
      panels.put(pr.getPreferredPosition(), pr);
    }
    panelsLayout(panels);
  }

  void panelsLayout(Multimap<Integer, PanelRenderer> panels) {
    if (panels.isEmpty()) {
      createContent(this);
    } else {

      final SashForm vertical = new SashForm(this, SWT.VERTICAL | SWT.SMOOTH);
      vertical.setLayout(new FillLayout());

      final int topHeight = configurePanels(vertical,
        panels.removeAll(SWT.TOP));

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

      final int canvasHeight = size.y - topHeight - bottomHeight;
      if (topHeight > 0 && bottomHeight > 0) {
        vertical.setWeights(varargs(topHeight, canvasHeight, bottomHeight));
      } else if (topHeight > 0) {
        vertical.setWeights(varargs(topHeight, canvasHeight));
      } else if (bottomHeight > 0) {
        vertical.setWeights(varargs(canvasHeight, bottomHeight));
      }

      final int canvasWidth = size.x - leftWidth - rightWidth;
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

  int configurePanels(SashForm parent, Collection<PanelRenderer> panels) {
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

  /**
   * Configure shell.
   */
  void createContent(Composite parent) {
    canvas = new Canvas(parent, SWT.DOUBLE_BUFFERED | SWT.NONE
      | SWT.NO_REDRAW_RESIZE | SWT.V_SCROLL | SWT.H_SCROLL);
    canvas.setBackground(display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

    origin = new org.eclipse.swt.graphics.Point(0, 0);
    size = START_SCREEN_SIZE;
    canvas.addPaintListener(this);
    canvas.addControlListener(this);
    this.layout();

    timeLabel = new Label(canvas, SWT.NONE);
    timeLabel.setText("hello world");
    timeLabel.pack();
    timeLabel.setLocation(TIME_LABEL_LOC);
    timeLabel
      .setBackground(canvas.getDisplay().getSystemColor(SWT.COLOR_WHITE));

    hBar = canvas.getHorizontalBar();
    hBar.addSelectionListener(this);
    vBar = canvas.getVerticalBar();
    vBar.addSelectionListener(this);
  }

  @SuppressWarnings("unused")
  void createMenu(Shell shell) {
    final Menu bar = new Menu(shell, SWT.BAR);
    shell.setMenuBar(bar);

    final MenuItem fileItem = new MenuItem(bar, SWT.CASCADE);
    fileItem.setText("&Control");

    final Menu submenu = new Menu(shell, SWT.DROP_DOWN);
    fileItem.setMenu(submenu);

    // play switch
    playPauseMenuItem = new MenuItem(submenu, SWT.PUSH);
    playPauseMenuItem.setText(PLAY_LABEL);
    playPauseMenuItem.setAccelerator(accelerators.get(MenuItems.PLAY));
    playPauseMenuItem.addListener(SWT.Selection, new Listener() {

      @Override
      public void handleEvent(@Nullable Event e) {
        assert e != null;
        onToglePlay((MenuItem) e.widget);
      }
    });

    new MenuItem(submenu, SWT.SEPARATOR);
    // step execution switch
    final MenuItem nextItem = new MenuItem(submenu, SWT.PUSH);
    nextItem.setText("Next tick\tCtrl+Shift+]");
    nextItem.setAccelerator(accelerators.get(MenuItems.NEXT_TICK));
    nextItem.addListener(SWT.Selection, new Listener() {
      @Override
      public void handleEvent(@Nullable Event e) {
        assert e != null;
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
    zoomInItem.setAccelerator(accelerators.get(MenuItems.ZOOM_IN));
    zoomInItem.setData(MenuItems.ZOOM_IN);

    final MenuItem zoomOutItem = new MenuItem(viewMenu, SWT.PUSH);
    zoomOutItem.setText("Zoom &out\tCtrl+-");
    zoomOutItem.setAccelerator(accelerators.get(MenuItems.ZOOM_OUT));
    zoomOutItem.setData(MenuItems.ZOOM_OUT);

    final Listener zoomingListener = new Listener() {
      @Override
      public void handleEvent(@Nullable Event e) {
        assert e != null;
        onZooming((MenuItem) e.widget);
      }
    };
    zoomInItem.addListener(SWT.Selection, zoomingListener);
    zoomOutItem.addListener(SWT.Selection, zoomingListener);

    // speedUp

    final Listener speedUpListener = new Listener() {

      @Override
      public void handleEvent(@Nullable Event e) {
        assert e != null;
        onSpeedChange((MenuItem) e.widget);
      }
    };

    final MenuItem increaseSpeedItem = new MenuItem(submenu, SWT.PUSH);
    increaseSpeedItem
      .setAccelerator(accelerators.get(MenuItems.INCREASE_SPEED));
    increaseSpeedItem.setText("Speed &up\tCtrl+]");
    increaseSpeedItem.setData(MenuItems.INCREASE_SPEED);
    increaseSpeedItem.addListener(SWT.Selection, speedUpListener);
    //
    final MenuItem decreaseSpeed = new MenuItem(submenu, SWT.PUSH);
    decreaseSpeed.setAccelerator(accelerators.get(MenuItems.DECREASE_SPEED));
    decreaseSpeed.setText("Slow &down\tCtrl+[");
    decreaseSpeed.setData(MenuItems.DECREASE_SPEED);
    decreaseSpeed.addListener(SWT.Selection, speedUpListener);

  }

  /*
   * Default implementation of the play/pause action. Can be overridden if
   * needed.
   *
   * @param source
   */
  void onToglePlay(MenuItem source) {
    if (clock.isTicking()) {
      source.setText(PLAY_LABEL);
    } else {
      source.setText("&Pause\tCtrl+P");
    }
    new Thread() {
      @Override
      public void run() {
        if (clock.isTicking()) {
          clock.stop();
        } else {
          clock.start();
        }
      }
    }.start();
  }

  /*
   * Default implementation of step execution action. Can be overridden if
   * needed.
   *
   * @param source
   */
  void onTick(MenuItem source) {
    if (clock.isTicking()) {
      clock.stop();
    }
    clock.tick();
  }

  void onZooming(MenuItem source) {
    if (source.getData() == MenuItems.ZOOM_IN) {
      if (zoomRatio == MAX_ZOOM_LEVEL) {
        return;
      }
      m *= 2;
      origin.x *= 2;
      origin.y *= 2;
      zoomRatio <<= 1;
    } else {
      if (zoomRatio < 2) {
        return;
      }
      m /= 2;
      origin.x /= 2;
      origin.y /= 2;
      zoomRatio >>= 1;
    }
    if (image != null) {
      image.dispose();
    }
    // this forces a redraw
    image = null;
    canvas.redraw();
  }

  void onSpeedChange(MenuItem source) {
    if (source.getData() == MenuItems.INCREASE_SPEED) {
      if (speedUp < MAX_SPEED_UP) {
        speedUp <<= 1;
      }
    } else {
      if (speedUp > MIN_SPEED_UP) {
        speedUp >>= 1;
      }
    }
  }

  Image renderStatic() {
    size = new org.eclipse.swt.graphics.Point((int) (m * viewRect.width),
      (int) (m * viewRect.height));
    final Image img = new Image(getDisplay(), size.x, size.y);
    final GC gc = new GC(img);

    for (final CanvasRenderer r : canvasRenderers) {
      r.renderStatic(gc, new ViewPort(new Point(0, 0), viewRect, m));
    }
    gc.dispose();
    return img;
  }

  @Override
  public void paintControl(@Nullable PaintEvent e) {
    assert e != null;
    final GC gc = e.gc;

    final boolean wasFirstTime = firstTime;
    if (firstTime || requestStaticRenderUpdate) {
      calculateSizes();
      firstTime = false;
    }

    if (image == null || requestStaticRenderUpdate) {
      image = renderStatic();
      updateScrollbars(false);
      requestStaticRenderUpdate = false;
    }

    final org.eclipse.swt.graphics.Point center = getCenteredOrigin();

    gc.drawImage(image, center.x, center.y);
    for (final CanvasRenderer renderer : canvasRenderers) {
      renderer.renderDynamic(gc, new ViewPort(new Point(center.x,
        center.y),
        viewRect, m),
        clock.getCurrentTime());
    }
    for (final PanelRenderer renderer : panelRenderers) {
      renderer.render();
    }

    final Rectangle content = image.getBounds();
    final Rectangle client = canvas.getClientArea();

    hBar.setVisible(content.width > client.width);
    vBar.setVisible(content.height > client.height);

    // auto play sim if required
    if (wasFirstTime && autoPlay) {
      onToglePlay(playPauseMenuItem);
    }
  }

  org.eclipse.swt.graphics.Point getCenteredOrigin() {
    final Rectangle rect = image.getBounds();
    final Rectangle client = canvas.getClientArea();
    final int zeroX = client.x + client.width / 2 - rect.width / 2;
    final int zeroY = client.y + client.height / 2 - rect.height / 2;
    return new org.eclipse.swt.graphics.Point(origin.x + zeroX, origin.y
      + zeroY);
  }

  void updateScrollbars(boolean adaptToScrollbar) {
    final Rectangle rect = image.getBounds();
    final Rectangle client = canvas.getClientArea();

    hBar.setMaximum(rect.width);
    vBar.setMaximum(rect.height);
    hBar.setThumb(Math.min(rect.width, client.width));
    vBar.setThumb(Math.min(rect.height, client.height));
    if (!adaptToScrollbar) {
      final org.eclipse.swt.graphics.Point center = getCenteredOrigin();
      hBar.setSelection(-center.x);
      vBar.setSelection(-center.y);
    }
  }

  private void calculateSizes() {
    double minX = Double.POSITIVE_INFINITY;
    double maxX = Double.NEGATIVE_INFINITY;
    double minY = Double.POSITIVE_INFINITY;
    double maxY = Double.NEGATIVE_INFINITY;

    boolean isDefined = false;
    for (final CanvasRenderer r : canvasRenderers) {
      final Optional<ViewRect> rect = r.getViewRect();
      if (rect.isPresent()) {
        minX = Math.min(minX, rect.get().min.x);
        maxX = Math.max(maxX, rect.get().max.x);
        minY = Math.min(minY, rect.get().min.y);
        maxY = Math.max(maxY, rect.get().max.y);
        isDefined = true;
      }
    }

    checkState(
      isDefined,
      "none of the available renderers implements getViewRect(), known "
        + "renderers: %s",
      canvasRenderers);

    viewRect = new ViewRect(new Point(minX, minY), new Point(maxX, maxY));

    final Rectangle area = canvas.getClientArea();
    if (viewRect.width > viewRect.height) {
      m = area.width / viewRect.width;
    } else {
      m = area.height / viewRect.height;
    }
    zoomRatio = 1;
  }

  @Override
  public void controlMoved(ControlEvent e) {}

  @Override
  public void controlResized(ControlEvent e) {
    if (image != null) {
      updateScrollbars(true);
      scrollHorizontal();
      scrollVertical();
      canvas.redraw();
    }
  }

  @Override
  public void widgetSelected(SelectionEvent e) {
    if (e.widget == vBar) {
      scrollVertical();
    } else {
      scrollHorizontal();
    }
  }

  void scrollVertical() {
    final org.eclipse.swt.graphics.Point center = getCenteredOrigin();
    final Rectangle content = image.getBounds();
    final Rectangle client = canvas.getClientArea();
    if (client.height > content.height) {
      origin.y = 0;
    } else {
      final int vSelection = vBar.getSelection();
      final int destY = -vSelection - center.y;
      canvas.scroll(center.x, destY, center.x, center.y, content.width,
        content.height, false);
      origin.y = -vSelection + origin.y - center.y;
    }
  }

  void scrollHorizontal() {
    final org.eclipse.swt.graphics.Point center = getCenteredOrigin();
    final Rectangle content = image.getBounds();
    final Rectangle client = canvas.getClientArea();
    if (client.width > content.width) {
      origin.x = 0;
    } else {
      final int hSelection = hBar.getSelection();
      final int destX = -hSelection - center.x;
      canvas.scroll(destX, center.y, center.x, center.y, content.width,
        content.height, false);
      origin.x = -hSelection + origin.x - center.x;
    }
  }

  @Override
  public void widgetDefaultSelected(SelectionEvent e) {}

  @Override
  public void tick(TimeLapse timeLapse) {}

  @Override
  public void afterTick(final TimeLapse timeLapse) {
    if (clock.isTicking()
      // when in realtime mode ignore the gui speed up
      && !(isRealtime && ((RealtimeClockController) clock)
        .getClockMode() == ClockMode.REAL_TIME)
      && lastRefresh + timeLapse.getTickLength() * speedUp > timeLapse
        .getStartTime()) {
      return;
    }
    lastRefresh = timeLapse.getStartTime();
    // TODO sleep should be relative to speedUp as well?
    if (!isRealtime) {
      try {
        Thread.sleep(SLEEP_MS);
      } catch (final InterruptedException e) {
        throw new RuntimeException(e);
      }
      if (display.isDisposed()) {
        return;
      }
    }
    display.syncExec(new Runnable() {
      @Override
      public void run() {
        if (!canvas.isDisposed()) {
          if (clock.getTickLength() > TIME_FORMATTER_THRESHOLD) {
            final StringBuilder sb = new StringBuilder();
            sb.append(FORMATTER.print(new Period(0, clock.getCurrentTime())));

            if (isRealtime) {
              sb.append(SPACE);
              sb.append(
                ((RealtimeClockController) clock).getClockMode().name());
            }
            timeLabel.setText(sb.toString());
          } else {
            timeLabel.setText(Long.toString(clock.getCurrentTime()));
          }
          timeLabel.pack();
          canvas.redraw();
        }
      }
    });
  }

  @Override
  public boolean register(Renderer element) {
    if (element instanceof PanelRenderer) {
      panelRenderers.add((PanelRenderer) element);
    }
    if (element instanceof CanvasRenderer) {
      canvasRenderers.add((CanvasRenderer) element);
    }
    return true;
  }

  @Override
  public boolean unregister(Renderer element) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Class<Renderer> getSupportedType() {
    return Renderer.class;
  }

  @Override
  public void registerModelProvider(ModelProvider mp) {
    modelProvider = mp;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <U> U get(Class<U> clazz) {
    if (clazz == RenderController.class) {
      return (U) this;
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public void requestStaticRenderUpdate() {
    requestStaticRenderUpdate = true;
  }

  static Builder builder(View.Builder vb) {
    return new AutoValue_SimulationViewer_Builder(vb);
  }

  @AutoValue
  abstract static class Builder extends
      AbstractModelBuilder<SimulationViewer, Renderer> {

    Builder() {
      setDependencies(Shell.class, ClockController.class, SimulatorAPI.class,
        MainView.class);
      setProvidingTypes(RenderController.class);
    }

    abstract View.Builder viewBuilder();

    @Override
    public SimulationViewer build(DependencyProvider dependencyProvider) {
      final Shell shell = dependencyProvider.get(Shell.class);
      final ClockController cc = dependencyProvider.get(ClockController.class);
      final SimulatorAPI sim = dependencyProvider.get(SimulatorAPI.class);
      final MainView mv = dependencyProvider.get(MainView.class);
      final SimulationViewer sv = new SimulationViewer(shell, cc, sim,
        viewBuilder());
      mv.addListener(new com.github.rinde.rinsim.event.Listener() {
        @Override
        public void handleEvent(com.github.rinde.rinsim.event.Event e) {
          sv.show();
        }
      });
      return sv;
    }
  }

}
