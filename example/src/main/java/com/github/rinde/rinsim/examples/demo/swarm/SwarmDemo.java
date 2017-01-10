/*
 * Copyright (C) 2011-2017 Rinde van Lon, imec-DistriNet, KU Leuven
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
package com.github.rinde.rinsim.examples.demo.swarm;

import java.util.List;

import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.PlaneRoadModelRenderer;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

/**
 * @author Rinde van Lon
 *
 */
public final class SwarmDemo {

  static final int FONT_SIZE = 12;
  static final double FONT_SPACING = 30d;
  static final long RANDOM_SEED = 123L;
  static final int SPEED_UP = 8;
  static final Point MIN_POINT = new Point(0, 0);
  static final Point MAX_POINT = new Point(4500, 1200);
  static final double MAX_SPEED = 1000d;
  static final int WHITE_THRESHOLD = (int) Math.pow(2, 24) / 2 - 1;

  static final Rectangle MEASURE_RECT = new Rectangle(0, 0, 100, 10);

  private SwarmDemo() {}

  /**
   * Starts the demo.
   * @param args No args.
   */
  public static void main(String[] args) {
    final String string = "AgentWise";
    final List<Point> points =
      measureString(string, FONT_SIZE, FONT_SPACING, 0);

    final RandomGenerator rng = new MersenneTwister(RANDOM_SEED);
    final Simulator sim = Simulator.builder()
      .addModel(RoadModelBuilders.plane()
        .withMinPoint(MIN_POINT)
        .withMaxPoint(MAX_POINT)
        .withDistanceUnit(SI.METER)
        .withSpeedUnit(NonSI.KILOMETERS_PER_HOUR)
        .withMaxSpeed(MAX_SPEED))
      .addModel(
        View.builder()
          .with(PlaneRoadModelRenderer.builder())
          .with(VehicleRenderer.builder())
          .with(DemoPanel.builder(string))
          .withSpeedUp(SPEED_UP))
      .build();

    for (final Point p : points) {
      sim.register(new Vehicle(p, rng));
    }
    sim.start();
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

    Display display = Display.getDefault();
    boolean haveToDispose = false;
    if (display == null) {
      display = new Display();
      haveToDispose = true;
    }

    final GC measureGC = new GC(new Image(display, MEASURE_RECT));
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
    for (int i = 0; i < image.getBounds().width; i++) {
      for (int j = vCorrection; j < image.getBounds().height; j++) {
        final int color = image.getImageData().getPixel(i, j);
        if (color < WHITE_THRESHOLD) {
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
    static final int LB = 30;
    static final double MUL = 40;

    Point destPos;
    RandomGenerator rng;

    double speed;
    boolean active;
    private Optional<RoadModel> rm;

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
        / (LB + MUL * rng.nextDouble());
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

}
