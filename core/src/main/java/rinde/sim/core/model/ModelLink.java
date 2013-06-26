/**
 * 
 */
package rinde.sim.core.model;

/**
 * A ModelLink defines an interest of a {@link Model} (a link) in a certain type
 * (T). The ModelLink receives calls to {@link #register(Object)} and
 * {@link #unregister(Object)} which it typically forwards to its associated
 * {@link Model}. The {@link ModelLink} does not contain a reference to the
 * {@link Model}, the {@link Model} declares its links via
 * {@link Model#getModelLinks()}. Typically, implementations of
 * {@link ModelLink} are inner classes of its model.
 * @param <T> The type in which the {@link Model} is interested.
 * 
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public interface ModelLink<T extends Object> {

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
