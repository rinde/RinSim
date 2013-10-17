/**
 * 
 */
package rinde.sim.core;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.measure.Measure;
import javax.measure.quantity.Duration;
import javax.measure.unit.Unit;

import org.apache.commons.math3.random.RandomGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rinde.sim.core.model.Model;
import rinde.sim.core.model.ModelManager;
import rinde.sim.core.model.ModelProvider;
import rinde.sim.event.Event;
import rinde.sim.event.EventAPI;
import rinde.sim.event.EventDispatcher;

/**
 * Simulator is the core class of a simulation. It is responsible for managing
 * time which it does by periodically providing {@link TimeLapse} instances to
 * registered {@link TickListener}s. Further it provides methods to start and
 * stop simulations. The simulator also acts as a facade through which
 * {@link Model}s and objects can be added to the simulator, more info about
 * models can be found in {@link ModelManager}.
 * 
 * The configuration phase of the simulator looks as follows:
 * <ol>
 * <li>register models using {@link #register(Model)}</li>
 * <li>call {@link #configure()}
 * <li>register objects using {@link #register(Object)}</li>
 * <li>start simulation by calling {@link #start()}</li>
 * </ol>
 * Note that objects can not be registered <b>before</b> calling
 * {@link #configure()} and {@link Model}s can not be registed <b>after</b>
 * configuring.
 * 
 * 
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be> - simulator API
 *         changes
 * 
 */
public class Simulator implements SimulatorAPI {

  /**
   * The logger of the simulator.
   */
  protected static final Logger LOGGER = LoggerFactory
      .getLogger(Simulator.class);

  /**
   * Enum that describes the possible types of events that the simulator can
   * dispatch.
   */
  public enum SimulatorEventType {
    /**
     * Indicates that the simulator has stopped.
     */
    STOPPED,

    /**
     * Indicates that the simulator has started.
     */
    STARTED,

    /**
     * Indicates that the simulator has been configured.
     */
    CONFIGURED
  }

  /**
   * Contains the set of registered {@link TickListener}s.
   */
  protected volatile Set<TickListener> tickListeners;

  /**
   * Reference to dispatcher of simulator events, can be used by subclasses to
   * issue additional events.
   */
  protected final EventDispatcher dispatcher;

  /**
   * @see #isPlaying
   */
  protected volatile boolean isPlaying;

  /**
   * @see #getCurrentTime()
   */
  protected long time;

  /**
   * Model manager instance.
   */
  protected final ModelManager modelManager;
  private boolean configured;

  private Set<Object> toUnregister;
  private final RandomGenerator rand;
  private final long timeStep;
  private final TimeLapse timeLapse;

  // TODO RandomGenerator should be moved into an own model. This way, objects
  // that need a reference to a random generator can get one by implementing
  // this model's interface. The model could have several policies for
  // distributing RNGs: ALL_SAME, CLASS_SAME, ALL_DIFFERENT. This would
  // indicate: every subscribing object uses same RNG, objects of the same
  // class share same RNG, all objects get a different RNG instance
  // respectively.

  // TODO investigate if a TimeModel should be created, this would move all
  // time/tick related stuff into its own class. Making it easier to extend
  // this part.
  /**
   * Create a new simulator instance.
   * @param r The random number generator that is used in this simulator.
   * @param step The time that passes each tick. This can be in any unit the
   *          programmer prefers.
   */
  public Simulator(RandomGenerator r, Measure<Long, Duration> step) {
    LOGGER.info("Simulator constructor, time step {}", step);
    checkArgument(step.getValue() > 0L, "Step must be a positive number.");
    timeStep = step.getValue();
    tickListeners = Collections
        .synchronizedSet(new LinkedHashSet<TickListener>());

    toUnregister = new LinkedHashSet<Object>();

    rand = r;
    time = 0L;
    // time lapse is reused in a Flyweight kind of style
    timeLapse = new TimeLapse(step.getUnit());

    modelManager = new ModelManager();

    dispatcher = new EventDispatcher(SimulatorEventType.values());
  }

  /**
   * This configures the {@link Model}s in the simulator. After calling this
   * method models can no longer be added, objects can only be registered after
   * this method is called.
   * @see ModelManager#configure()
   */
  public void configure() {
    for (final Model<?> m : modelManager.getModels()) {
      if (m instanceof TickListener) {
        LOGGER.info("adding " + m.getClass().getName() + " as a tick listener");
        addTickListener((TickListener) m);
      }
    }
    modelManager.configure();
    configured = true;
    dispatcher.dispatchEvent(new Event(SimulatorEventType.CONFIGURED, this));
  }

  // TODO create a SimulatorBuilder for configuration of Simulator?
  // TODO should fail on error instead of returning a boolean
  /**
   * Register a model to the simulator.
   * @param model The {@link Model} instance to register.
   * @return true if successful, false otherwise
   */
  public boolean register(Model<?> model) {
    if (configured) {
      throw new IllegalStateException(
          "cannot add model after calling configure()");
    }
    injectDependencies(model);
    final boolean result = modelManager.add(model);
    if (result) {
      LOGGER.info("registering model :" + model.getClass().getName()
          + " for type:" + model.getSupportedType().getName());
    }
    return result;
  }

