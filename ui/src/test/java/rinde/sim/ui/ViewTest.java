package rinde.sim.ui;

import javax.measure.Measure;
import javax.measure.unit.SI;

import org.apache.commons.math3.random.MersenneTwister;
import org.eclipse.swt.widgets.Display;
import org.junit.Test;

import rinde.sim.core.Simulator;
import rinde.sim.core.TickListener;
import rinde.sim.core.TimeLapse;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.DefaultPDPModel;
import rinde.sim.core.model.pdp.Depot;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.road.PlaneRoadModel;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.ui.renderers.PlaneRoadModelRenderer;
import rinde.sim.ui.renderers.RoadUserRenderer;
import rinde.sim.ui.renderers.TestRenderer;
import rinde.sim.ui.renderers.UiSchema;

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
          final Display disp = findDisplay();
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
    sim.register(new TestDepot());
    final UiSchema uis = new UiSchema();
    uis.add(TestDepot.class, "/graphics/perspective/tall-building-64.png");
    View.create(sim)
        .setTitleAppendix("ViewTest")
        .enableAutoPlay()
        .with(new PlaneRoadModelRenderer())
        .with(new RoadUserRenderer(uis, false))
        .show();
  }

  static Display findDisplay() {
    for (final Thread t : Thread.getAllStackTraces().keySet()) {
      final Display disp = Display.findDisplay(t);
      if (disp != null) {
        return disp;
      }
    }
    throw new IllegalStateException("There is no display");
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
