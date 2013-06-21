package rinde.sim.core.model;

/**
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * @param <T> basic type of element supported by model
 */
public interface Model<T> {

    // FIXME should be injected with a interface not the class itself
    // TODO, maybe add return value? e.g. other models?
    void initModel(ModelManager mm);

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
