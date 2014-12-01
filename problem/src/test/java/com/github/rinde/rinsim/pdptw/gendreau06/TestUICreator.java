package com.github.rinde.rinsim.pdptw.gendreau06;

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
