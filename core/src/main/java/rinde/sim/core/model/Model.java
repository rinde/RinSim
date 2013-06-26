package rinde.sim.core.model;

import java.util.List;

/**
 * A model is an object that models a part of a problem in a simulation. A model
 * can be linked to one or more types which it manages. These links need to be
 * defined using {@link ModelLink}s.
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public interface Model {

    /**
     * @return The list of {@link ModelLink}s.
     */
    List<? extends ModelLink<?>> getModelLinks();
}
