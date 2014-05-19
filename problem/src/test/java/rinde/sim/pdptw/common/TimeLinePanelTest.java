package rinde.sim.pdptw.common;

import java.io.File;

import org.eclipse.swt.SWT;
import org.junit.Test;

import rinde.sim.core.Simulator;
import rinde.sim.core.model.pdp.Depot;
import rinde.sim.core.model.pdp.Parcel;
import rinde.sim.core.model.pdp.Vehicle;
import rinde.sim.pdptw.central.Central;
import rinde.sim.pdptw.central.RandomSolver;
import rinde.sim.pdptw.experiment.Experiment;
import rinde.sim.pdptw.gendreau06.Gendreau06ObjectiveFunction;
import rinde.sim.pdptw.gendreau06.Gendreau06Parser;
import rinde.sim.scenario.ScenarioController.UICreator;
import rinde.sim.ui.View;
import rinde.sim.ui.renderers.PDPModelRenderer;
import rinde.sim.ui.renderers.PlaneRoadModelRenderer;
import rinde.sim.ui.renderers.RoadUserRenderer;
import rinde.sim.ui.renderers.UiSchema;

/**
 * Tests the {@link TimeLinePanel}.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class TimeLinePanelTest {

  /**
   * Test the gui.
   */
  @Test
  public void guiTest() {

    Experiment
        .build(Gendreau06ObjectiveFunction.instance())
        .addConfiguration(Central.solverConfiguration(RandomSolver.supplier()))
        .addScenario(
            Gendreau06Parser
                .parse(new File(
                    "../example/src/main/resources/data/gendreau06/req_rapide_1_240_24")))
        .showGui(new UICreator() {

          @Override
          public void createUI(Simulator sim) {
            final UiSchema schema = new UiSchema(false);
            schema.add(Vehicle.class, SWT.COLOR_RED);
            schema.add(Depot.class, SWT.COLOR_CYAN);
            schema.add(Parcel.class, SWT.COLOR_BLUE);
            View.create(sim)
                .with(new PlaneRoadModelRenderer())
                .with(new RoadUserRenderer(schema, false))
                .with(new PDPModelRenderer())
                .with(new TimeLinePanel())
                .setSpeedUp(200)
                .enableAutoClose()
                .enableAutoPlay()
                .show();
          }
        })
        .perform();
  }
}
