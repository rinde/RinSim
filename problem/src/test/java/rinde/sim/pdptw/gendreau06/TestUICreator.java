/**
 * 
 */
package rinde.sim.pdptw.gendreau06;

import rinde.sim.core.Simulator;
import rinde.sim.pdptw.common.DynamicPDPTWProblem;
import rinde.sim.pdptw.common.DynamicPDPTWProblem.DefaultUICreator;
import rinde.sim.ui.View;
import rinde.sim.ui.renderers.Renderer;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
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
        .setSpeedUp(speedup).enableAutoClose().enableAutoPlay().show();
  }

}
