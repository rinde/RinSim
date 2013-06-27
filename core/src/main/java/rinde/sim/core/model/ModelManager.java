/**
 * 
 */
package rinde.sim.core.model;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Sets.newLinkedHashSet;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Models manager keeps track of all models used in the simulator. Further it
 * provides methods for registering and unregistering objects to these models.
 * To obtain an instance use {@link #build(Model...)} or {@link #builder()}.
 * 
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class ModelManager {

    /**
     * An immutable map of types to {@link ModelLink}s.
     */
    protected final Map<Class<?>, ModelLink<?>> registry;

    /**
     * An immutable list of models.
     */
    protected final List<Model> models;

    /**
     * Instantiate a new model manager.
     * @param reg {@link #registry}
     * @param ms {@link #models}
     */
    protected ModelManager(Map<Class<?>, ModelLink<?>> reg, List<Model> ms) {
        registry = reg;
        models = ms;
    }

    /**
     * Add object to all models that support a given object.
     * @param object object to register
     * @param <T> the type of object to register
     */
    @SuppressWarnings("unchecked")
    public <T> void register(T object) {
        checkArgument(!(object instanceof Model), "Can not register a model, for adding models see %s", ModelManager.Builder.class);
        boolean isRegistered = false;
        final Set<Class<?>> modelSupportedTypes = registry.keySet();
        for (final Class<?> modelSupportedType : modelSupportedTypes) {
            if (modelSupportedType.isAssignableFrom(object.getClass())) {
                final ModelLink<T> assignableModelLink = (ModelLink<T>) registry
                        .get(modelSupportedType);
                isRegistered |= assignableModelLink.register(object);
            }
        }
        checkArgument(isRegistered, "The object could not be registered to any model.");
    }

    /**
     * Unregister an object from any models it was registered to. If the object
     * was not registered to any {@link Model} known to {@link ModelManager} an
     * exception is thrown. This method can not be used to unregister
     * {@link Model}s.
     * @param object Object to unregister
     * @param <T> the type of object to unregister
     */
    @SuppressWarnings("unchecked")
    public <T> void unregister(T object) {
        checkArgument(!(object instanceof Model), "Models can not be unregistered.");

        boolean isUnregistered = false;
        final Set<Class<?>> modelSupportedTypes = registry.keySet();
        for (final Class<?> modelSupportedType : modelSupportedTypes) {
            // check if object is a known type
            if (modelSupportedType.isAssignableFrom(object.getClass())) {
                final ModelLink<T> assignableModelLink = (ModelLink<T>) registry
                        .get(modelSupportedType);
                isUnregistered |= assignableModelLink.unregister(object);
            }
        }
        checkArgument(isUnregistered, "The object could not be unregistered from any model. Current models: %s", models);
    }

    /**
     * @return A {@link Builder} to construct {@link ModelManager} instances.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Convenience method for creating a {@link ModelManager}, uses
     * {@link Builder} internally.
     * @param models The models which are used in the manager.
     * @return A fully configured {@link ModelManager}.
     */
    public static ModelManager build(Model... models) {
        final Builder b = builder();
        for (final Model m : models) {
            b.add(m);
        }
        return b.build();
    }

    /**
     * A builder for creating {@link ModelManager} instances.
     * 
     * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
     */
    public static final class Builder {

        private final ImmutableMap.Builder<Class<?>, ModelLink<?>> registryBuilder;
        private final ImmutableList.Builder<Model> modelsBuilder;
        private final Set<Class<?>> linkTypes;

        private Builder() {
            registryBuilder = ImmutableMap.builder();
            modelsBuilder = ImmutableList.builder();
            linkTypes = newLinkedHashSet();
        }

        /**
         * Adds a model to the manager. Any type be linked to by a
         * {@link ModelLink} at most once. {@link Model}, will cause
         * {@link #build()} to fail.
         * @param model The model to be added.
         * @return This builder instance.
         * @throws IllegalArgumentException if a ModelLink links to an already
         *             linked type.
         */
        public Builder add(Model model) {
            final Collection<? extends ModelLink<?>> modelLinks = model
                    .getModelLinks();
            checkArgument(!modelLinks.isEmpty(), "Implementations of \"%s\" need to define at least one link. Model: %s", Model.class, model);
            for (final ModelLink<?> modelLink : modelLinks) {
                final Class<?> supportedType = modelLink.getSupportedType();
                checkArgument(supportedType != null, "Implementations of \"%s\" must implement getSupportedType() and return a non-null", ModelLink.class);
                // TODO improve error message: point to two Models + ModelLinks
                // which have caused the error
                checkArgument(!linkTypes.contains(supportedType), "Any type can be linked to by only one ModelLink. Found a duplicate in Model: %s, ModelLink: %s, type: %s", model, modelLink, supportedType);
                linkTypes.add(supportedType);
                registryBuilder.put(supportedType, modelLink);
            }
            modelsBuilder.add(model);
            return this;
        }

        /**
         * Creates a new {@link ModelManager} instance as configured by this
         * builder.
         * @return The newly created instance.
         * @throws IllegalArgumentException if duplicate {@link Model}s or
         *             duplicate {@link ModelLink}s have been added.
         */
        public ModelManager build() {
            final Map<Class<?>, ModelLink<?>> reg = registryBuilder.build();
            final List<Model> models = modelsBuilder.build();
            // link models to each other
            final Set<Entry<Class<?>, ModelLink<?>>> set = reg.entrySet();
            for (final Entry<Class<?>, ModelLink<?>> entry : set) {
                for (final Model m : models) {
                    if (entry.getKey().isAssignableFrom(m.getClass())) {
                        // use workaround for registering (is needed for
                        // generics)
                        register(entry.getValue(), m);
                    }
                }
            }
            return new ModelManager(reg, models);
        }

        public boolean containsLinkFor(Class<?> cl) {
            return linkTypes.contains(cl);
        }

        @SuppressWarnings("unchecked")
        private static <T> void register(ModelLink<T> ml, Model m) {
            ml.register((T) m);
        }
    }
}
