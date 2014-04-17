/**
 * 
 */
package rinde.sim.pdptw.common;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static rinde.sim.core.model.pdp.PDPScenarioEvent.TIME_OUT;

import java.util.List;
import java.util.Map;

import javax.measure.Measure;

import org.apache.commons.math3.random.MersenneTwister;
import org.eclipse.swt.SWT;

import rinde.sim.core.Simulator;
import rinde.sim.core.TickListener;
import rinde.sim.core.TimeLapse;
import rinde.sim.core.model.Model;
import rinde.sim.core.model.pdp.Depot;
import rinde.sim.core.model.pdp.Parcel;
import rinde.sim.core.model.pdp.Vehicle;
import rinde.sim.pdptw.scenario.PDPScenario;
import rinde.sim.scenario.ScenarioController;
import rinde.sim.scenario.ScenarioController.UICreator;
import rinde.sim.scenario.TimedEvent;
import rinde.sim.scenario.TimedEventHandler;
import rinde.sim.ui.View;
import rinde.sim.ui.renderers.CanvasRenderer;
import rinde.sim.ui.renderers.PDPModelRenderer;
import rinde.sim.ui.renderers.PlaneRoadModelRenderer;
import rinde.sim.ui.renderers.Renderer;
import rinde.sim.ui.renderers.RoadUserRenderer;
import rinde.sim.ui.renderers.UiSchema;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;

