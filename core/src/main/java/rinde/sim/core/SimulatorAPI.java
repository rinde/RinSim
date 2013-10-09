package rinde.sim.core;

import javax.measure.quantity.Duration;
import javax.measure.unit.Unit;

import org.apache.commons.math3.random.RandomGenerator;

import rinde.sim.event.EventAPI;

/**
 * Limited simulator API that provides an API for simulation elements (e.g.,
 * agents).
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * @since 2.0
 * 
 */
public interface SimulatorAPI {

  /**
   * Register a given entity in the simulator. During registration the object is
   * provided all features it requires (declared by interfaces) and bound to the
   * required models (if they were registered in the simulator before).
   * @param o object to register
   * @return <code>true</code> when registration of the object in the simulator
   *         was successful
   * @throws IllegalStateException when simulator is not configured (by calling
   *           {@link Simulator#configure()}
   */
  boolean register(Object o);

  /**
   * Unregister an object from simulator.
   * @param o The object to be unregistered.
   * @return True if the object could be unregistered, false otherwise.
   */
  boolean unregister(Object o);

  /**
   * Get access to the main random generator used in the simulator.
   * @return the random generator of the simulator
   */
  RandomGenerator getRandomGenerator();

  /**
   * @return The current simulation time.
   */
  long getCurrentTime();

  /**
   * @return The time step (in simulation time) which is added to current time
   *         at every tick.
   */
  long getTimeStep();

  /**
   * @return The unit of time that is used for generating ticks.
   */
  Unit<Duration> getTimeUnit();

  /**
   * Reference to the {@link EventAPI} of the Simulator. Can be used to add
   * listeners to events dispatched by the simulator. Simulator events are
   * defined in {@link rinde.sim.core.Simulator.SimulatorEventType}.
   * @return {@link EventAPI}
   */
  EventAPI getEventAPI();
}
