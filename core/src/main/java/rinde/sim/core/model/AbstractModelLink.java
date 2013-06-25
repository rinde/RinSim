/**
 * 
 */
package rinde.sim.core.model;

/**
 * Basic implementation that provides a getSupportedType method implementation.
 * @param <T> The type that is supported by this model.
 * 
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public abstract class AbstractModelLink<T> implements ModelLink<T> {

    private final Class<T> clazz;

    /**
     * Create a new model.
     * @param pClazz The class that represents the supported type of this model.
     */
    protected AbstractModelLink(Class<T> pClazz) {
        this.clazz = pClazz;
    }

    @Override
    public final Class<T> getSupportedType() {
        return clazz;
    }
}
