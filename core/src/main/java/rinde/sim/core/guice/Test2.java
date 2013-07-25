/**
 * 
 */
package rinde.sim.core.guice;

import rinde.sim.core.graph.Point;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.FactoryModuleBuilder;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class Test2 {

    public static void main(String[] args) {

        // try the assisted inject such that it can be used for models
        //

        // three phases:

        // 1. general setup: should link all classes+factories
        // 2. add models
        // 3. add rest

        // try to merge 1 and 2

        final Module m = new AbstractModule() {
            @Override
            protected void configure() {
                install(new FactoryModuleBuilder()
                        .implement(Model.class, TestModel.class)
                        .build(TestModelFactory.class));
            }
        };

        final Injector inj = Guice.createInjector(m);

        // factories can not be used for singletons

        final TestModelFactory tmf = inj.getInstance(TestModelFactory.class);
        tmf.create(new Point(1, 2));

        inj.getInstance(Model.class);

    }

    public interface TestModelFactory {
        Model create(Point p);
    }

    public static class TestModel implements Model {
        @Inject
        public TestModel(@Assisted Point p) {
            System.out.println("create test model: " + p);
        }
    }

}
