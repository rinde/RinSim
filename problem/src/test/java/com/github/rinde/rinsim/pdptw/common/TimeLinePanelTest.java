package com.github.rinde.rinsim.pdptw.common;

import java.io.File;

import org.eclipse.swt.SWT;
import org.junit.Test;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.pdp.Depot;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.pdptw.central.Central;
import com.github.rinde.rinsim.pdptw.central.RandomSolver;
import com.github.rinde.rinsim.pdptw.common.TimeLinePanel;
import com.github.rinde.rinsim.pdptw.experiment.Experiment;
import com.github.rinde.rinsim.pdptw.gendreau06.Gendreau06ObjectiveFunction;
import com.github.rinde.rinsim.pdptw.gendreau06.Gendreau06Parser;
import com.github.rinde.rinsim.scenario.ScenarioController.UICreator;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.PDPModelRenderer;
import com.github.rinde.rinsim.ui.renderers.PlaneRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import com.github.rinde.rinsim.ui.renderers.UiSchema;

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
