/**
 * 
 */
package rinde.sim.core;

import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

import rinde.sim.core.model.Model;
import rinde.sim.core.model.rng.RandomModel;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class SimulatorBuilder {
    // TODO should be extensible, esp. for problem instances
    // should be possible to disable some of the options to add restrictions in
    // the config

    protected List<Model<?>> models;

    protected SimulatorBuilder() {
        models = newArrayList();
    }

    public SimulatorBuilder addRandomModel(long seed) {
        models.add(new RandomModel(seed));
        return this;
    }

    public SimulatorBuilder addModel(Model<?> m) {
        models.add(m);
        return this;
    }

    public SimulatorBuilder addScenario() {
        // TODO add scenario
        return this;
    }

    public SimulatorBuilder addStopCondition() {
        // TODO add stop conditions
        return this;
    }

    public Simulator build() {
        // TODO check all configuration stuff

        // TODO add TimeModel if not specified
        final Simulator s = new Simulator();
        for (final Model<?> m : models) {
            s.register(m);
        }
        return s;
    }

    public static SimulatorBuilder create() {
        return new SimulatorBuilder();
    }

    // public static <T extends SimulatorBuilder> T create(Class<T> clazz) {
    //
    // }

}
