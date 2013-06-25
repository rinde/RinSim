/**
 * 
 */
package rinde.sim.core;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rinde.sim.core.model.Model;
import rinde.sim.core.model.ModelManager;
import rinde.sim.core.model.ModelProvider;
import rinde.sim.core.model.time.TickListener;
import rinde.sim.core.model.time.TimeLapse;
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

        CONFIGURED
    }

    /**
     * Model manager instance.
     */
    protected final ModelManager modelManager;
    private boolean configured;

    private final Set<Object> toUnregister;

    /**
     * Reference to dispatcher of simulator events, can be used by subclasses to
     * issue additional events.
     */
    protected final EventDispatcher dispatcher;

    // private final ReentrantLock unregisterLock;

    // TODO investigate if a TimeModel should be created, this would move all
    // time/tick related stuff into its own class. Making it easier to extend
    // this part.
    /**
     * Create a new simulator instance.
     * @param r The random number generator that is used in this simulator.
     * @param step The time that passes each tick. This can be in any unit the
     *            programmer prefers.
     */
    public Simulator() {

        // unregisterLock = new ReentrantLock();
        toUnregister = new LinkedHashSet<Object>();

        modelManager = new ModelManager();

        dispatcher = new EventDispatcher(SimulatorEventType.values());
    }

    public void start() {
        if (!configured) {
            throw new IllegalStateException(
                    "Simulator can not be started when it is not configured.");
        }
        // TODO time model start?
    }

    /**
     * This configures the {@link Model}s in the simulator. After calling this
     * method models can no longer be added, objects can only be registered
     * after this method is called.
     * @see ModelManager#configure()
     */
    public void configure() {
        // for (final Model<?> m : modelManager.getModels()) {
        // if (m instanceof TickListener) {
        // LOGGER.info("adding " + m.getClass().getName()
        // + " as a tick listener");
        // addTickListener((TickListener) m);
        // }
        // }
        modelManager.configure();
        configured = true;
        dispatcher
                .dispatchEvent(new Event(SimulatorEventType.CONFIGURED, this));
    }

    // TODO create a SimulatorBuilder for configuration of Simulator?
    // TODO should fail on error instead of returning a boolean
    /**
     * Register a model to the simulator.
     * @param model The {@link Model} instance to register.
     * @return true if succesful, false otherwise
     */
    public void register(Model model) {
        if (model == null) {
            throw new IllegalArgumentException("model can not be null");
        }
        if (configured) {
            throw new IllegalStateException(
                    "cannot add model after calling configure()");
        }
        // final boolean result =
        modelManager.add(model);
        // if (result) {
        LOGGER.info("registering model :" + model.getClass().getName()
                + " with links:" + model.getModelLinks());
        // }
        // return result;
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public boolean register(Object obj) {
        if (obj == null) {
            throw new IllegalArgumentException("parameter can not be null");
        }
        if (obj instanceof Model) {
            register((Model) obj);
            return true;
        }
        if (!configured) {
            throw new IllegalStateException(
                    "can not add object before calling configure()");
        }
        injectDependencies(obj);
        // if (obj instanceof TickListener) {
        // addTickListener((TickListener) obj);
        // }
        modelManager.register(obj);
        return true;
    }

    /**
     * Unregistration from the models is delayed until all ticks are processed.
     * 
     * @see rinde.sim.core.SimulatorAPI#unregister(java.lang.Object)
     */
    @Override
    public boolean unregister(Object o) {
        if (o == null) {
            throw new IllegalArgumentException("parameter cannot be null");
        }
        if (o instanceof Model) {
            throw new IllegalArgumentException("can not unregister a model");
        }
        if (!configured) {
            throw new IllegalStateException(
                    "can not unregister object before calling configure()");
        }
        // if (o instanceof TickListener) {
        // removeTickListener((TickListener) o);
        // }
        // unregisterLock.lock();
        try {
            toUnregister.add(o);
        } finally {
            // unregisterLock.unlock();
        }
        return true;
    }

    /**
     * Inject all required dependecies basing on the declared types of the
     * object.
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
    public List<Model> getModels() {
        return modelManager.getModels();
    }

    /**
     * Returns the {@link ModelProvider} that has all registered models.
     * @return The model provider
     */
    public ModelProvider getModelProvider() {
        return modelManager;
    }

    public boolean isConfigured() {
        return configured;
    }

    /**
     * Reference to the {@link EventAPI} of the Simulator. Can be used to add
     * listeners to events dispatched by the simulator. Simulator events are
     * defined in {@link SimulatorEventType}.
     * @return {@link EventAPI}
     */
    public EventAPI getEventAPI() {
        return dispatcher.getPublicEventAPI();
    }
}
