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
package com.github.rinde.rinsim.examples.demo;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomAdaptor;
import org.apache.commons.math3.random.RandomGenerator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.TickListener;
import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.model.ModelProvider;
import com.github.rinde.rinsim.core.model.ModelReceiver;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.PlaneRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.ModelRenderer;
import com.github.rinde.rinsim.ui.renderers.PanelRenderer;
import com.github.rinde.rinsim.ui.renderers.PlaneRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.ViewPort;
import com.github.rinde.rinsim.ui.renderers.ViewRect;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

/**
 * @author Rinde van Lon
 *
 */
public class SwarmDemo {

  private static final int FONT_SIZE = 12;

  /**
   * Starts the demo.
   * @param args No args.
   */
  public static void main(String[] args) {
    final String string = "AgentWise";
    final List<Point> points = measureString(string, FONT_SIZE, 30, 0);

    final RandomGenerator rng = new MersenneTwister(123);
    final Simulator sim = Simulator.builder()
        .addModel(PlaneRoadModel.builder()
            .setMinPoint(new Point(0, 0))
            .setMaxPoint(new Point(4500, 1200))
            .setDistanceUnit(SI.METER)
            .setSpeedUnit(NonSI.KILOMETERS_PER_HOUR)
            .setMaxSpeed(1000d)
            .build())
        .build();

    for (final Point p : points) {
      sim.register(new Vehicle(p, rng));
    }
    View.create(sim)
        .with(PlaneRoadModelRenderer.create(), new VehicleRenderer(),
            new DemoPanel(string, rng))
        .setSpeedUp(8)
        .show();
  }

  /**
   * Measures the specified string using the specified font size.
   * @param string The string to measure.
   * @param fontSize The fontsize to use.
   * @param spacing The spacing between the points.
   * @param vCorrection The vertical correction for all the points.
   * @return A list of points, each point represents a pixel in the measured
   *         string.
   */
  public static ImmutableList<Point> measureString(String string, int fontSize,
      double spacing, int vCorrection) {
    if (string.trim().isEmpty()) {
      return ImmutableList.of();
    }
    final String stringToDraw = string;

    Display display = Display.getDefault();// getCurrent();
    boolean haveToDispose = false;
    if (display == null) {
      display = new Display();
      haveToDispose = true;
    }

    final GC measureGC = new GC(new Image(display, 100, 10));
    final Font initialFont = measureGC.getFont();
    final FontData[] fontData = initialFont.getFontData();
    for (int i = 0; i < fontData.length; i++) {
      fontData[i].setHeight(fontSize);
    }
    final Font newFont = new Font(display, fontData);
    measureGC.setFont(newFont);

    final org.eclipse.swt.graphics.Point extent = measureGC
        .textExtent(stringToDraw);
    measureGC.dispose();

    final Image image = new Image(display, extent.x, extent.y);
    final GC gc = new GC(image);
    gc.setFont(newFont);

    gc.setForeground(new Color(display, new RGB(0, 0, 0)));
    gc.drawText(stringToDraw, 0, 0);

    final ImmutableList.Builder<Point> coordinateBuilder = ImmutableList
        .builder();
    final int white = (int) Math.pow(2, 24) / 2 - 1;
    for (int i = 0; i < image.getBounds().width; i++) {
      for (int j = vCorrection; j < image.getBounds().height; j++) {
        final int color = image.getImageData().getPixel(i, j);
        if (color < white) {
          coordinateBuilder.add(new Point(i * spacing, (j - vCorrection)
              * spacing));
        }
      }
    }
    final ImmutableList<Point> points = coordinateBuilder.build();

    image.dispose();
    if (haveToDispose) {
      display.dispose();
    }
    return points;
  }

  static final class Vehicle implements MovingRoadUser, TickListener {
    Point destPos;
    RandomGenerator rng;

    private Optional<RoadModel> rm;
    double speed;
    boolean active;

    Vehicle(Point p, RandomGenerator r) {
      destPos = p;
      rng = r;
      active = true;
      rm = Optional.absent();
    }

    @Override
    public void initRoadUser(RoadModel model) {
      final Point startPos = model.getRandomPosition(rng);
      model.addObjectAt(this, startPos);

      rm = Optional.of(model);
      setDestination(destPos);
    }

    @Override
    public double getSpeed() {
      return speed;
    }

