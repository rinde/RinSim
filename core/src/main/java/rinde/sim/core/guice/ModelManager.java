package rinde.sim.core.guice;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newLinkedHashMap;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import rinde.sim.core.IBuilder;
import rinde.sim.core.guice.ConcreteModelABC.AbstractBuilder;
import rinde.sim.core.guice.ConcreteModelABC.ModelBGuard;
import rinde.sim.core.guice.Main.GenericModule;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.reflect.Invokable;
import com.google.common.reflect.Parameter;
import com.google.common.reflect.TypeToken;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

class ModelManager {

    List<Module> modules = newArrayList();

    Multimap<TypeToken<?>, Tuple> registerers;

    Injector injector;

    public ModelManager() {
        registerers = LinkedHashMultimap.create();
    }

    // public void register(Class<?> model) {
    // checkArgument(!model.isInterface());
    // // this adds models as modules, it uses its annotation for bindings
    // // modules.add(new GenericModule(model, model.getAnnotation(Model.class)
    // // .value()));
    //
    // }

    static class BuilderWrapper<T> implements Provider<T> {

        private final IBuilder<T> builder;

        public BuilderWrapper(IBuilder<T> b) {
            builder = b;
        }

        @Override
        public final T get() {
            return builder.build();
        }

        @Override
        public String toString() {
            return super.toString() + "[" + builder + "]";
        }
    }

    public <T extends Model> void register(IBuilder<T> builder) {
        // new TypeToken<T>() {}.where(new TypeParameter<T>() {}, provider);

        System.out.println("Register builder of: " + builder.getType());
        modules.add(new GenericModule<T>(builder.getType(),
                new BuilderWrapper<T>(builder)));

        for (final Method m : builder.getType().getDeclaredMethods()) {

            final Invokable<?, Object> inv = Invokable.from(m);
            if (inv.isAnnotationPresent(Registerer.class)) {
                m.setAccessible(true);
                final List<Parameter> params = inv.getParameters();
                checkArgument(params.size() == 1);

                System.out.println(" > found registerer for: "
                        + params.get(0).getType());

                registerers.put(params.get(0).getType(), new Tuple(inv, builder
                        .getType()));
            } else if (inv.isAnnotationPresent(Controller.class)) {
                m.setAccessible(true);
                // create provider based on return type
                inv.getReturnType();

            }
        }
    }

    public static class Tuple {
        public final Invokable<?, ?> inv;
        public final Class<?> ref;

        public Tuple(Invokable<?, ?> invokable, Class<?> reference) {
            inv = invokable;
            ref = reference;
        }

    }

    // does not register, but inits object
    // useful for model introspection objects
    public void addWithoutRegister(AbstractBuilder<?> objBu) {

    }

    /**
     * Registers
     * @param objectBuilder
     */
    // attempts to register, and throws exception if impossible
    public <T> void add(AbstractBuilder<T> objectBuilder) {

        // injector.injectMembers(objectBuilder);
        // final T obj = objectBuilder.build();

        // get init data

        final Map<Class<?>, Object> map = newLinkedHashMap();
        final Field[] fields = objectBuilder.getClass().getDeclaredFields();
        for (final Field f : fields) {
            if (f.getAnnotation(InitData.class) != null) {
                f.setAccessible(true);
                try {
                    map.put(f.getType(), f.get(objectBuilder));
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        System.out.println(map);

        // put in provider

        final T obj = injector.getInstance(objectBuilder.getType());

        register(obj, objectBuilder.getType());
    }

    // TODO how to add initialisation parameters?
    // like position and speed for a RoadUser
    // RoadUserData {
    // Point p;
    // double speed;
    // }
    // return this object in the builder as well?
    // link via interface? RoadUserBuilder#getRoadUserData()
    // @DataFor(RoadUser.class)
    // or just @InitData (and use return type of method)

    // we can enforce this by checking builder build type e.g. RoadUser
    // make sure an @InitData annotation is present on one of the methods
    // (or fields?)
    //
    // @InitData(RoadUserData.class)
    // interface RoadUser

    // TODO check for compatibility with wrapper/adapter/guard objects

    private <T> void register(T obj, Class<T> clz) {

        final Collection<Tuple> coll = registerers.get(TypeToken.of(clz));

        for (final Tuple t : coll) {
            final Object o = injector.getInstance(t.ref);
            try {
                System.out.println(t.inv);
                System.out.println(o);
                ((Invokable<Object, Object>) t.inv).invoke(o, obj);
            } catch (final InvocationTargetException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (final IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public void configure() {
        // modules.add(new ConcreteModelModule());
        final InjectionListener<ModelBGuard> il = new InjectionListener<ModelBGuard>() {
            @Override
            public void afterInjection(ModelBGuard injectee) {
                System.out.println("INJECTED: " + injectee);
                // TODO Auto-generated method stub

                register(injectee, ModelBGuard.class);
            }
        };
        final TypeListener l = new TypeListener() {
            @SuppressWarnings("unchecked")
            @Override
            public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
                System.out.println("HEAR HEAR: " + type + " " + encounter);
                if (type.equals(TypeLiteral.get(ModelBGuard.class))) {
                    encounter.register((InjectionListener<I>) il);
                }

            }
        };

        final Module m = new AbstractModule() {

            @Override
            protected void configure() {
                // TODO Auto-generated method stub
                bindListener(Matchers.any(), l);
            }

        };
        modules.add(m);
        injector = Guice.createInjector(modules);

        // TODO add Agent in a model on a position!

        // TODO how about TickListener + TimeModel? -> subclass EventBus for
        // TimeLapse stuff

        // final Builder b = injector.getInstance(Builder.class);

        // final Agent a1 = injector.getInstance(Agent.class);
        // final Agent a2 = injector.getInstance(Agent.class);
        //
        // injector.getInstance(ModelA.class).register(a1);
        // injector.getInstance(ModelA.class).register(a2);
        //
        // System.out.println("A " + injector.getInstance(ModelA.class));
        // System.out.println("B " + injector.getInstance(ModelB.class));
        // System.out.println("C " + injector.getInstance(ModelC.class));
        // System.out.println(injector.getInstance(ConcreteModelABC.class));
        //
        // // final ConcreteModel cm =
        // injector.getInstance(ConcreteModel.class);
        //
        // System.out.println(a1);
    }
}
