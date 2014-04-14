/**
 * 
 */
package rinde.sim.pdptw.common;

import java.util.Collection;
import java.util.Set;

import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.pdptw.common.DynamicPDPTWProblem.SimulationInfo;
import rinde.sim.scenario.Scenario;
import rinde.sim.scenario.TimedEvent;
import rinde.sim.util.TimeWindow;

import com.google.common.base.Predicate;

/**
 * A {@link Scenario} that defines a <i>dynamic pickup-and-delivery problem with
 * time windows</i>. It contains all information needed to instantiate an entire
 * simulation.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public abstract class DynamicPDPTWScenario extends Scenario {

  private static final long serialVersionUID = 7258024865764689371L;

  /**
   * New empty instance.
   */
  protected DynamicPDPTWScenario() {
    super();
  }

  protected DynamicPDPTWScenario(Collection<? extends TimedEvent> events,
      Set<Enum<?>> supportedTypes) {
    super(events, supportedTypes);
  }

  /**
   * @return A {@link RoadModel} instance that should be used in this scenario.
   */
  public abstract RoadModel createRoadModel();

  /**
   * @return A {@link PDPModel} instance that should be used in this scenario.
   */
  public abstract PDPModel createPDPModel();

  /**
   * @return The {@link TimeWindow} of the scenario indicates the start and end
   *         of scenario.
   */
  public abstract TimeWindow getTimeWindow();

  /**
   * @return The size of a tick.
   */
  public abstract long getTickSize();

  /**
   * @return The stop condition indicating when a simulation should end.
   */
  public abstract Predicate<SimulationInfo> getStopCondition();

  /**
   * @return The time unit used in the simulator.
   */
  public abstract Unit<Duration> getTimeUnit();

  /**
   * @return The speed unit used in the {@link RoadModel}.
   */
  public abstract Unit<Velocity> getSpeedUnit();

  /**
   * @return The distance unit used in the {@link RoadModel}.
   */
  public abstract Unit<Length> getDistanceUnit();

  public abstract ProblemClass getProblemClass();

  // used to distinguish between two instances from the same class
  public abstract String getProblemInstanceId();

  public interface ProblemClass {
    String getId();
  }
}
