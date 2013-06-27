/**
 * 
 */
package rinde.sim.core.model.rng;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import rinde.sim.core.IBuilder;
import rinde.sim.core.model.SimpleModel;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class RandomModel extends SimpleModel<RandomReceiver> {
    // TODO RandomGenerator should be moved into an own model. This way, objects
    // that need a reference to a random generator can get one by implementing
    // this model's interface. The model could have several policies for
    // distributing RNGs: ALL_SAME, CLASS_SAME, ALL_DIFFERENT. This would
    // indicate: every subscribing object uses same RNG, objects of the same
    // class share same RNG, all objects get a different RNG instance
    // respectively.

    protected RandomGenerator randomGenerator;

    protected RandomModel(long seed) {
        this(new MersenneTwister(seed));
    }

    protected RandomModel(RandomGenerator rng) {
        super(RandomReceiver.class);
        randomGenerator = rng;
    }

    @Override
    public boolean register(RandomReceiver element) {
        element.receiveRandom(randomGenerator);
        return true;
    }

    @Override
    public boolean unregister(RandomReceiver element) {
        return true;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static RandomModel build(long seed) {
        return builder().withSeed(seed).build();
    }

    public static class Builder implements IBuilder<RandomModel> {

        protected long seed;

        public Builder withSeed(long s) {
            seed = s;
            return this;
        }

        @Override
        public RandomModel build() {
            return new RandomModel(seed);
        }

    }
}
