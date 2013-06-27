/**
 * 
 */
package rinde.sim.core;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rinde.sim.core.model.Model;
import rinde.sim.core.model.ModelManager;
import rinde.sim.core.model.rng.RandomModel;
import rinde.sim.core.model.time.TickListener;
import rinde.sim.core.model.time.Time;
import rinde.sim.core.model.time.TimeController;
import rinde.sim.core.model.time.TimeModel;

/**
 * Simulator is the core class of a simulation. A simulator can only be created
 * by an instance of {@link AbstractBuilder}, the default implementation is
 * {@link Builder}.
 * <p>
 * A typical configuration looks like: TODO no longer correct:
 * 
 * <pre>
 * Simulator sim = Simulator.builder().addTimeModel(1000).add(model1).add(model2)
 *         .build();
 * </pre>
 * 
 * Now normal objects can be added to the simulator, e.g.:
 * 
 * <pre>
 * sim.register(myObject);
 * </pre>
 * 
 * Once you're done, you can start the simulator by calling:
 * 
 * <pre>
 * sim.start();
 * </pre>
 * 
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be> - simulator API
 *         changes
 */
public class Simulator implements SimulatorAPI {

    /**
     * The logger of the simulator.
     */
    protected static final Logger LOGGER = LoggerFactory
            .getLogger(Simulator.class);

    /**
     * Model manager instance.
     */
    protected final ModelManager modelManager;

    /**
     * An indirect reference to a {@link Time} object, to allow controlling time
     * progress.
     */
    protected final TimeReference timeReference;

    /**
     * Create a new simulator instance.
     * @param r The random number generator that is used in this simulator.
     * @param step The time that passes each tick. This can be in any unit the
     *            programmer prefers.
     */
    private Simulator(TimeReference tr, ModelManager mm) {
        timeReference = tr;
        modelManager = mm;
    }

    /**
     * Starts the simulation.
     */
    public void start() {
        timeReference.start();
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void register(Object obj) {
        modelManager.register(obj);
    }

    /**
     * Unregistration from the models is delayed until all ticks are processed.
     * 
     * @see rinde.sim.core.SimulatorAPI#unregister(java.lang.Object)
     */
    @Override
    public void unregister(Object o) {
        modelManager.unregister(o);
    }

    /**
     * @return A {@link Builder} to construct {@link Simulator} instances.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Convenience method for creating a {@link Simulator} with the default
     * {@link TimeModel}, specified timeStep and any {@link Model}s. For
     * requirements about adding {@link Model}s see,
     * {@link rinde.sim.core.model.ModelManager.Builder}.
     * @param timeStep The time step to use in the {@link TimeModel}.
     * @param models The models to use.
     * @return A {@link Simulator}.
     */
    public static Simulator build(long timeStep, Model... models) {
        final Builder b = builder();
        b.addTimeModel(timeStep);
        for (final Model m : models) {
            b.add(m);
        }
        return b.build();
    }

    private static final class TimeReference implements TimeController {

        @Nullable
        private Time time;

        @Override
        public void receiveTime(Time t) {
            time = t;
        }

        @SuppressWarnings("null")
        public void start() {
            // this variable can never be null, see AbstractBuilder.build()
            time.start();
        }
    }

    /**
     * This is the default builder for creating {@link Simulator} objects. For
     * more information about the requirements for adding {@link Model}s, see
     * {@link rinde.sim.core.model.ModelManager.Builder}.
     * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
     */
    public static class Builder extends AbstractBuilder {

        /**
         * Adds a {@link RandomModel} to the simulator.
         * @param seed The seed which will be used for the random number
         *            generator.
         * @return This builder instance.
         */
        public Builder addRandomModel(long seed) {
            return (Builder) add(RandomModel.builder().withSeed(seed));
        }

        // TODO think about ordering of Builders vs Models -> it can no longer
        // follow insertion order since they are distributed over two
        // datastructures
        /**
         * Add a Model, see {@link rinde.sim.core.model.ModelManager.Builder}
         * for more information.
         * @param m The model to add.
         * @return This builder instance.
         */
        public Builder add(Model m) {
            mmBuilder.add(m);
            return this;
        }

        /**
         * Add a {@link TimeModel} with the specified timeStep. See
         * {@link rinde.sim.core.model.ModelManager.Builder} for more
         * information about adding models.
         * @param timeStep The timestep to use in the {@link TimeModel}.
         * @return This builder instance.
         */
        public Builder addTimeModel(long timeStep) {
            return (Builder) add(TimeModel.builder().withTimeStep(timeStep));
        }

        // /**
        // * Add a {@link TimeModel}. See
        // * {@link rinde.sim.core.model.ModelManager.Builder} for more
        // * information about adding models.
        // * @param tm The {@link TimeModel} to add.
        // * @return This builder instance.
        // */
        // public Builder addTimeModel(TimeModel tm) {
        // mmBuilder.add(tm);
        // return this;
        // }

        /**
         * @return This builder instance.
         */
        public Builder addScenario() {
            // TODO add scenario
            throw new UnsupportedOperationException();
        }

        /**
         * @return This builder instance.
         */
        public Builder addStopCondition() {
            // TODO add stop conditions
            throw new UnsupportedOperationException();
        }

    }

    /**
     * Abstract builder for creating {@link Simulator} objects.
     * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
     */
    public abstract static class AbstractBuilder implements IBuilder<Simulator> {
        /**
         * Reference to the {@link rinde.sim.core.model.ModelManager.Builder}
         * that is used.
         */
        protected ModelManager.Builder mmBuilder;

        protected List<IBuilder<? extends Model>> modelBuilders;

        /**
         * Instantiates the builder.
         */
        protected AbstractBuilder() {
            mmBuilder = ModelManager.builder();
            modelBuilders = newArrayList();
        }

        public AbstractBuilder add(IBuilder<? extends Model> b) {
            modelBuilders.add(b);
            return this;
        }

        /**
         * @return A configured {@link Simulator}.
         * @throws IllegalArgumentException if there is no Model responsible for
         *             {@link TickListener}s.
         */
        @Override
        public Simulator build() {

            for (final IBuilder<? extends Model> builder : modelBuilders) {
                mmBuilder.add(builder.build());
            }

            // TODO check for repeated calls in case Models have been added
            // directly
            checkArgument(mmBuilder.containsLinkFor(TickListener.class), "A Simulator can not be build without a ModelLink to TickListener, for a default implementation use TimeModel via addTimeModel(..)");
            final TimeReference tr = new TimeReference();
            final ModelManager mm = mmBuilder.build();
            mm.register(tr);
            return new Simulator(tr, mm);
        }
    }
}
