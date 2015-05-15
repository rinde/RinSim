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
package com.github.rinde.rinsim.examples.demo.factory;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;

import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.annotation.Nullable;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.event.Listener;
import com.github.rinde.rinsim.examples.demo.SwarmDemo;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Graphs;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.MultimapGraph;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.GraphRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import com.github.rinde.rinsim.ui.renderers.UiSchema;
import com.google.common.collect.ImmutableList;
import com.google.common.math.DoubleMath;

/**
 * @author Rinde van Lon
 *
 */
public final class FactoryExample {

  static final double POINT_DISTANCE = 5d;
  static final long SERVICE_DURATION = 120000;
  static final double AGV_SPEED = 10;
  static final int CANVAS_MARGIN = 30;

  // spacing between text pixels
  static final double SPACING = 30d;

  private FactoryExample() {}

  /**
   * Starts the example.
   * @param args One optional argument specifying the simulation end time is
   *          supported.
   */
  public static void main(@Nullable String[] args) {
    final long endTime = args != null && args.length >= 1 ? Long
      .parseLong(args[0]) : Long.MAX_VALUE;

    final Display d = new Display();
    @Nullable
    Monitor sec = null;
    for (final Monitor m : d.getMonitors()) {
      if (d.getPrimaryMonitor() != m) {
        sec = m;
        break;
      }
    }
    System.out.println(sec);

    run(endTime, d, sec, null);
  }

  public static Simulator run(final long endTime, Display display,
    @Nullable Monitor m, @Nullable Listener list) {

    final Rectangle rect;
    if (m != null) {
      if (list != null) {
        rect = m.getClientArea();
      }
      else {// full screen
        rect = m.getBounds();
      }
    } else {
      rect = display.getPrimaryMonitor().getClientArea();
    }

    List<String> WORDS = asList(" BioCo3 \nDistriNet");
    int FONT_SIZE = 10;
    // spacing between vertical lines in line units
    int VERTICAL_LINE_SPACING = 6;
    int NUM_VECHICLES = 12;
    // screen
    if (rect.width == 1920) {
      // WORDS = asList("AgentWise\nKU Leuven", "iMinds\nDistriNet");
      // WORDS = asList("  Agent \n  Wise ", "  Distri \n  Net  ");
      WORDS = asList(" iMinds \nDistriNet");
      FONT_SIZE = 10;
      VERTICAL_LINE_SPACING = 6;
      NUM_VECHICLES = 12;
    }

    final ImmutableList.Builder<ImmutableList<Point>> pointBuilder = ImmutableList
      .builder();

    for (final String word : WORDS) {
      pointBuilder.add(SwarmDemo.measureString(word, FONT_SIZE, SPACING, 2));
    }

    final ImmutableList<ImmutableList<Point>> points = pointBuilder.build();
    int max = 0;
    double xMax = 0;
    double yMax = 0;
    for (final List<Point> ps : points) {
      max = Math.max(max, ps.size());
      for (final Point p : ps) {
        xMax = Math.max(p.x, xMax);
        yMax = Math.max(p.y, yMax);
      }
    }

    int width = DoubleMath.roundToInt(xMax / SPACING, RoundingMode.CEILING);
    width += VERTICAL_LINE_SPACING - width % VERTICAL_LINE_SPACING;
    width += width / VERTICAL_LINE_SPACING % 2 == 0 ? VERTICAL_LINE_SPACING
      : 0;

    int height = DoubleMath.roundToInt(yMax / SPACING, RoundingMode.CEILING) + 2;
    height += height % 2;
    final Graph<?> g = createGrid(width, height, 1, VERTICAL_LINE_SPACING,
      SPACING);

    final List<Point> borderNodes = newArrayList(getBorderNodes(g));
    Collections.shuffle(borderNodes, new Random(123));

    final UiSchema uis = new UiSchema(false);
    uis.add(AGV.class, "/graphics/flat/forklift2.png");

    View.Builder view = View.builder()
      .with(GraphRoadModelRenderer.builder()
        .withMargin(CANVAS_MARGIN))
      .with(BoxRenderer.builder())
      .with(
        RoadUserRenderer.builder()
          .withImageAssociation(AGV.class, "/graphics/flat/forklift2.png")
      )
      .withTitleAppendix("Factory Demo")
      .withAutoPlay()
      .withAutoClose()
      .withSpeedUp(4);

    if (m != null) {
      view = view.withMonitor(m)
        .withResolution(m.getClientArea().width, m.getClientArea().height)
        .withDisplay(display);

      if (list != null) {
        view = view.withCallback(list)
          .withAsync();
      }
      else {
        view = view.withFullScreen();
      }
    }

    final RandomGenerator rng = new MersenneTwister(123);
    final Simulator simulator = Simulator
      .builder()
      .setRandomGenerator(rng)
      .addModel(
        BlockingGraphRoadModel.blockingBuilder(g)
          .withDistanceUnit(SI.METER)
          .withSpeedUnit(NonSI.KILOMETERS_PER_HOUR)
      )
      .addModel(
        DefaultPDPModel.builder()
      )
      .addModel(
        AgvModel.builder().withPoints(
          ImmutableList.<ImmutableList<Point>> builder()
            .addAll(points)
            .add(ImmutableList.copyOf(borderNodes))
            .build(),
          getBorderNodes(g))
      )
      .addModel(view)
      .build();

    for (int i = 0; i < NUM_VECHICLES; i++) {
      simulator.register(new AGV(rng));
    }

    simulator.addTickListener(new TickListener() {
      @Override
      public void tick(TimeLapse time) {
        if (time.getStartTime() > endTime) {
          simulator.stop();
        }
      }

      @Override
      public void afterTick(TimeLapse timeLapse) {}
    });

    simulator.start();
    return simulator;
  }

