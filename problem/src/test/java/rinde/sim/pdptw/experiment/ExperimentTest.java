/**
 * 
 */
package rinde.sim.pdptw.experiment;

import rinde.sim.pdptw.common.DynamicPDPTWProblem;
import rinde.sim.pdptw.common.DynamicPDPTWScenario;
import rinde.sim.pdptw.common.ObjectiveFunction;
import rinde.sim.pdptw.common.StatisticsDTO;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class ExperimentTest {

  public static StatisticsDTO singleRun(DynamicPDPTWScenario scenario,
      MASConfiguration c, long seed, ObjectiveFunction objFunc, boolean showGui) {
    return Experiment.singleRun(scenario, c, seed, objFunc, showGui).stats;
  }

  public static DynamicPDPTWProblem init(DynamicPDPTWScenario scenario,
      MASConfiguration config, long seed, boolean showGui) {
    return Experiment.init(scenario, config, seed, showGui);
  }
}
