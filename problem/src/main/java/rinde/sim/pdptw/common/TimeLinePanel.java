/**
 * 
 */
package rinde.sim.pdptw.common;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;

import java.math.RoundingMode;
import java.util.List;

import javax.annotation.Nullable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ScrollBar;

import rinde.sim.core.TickListener;
import rinde.sim.core.TimeLapse;
import rinde.sim.core.model.ModelProvider;
import rinde.sim.core.model.ModelReceiver;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.PDPModel.PDPModelEventType;
import rinde.sim.core.model.pdp.PDPModelEvent;
import rinde.sim.core.model.pdp.Parcel;
import rinde.sim.event.Event;
import rinde.sim.event.Listener;
import rinde.sim.ui.renderers.PanelRenderer;
import rinde.sim.util.TimeFormatter;

import com.google.common.math.DoubleMath;

/**
 * Time line panel is an UI element that shows a real time visualization of
 * parcels and their time windows in a simulation.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public final class TimeLinePanel implements ModelReceiver, PanelRenderer,
    TickListener {

  private static final boolean IS_MAC_OR_WINDOWS;
  static {
    final String name = System.getProperty("os.name").toLowerCase();
    IS_MAC_OR_WINDOWS = name.contains("win") || name.contains("mac");
  }

  Canvas canvas;
  Canvas barCanvas;
  Point origin = new Point(0, 0);
  ScrollBar hBar;
  ScrollBar vBar;
  long currentTime = 0;
  Timeline timeline;

  public TimeLinePanel() {}

  @Override
  public void registerModelProvider(ModelProvider mp) {
    final PDPModel pdp = mp.getModel(PDPModel.class);
    checkState(pdp != null, "PDPModel is required.");
    pdp.getEventAPI().addListener(new Listener() {
      @Override
      public void handleEvent(Event e) {
        if (e.getEventType() == PDPModelEventType.NEW_PARCEL) {
          final PDPModelEvent event = (PDPModelEvent) e;
          timeline.addParcel(new ParcelInfo(event.time, event.parcel));
        }
      }
    }, PDPModelEventType.NEW_PARCEL);
  }

  @Override
  public void initializePanel(Composite parent) {
    final TimelineBar timelineBar = new TimelineBar(parent.getDisplay());
    timeline = new Timeline(parent.getDisplay());

    final GridLayout layout = new GridLayout(1, false);
    layout.marginHeight = 2;
    layout.marginWidth = 2;
    layout.verticalSpacing = 0;
    parent.setLayout(layout);
    barCanvas = new Canvas(parent, SWT.NONE);
    final GridData barData = new GridData(SWT.FILL, SWT.TOP, true, false);
    barData.minimumHeight = 22;
    barData.heightHint = 22;
    barCanvas.setLayoutData(barData);
    barCanvas.setSize(20, 20);
    barCanvas.addPaintListener(new PaintListener() {
      @Override
      public void paintControl(PaintEvent e) {
        timelineBar.update(timeline.getWidth());
        e.gc.drawImage(timelineBar.contents, origin.x, 0);
        e.gc.setForeground(e.display.getSystemColor(SWT.COLOR_RED));
        e.gc.drawLine(origin.x + (int) (currentTime / timeline.timePerPixel),
            10, origin.x + (int) (currentTime / timeline.timePerPixel),
            barCanvas.getClientArea().height);
      }
    });
    canvas = new Canvas(parent, SWT.DOUBLE_BUFFERED | SWT.NONE | SWT.V_SCROLL
        | SWT.H_SCROLL);
    canvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    canvas.addPaintListener(new PaintListener() {

      @Override
      public void paintControl(PaintEvent e) {
        final int timeX = origin.x + DoubleMath.roundToInt(currentTime
            / timeline.timePerPixel, RoundingMode.HALF_UP);
        timeline.update(timeX);

        e.gc.drawImage(timeline.contents, origin.x, origin.y);
        e.gc.setForeground(e.display.getSystemColor(SWT.COLOR_RED));
        e.gc.drawLine(timeX, 0, timeX,
            canvas.getClientArea().height);

        hBar.setMaximum(timeline.getWidth() == 0 ? 1 : timeline.getWidth() + 20);
        vBar.setMaximum(timeline.getHeight() + 5);
        hBar.setThumb(Math.min(timeline.getWidth() + 20,
            canvas.getClientArea().width));
        vBar.setThumb(Math.min(timeline.getHeight() + 5,
            canvas.getClientArea().height));
      }
    });

    hBar = canvas.getHorizontalBar();
    hBar.setIncrement(2);
    hBar.setPageIncrement(20);
    hBar.addListener(SWT.Selection, new org.eclipse.swt.widgets.Listener() {
      @Override
      public void handleEvent(org.eclipse.swt.widgets.Event e) {
        final int hSelection = hBar.getSelection();
        final int destX = -hSelection - origin.x;
        canvas.scroll(destX, 0, 0, 0, timeline.getWidth(),
            timeline.getHeight(), false);
        barCanvas.scroll(destX, 0, 0, 0,
            timelineBar.contents.getBounds().width,
            timelineBar.contents.getBounds().height, false);
        origin.x = -hSelection;
      }
    });
    vBar = canvas.getVerticalBar();
    vBar.setIncrement(2);
    vBar.setPageIncrement(20);
    vBar.addListener(SWT.Selection, new org.eclipse.swt.widgets.Listener() {
      @Override
      public void handleEvent(@Nullable org.eclipse.swt.widgets.Event e) {
        final int vSelection = vBar.getSelection();
        final int destY = -vSelection - origin.y;
        canvas.scroll(0, destY, 0, 0, timeline.getWidth(),
            timeline.getHeight(), false);
        origin.y = -vSelection;
      }
    });
    canvas.redraw();
    barCanvas.redraw();
  }

  @Override
  public int preferredSize() {
    return 200;
  }

  @Override
  public int getPreferredPosition() {
    return SWT.TOP;
  }

  @Override
  public String getName() {
    return "Timeline";
  }

  @Override
  public void tick(TimeLapse timeLapse) {}

  @Override
  public void afterTick(TimeLapse timeLapse) {
    currentTime = timeLapse.getStartTime();
  }

  @Override
  public void render() {
    if (canvas.isDisposed()) {
      return;
    }
    canvas.getDisplay().syncExec(new Runnable() {
      @Override
      public void run() {
        if (!canvas.isDisposed()) {
          canvas.redraw();
          barCanvas.redraw();
        }
      }
    });
  }

  static Image createNewTransparentImg(Display d, int w, int h) {
    final Color bg = d.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
    final PaletteData palette = new PaletteData(new RGB[] { bg.getRGB() });
    final ImageData sourceData = new ImageData(w, h, 1, palette);
    if (IS_MAC_OR_WINDOWS) {
      sourceData.transparentPixel = 0;
    }
    return new Image(d, sourceData);
  }

  class ParcelInfo {
    final long eventTime;
    final Parcel parcel;

    ParcelInfo(long time, Parcel p) {
      eventTime = time;
      parcel = p;
    }
  }

  class TimelineBar {
    protected final Display display;
    protected Image contents;
    protected Font font;

    TimelineBar(Display d) {
      display = d;
      contents = createNewTransparentImg(display, 700, 20);
      font = new Font(display, "arial", 10, SWT.NORMAL);
      drawTimeline();
    }

    void update(int width) {
      if (contents.getBounds().width < width) {
        contents.dispose();
        contents = createNewTransparentImg(display, width + 30, 20);
        drawTimeline();
      }
    }

    void drawTimeline() {
      final GC gc = new GC(contents);
      gc.setAdvanced(true);
      gc.setTextAntialias(SWT.OFF);

      final int large = 600000 / 15000;
      final int small = large / 5;
      for (int i = 0; i < contents.getBounds().width; i += small) {
        final int height = i % large == 0 ? 10 : 5;
        if (i % large == 0) {
          String time = TimeFormatter.format(15000 * i);
          time = time.substring(0, time.length() - 3);
          gc.setFont(font);
          final Point size = gc.textExtent(time);
          gc.drawText(time, i - size.x / 2, 0, true);
        }
        gc.drawLine(i, 20 - height, i, 20);
      }
      gc.dispose();
    }
  }

  class Timeline {
    @Nullable
    Display display;
    @Nullable
    Image contents;
    List<ParcelInfo> parcels;
    List<ParcelInfo> newParcels;
    long timePerPixel = 15000;

    int rowHeight = 15;
    private int height;
    int width;

    Color lineColor;
    Color pickupColor;
    Color deliveryColor;
    Color gridColor;

    Timeline(Display d) {
      display = d;
      parcels = newArrayList();
      newParcels = newArrayList();
      lineColor = d.getSystemColor(SWT.COLOR_WIDGET_BORDER);
      pickupColor = d.getSystemColor(SWT.COLOR_BLUE);
      deliveryColor = d.getSystemColor(SWT.COLOR_DARK_RED);
      gridColor = d.getSystemColor(SWT.COLOR_GRAY);
    }

    void ensureImg() {
      if (contents == null) {
        contents = createNewTransparentImg(display, 700, 100);
        final GC gc = new GC(contents);
        drawGrid(gc, 700, 100);
        gc.dispose();
      } else {
        final boolean widthViolation = width > contents.getBounds().width;
        final boolean heightViolation = height > contents.getBounds().height;
        if (widthViolation || heightViolation) {
          final int newWidth = Math.max(width, contents.getBounds().width)
              + (widthViolation ? (int) (60 * 60000 / timePerPixel) : 0);
          final int newHeight = contents.getBounds().height
              + (heightViolation ? 10 * rowHeight : 0);

          final Image newContents = createNewTransparentImg(display, newWidth,
              newHeight);
          // copy previous image to new image
          final GC gc = new GC(newContents);
          // draw vertical grid lines
          drawGrid(gc, newWidth, newHeight);
          gc.drawImage(contents, 0, 0);

          gc.dispose();
          contents.dispose();
          contents = newContents;
        }
      }
    }

    void drawGrid(GC gc, int w, int h) {
      for (int i = 0; i < w; i += 40) {
        gc.setForeground(gridColor);
        gc.drawLine(i, 0, i, h);
      }
    }

    void addParcel(final ParcelInfo p) {
      newParcels.add(p);

      width = Math.max(width,
          (int) (p.parcel.getDeliveryTimeWindow().end / timePerPixel));
    }

    void update(int timeX) {
      if (display == null) {
        return;
      }
      final int oldHeight = height;

      // make copy to avoid concurrency problems
      final List<ParcelInfo> copyNewParcels = newArrayList(newParcels);
      newParcels.clear();

      parcels.addAll(copyNewParcels);
      height = rowHeight + parcels.size() * rowHeight;
      width = Math.max(width, timeX);
      ensureImg();
      for (int i = 0; i < copyNewParcels.size(); i++) {
        drawParcel(copyNewParcels.get(i), -rowHeight + oldHeight + i
            * rowHeight);
      }
    }

    void drawParcel(ParcelInfo p, int y) {
      final int startX = (int) (p.eventTime / timePerPixel);
      final int startPickX = (int) (p.parcel.getPickupTimeWindow().begin / timePerPixel);
      final int endPickX = (int) (p.parcel.getPickupTimeWindow().end / timePerPixel);
      final int startDelX = (int) (p.parcel.getDeliveryTimeWindow().begin / timePerPixel);
      final int endDelX = (int) (p.parcel.getDeliveryTimeWindow().end / timePerPixel);

      final GC gc = new GC(contents);
      gc.setForeground(lineColor);
      gc.drawLine(startX, y + 1, startX, y + 13);
      gc.drawLine(startX, y + 8, startPickX, y + 8);

      gc.setBackground(pickupColor);
      gc.fillRectangle(startPickX, y + 2, Math.max(endPickX - startPickX, 1), 6);
      gc.drawRectangle(startPickX, y + 2, Math.max(endPickX - startPickX, 1), 6);

      gc.drawLine(endPickX, y + 8, startDelX, y + 8);

      gc.setBackground(deliveryColor);
      gc.fillRectangle(startDelX, y + 8, Math.max(endDelX - startDelX, 1), 6);
      gc.drawRectangle(startDelX, y + 8, Math.max(endDelX - startDelX, 1), 6);

      gc.dispose();
    }

    int getHeight() {
      return height;
    }

    int getWidth() {
      return width;
    }

    void dispose() {
      contents.dispose();
    }
  }

}
