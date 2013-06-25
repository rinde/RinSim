/**
 * 
 */
package rinde.sim.core.model;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public interface ModelLink<T> {

    /**
     * Register element in a model.
     * @param element the <code>! null</code> should be imposed
     * @return true if the object was successfully registered
     */
    boolean register(T element);

    /**
     * Unregister element from a model.
     * @param element the <code>! null</code> should be imposed
     * @return true if the unregistration changed the model (element was part of
     *         the model and it was succesfully removed)
     */
    boolean unregister(T element);

    /**
     * @return The class of the type supported by this model.
     */
    Class<T> getSupportedType();

}
