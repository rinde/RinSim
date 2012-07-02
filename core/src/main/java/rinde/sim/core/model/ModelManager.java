/**
 * 
 */
package rinde.sim.core.model;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

/**
 * Models manager keeps track of all models used in the simulator. It is
 * responsible for adding a simulation object to the appropriate models
 * 
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class ModelManager {

    private final Multimap<Class<? extends Object>, Model<? extends Object>> registry;
    private final List<Model<? extends Object>> models;
    private boolean configured;

    /**
     * Instantiate a new model manager.
     */
    public ModelManager() {
        registry = LinkedHashMultimap.create();
        models = new LinkedList<Model<? extends Object>>();
    }

    /**
     * Adds a model to the manager. Note: a model can be added only once.
     * @param model The model to be added.
     * @return true when the addition was sucessful, false otherwise.
     * @throws IllegalStateException when method called after calling configure
     */
    public boolean add(Model<?> model) {
        if (model == null) {
            throw new IllegalArgumentException("model can not be null");
        }
        checkState(!configured, "model can not be registered after configure()");
        Class<?> supportedType = model.getSupportedType();
        checkArgument(supportedType != null, "model must implement getSupportedType() and return a non-null");
        models.add(model);
        boolean result = registry.put(supportedType, model);
        if (!result) {
            models.remove(model);
        }
        return result;
    }

    /**
     * Method that allows for initialization of the manager (e.g., resolution of
     * the dependencies between models) Should be called after all models were
     * registered in the manager.
     */
    public void configure() {
        configured = true;
    }

    /**
     * Add object to all models that support a given object.
     * @param object object to register
     * @param <T> the type of object to register
     * @return <code>true</code> if object was added to at least one model
     */
    @SuppressWarnings("unchecked")
    public <T> boolean register(T object) {
        if (object == null) {
            throw new IllegalArgumentException("Can not register null");
        }
        if (object instanceof Model) {
            checkState(!configured, "model can not be registered after configure()");
            return add((Model<?>) object);
        }
        checkState(configured, "can not register an object if configure() has not been called");

        boolean result = false;
        Set<Class<?>> modelSupportedTypes = registry.keySet();
        for (Class<?> modelSupportedType : modelSupportedTypes) {
            if (modelSupportedType.isAssignableFrom(object.getClass())) {
                Collection<Model<?>> assignableModels = registry
                        .get(modelSupportedType);
                for (Model<?> m : assignableModels) {
                    result |= ((Model<T>) m).register(object);
                }
            }
        }
        return result;
    }

    /**
     * Unregister an object from all models it was attached to.
     * @param object object to unregister
     * @param <T> the type of object to unregister
     * @return <code>true</code> when the unregistration succeeded in at least
     *         one model
     * @throws IllegalStateException if the method is called before simulator is
     *             configured
     */
    @SuppressWarnings("unchecked")
    public <T> boolean unregister(T object) {
        if (object == null) {
            throw new IllegalArgumentException("can not unregister null");
        }
        checkArgument(!(object instanceof Model), "can not unregister a model");
        checkState(configured, "can not unregister when not configured, call configure() first");

        boolean result = false;
        Set<Class<?>> modelSupportedTypes = registry.keySet();
        for (Class<?> modelSupportedType : modelSupportedTypes) {
            // check if object is from a known type
            if (modelSupportedType.isAssignableFrom(object.getClass())) {
                Collection<Model<?>> assignableModels = registry
                        .get(modelSupportedType);
                for (Model<?> m : assignableModels) {
                    result |= ((Model<T>) m).unregister(object);
                }
            }
        }
        return result;
    }

    /**
     * @return An unmodifiable view on all registered models.
     */
    public List<Model<?>> getModels() {
        return Collections.unmodifiableList(models);
    }
}
