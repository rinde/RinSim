/*
 * Copyright (C) 2011-2014 Rinde van Lon, iMinds DistriNet, KU Leuven
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
package com.github.rinde.rinsim.examples.pdptw.gradientfield;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.pdptw.DefaultDepot;
import com.github.rinde.rinsim.pdptw.common.RouteRenderer;
import com.github.rinde.rinsim.pdptw.experiment.Experiment;
import com.github.rinde.rinsim.pdptw.gendreau06.Gendreau06ObjectiveFunction;
import com.github.rinde.rinsim.pdptw.gendreau06.Gendreau06Parser;
import com.github.rinde.rinsim.pdptw.gendreau06.Gendreau06Scenario;
import com.github.rinde.rinsim.scenario.ScenarioController.UICreator;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.PDPModelRenderer;
import com.github.rinde.rinsim.ui.renderers.PlaneRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import com.github.rinde.rinsim.ui.renderers.UiSchema;

/**
 * Example of a gradient field MAS for the Gendreau et al. (2006) dataset.
 * <p>
 * If this class is run on MacOS it might be necessary to use
 * -XstartOnFirstThread as a VM argument.
 * @author David Merckx
 * @author Rinde van Lon 
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

    final Gendreau06ObjectiveFunction objFunc = Gendreau06ObjectiveFunction
        .instance();
    Experiment
        .build(objFunc)
        .withRandomSeed(123)
        .withThreads(1)
        .addConfiguration(new GradientFieldConfiguration())
        .addScenario(scenario)
        .showGui(uic)
        .repeat(1)
        .perform();
  }
}
