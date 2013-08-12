package rinde.sim.core.guice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.util.List;

import rinde.sim.core.IBuilder;
import rinde.sim.core.graph.Point;
import rinde.sim.core.guice.Main.Agent;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;

// @Model({ ModelA.class, ModelBGuard.class, ModelC.class })
public class ConcreteModelABC implements ModelA, ModelC, Model {

    private final RootAPI rootModel;
    private final Point point;

    ConcreteModelABC(RootAPI rm, Point p) {
        rootModel = rm;
        point = p;
    }

    @Registerer
    @Override
    public void register(Object o) {

        System.out.println(this + " registering object " + o);
    }

    @Registerer
    public void register(List<String> strings) {

    }

    @Registerer
    public void register(Point p) {

    }

    @Registerer
    private void register(ModelBGuard mbg) {
        System.out.println("Register ModelBGuard: " + mbg);
    }

    @Registerer
    void register(Agent a) {
        System.out.println(this + " registering agent " + a);
    }

    // should be put in a provider for ModelB
    // whenever someone requires ModelB we give them a guard instead.
    @Controller
    ModelB createController(Point p) {
        return new ModelBGuard(this, p);
    }

    @Override
    public void test() {
        System.out.println(this + " test: " + rootModel);
        System.out.println(point);
    }

    // public static ModelDefinition init(Point p) {
    // return new ConcreteModelDefinition(ConcreteModel.class, p);
    // }

    public static Builder builder() {
        return new Builder();
    }

    static abstract class NoArgBuilder<T> extends AbstractBuilder<T> {
        @Override
        public T build() {
            try {
                final Constructor<T> constructor = getType()
                        .getDeclaredConstructor();
                constructor.setAccessible(true);
                return constructor.newInstance();
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String toString() {
            return "NoArgBuilder<" + getType() + ">";
        }
    }

    static abstract class AbstractBuilder<T> implements IBuilder<T> {

        @Override
        @SuppressWarnings({ "unchecked", "serial" })
        // we know this is safe
        public final Class<T> getType() {
            return (Class<T>) ((ParameterizedType) (new TypeToken<AbstractBuilder<T>>(
                    getClass()) {}.getType())).getActualTypeArguments()[0];
        }
    }

    public static class ModelBGuard implements ModelB {

        private ModelBGuard(ConcreteModelABC abc, Point p) {
            System.out.println("ModelBGuard init");
            System.out.println(" > " + abc);
        }

        public static class Builder extends AbstractBuilder<ModelBGuard> {

            ConcreteModelABC modelABC;
            Point p;

            @Inject
            public void inject(ConcreteModelABC abc) {
                modelABC = abc;
            }

            @InitData
            public void inject(Point point) {
                p = point;
            }

            @Override
            public ModelBGuard build() {
                return new ModelBGuard(modelABC, p);
            }

        }
    }

    public static class Builder extends AbstractBuilder<ConcreteModelABC> {
        RootAPI rootModel;
        Point point;

        @Inject
        Builder inject(RootAPI rm) {
            rootModel = rm;
            return this;
        }

        public Builder atPosition(Point p) {
            point = p;
            return this;
        }

        @Override
        public ConcreteModelABC build() {
            return new ConcreteModelABC(rootModel, point);
        }
    }
}

// interface ModelDefinition {
//
// Map<? extends Key<?>, ? extends Object> getKeyMapping();
//
// }
//
// interface Parameters<T> {
//
// }

// perhaps it can link to Parameters<ConcreteModel>
// a link should be created: Parameters<ConcreteModel> ->
// Provider<Parameters<ConcreteModel>>
// which should be constructed internally

// in that case, init() should return: a tuple of a Class and Parameters? or
// only Parameters?

// make sure instance of this class is bound to its type
// class ConcreteModelParameters {
// final Point point;
//
// public ConcreteModelParameters(Point p) {
// point = p;
// }
// }

interface ModelA extends PublicAPI {
    void test();

    public void register(Object o);
}

interface ModelB extends PublicAPI {}

interface ModelC extends PublicAPI {}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface Controller {

}

// class ConcreteModelDefinition implements ModelDefinition {
//
// Map<? extends Key<?>, ? extends Object> mapping;
//
// public ConcreteModelDefinition(Class<?> clz, Point p) {
// checkArgument(clz.getConstructors().length == 1);
// // clz.getConstructors()[0]
//
// mapping = ImmutableMap.of(Key.get(Point.class, ModelPosition.class), p);
// }
//
// @Override
// public Map<? extends Key<?>, ? extends Object> getKeyMapping() {
// return mapping;
// }
// }
//
// class ConcreteModelModule extends AbstractModule {
//
// @Override
// protected void configure() {
// bind(Point.class).annotatedWith(ModelPosition.class)
// .toInstance(new Point(2, 3));
//
// bind(GenericModel.class).to(ConcreteModel.class);
// bind(ConcreteModel.class).in(Singleton.class);
// }
// }
//
// @BindingAnnotation
// @Target({ FIELD, PARAMETER, METHOD })
// @Retention(RUNTIME)
// @interface ModelPosition {}
