/*
 * Copyright (C) 2011-2015 Rinde van Lon, iMinds-DistriNet, KU Leuven
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
package com.github.rinde.rinsim.pdptw.common;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Verify.verify;
import static com.google.common.base.Verify.verifyNotNull;
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
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.Model.AbstractModelVoid;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.PDPModel.PDPModelEventType;
import com.github.rinde.rinsim.core.model.pdp.PDPModelEvent;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.Listener;
import com.github.rinde.rinsim.ui.renderers.PanelRenderer;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.auto.value.AutoValue;
import com.google.common.base.Optional;
import com.google.common.math.DoubleMath;

/**
 * Time line panel is an UI element that shows a real time visualization of
 * parcels and their time windows in a simulation.
 * @author Rinde van Lon
 */
public final class TimeLinePanel extends AbstractModelVoid implements
    PanelRenderer, TickListener {

  static final PeriodFormatter FORMATTER = new PeriodFormatterBuilder()
      .minimumPrintedDigits(2)
      .printZeroAlways()
      .appendHours()
      .appendLiteral(":")
      .appendMinutes()
      .toFormatter();

  static final int PANEL_PX = 200;
  static final int MARGIN_PX = 2;
  static final int BAR_HEIGHT_PX = 22;
  static final int WIDTH_PX = 700;
  static final int FONT_SIZE = 10;
  static final long TIME_PER_PIXEL = 15000;
  static final int V_THUMB_SIZE = 5;
  static final int H_THUMB_SIZE = 20;

  static final int SCROLL_INCR = 2;
  static final int SCROLL_PAGE_INCR = 20;

  private static final boolean IS_MAC_OR_WINDOWS;

  static {
    final String name = System.getProperty("os.name").toLowerCase();
    IS_MAC_OR_WINDOWS = name.contains("win") || name.contains("mac");
  }

  Optional<Canvas> canvas;
  Optional<Canvas> barCanvas;
  Point origin = new Point(0, 0);
  long currentTime;

  final PDPModel pdpModel;

  TimeLinePanel(PDPModel pm) {
    pdpModel = pm;
    canvas = Optional.absent();
    barCanvas = Optional.absent();
  }

  @Override
  public void initializePanel(Composite parent) {
    final TimelineBar timelineBar = new TimelineBar(parent.getDisplay());
    final Timeline timeline = new Timeline(parent.getDisplay());
    pdpModel.getEventAPI().addListener(new Listener() {
      @Override
      public void handleEvent(Event e) {
        if (e.getEventType() == PDPModelEventType.NEW_PARCEL) {
          verify(e instanceof PDPModelEvent);

          final PDPModelEvent event = (PDPModelEvent) e;
          timeline.addParcel(new ParcelInfo(event.time,
              verifyNotNull(event.parcel)));
        }
      }
    }, PDPModelEventType.NEW_PARCEL);

    final GridLayout layout = new GridLayout(1, false);
    layout.marginHeight = MARGIN_PX;
    layout.marginWidth = MARGIN_PX;
    layout.verticalSpacing = 0;
    parent.setLayout(layout);
    barCanvas = Optional.of(new Canvas(parent, SWT.NONE));
    final GridData barData = new GridData(SWT.FILL, SWT.TOP, true, false);
    barData.minimumHeight = BAR_HEIGHT_PX;
    barData.heightHint = BAR_HEIGHT_PX;
    barCanvas.get().setLayoutData(barData);
    barCanvas.get().addPaintListener(new PaintListener() {
      @Override
      public void paintControl(@Nullable PaintEvent e) {
        assert e != null;
        timelineBar.update(timeline.getWidth());
        e.gc.setBackground(
          e.display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        e.gc.fillRectangle(0, 0,
          barCanvas.get().getClientArea().width,
          barCanvas.get().getClientArea().height);

        e.gc.drawImage(timelineBar.contents, origin.x, 0);
        e.gc.setForeground(e.display.getSystemColor(SWT.COLOR_RED));
        e.gc.drawLine(origin.x + (int) (currentTime / TIME_PER_PIXEL),
          FONT_SIZE, origin.x + (int) (currentTime / TIME_PER_PIXEL),
          barCanvas.get().getClientArea().height);
      }
    });
    canvas = Optional.of(new Canvas(parent, SWT.DOUBLE_BUFFERED | SWT.NONE
        | SWT.V_SCROLL | SWT.H_SCROLL));
    final ScrollBar hBar = canvas.get().getHorizontalBar();
    final ScrollBar vBar = canvas.get().getVerticalBar();

    canvas.get().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    canvas.get().addPaintListener(new PaintListener() {
      @Override
      public void paintControl(@Nullable PaintEvent e) {
        assert e != null;
        final int timeX = DoubleMath.roundToInt(origin.x + currentTime
            / TIME_PER_PIXEL,
          RoundingMode.HALF_UP);

        final int height = timeline.getHeight();
        timeline.update(timeX);

        final boolean shouldScroll = timeline.getHeight() > height
            && vBar.getMaximum() == vBar.getSelection() + vBar.getThumb();

        e.gc.setBackground(
          e.display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        e.gc.fillRectangle(0, 0,
          canvas.get().getClientArea().width,
          canvas.get().getClientArea().height);
        e.gc.drawImage(timeline.contents.get(), origin.x, origin.y);
        e.gc.setForeground(e.display.getSystemColor(SWT.COLOR_RED));
        e.gc.drawLine(timeX, 0, timeX, canvas.get().getClientArea().height);
        hBar.setMaximum(timeline.getWidth() == 0 ? 1 : timeline.getWidth()
            + H_THUMB_SIZE);
        vBar.setMaximum(timeline.getHeight() + V_THUMB_SIZE);
        hBar.setThumb(Math.min(timeline.getWidth() + H_THUMB_SIZE, canvas.get()
            .getClientArea().width));
        vBar.setThumb(Math.min(timeline.getHeight() + V_THUMB_SIZE, canvas
            .get().getClientArea().height));

        // if view is currently scrolled down, automatically scroll down when
        // view is expanded downward (similar to the behavior of a terminal)
        if (shouldScroll) {
          vBar.setSelection(vBar.getMaximum());
          final int vSelection = vBar.getSelection();
          final int destY = -vSelection - origin.y;
          canvas.get().scroll(0, destY, 0, 0, timeline.getWidth(),
            timeline.getHeight(), false);
          origin.y = -vSelection;
        }
      }
    });

    hBar.setIncrement(SCROLL_INCR);
    hBar.setPageIncrement(SCROLL_PAGE_INCR);
    hBar.addListener(SWT.Selection, new org.eclipse.swt.widgets.Listener() {
      @Override
      public void handleEvent(@Nullable org.eclipse.swt.widgets.Event e) {
        final int hSelection = hBar.getSelection();
        final int destX = -hSelection - origin.x;
        canvas.get().scroll(destX, 0, 0, 0, timeline.getWidth(),
          timeline.getHeight(), false);
        barCanvas.get().scroll(destX, 0, 0, 0,
          timelineBar.contents.getBounds().width,
          timelineBar.contents.getBounds().height, false);
        origin.x = -hSelection;
      }
    });
    vBar.setIncrement(SCROLL_INCR);
    vBar.setPageIncrement(SCROLL_PAGE_INCR);
    vBar.addListener(SWT.Selection, new org.eclipse.swt.widgets.Listener() {
      @Override
      public void handleEvent(@Nullable org.eclipse.swt.widgets.Event e) {
        final int vSelection = vBar.getSelection();
        final int destY = -vSelection - origin.y;
        canvas.get().scroll(0, destY, 0, 0, timeline.getWidth(),
          timeline.getHeight(), false);
        origin.y = -vSelection;
      }
    });
    canvas.get().redraw();
    barCanvas.get().redraw();
  }

  @Override
  public int preferredSize() {
    return PANEL_PX;
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
    checkState(canvas.isPresent());
    if (canvas.get().isDisposed()) {
      return;
    }
    canvas.get().getDisplay().syncExec(new Runnable() {
      @Override
      public void run() {
        if (!canvas.get().isDisposed()) {
          canvas.get().redraw();
          barCanvas.get().redraw();
        }
      }
    });
  }

  static Image createNewTransparentImg(Display d, int w, int h) {
    final Color bg = d.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
    final PaletteData palette = new PaletteData(new RGB[] {bg.getRGB()});
    final ImageData sourceData = new ImageData(w, h, 1, palette);
    if (IS_MAC_OR_WINDOWS) {
      sourceData.transparentPixel = 0;
    }
    return new Image(d, sourceData);
  }

  /**
   * @return A new {@link Builder}.
   */
  public static Builder builder() {
    return new AutoValue_TimeLinePanel_Builder();
  }

  /**
   * Builder for {@link TimeLinePanel}.
   * @author Rinde van Lon
   */
  @AutoValue
  public abstract static class Builder extends
      AbstractModelBuilder<TimeLinePanel, Void> {

    Builder() {
      setDependencies(PDPModel.class);
    }

    @Override
    public TimeLinePanel build(DependencyProvider dependencyProvider) {
      return new TimeLinePanel(dependencyProvider.get(PDPModel.class));
    }
  }

  static class ParcelInfo {
    final long eventTime;
    final Parcel parcel;

    ParcelInfo(long time, Parcel p) {
      eventTime = time;
      parcel = p;
    }
  }

  static class TimelineBar {
    static final int LARGE_TICK_HEIGHT = 10;
    static final int SMALL_TICK_HEIGHT = 5;
    static final int LARGE_TICK_DIST = 40;
    static final int SMALL_TICK_DIST = 8;
    static final int TL_BAR_HEIGHT_PX = 20;
    static final int ADDITIONAL_WIDTH = 30;

    protected final Display display;
    protected Image contents;
    protected Font font;

    TimelineBar(Display d) {
      display = d;
      contents = createNewTransparentImg(display, WIDTH_PX, TL_BAR_HEIGHT_PX);
      font = new Font(display, "arial", FONT_SIZE, SWT.NORMAL);
      drawTimeline();
    }

    void update(int width) {
      if (contents.getBounds().width < width) {
        contents.dispose();
        contents = createNewTransparentImg(display, width + ADDITIONAL_WIDTH,
          TL_BAR_HEIGHT_PX);
        drawTimeline();
      }
    }

    final void drawTimeline() {
      final GC gc = new GC(contents);

      gc.setBackground(display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
      gc.fillRectangle(0, 0, contents.getBounds().width,
        contents.getBounds().height);

      gc.setAdvanced(true);
      gc.setTextAntialias(SWT.ON);

      for (int i = 0; i < contents.getBounds().width; i += SMALL_TICK_DIST) {
        final int height = i % LARGE_TICK_DIST == 0 ? LARGE_TICK_HEIGHT
            : SMALL_TICK_HEIGHT;
        if (i % LARGE_TICK_DIST == 0) {

          final String time = FORMATTER
              .print(new Period(0L, TIME_PER_PIXEL * i));
          gc.setFont(font);
          final Point size = gc.textExtent(time);
          gc.drawText(time, i - size.x / 2, 0, true);
        }
        gc.drawLine(i, TL_BAR_HEIGHT_PX - height, i, TL_BAR_HEIGHT_PX);
      }
      gc.dispose();
    }
  }

  static class Timeline {
    static final int ROW_HEIGHT = 15;
    static final int VERTICAL_DIST = 40;
    static final long HOUR = 60 * 60 * 1000;
    static final int START_HEIGHT = 100;
    static final int ADDITIONAL_HEIGHT_FACTOR = 10;

    static final int BAR_H = 12;
    static final int HALF_BAR_H = BAR_H / 2;
    static final int BAR_START_OFFSET_Y = 1;
    static final int BAR_END_OFFSET_Y = BAR_H + BAR_START_OFFSET_Y;
    static final int BAR_MIDDLE_OFFSET_Y = 8;

    final Display display;
    Optional<Image> contents;
    List<ParcelInfo> parcels;
    List<ParcelInfo> newParcels;

    Color lineColor;
    Color pickupColor;
    Color deliveryColor;
    Color gridColor;

    int width;
    private int height;

    Timeline(Display d) {
      display = d;
      parcels = newArrayList();
      newParcels = newArrayList();
      lineColor = d.getSystemColor(SWT.COLOR_WIDGET_BORDER);
      pickupColor = d.getSystemColor(SWT.COLOR_BLUE);
      deliveryColor = d.getSystemColor(SWT.COLOR_DARK_RED);
      gridColor = d.getSystemColor(SWT.COLOR_GRAY);
      contents = Optional.absent();
    }

    void ensureImg() {
      if (!contents.isPresent()) {
        contents = Optional.of(createNewTransparentImg(display, WIDTH_PX,
          START_HEIGHT));
        final GC gc = new GC(contents.get());
        drawVerticals(gc, WIDTH_PX, START_HEIGHT);
        gc.dispose();
      } else {
        final boolean wViolation = width > contents.get().getBounds().width;
        final boolean hViolation = height > contents.get().getBounds().height;
        if (wViolation || hViolation) {
          final int newWidth = Math
              .max(width, contents.get().getBounds().width)
              + (wViolation ? (int) (HOUR / TIME_PER_PIXEL) : 0);
          final int newHeight = contents.get().getBounds().height
              + (hViolation ? ADDITIONAL_HEIGHT_FACTOR * ROW_HEIGHT : 0);

          final Image newContents = createNewTransparentImg(display, newWidth,
            newHeight);
          // copy previous image to new image
          final GC gc = new GC(newContents);
          // draw vertical grid lines
          drawVerticals(gc, newWidth, newHeight);
          gc.drawImage(contents.get(), 0, 0);

          gc.dispose();
          contents.get().dispose();
          contents = Optional.of(newContents);
        }
      }
    }

    void drawVerticals(GC gc, int w, int h) {
      for (int i = 0; i < w; i += VERTICAL_DIST) {
        gc.setForeground(gridColor);
        gc.drawLine(i, 0, i, h);
      }
    }

    void addParcel(final ParcelInfo p) {
      newParcels.add(p);

      width = Math.max(width,
        (int) (p.parcel.getDeliveryTimeWindow().end() / TIME_PER_PIXEL));
    }

    void update(int timeX) {
      if (display.isDisposed()) {
        return;
      }
      final int oldHeight = height;

      // make copy to avoid concurrency problems
      final List<ParcelInfo> copyNewParcels;
      synchronized (newParcels) {
        copyNewParcels = newArrayList(newParcels);
        newParcels.clear();
      }

      parcels.addAll(copyNewParcels);
      height = parcels.size() * ROW_HEIGHT;

      width = Math.max(width, timeX);
      ensureImg();
      for (int i = 0; i < copyNewParcels.size(); i++) {
        drawParcel(copyNewParcels.get(i), oldHeight + i * ROW_HEIGHT);
      }
    }

    void drawParcel(ParcelInfo p, int y) {
      final TimeWindow pi = p.parcel.getPickupTimeWindow();
      final TimeWindow de = p.parcel.getDeliveryTimeWindow();

      final int startX = (int) (p.eventTime / TIME_PER_PIXEL);
      final int startPickX = (int) (pi.begin() / TIME_PER_PIXEL);
      final int endPickX = (int) (pi.end() / TIME_PER_PIXEL);
      final int startDelX = (int) (de.begin() / TIME_PER_PIXEL);
      final int endDelX = (int) (de.end() / TIME_PER_PIXEL);

      final GC gc = new GC(contents.get());
      gc.setForeground(lineColor);
      gc.drawLine(startX, y + BAR_START_OFFSET_Y, startX, y
          + BAR_END_OFFSET_Y);
      gc.drawLine(startX, y + BAR_MIDDLE_OFFSET_Y, startPickX, y
          + BAR_MIDDLE_OFFSET_Y);

      gc.setBackground(pickupColor);
      gc.fillRectangle(startPickX, y + 2,
        Math.max(endPickX - startPickX, 1), HALF_BAR_H);
      gc.drawRectangle(startPickX, y + 2,
        Math.max(endPickX - startPickX, 1), HALF_BAR_H);

      gc.drawLine(endPickX, y + BAR_MIDDLE_OFFSET_Y, startDelX, y
          + BAR_MIDDLE_OFFSET_Y);

      gc.setBackground(deliveryColor);
      gc.fillRectangle(startDelX, y + BAR_MIDDLE_OFFSET_Y,
        Math.max(endDelX - startDelX, 1), HALF_BAR_H);
      gc.drawRectangle(startDelX, y + BAR_MIDDLE_OFFSET_Y,
        Math.max(endDelX - startDelX, 1), HALF_BAR_H);

      gc.dispose();
    }

    int getHeight() {
      return height;
    }

    int getWidth() {
      return width;
    }

    void dispose() {
      contents.get().dispose();
    }
  }
}
