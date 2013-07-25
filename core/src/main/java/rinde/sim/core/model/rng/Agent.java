/**
 * 
 */
package rinde.sim.core.model.rng;

import static com.google.common.collect.Maps.newHashMap;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import rinde.sim.core.IBuilder;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.Invokable;
import com.google.common.reflect.Parameter;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class Agent {

    /**
     * 
     */
    protected final RandomModel randomModel;

    private Agent(RandomModel rm) {
        randomModel = rm;
    }

    public static class Builder implements IBuilder<Agent> {

        @Nullable
        protected RandomModel randomModel;

        @Injectable
        public Builder injectRandomModel(RandomModel rm) {
            randomModel = rm;
            return this;
        }

        @Override
        public Agent build() {
            return new Agent(randomModel);
        }

    }

    // should be defined in a builder or a model
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Injectable {}

    // injectors must have a return type, this can be checked upon registration
    // of model?
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Injector {
        /**
         * The type which it can inject in injectables
         * @return
         */
        Class<?> value();
    }

    interface Model {}

    public static class ModelTest implements Model {

        public RandomModel inject() {
            return new RandomModel(123);
        }

        // or do type via return type?
        @Injector(RandomModel.class)
        public boolean inject(Object o) {

            // getInjectableFor(RandomModel.class).call(new RandomModel(123))
            b.injectRandomModel(new RandomModel(123));
            return true;
        }

    }

    public static class Manager {

        final Map<Class<?>, Invokable<?, ?>> mapping = newHashMap();

        public Manager() {}

        public void add(Model m) {
            final Set<Invokable<?, ?>> set = getMethodsWithAnnotation(m, Injector.class);
            for (final Invokable<?, ?> i : set) {
                mapping.put(i.getAnnotation(Injector.class).value(), i);
            }
        }

        public void register(Object o) {
            final Set<Invokable<?, ?>> set = getMethodsWithAnnotation(o, Injectable.class);
            for (final Invokable<?, ?> i : set) {

                final Invokable<?, ?> injector = mapping.get(i.getParameters()
                        .iterator().next().getType().getRawType());
                injector.invoke(receiver, args)
            }
        }
    }

    static <T extends Annotation> ImmutableSet<InjectorInstance> getAnnots(
            Object o, Class<T> annot) {
        final ImmutableSet.Builder<InjectorInstance> builder = ImmutableSet
                .builder();
        for (final Method m : o.getClass().getMethods()) {
            final T annotation = m.getAnnotation(annot);
            if (annotation != null) {
                builder.add(new InjectorInstance(o, Invokable.from(m)));
            }
        }
        return builder.build();
    }

    static ImmutableSet<Invokable<?, ?>> getMethodsWithAnnotation(Object o,
            Class<? extends Annotation> clz) {
        final ImmutableSet.Builder<Invokable<?, ?>> builder = ImmutableSet
                .builder();
        for (final Method m : o.getClass().getMethods()) {
            final Annotation annotation = m.getAnnotation(clz);
            if (annotation != null) {
                builder.add(Invokable.from(m));
            }
        }
        return builder.build();
    }

    public static void main(String[] args) {

        final Builder b = new Builder();

        final ModelTest mt = new ModelTest();

        final Set<InjectorInstance> set = getAnnots(mt, Injector.class);

        final Map<Class<?>, InjectorInstance> map = newHashMap();
        for (final InjectorInstance ii : set) {
            final Parameter param = (Parameter) ii.m.getParameters().iterator()
                    .next();
            map.put(param.getType().getRawType(), ii);
        }

        System.out.println(map);

        // final Map<Class<?>, Meth<?>> map = newHashMap();
        //
        // for (final Method m : mt.getClass().getMethods()) {
        // final Injector annot = m.getAnnotation(Injector.class);
        // if (annot != null) {
        // System.out.println(Arrays.toString(m.getParameterTypes()));
        //
        // map.put(m.getParameterTypes()[0], new Meth(mt, Invokable
        // .from(m)));
        // }
        // }

        // System.out.println(map);

        final Method[] methods = b.getClass().getMethods();

        for (final Method m : methods) {
            final Injectable annot = m.getAnnotation(Injectable.class);
            if (annot != null) {

                final Class<?>[] types = m.getParameterTypes();

                // how to find instances of these types?
                // type to instance map? -> via injector annot
                System.out.println(Arrays.toString(types));
                final Invokable<?, ?> i = Invokable.from(m);
                final List<Parameter> params = i.getParameters();

                // i.invoke(b, args)
                // i.invoke(b, args)
                // m.invoke(obj, args)
            }
        }

    }

    static class InjectorInstance<T> {
        public final Object ref;
        public final Invokable<T, Boolean> m;

        public InjectorInstance(Object r, Invokable<T, Boolean> mm) {
            ref = r;
            m = mm;
        }

    }
}
