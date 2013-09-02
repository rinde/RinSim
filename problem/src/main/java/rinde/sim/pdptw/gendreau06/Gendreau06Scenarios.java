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
  private final boolean online;

  public Gendreau06Scenarios(String dir, boolean online,
      GendreauProblemClass... classes) {
    this.dir = dir;
    this.online = online;
    this.classes = ImmutableList.copyOf(classes);
  }

  public Gendreau06Scenarios(String dir, GendreauProblemClass... classes) {
    this(dir, true, classes);
  }

  @Override
  public ImmutableList<DynamicPDPTWScenario> provide() {
    final ImmutableList.Builder<DynamicPDPTWScenario> scenarios = ImmutableList
        .builder();
    for (final GendreauProblemClass claz : classes) {
      final List<String> files = ExperimentUtil.getFilesFromDir(dir,
          claz.fileId);
      for (final String file : files) {
        try {
          Gendreau06Scenario scen = Gendreau06Parser.parse(file, claz.vehicles);
          if (!online) {
            scen = DynamicPDPTWScenario.convertToOffline(scen);
          }
          scenarios.add(scen);
        } catch (final IOException e) {
          throw new IllegalArgumentException(
              "Failed loading scenario: " + file, e);
        }
      }
    }
    return scenarios.build();
  }

}
