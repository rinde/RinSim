/**
 * 
 */
package rinde.sim.core.model;

import java.util.List;

import com.google.common.collect.ImmutableList;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public abstract class SimpleModel<T> extends AbstractModelLink<T> implements
        Model {

    protected SimpleModel(Class<T> clazz) {
        super(clazz);
    }

    @Override
    public final List<? extends ModelLink<?>> getModelLinks() {
        return ImmutableList.of(this);
    }

}
