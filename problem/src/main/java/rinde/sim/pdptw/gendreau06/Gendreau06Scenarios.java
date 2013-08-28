/**
 * 
 */
package rinde.sim.pdptw.gendreau06;

import java.io.IOException;
import java.util.List;

import rinde.sim.pdptw.common.DynamicPDPTWScenario;
import rinde.sim.pdptw.experiments.ExperimentUtil;
import rinde.sim.pdptw.experiments.ScenarioProvider;

import com.google.common.collect.ImmutableList;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class Gendreau06Scenarios implements ScenarioProvider {

  private final String dir;
  private final List<GendreauProblemClass> classes;

  public Gendreau06Scenarios(String dir, GendreauProblemClass... classes) {
    this.dir = dir;
    this.classes = ImmutableList.copyOf(classes);
  }

  @Override
  public ImmutableList<DynamicPDPTWScenario> provide() {
    final ImmutableList.Builder<DynamicPDPTWScenario> scenarios = ImmutableList
        .builder();
    for (final GendreauProblemClass claz : classes) {
      final List<String> files = ExperimentUtil
          .getFilesFromDir(dir, claz.fileId);
      for (final String file : files) {
        try {
          scenarios.add(Gendreau06Parser.parse(file, claz.vehicles));
        } catch (final IOException e) {
          throw new IllegalArgumentException(
              "Failed loading scenario: " + file, e);
        }
      }
    }
    return scenarios.build();
  }

}
