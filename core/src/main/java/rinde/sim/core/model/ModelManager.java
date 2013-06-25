/**
 * 
 */
package rinde.sim.core.model;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;

import java.util.Collection;
import java.util.Collections;
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
public class ModelManager implements ModelProvider {

    private final Multimap<Class<? extends Object>, ModelLink<? extends Object>> registry;
    private final List<Model> models;
    private boolean configured;

    /**
     * Instantiate a new model manager.
     */
    public ModelManager() {
        registry = LinkedHashMultimap.create();
        models = newArrayList();
    }

    /**
     * Adds a model to the manager. Note: a model can be added only once.
     * @param model The model to be added.
     * @throws IllegalStateException when method called after calling configure
     */
    public void add(Model model) {
        if (model == null) {
            throw new IllegalArgumentException("model can not be null");
        }
        checkState(!configured, "model can not be registered after configure()");
        final Collection<? extends ModelLink<?>> modelLinks = model
                .getModelLinks();
        checkArgument(!modelLinks.isEmpty(), "every model needs to define at least one link");
        for (final ModelLink<?> m : modelLinks) {
            final Class<?> supportedType = m.getSupportedType();

            checkArgument(supportedType != null, "modellink must implement getSupportedType() and return a non-null");
            checkArgument(!registry.containsKey(supportedType), "Model conflict: there can be at most one ModelLink for every class");
            registry.put(supportedType, m);
        }
        models.add(model);
    }

    /**
     * Method that allows for initialization of the manager (e.g., resolution of
     * the dependencies between models) Should be called after all models were
     * registered in the manager.
     */
    public void configure() {
        for (final Model m : models) {
            if (m instanceof ModelReceiver) {
                ((ModelReceiver) m).registerModelProvider(this);
            }
        }
        configured = true;
    }

    /**
     * Add object to all models that support a given object.
     * @param object object to register
     * @param <T> the type of object to register
     * @return <code>true</code> if object was added to at least one model
     */
    @SuppressWarnings("unchecked")
    public <T> void register(T object) {
        if (object == null) {
            throw new IllegalArgumentException("Can not register null");
        }
        if (object instanceof Model) {
            checkState(!configured, "model can not be registered after configure()");
            add((Model) object);
            return;
        }
        checkState(configured, "can not register an object if configure() has not been called");

        boolean result = false;
        final Set<Class<?>> modelSupportedTypes = registry.keySet();
        for (final Class<?> modelSupportedType : modelSupportedTypes) {
            if (modelSupportedType.isAssignableFrom(object.getClass())) {
                final Collection<ModelLink<?>> assignableModels = registry
                        .get(modelSupportedType);
                for (final ModelLink<?> m : assignableModels) {
                    result |= ((ModelLink<T>) m).register(object);
                }
            }
        }
        checkState(result);
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
    public <T> void unregister(T object) {
        if (object == null) {
            throw new IllegalArgumentException("can not unregister null");
        }
        checkArgument(!(object instanceof Model), "can not unregister a model");
        checkState(configured, "can not unregister when not configured, call configure() first");

        boolean result = false;
        final Set<Class<?>> modelSupportedTypes = registry.keySet();
        for (final Class<?> modelSupportedType : modelSupportedTypes) {
            // check if object is from a known type
            if (modelSupportedType.isAssignableFrom(object.getClass())) {
                final Collection<ModelLink<?>> assignableModels = registry
                        .get(modelSupportedType);
                for (final ModelLink<?> m : assignableModels) {
                    result |= ((ModelLink<T>) m).unregister(object);
                }
            }
        }
        checkState(result);
    }

    /**
     * @return An unmodifiable view on all registered models.
     */
    public List<Model> getModels() {
        return Collections.unmodifiableList(models);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Model> T getModel(Class<T> clazz) {
        for (final Model model : models) {
            if (clazz.isInstance(model)) {
                return (T) model;
            }
        }
        throw new IllegalArgumentException("There is no model of type: "
                + clazz);
    }
}
