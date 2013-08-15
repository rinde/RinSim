/**
 * 
 */
package rinde.sim.core.model;

import java.util.List;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class TestModelProvider implements ModelProvider {

    List<? extends Model<?>> models;

    public TestModelProvider(List<? extends Model<?>> ms) {
        models = ms;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Model<?>> T getModel(Class<T> clazz) {
        for (final Model<?> model : models) {
            if (clazz.isInstance(model)) {
                return (T) model;
            }
        }
        throw new IllegalArgumentException("There is no model of type: "
                + clazz);
    }

}
