/**
 * 
 */
package rinde.sim.core.model;

import java.util.List;

import com.google.common.collect.ImmutableList;

/**
 * This is a {@link Model} which defines exactly one {@link ModelLink}, namely
 * itself.
 * @param <T> The type in which the model is interested.
 * 
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public abstract class SimpleModel<T> extends AbstractModelLink<T> implements
        Model {

    /**
     * Instantiates the model with the corresponding class.
     * @param clazz The class of the type in which the model is interested.
     */
    protected SimpleModel(Class<T> clazz) {
        super(clazz);
    }

    @Override
    public final List<? extends ModelLink<?>> getModelLinks() {
        return ImmutableList.of(this);
    }

}