  static void addPath(Graph<?> graph, Point... points) {
    final List<Point> newPoints = newArrayList();
    for (int i = 0; i < points.length - 1; i++) {
      final double dist = Point.distance(points[i], points[i + 1]);
      final Point unit = Point.divide(Point.diff(points[i + 1], points[i]),
        dist);
      final int numPoints = DoubleMath.roundToInt(dist / POINT_DISTANCE,
        RoundingMode.FLOOR);
      for (int j = 0; j < numPoints; j++) {
        final double factor = j * POINT_DISTANCE;
        newPoints.add(new Point(points[i].x + factor * unit.x, points[i].y
          + factor * unit.y));
      }
    }
    newPoints.add(points[points.length - 1]);
    Graphs.addPath(graph, newPoints.toArray(new Point[newPoints.size()]));
  }

  static ImmutableList<Point> getBorderNodes(Graph<?> g) {
    final Set<Point> points = g.getNodes();
    double xMin = Double.MAX_VALUE;
    double yMin = Double.MAX_VALUE;
    double xMax = Double.MIN_VALUE;
    double yMax = Double.MIN_VALUE;

    for (final Point p : points) {
      xMin = Math.min(xMin, p.x);
      yMin = Math.min(yMin, p.y);
      xMax = Math.max(xMax, p.x);
      yMax = Math.max(yMax, p.y);
    }
    final ImmutableList.Builder<Point> builder = ImmutableList.builder();
    for (final Point p : points) {
      if (p.x == xMin || p.x == xMax || p.y == yMin || p.y == yMax) {
        builder.add(p);
      }
    }
    return builder.build();
  }

  static Graph<LengthData> createGrid(int width, int height, int hLines,
    int vLines, double distance) {
    final Graph<LengthData> graph = new MultimapGraph<LengthData>();

    int v = 0;
    // draw vertical lines
    for (int i = 0; i < width + 1; i++) {
      Point prev = new Point(i * distance, 0);
      if (i % vLines == 0) {
        for (int j = 1; j < height; j++) {
          final Point cur = new Point(i * distance, j * distance);
          if (v % 2 == 0) {
            graph.addConnection(prev, cur);
          } else {
            graph.addConnection(cur, prev);
          }
          prev = cur;
        }
        v++;
      }
    }

    int y = 1;
    for (int i = 0; i < height; i++) {
      Point prev = new Point(0, i * distance);
      if (i % hLines == 0) {
        for (int j = 1; j < width + 1; j++) {
          final Point cur = new Point(j * distance, i * distance);
          if (y % 2 == 0) {
            graph.addConnection(prev, cur);
          } else {
            graph.addConnection(cur, prev);
          }
          prev = cur;
        }
      }
      y++;
    }
    return graph;
  }
}
