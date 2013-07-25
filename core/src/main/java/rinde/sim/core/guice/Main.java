/**
 * 
 */
package rinde.sim.core.guice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import rinde.sim.core.graph.Point;
import rinde.sim.core.guice.ConcreteModelABC.AbstractBuilder;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.Subscribe;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class Main {

    public static void main(String[] args) {

        final ModelManager mm = new ModelManager();
        mm.register(ConcreteRootModel.builder());
        mm.register(ConcreteModelABC.builder());
        // mm.register(ConcreteModel.class);
        // mm.register(new ConcreteModelBuilder());
        // mm.register(new ConcreteRootModelBuilder());
        mm.configure();

        mm.add(new AgentBuilder());
    }

    static class GenericModule<T> extends AbstractModule {

        private final Provider<T> provider;
        private final Class<T> rootType;
        private final ImmutableList<Class<?>> bindings;

        public GenericModule(Class<T> rt, Provider<T> p) {
            provider = p;
            rootType = rt;
            final Class<?>[] interfaces = rootType.getInterfaces();
            final ImmutableList.Builder<Class<?>> builder = ImmutableList
                    .builder();
            for (final Class<?> inf : interfaces) {
                if (PublicAPI.class.isAssignableFrom(inf)) {
                    builder.add(inf);
                }
            }
            bindings = builder.build();
        }

        @Override
        protected void configure() {

            // if (rootType.equals(ConcreteModel.class)) {
            // System.out.println("add factory");
            // install(new FactoryModuleBuilder()
            // .implement(GenericModel.class, ConcreteModel.class)
            // .build(ConcreteModelFactory.class));
            //
            // for (int i = 0; i < bindings.length; i++) {
            // bind(bindings[i], rootType);
            // }
            //
            // bind(rootType).in(Singleton.class);
            // } else {

            for (final Class<?> binding : bindings) {

                // if (binding.isAssignableFrom(rootType)) {
                bindHelper(binding, rootType);
                // } else {
                // System.out.println(binding + ", " + rootType
                // + " are not subtypes!");
                // checkArgument((binding.isMemberClass()
                // || binding.isLocalClass() || binding
                // .isAnonymousClass())
                // && !Modifier.isAbstract(binding.getModifiers()));
                // }
                // bind(bindings[i], provider);
                // bind(bindings[i]).in(Singleton.class);
            }
            System.out.println(rootType + " -> " + provider);
            bind(rootType).toProvider(provider).asEagerSingleton();

            // bind(rootType).in(Singleton.class);

            // }
        }

        private <U> void bindHelper(Class<U> interfaceType, Provider<?> prov) {
            System.out.println(interfaceType + " -> " + prov);
            bind(interfaceType).toProvider((Provider<? extends U>) prov)
                    .asEagerSingleton();;

        }

        // helper method for casting to generic type -> this is safe! TODO check
        @SuppressWarnings("unchecked")
        private <U> void bindHelper(Class<U> superType,
                Class<?> implementationType) {
            System.out.println(superType + " -> " + implementationType);
            bind(superType).to((Class<? extends U>) implementationType);
        }
    }

    interface ConcreteModelFactory {
        ModelA create(Point p);
    }

    static class Agent {

        @Inject
        public Agent(ModelA gm, ModelB mb) {
            System.out.println("Agent init");
            System.out.println(" > " + gm);
            System.out.println(" > " + mb);
            gm.test();
        }

        @Subscribe
        public void tick(TestEvent tl) {
            System.out.println(tl);
        }
    }

    static class AgentBuilder extends AbstractBuilder<Agent> {
        // ModelA modelA;
        // ModelBGuard modelBguard;

        @InitData
        final Point startPosition;

        public AgentBuilder() {
            startPosition = new Point(10, 16);
        }

        // @Inject
        // void inject(ModelA ma, ModelBGuard mb) {
        // modelA = ma;
        // modelBguard = mb;
        // }

        @Override
        public Agent build() {
            return null;// new Agent(modelA, modelBguard);
        }
    }

    static class TestEvent {}

}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface Registerer {

}
