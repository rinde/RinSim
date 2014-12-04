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
package com.github.rinde.rinsim.scenario.gendreau06;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.pdptw.common.DynamicPDPTWProblem;
import com.github.rinde.rinsim.pdptw.common.DynamicPDPTWProblem.DefaultUICreator;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.Renderer;

/**
 * @author Rinde van Lon 
 * 
 */
public class TestUICreator extends DefaultUICreator {

  public TestUICreator(DynamicPDPTWProblem prob, int speed) {
    super(prob, speed);
  }

  @Override
  public void createUI(Simulator sim) {
    initRenderers();
    View.create(sim).with(renderers.toArray(new Renderer[] {}))
        .setSpeedUp(speedup)
        .enableAutoClose()
        .enableAutoPlay()
        .show();
  }

}
