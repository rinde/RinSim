/**
 * 
 */
package rinde.sim.pdptw.experiment;

import rinde.sim.pdptw.common.DynamicPDPTWProblem;
import rinde.sim.pdptw.common.DynamicPDPTWScenario;
import rinde.sim.pdptw.common.ObjectiveFunction;
import rinde.sim.pdptw.common.StatsTracker.StatisticsDTO;
import rinde.sim.pdptw.experiment.Experiment;
import rinde.sim.pdptw.experiment.MASConfiguration;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class ExperimentTest {

  public static StatisticsDTO singleRun(DynamicPDPTWScenario scenario,
      MASConfiguration c, ObjectiveFunction objFunc, boolean showGui) {
    return Experiment.singleRun(scenario, c, objFunc, showGui);
  }

  public static DynamicPDPTWProblem init(DynamicPDPTWScenario scenario,
      MASConfiguration config, boolean showGui) {
    return Experiment.init(scenario, config, showGui);
  }
}
