package com.github.rinde.rinsim.pdptw.common;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests the {@link TimeLinePanel}.
 * @author Rinde van Lon 
 */
public class TimeLinePanelTest {

  /**
   * Test the gui.
   */
  @Test
  @Ignore
  public void guiTest() {
    // TODO enable this test again as soon as cyclic dependencies problem is
    // solved
    // Experiment
    // .build(TestObjectiveFunction.INSTANCE)
    // .addConfiguration(TestMASConfiguration.create("config"))
    // .addScenario(ScenarioTestUtil.create(1730))
    // .showGui(new UICreator() {
    //
    // @Override
    // public void createUI(Simulator sim) {
    // final UiSchema schema = new UiSchema(false);
    // schema.add(Vehicle.class, SWT.COLOR_RED);
    // schema.add(Depot.class, SWT.COLOR_CYAN);
    // schema.add(Parcel.class, SWT.COLOR_BLUE);
    // View.create(sim)
    // .with(new PlaneRoadModelRenderer())
    // .with(new RoadUserRenderer(schema, false))
    // .with(new PDPModelRenderer())
    // .with(new TimeLinePanel())
    // .setSpeedUp(200)
    // .enableAutoClose()
    // .enableAutoPlay()
    // .show();
    // }
    // })
    // .perform();
  }
}