  @Override
  public boolean register(Object obj) {
    if (obj instanceof Model<?>) {
      return register((Model<?>) obj);
    }
    if (!configured) {
      throw new IllegalStateException(
          "can not add object before calling configure()");
    }
    LOGGER.info("{} - register({})", time, obj);
    injectDependencies(obj);
    if (obj instanceof TickListener) {
      addTickListener((TickListener) obj);
    }
    return modelManager.register(obj);
  }

  /**
   * {@inheritDoc} Unregistration from the models is delayed until all ticks are
   * processed.
   */
  @Override
  public boolean unregister(Object o) {
    if (o instanceof Model<?>) {
      throw new IllegalArgumentException("can not unregister a model");
    }
    if (!configured) {
      throw new IllegalStateException(
          "can not unregister object before calling configure()");
    }
    if (o instanceof TickListener) {
      removeTickListener((TickListener) o);
    }
    toUnregister.add(o);
    return true;
  }

  /**
   * Inject all required dependecies basing on the declared types of the object.
   * @param o object that need to have dependecies injected
   */
  protected void injectDependencies(Object o) {
    if (o instanceof SimulatorUser) {
      ((SimulatorUser) o).setSimulator(this);
    }
  }

  /**
   * Returns a safe to modify list of all models registered in the simulator.
   * @return list of models
   */
  public List<Model<?>> getModels() {
    return modelManager.getModels();
  }

  /**
   * Returns the {@link ModelProvider} that has all registered models.
   * @return The model provider
   */
  public ModelProvider getModelProvider() {
    return modelManager;
  }

  @Override
  public long getCurrentTime() {
    return time;
  }

  @Override
  public long getTimeStep() {
    return timeStep;
  }

  /**
   * Adds a tick listener to the simulator.
   * @param listener The listener to add.
   */
  public void addTickListener(TickListener listener) {
    tickListeners.add(listener);
  }

  /**
   * Removes the listener specified. Implemented in O(1).
   * @param listener The listener to remove
   */
  public void removeTickListener(TickListener listener) {
    tickListeners.remove(listener);
  }

  /**
   * Start the simulation.
   */
  public void start() {
    if (!configured) {
      throw new IllegalStateException(
          "Simulator can not be started when it is not configured.");
    }
    if (!isPlaying) {
      dispatcher.dispatchEvent(new Event(SimulatorEventType.STARTED, this));
    }
    isPlaying = true;
    while (isPlaying) {
      tick();
    }
    dispatcher.dispatchEvent(new Event(SimulatorEventType.STOPPED, this));
  }

  /**
   * Advances the simulator with one step (the size is determined by the time
   * step).
   */
  public void tick() {
    // unregister all pending objects
    Set<Object> copy;
    copy = toUnregister;
    toUnregister = new LinkedHashSet<Object>();

    for (final Object c : copy) {
      modelManager.unregister(c);
    }

    // using a copy to avoid concurrent modifications of this set
    // this also means that adding or removing a TickListener is
    // effectively executed after a 'tick'

    final List<TickListener> localCopy = new ArrayList<TickListener>();
    localCopy.addAll(tickListeners);

    final long end = time + timeStep;
    LOGGER.trace("{} ->----> tick ->----> {}", time, end);
    for (final TickListener t : localCopy) {
      timeLapse.initialize(time, end);
      t.tick(timeLapse);
    }
    timeLapse.initialize(time, end);
    // in the after tick the TimeLapse can no longer be consumed
    timeLapse.consumeAll();
    for (final TickListener t : localCopy) {
      t.afterTick(timeLapse);
    }
    time += timeStep;

  }

  /**
   * Either starts or stops the simulation depending on the current state.
   */
  public void togglePlayPause() {
    if (!isPlaying) {
      start();
    } else {
      isPlaying = false;
    }
  }

  /**
   * Resets the time to 0.
   */
  public void resetTime() {
    time = 0L;
  }

  /**
   * Stops the simulation.
   */
  public void stop() {
    isPlaying = false;
  }

  /**
   * @return true if simulator is playing, false otherwise.
   */
  public boolean isPlaying() {
    return isPlaying;
  }

  /**
   * @return <code>true</code> if the simulator is configured, see
   *         {@link Simulator} for more information.
   */
  public boolean isConfigured() {
    return configured;
  }

  /**
   * @return An unmodifiable view on the set of tick listeners.
   */
  public Set<TickListener> getTickListeners() {
    return Collections.unmodifiableSet(tickListeners);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RandomGenerator getRandomGenerator() {
    return rand;
  }

  @Override
  public Unit<Duration> getTimeUnit() {
    return timeLapse.getTimeUnit();
  }

  @Override
  public EventAPI getEventAPI() {
    return dispatcher.getPublicEventAPI();
  }
}
