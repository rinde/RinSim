/**
 * 
 */
package rinde.sim.examples.pdptw.gradientfield;

import rinde.sim.core.Simulator;
import rinde.sim.pdptw.common.DefaultDepot;
import rinde.sim.pdptw.common.RouteRenderer;
import rinde.sim.pdptw.experiment.Experiment;
import rinde.sim.pdptw.gendreau06.Gendreau06ObjectiveFunction;
import rinde.sim.pdptw.gendreau06.Gendreau06Parser;
import rinde.sim.pdptw.gendreau06.Gendreau06Scenario;
import rinde.sim.scenario.ScenarioController.UICreator;
import rinde.sim.ui.View;
import rinde.sim.ui.renderers.PDPModelRenderer;
import rinde.sim.ui.renderers.PlaneRoadModelRenderer;
import rinde.sim.ui.renderers.RoadUserRenderer;
import rinde.sim.ui.renderers.UiSchema;

/**
 * Example of a gradient field MAS for the Gendreau et al. (2006) dataset.
 * <p>
 * If this class is run on MacOS it might be necessary to use
 * -XstartOnFirstThread as a VM argument.
 * @author David Merckx
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class GradientFieldExample {

  private GradientFieldExample() {}

  public static void main(String[] args) {
    run(false);
  }

  public static void run(final boolean testing) {
    final UICreator uic = new UICreator() {
      @Override
      public void createUI(Simulator sim) {
        final UiSchema schema = new UiSchema(false);
        schema.add(Truck.class, "/graphics/perspective/bus-44.png");
        schema.add(DefaultDepot.class, "/graphics/flat/warehouse-32.png");
        schema.add(GFParcel.class, "/graphics/flat/hailing-cab-32.png");
        final View.Builder viewBuilder = View.create(sim)
            .with(
                new PlaneRoadModelRenderer(),
                new RoadUserRenderer(schema, false),
                new RouteRenderer(),
                new GradientFieldRenderer(),
                new PDPModelRenderer(false)
            );
        if (testing) {
          viewBuilder.enableAutoClose().enableAutoPlay().setSpeedUp(64)
              .stopSimulatorAtTime(60 * 60 * 1000);
        }
        viewBuilder.show();
      }
    };

    final Gendreau06Scenario scenario = Gendreau06Parser
        .parser().addFile(GradientFieldExample.class
            .getResourceAsStream("/data/gendreau06/req_rapide_1_240_24"),
            "req_rapide_1_240_24")
        .allowDiversion()
        .parse().get(0);

    final Gendreau06ObjectiveFunction objFunc = new Gendreau06ObjectiveFunction();
    Experiment
        .build(objFunc)
        .withRandomSeed(123)
        .addConfiguration(new GradientFieldConfiguration())
        .addScenario(scenario)
        .showGui(uic)
        .repeat(1)
        .perform();
  }
}
