/**
 * 
 */
package rinde.sim.pdptw.common;

import static com.google.common.collect.Lists.newArrayList;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.PDPScenarioEvent;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.pdptw.common.DynamicPDPTWProblem.SimulationInfo;
import rinde.sim.scenario.Scenario;
import rinde.sim.scenario.TimedEvent;
import rinde.sim.util.TimeWindow;
import rinde.sim.util.spec.Specification.ISpecification;

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
  public abstract ISpecification<SimulationInfo> getStopCondition();

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

  protected abstract DynamicPDPTWScenario newInstance(
      Collection<? extends TimedEvent> events);

  @SuppressWarnings("unchecked")
  public static <T extends DynamicPDPTWScenario> T convertToOffline(T scenario) {
    final List<TimedEvent> events = scenario.asList();
    final List<TimedEvent> newEvents = newArrayList();
    for (final TimedEvent e : events) {
      final Class<?> clazz = e.getClass();
      if (clazz == AddDepotEvent.class) {
        newEvents.add(new AddDepotEvent(e.time, ((AddDepotEvent) e).position));
      } else if (clazz == AddVehicleEvent.class) {
        newEvents.add(new AddVehicleEvent(e.time,
            ((AddVehicleEvent) e).vehicleDTO));
      } else if (e.getEventType() == PDPScenarioEvent.TIME_OUT) {
        newEvents.add(new TimedEvent(e.getEventType(), e.time));
      } else if (clazz == AddParcelEvent.class) {
        final AddParcelEvent old = (AddParcelEvent) e;
        final ParcelDTO newDto = new ParcelDTO(//
            old.parcelDTO.pickupLocation,//
            old.parcelDTO.destinationLocation, //
            old.parcelDTO.pickupTimeWindow,//
            old.parcelDTO.deliveryTimeWindow,//
            old.parcelDTO.neededCapacity,//
            -1,// CHANGING ORDER ARRIVAL TIME TO -1
            old.parcelDTO.pickupDuration,//
            old.parcelDTO.deliveryDuration);
        newEvents.add(new AddParcelEvent(newDto));
      } else {
        throw new IllegalArgumentException("Unrecognized class: " + clazz
            + ", instance: " + e);
      }
    }
    return (T) scenario.newInstance(newEvents);
  }

  public interface ProblemClass {
    String getId();
  }
}