// TODO rename to ProblemInstance? or to Problem?
/**
 * A problem instance for the class of problems which is called dynamic
 * pickup-and-delivery problems with time windows, often abbreviated as dynamic
 * PDPTW.
 * <p>
 * A problem instance is an instance which sets up everything related to the
 * 'problem' which one tries to solve. The idea is that a user only needs to
 * worry about adding its own solution to this instance.
 * <p>
 * By default this class needs very little customization, it needs to be given a
 * scenario which it then uses to configure the simulation. Further it is
 * required to plug your own vehicle in by using
 * {@link #addCreator(Class, Creator)}. Optionally this method can also be used
 * to plug in custom parcels and depots.
 * <p>
 * Currently the Gendreau et al. (2006) benchmark is supported. In the future
 * this class will also support the Fabri & Recht and Pankratz benchmarks.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class DynamicPDPTWProblem {

  // TODO create a builder for configuration of problems
  // TODO a scenario should be an event list AND a pre-configured set of models
  // describing the complete problem

  // TODO a StopCondition should be a first class simulator entity

  // TODO perhaps a UI config should also be bundled easily?

  // TODO stats system should be more modular (per model?) and hook directly in
  // the simulator

  // TODO if there can be some generic way to hook custom agents into the
  // simulator/scenario, this class can probably be removed

  /**
   * A map which contains the default {@link Creator}s.
   */
  protected static final ImmutableMap<Class<?>, Creator<?>> DEFAULT_EVENT_CREATOR_MAP;
  static {
    DEFAULT_EVENT_CREATOR_MAP = ImmutableMap.of(
        (Class<?>) AddParcelEvent.class, new Creator<AddParcelEvent>() {
          @Override
          public boolean create(Simulator sim, AddParcelEvent event) {
            return sim.register(new DefaultParcel(event.parcelDTO));
          }
        }, AddDepotEvent.class, new Creator<AddDepotEvent>() {
          @Override
          public boolean create(Simulator sim, AddDepotEvent event) {
            return sim.register(new DefaultDepot(event.position));
          }
        });
  }

  /**
   * Map containing the {@link Creator}s which handle specific
   * {@link TimedEvent}s.
   */
  protected final Map<Class<?>, Creator<?>> eventCreatorMap;

  /**
   * The {@link ScenarioController} which is used to play the scenario.
   */
  protected final ScenarioController controller;

  /**
   * The {@link Simulator} which is used for the simulation.
   */
  protected final Simulator simulator;

  /**
   * The {@link UICreator} which is used for creating the default UI.
   */
  protected final DefaultUICreator defaultUICreator;

  /**
   * The {@link StatsTracker} which is used internally for gathering statistics.
   */
  protected final StatsTracker statsTracker;

  /**
   * The {@link StopCondition} which is used as the condition when the
   * simulation has to stop.
   */
  protected Predicate<SimulationInfo> stopCondition;

  /**
   * Create a new problem instance using the specified scenario.
   * @param scen The the {@link PDPScenario} which is used in this
   *          problem.
   * @param randomSeed The random seed which will be passed into the random
   *          number generator in the simulator.
   * @param models An optional list of models which can be added, with this
   *          option custom models for specific solutions can be added.
   */
  public DynamicPDPTWProblem(final PDPScenario scen, long randomSeed,
      Model<?>... models) {
    simulator = new Simulator(new MersenneTwister(randomSeed), Measure.valueOf(
        scen.getTickSize(), scen.getTimeUnit()));
    final List<? extends Model<?>> scenarioModels = scen.createModels();
    for (final Model<?> m : scenarioModels) {
      simulator.register(m);
    }
    for (final Model<?> m : models) {
      simulator.register(m);
    }
    eventCreatorMap = newHashMap();

    final TimedEventHandler handler = new TimedEventHandler() {
      @SuppressWarnings("unchecked")
      @Override
      public boolean handleTimedEvent(TimedEvent event) {
        if (eventCreatorMap.containsKey(event.getClass())) {
          return ((Creator<TimedEvent>) eventCreatorMap.get(event.getClass()))
              .create(simulator, event);
        } else if (DEFAULT_EVENT_CREATOR_MAP.containsKey(event.getClass())) {
          return ((Creator<TimedEvent>) DEFAULT_EVENT_CREATOR_MAP.get(event
              .getClass())).create(simulator, event);
        } else if (event.getEventType() == TIME_OUT) {
          return true;
        }
        return false;
      }
    };
    final int ticks = scen.getTimeWindow().end == Long.MAX_VALUE ? -1
        : (int) (scen.getTimeWindow().end - scen.getTimeWindow().begin);
    controller = new ScenarioController(scen, simulator, handler, ticks);
    statsTracker = new StatsTracker(controller, simulator);

    stopCondition = scen.getStopCondition();

    simulator.addTickListener(new TickListener() {

      @Override
      public void tick(TimeLapse timeLapse) {}

      @Override
      public void afterTick(TimeLapse timeLapse) {
        if (stopCondition.apply(new SimulationInfo(statsTracker
            .getStatsDTO(), scen))) {
          simulator.stop();
        }
      }
    });
    defaultUICreator = new DefaultUICreator(this);
  }

  /**
   * @return The statistics of the current simulation. Note that calling this
   *         method while the simulation is not yet finished gives the
   *         statistics that were gathered up until that moment.
   */
  public StatisticsDTO getStatistics() {
    return statsTracker.getStatsDTO();
  }

  /**
   * Enables UI using a default visualization.
   */
  public void enableUI() {
    enableUI(defaultUICreator);
  }

  /**
   * Allows to add an additional {@link CanvasRenderer} to the default UI.
   * @param r The {@link CanvasRenderer} to add.
   */
  public void addRendererToUI(CanvasRenderer r) {
    defaultUICreator.addRenderer(r);
  }

  /**
   * Adds a {@link StopCondition} which indicates when the simulation has to
   * stop. The condition is added in an OR fashion to the predefined stop
   * condition of the scenario. So after this method is called the simulation
   * stops if the scenario stop condition is true OR new condition is true.
   * Subsequent invocations of this method will just add more conditions in the
   * same way.
   * @param condition The stop condition to add.
   */
  public void addStopCondition(Predicate<SimulationInfo> condition) {
    stopCondition = Predicates.or(stopCondition, condition);
  }

  /**
   * Enables UI by allowing plugging in a custom {@link UICreator}.
   * @param creator The creator to use.
   */
  public void enableUI(UICreator creator) {
    controller.enableUI(creator);
  }

  /**
   * Executes a simulation of the problem. When the simulation is finished (and
   * this method returns) the statistics of the simulation are returned.
   * @return The statistics that were gathered during the simulation.
   */
  public StatisticsDTO simulate() {
    checkState(eventCreatorMap.containsKey(AddVehicleEvent.class),
        "A creator for AddVehicleEvent is required, use %s.addCreator(..)",
        this.getClass().getName());
    controller.start();
    return getStatistics();
  }

  /**
   * This method exposes the {@link Simulator} that is managed by this problem
   * instance. Be careful with using it since it is possible to significantly
   * alter the behavior of the simulation.
   * @return The simulator.
   */
  public Simulator getSimulator() {
    return simulator;
  }

  /**
   * Using this method a {@link Creator} instance can be associated with a
   * certain event. The creator will be called when the event is issued, it is
   * the responsibility of the {@link Creator} the create the appropriate
   * response. This method will override a previously existing creator for the
   * specified event type if applicable.
   * @param eventType The event type to which the creator will be associated.
   * @param creator The creator that will be used.
   * @param <T> The type of the event.
   */
  public <T extends TimedEvent> void addCreator(Class<T> eventType,
      Creator<T> creator) {
    checkArgument(
        eventType == AddVehicleEvent.class || eventType == AddParcelEvent.class
            || eventType == AddDepotEvent.class,
        "A creator can only be added to one of the following classes: AddVehicleEvent, AddParcelEvent, AddDepotEvent.");
    eventCreatorMap.put(eventType, creator);
  }

  /**
   * Factory for handling a certain type {@link TimedEvent}s. It is the
   * responsible of this instance to create the appropriate object when an event
   * occurs. All created objects can be added to the {@link Simulator} by using
   * {@link Simulator#register(Object)}.
   * @param <T> The specific subclass of {@link TimedEvent} for which the
   *          creator should create objects.
   */
  public interface Creator<T extends TimedEvent> {
    /**
     * Should add an object to the simulation.
     * @param sim The simulator to which the objects can be added.
     * @param event The {@link TimedEvent} instance that contains the event
     *          details.
     * @return <code>true</code> if the creation and adding of the object was
     *         successful, <code>false</code> otherwise.
     */
    boolean create(Simulator sim, T event);
  }

  /**
   * This class contains default stop conditions which can be used in the
   * problem. If you want to create your own stop condition you can do it in the
   * following way:
   * 
   * <pre>
   * StopCondition sc = new StopCondition() {
   *   &#064;Override
   *   public boolean isSatisfiedBy(SimulationInfo context) {
   *     return true; // &lt;- insert your own condition here
   *   }
   * };
   * </pre>
   * 
   * StopConditions can be combined into more complex conditions by using
   * {@link Specification#of(Spec)}.
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  public abstract static class StopCondition implements
      Predicate<SimulationInfo> {

    /**
     * The simulation is terminated once the
     * {@link rinde.sim.core.model.pdp.PDPScenarioEvent#TIME_OUT} event is
     * dispatched.
     */
    public static final StopCondition TIME_OUT_EVENT = new StopCondition() {
      @Override
      public boolean apply(SimulationInfo context) {
        return context.stats.simFinish;
      }
    };

    /**
     * The simulation is terminated as soon as all the vehicles are back at the
     * depot, note that this can be before or after the
     * {@link rinde.sim.core.model.pdp.PDPScenarioEvent#TIME_OUT} event is
     * dispatched.
     */
    public static final StopCondition VEHICLES_DONE_AND_BACK_AT_DEPOT = new StopCondition() {
      @Override
      public boolean apply(SimulationInfo context) {
        return context.stats.totalVehicles == context.stats.vehiclesAtDepot
            && context.stats.movedVehicles > 0
            && context.stats.totalParcels == context.stats.totalDeliveries;
      }
    };

    /**
     * The simulation is terminated as soon as any tardiness occurs.
     */
    public static final StopCondition ANY_TARDINESS = new StopCondition() {
      @Override
      public boolean apply(SimulationInfo context) {
        return context.stats.pickupTardiness > 0
            || context.stats.deliveryTardiness > 0;
      }
    };
  }

  /**
   * This is an immutable state object which is exposed to {@link StopCondition}
   * s.
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  public static class SimulationInfo {
    /**
     * The current statistics.
     */
    public final StatisticsDTO stats;

    /**
     * The scenario which is playing.
     */
    public final PDPScenario scenario;

    /**
     * Instantiate a new instance using statistics and scenario.
     * @param st Statistics.
     * @param scen Scenario.
     */
    protected SimulationInfo(StatisticsDTO st, PDPScenario scen) {
      stats = st;
      scenario = scen;
    }
  }

  /**
   * A default {@link UICreator} used for creating a UI for a problem.
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  public static class DefaultUICreator implements UICreator {
    /**
     * A list of renderers.
     */
    protected final List<Renderer> renderers;

    /**
     * The speedup that is passed to the gui.
     */
    protected final int speedup;

    private final DynamicPDPTWProblem problem;

    /**
     * Create a GUI for the specified problem.
     * @param prob The problem to create a GUI for.
     */
    public DefaultUICreator(DynamicPDPTWProblem prob) {
      this(prob, 1);
    }

    /**
     * Create a GUI for the specified problem with the specified speed.
     * @param prob The problem to create a GUI for.
     * @param speed The speed to use.
     */
    public DefaultUICreator(DynamicPDPTWProblem prob, int speed) {
      checkArgument(speed >= 1, "speed must be a positive integer");
      speedup = speed;
      problem = prob;
      renderers = newArrayList();
    }

    /**
     * @return The default road model renderer.
     */
    protected Renderer planeRoadModelRenderer() {
      return new PlaneRoadModelRenderer(0.05);
    }

    /**
     * @return The default road user renderer.
     */
    protected Renderer roadUserRenderer() {
      final UiSchema schema = new UiSchema(false);
      schema.add(Vehicle.class, SWT.COLOR_RED);
      schema.add(Depot.class, SWT.COLOR_CYAN);
      schema.add(Parcel.class, SWT.COLOR_BLUE);
      return new RoadUserRenderer(schema, false);
    }

    /**
     * @return The default pdp model renderer.
     */
    protected Renderer pdpModelRenderer() {
      return new PDPModelRenderer(false);
    }

    /**
     * Initializes all renderers.
     */
    protected void initRenderers() {
      renderers.add(planeRoadModelRenderer());
      renderers.add(roadUserRenderer());
      renderers.add(pdpModelRenderer());
      renderers.add(new StatsPanel(problem.statsTracker));
    }

    @Override
    public void createUI(Simulator sim) {
      initRenderers();
      View.create(sim).with(renderers.toArray(new Renderer[] {}))
          .setSpeedUp(speedup).show();
    }

    /**
     * Add a renderer.
     * @param r The renderer to add.
     */
    public void addRenderer(Renderer r) {
      renderers.add(r);
    }
  }
}
