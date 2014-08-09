package com.github.rinde.rinsim.ui;

import javax.measure.Measure;
import javax.measure.unit.SI;

import org.apache.commons.math3.random.MersenneTwister;
import org.eclipse.swt.widgets.Display;
import org.junit.Test;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.TickListener;
import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.graph.Point;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.Depot;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.road.PlaneRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.PlaneRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import com.github.rinde.rinsim.ui.renderers.TestRenderer;
import com.github.rinde.rinsim.ui.renderers.UiSchema;

public class ViewTest {

  /**
   * Simple GUI test.
   */
  @Test
  public void testRenderer() {
    final Simulator sim = new Simulator(new MersenneTwister(123),
        Measure.valueOf(1000L, SI.MILLI(SI.SECOND)));
    sim.register(new PlaneRoadModel(new Point(0, 0), new Point(10, 10), 50));
    sim.configure();
    sim.addTickListener(new TickListener() {
      @Override
      public void tick(TimeLapse timeLapse) {
        if (timeLapse.getTime() >= 10000) {
          sim.stop();
        }
      }

      @Override
      public void afterTick(TimeLapse timeLapse) {}
    });

    View.create(sim)
        .setTitleAppendix("ViewTest")
        .enableAutoClose()
        .enableAutoPlay()
        .with(new PlaneRoadModelRenderer())
        .with(new TestRenderer())
        .show();
  }

  /**
   * Closes the window while the simulator is running. This emulates what
   * happens when a user closes the window.
   */
  @Test
  public void closeWindowWhilePlaying() {
    final Simulator sim = new Simulator(new MersenneTwister(123),
        Measure.valueOf(1000L, SI.MILLI(SI.SECOND)));
    sim.register(new PlaneRoadModel(new Point(0, 0), new Point(10, 10), 50));
    sim.register(new DefaultPDPModel());
    sim.configure();
    sim.addTickListener(new TickListener() {
      @Override
      public void tick(TimeLapse timeLapse) {
        if (timeLapse.getTime() >= 10000) {
          final Display disp = UITestTools.findDisplay();
          disp.syncExec(
              new Runnable() {
                @Override
                public void run() {
                  disp.getActiveShell().close();
                }
              });
        }
      }

      @Override
      public void afterTick(TimeLapse timeLapse) {}
    });

    UITestTools.startAndClose(-1);

    sim.register(new TestDepot());
    final UiSchema uis = new UiSchema();
    uis.add(TestDepot.class, "/graphics/perspective/tall-building-64.png");
    View.create(sim)
        .setTitleAppendix("ViewTest")
        .with(new PlaneRoadModelRenderer())
        .with(new RoadUserRenderer(uis, false))
        .show();
  }

  /**
   * Tests a view with a simulator that is not configured.
   */
  @Test(expected = IllegalStateException.class)
  public void failNotConfiguredSim() {
    final Simulator sim = new Simulator(new MersenneTwister(123),
        Measure.valueOf(1000L, SI.MILLI(SI.SECOND)));
    View.create(sim).show();
  }

  static class TestDepot extends Depot {
    public TestDepot() {
      setStartPosition(new Point(1, 1));
    }

    @Override
    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {}
  }
}
