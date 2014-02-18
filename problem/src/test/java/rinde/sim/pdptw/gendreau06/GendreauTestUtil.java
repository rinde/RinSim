/**
 * 
 */
package rinde.sim.pdptw.gendreau06;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import rinde.sim.scenario.ScenarioBuilder;
import rinde.sim.scenario.ScenarioBuilder.ScenarioCreator;
import rinde.sim.scenario.TimedEvent;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class GendreauTestUtil {

  public static Gendreau06Scenario create(
      Collection<? extends TimedEvent> pEvents, Set<Enum<?>> pSupportedTypes,
      long ts) {
    return new Gendreau06Scenario(pEvents, pSupportedTypes, ts,
        GendreauProblemClass.SHORT_LOW_FREQ, 1, false);
  }

  public static Gendreau06Scenario create(Collection<TimedEvent> parcels) {
    return create(parcels, 1);
  }

  public static Gendreau06Scenario create(Collection<TimedEvent> parcels,
      int numTrucks) {
    final Gendreau06Scenario gs = Gendreau06Parser.parse(new BufferedReader(
        new StringReader("")), "req_rapide_1_240_24", numTrucks, 1000L);
    final ScenarioBuilder sb = new ScenarioBuilder(gs.getPossibleEventTypes());
    sb.addEvents(gs.asList());
    sb.addEvents(parcels);
    return sb.build(new ScenarioCreator<Gendreau06Scenario>() {
      @Override
      public Gendreau06Scenario create(List<TimedEvent> eventList,
          Set<Enum<?>> eventTypes) {
        return GendreauTestUtil.create(eventList, eventTypes, 1000);
      }
    });
  }

}