    @Override
    public void tick(TimeLapse timeLapse) {
      rm.get().moveTo(this, destPos, timeLapse);
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {}

    /**
     * Change the destination of the vehicle.
     * @param dest The new destination.
     */
    public void setDestination(Point dest) {
      active = true;
      doSetDestination(dest);
    }

    private void doSetDestination(Point dest) {
      destPos = dest;
      speed = Point.distance(rm.get().getPosition(this), destPos)
          / (30 + 40 * rng.nextDouble());
    }

    /**
     * Moves the vehicle to a point on the border of the plane.
     */
    public void setInactive() {
      if (active) {
        active = false;
        final List<Point> bounds = rm.get().getBounds();
        final Point p = rm.get().getRandomPosition(rng);
        if (rng.nextBoolean()) {
          doSetDestination(new Point(bounds.get(rng.nextInt(2)).x, p.y));
        } else {
          doSetDestination(new Point(p.x, bounds.get(rng.nextInt(2)).y));
        }
      }
    }
  }

  static class DemoPanel implements PanelRenderer, Listener, ModelReceiver {
    Optional<RoadModel> rm;
    Set<Vehicle> vehicles;

    final String startString;
    RandomGenerator rng;

    DemoPanel(String s, RandomGenerator r) {
      startString = s;
      rng = r;
      vehicles = newHashSet();
      rm = Optional.absent();
    }

    @Override
    public void initializePanel(Composite parent) {
      final FillLayout rl = new FillLayout();
      parent.setLayout(rl);
      final Text t = new Text(parent, SWT.SINGLE | SWT.ICON_CANCEL | SWT.CANCEL);
      t.setText(startString);

      final int chars = 30;
      final GC gc = new GC(t);
      final FontMetrics fm = gc.getFontMetrics();
      final int width = chars * fm.getAverageCharWidth();
      final int height = fm.getHeight();
      gc.dispose();
      t.setSize(t.computeSize(width, height));
      t.addListener(SWT.DefaultSelection, this);
      t.addListener(SWT.Modify, this);

    }

    @Override
    public int preferredSize() {
      return 30;
    }

    @Override
    public int getPreferredPosition() {
      return SWT.TOP;
    }

    @Override
    public String getName() {
      return "Demo";
    }

    @Override
    public void handleEvent(@Nullable Event event) {
      checkNotNull(event);
      final Iterator<Point> points = measureString(
          ((Text) event.widget).getText(), FONT_SIZE, 30d, 0).iterator();
      final List<Vehicle> vs = newArrayList(vehicles);
      if (event.type == SWT.DefaultSelection) {
        Collections.shuffle(vs, new RandomAdaptor(rng));
      }
      for (final Vehicle v : vs) {
        if (points.hasNext()) {
          v.setDestination(points.next());
        } else {
          v.setInactive();
        }
      }
    }

    @Override
    public void registerModelProvider(ModelProvider mp) {
      rm = Optional.fromNullable(mp.tryGetModel(RoadModel.class));
      vehicles = rm.get().getObjectsOfType(Vehicle.class);
    }

    @Override
    public void render() {}
  }

  static final class VehicleRenderer implements ModelRenderer {
    private Optional<RoadModel> rm;

    VehicleRenderer() {
      rm = Optional.absent();
    }

    @Override
    public void renderDynamic(GC gc, ViewPort vp, long time) {
      final int radius = 2;
      gc.setBackground(new Color(gc.getDevice(), 255, 0, 0));
      final Map<RoadUser, Point> objects = rm.get().getObjectsAndPositions();
      synchronized (objects) {
        for (final Entry<RoadUser, Point> entry : objects.entrySet()) {
          final Point p = entry.getValue();
          gc.fillOval((int) (vp.origin.x + (p.x - vp.rect.min.x) * vp.scale)
              - radius, (int) (vp.origin.y + (p.y - vp.rect.min.y) * vp.scale)
              - radius, 2 * radius, 2 * radius);
        }
      }
    }

    @Override
    public void renderStatic(GC gc, ViewPort vp) {}

    @Nullable
    @Override
    public ViewRect getViewRect() {
      return null;
    }

    @Override
    public void registerModelProvider(ModelProvider mp) {
      rm = Optional.fromNullable(mp.tryGetModel(RoadModel.class));
    }
  }

}
