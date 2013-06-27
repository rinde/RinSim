package rinde.sim.core.model;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static rinde.sim.core.model.ModelManagerTest.DebugModel.Action.ALLOW;
import static rinde.sim.core.model.ModelManagerTest.DebugModel.Action.REJECT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import rinde.sim.core.graph.ConnectionData;
import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.LengthData;
import rinde.sim.core.graph.MultimapGraph;
import rinde.sim.core.model.ModelManager.Builder;
import rinde.sim.core.model.road.GraphRoadModel;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.core.model.road.RoadUser;

import com.google.common.collect.ImmutableList;

/**
 * Tests for {@link ModelManager}.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class ModelManagerTest {

    // TODO test multiple calls to Builder.build() -> should throw error?

    /**
     * Object registration fails since there are no models to which it can be
     * registered.
     */
    @Test(expected = IllegalArgumentException.class)
    public void emptyModelManager() {
        final ModelManager manager = ModelManager.builder().build();
        manager.register(new Object());
    }

    /**
     * Registry should be immutable.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void checkImmutabilityOfRegistry() {
        ModelManager.builder().build().registry.clear();
    }

    /**
     * Models should be immutable.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void checkImmutabilityOfModels() {
        ModelManager.builder().build().models.clear();
    }

    /**
     * Bar can not be registered because there is no model in which it can be
     * registered.
     */
    @Test
    public void addOtherFooModel() {
        final OtherFooModel model = new OtherFooModel();
        final ModelManager manager = ModelManager.builder().add(model).build();
        manager.register(new Foo());
        try {
            manager.register(new Bar());
            fail();
        } catch (final IllegalArgumentException e) {}
        assertEquals(1, model.calledRegister);
        // assertEquals(1, model.calledTypes);
    }

    /**
     * Succesful registration with two different models and object types.
     */
    @Test
    public void addWhenTwoModels() {
        final OtherFooModel model = new OtherFooModel();
        final BarModel model2 = new BarModel();
        final ModelManager manager = ModelManager.builder().add(model)
                .add(model2).build();
        manager.register(new Foo());
        manager.register(new Bar());
        manager.register(new Foo());
        assertEquals(2, model.calledRegister);
        // assertEquals(1, model.calledTypes);
        assertEquals(1, model2.calledRegister);

        assertArrayEquals(new Model[] { model, model2 }, manager.models.toArray(new Model[2]));
    }

    /**
     * Models can not link to the same type more than once.
     */
    @Test(expected = IllegalArgumentException.class)
    public void duplicateModelLink() {
        final ModelLink<UserA> mlA = new DebugModelLink<UserA>(UserA.class);
        final Model m1 = new MultiModel(mlA, mlA);
        ModelManager.builder().add(m1).build();

    }

    /**
     * Two models can not link to the same type.
     */
    @Test(expected = IllegalArgumentException.class)
    public void duplicateModelLinkDifferentModels() {
        final ModelLink<UserA> mlA = new DebugModelLink<UserA>(UserA.class);
        final ModelLink<UserB> mlB = new DebugModelLink<UserB>(UserB.class);
        final ModelLink<UserC> mlC = new DebugModelLink<UserC>(UserC.class);

        final Model m1 = new MultiModel(mlA, mlB, mlC);
        final Model m2 = new MultiModel(mlB);
        ModelManager.builder().add(m1).add(m2);
    }

    /**
     * Tests whether inter-model dependencies are correctly handled.
     */
    @Test
    public void modelDependencies() {
        final DebugModelLink<UserA> mlA = new DebugModelLink<UserA>(UserA.class);
        final DebugModelLink<UserB> mlB = new DebugModelLink<UserB>(UserB.class);
        final DebugModelLink<UserC> mlC = new DebugModelLink<UserC>(UserC.class);
        assertTrue(mlA.registered.isEmpty());
        assertTrue(mlB.registered.isEmpty());
        assertTrue(mlC.registered.isEmpty());
        // modelA depends on modelC, i.e. implements UserC -> it will be
        // registered at modelC
        // the following models are chained in the sense that A depends on C, C
        // depends on B, B depends on A
        final Model modelA = new ModelDependsOnC(mlA);
        final Model modelB = new ModelDependsOnA(mlB);
        final Model modelC = new ModelDependsOnB(mlC);

        final ModelManager manager = ModelManager.build(modelA, modelB, modelC);

        assertEquals(1, mlA.registered.size());
        assertEquals(modelB, mlA.registered.iterator().next());

        assertEquals(1, mlB.registered.size());
        assertEquals(modelC, mlB.registered.iterator().next());

        assertEquals(1, mlC.registered.size());
        assertEquals(modelA, mlC.registered.iterator().next());
    }

    /**
     * Checks that the ModelManager iterates over the Models and ModelLinks in
     * the insertion order.
     */
    @Test
    public void checkInsertionOrder() {
        // all entries in registry and models should be sorted according to
        // insertion order

        final DebugModelLink<UserA> mlA = new DebugModelLink<UserA>(UserA.class);
        final DebugModelLink<UserB> mlB = new DebugModelLink<UserB>(UserB.class);
        final DebugModelLink<UserC> mlC = new DebugModelLink<UserC>(UserC.class);
        final DebugModelLink<ObjectA> mlD = new DebugModelLink<ObjectA>(
                ObjectA.class);
        final DebugModelLink<ObjectB> mlE = new DebugModelLink<ObjectB>(
                ObjectB.class);
        final DebugModelLink<ObjectC> mlF = new DebugModelLink<ObjectC>(
                ObjectC.class);

        final Model modelAB = new MultiModel(mlA, mlB);
        @SuppressWarnings("unchecked")
        final List<ModelLink<?>> links = asList((ModelLink<?>) mlC, mlD, mlE, mlF);
        final List<Model> models = newArrayList();
        models.add(modelAB);
        for (final ModelLink<?> l : links) {
            models.add(new MultiModel(l));
        }

        final Builder b = ModelManager.builder();
        for (final Model m : models) {
            b.add(m);
        }
        final ModelManager manager = b.build();

        assertEquals(models, manager.models);
        @SuppressWarnings("unchecked")
        final List<ModelLink<?>> expectedLinks = newArrayList((ModelLink<?>) mlA, mlB);
        expectedLinks.addAll(links);
        final List<ModelLink<?>> registryValues = newArrayList(manager.registry
                .values());
        assertEquals(expectedLinks, registryValues);

    }

    // is no statically checked
    /**
     * ModelLink which returns <code>null</code> for
     * {@link ModelLink#getSupportedType()} is not allowed.
     */
    // @Test(expected = IllegalArgumentException.class)
    // public void addFaultyModel() {
    // final DebugModel<ObjectA> model = new DebugModel<ObjectA>(null);
    // ModelManager.builder().add(model);
    // }

    /**
     * Can not register a model as a user.
     */
    @Test(expected = IllegalArgumentException.class)
    public void registerModelAsUser() {
        final ModelManager manager = ModelManager.builder()
                .add(new OtherFooModel()).build();
        manager.register(new GraphRoadModel(new MultimapGraph<LengthData>()));
    }

    /**
     * Register a user to a model which throws an exception itself.
     */
    @Test(expected = IntendedException.class)
    public void registerWithBrokenModel() {
        final ModelManager manager = ModelManager.builder()
                .add(new BrokenRoadModel(new MultimapGraph<LengthData>()))
                .build();
        manager.register(new RoadUser() {
            @Override
            public void initRoadUser(RoadModel model) {}
        });
    }

    /**
     * Can not unregister an object when there are no models.
     */
    @Test
    public void unregisterWithoutModels() {
        final ModelManager manager = ModelManager.builder().build();
        assertEquals(0, manager.models.size());
        assertEquals(0, manager.registry.size());
        try {
            manager.unregister(new Object());
            fail();
        } catch (final IllegalArgumentException e) {}
    }

    /**
     * Models can not be unregistered.
     */
    @Test(expected = IllegalArgumentException.class)
    public void unregisterModel() {
        final ModelManager manager = ModelManager.builder().build();
        manager.unregister(new GraphRoadModel(new MultimapGraph<LengthData>()));
    }

    /**
     * Can not unregister object which was not registered.
     */
    @Test(expected = IllegalArgumentException.class)
    public void unregister() {
        final ModelManager manager = ModelManager.builder()
                .add(new GraphRoadModel(new MultimapGraph<LengthData>()))
                .build();
        manager.unregister(new RoadUser() {
            @Override
            public void initRoadUser(RoadModel model) {}
        });
    }

    /**
     * Check register call count.
     */
    @Test
    public void unregisterRegistered() {
        final OtherFooModel model = new OtherFooModel();
        final BarModel model2 = new BarModel();
        final ModelManager manager = ModelManager.builder().add(model)
                .add(model2).build();

        final Foo foo = new Foo();
        final Bar bar = new Bar();

        manager.register(foo);
        manager.register(bar);
        manager.unregister(foo);

        assertEquals(1, model.calledRegister);
        assertEquals(1, model2.calledRegister);
        assertEquals(1, model.callUnregister);
    }

    /**
     * Checks registration with many (changing) models.
     */
    @Test
    public void manyModelsTest() {
        final ModelA mA = new ModelA();
        final ModelB mB = new ModelB();
        final ModelC mC = new ModelC();

        final ModelManager manager = ModelManager.builder().add(mA).add(mB)
                .add(mC).build();

        final ObjectA a1 = new ObjectA();
        manager.register(a1);
        assertEquals(asList(a1), mA.getRegisteredElements());

        mA.setRegisterAction(REJECT);
        final ObjectA a2 = new ObjectA();
        try {
            manager.register(a2);
            fail();
        } catch (final IllegalArgumentException e) {}
        assertEquals(asList(a1, a2), mA.getRegisteredElements());

        mA.setRegisterAction(ALLOW);
        manager.register(a1);// allow duplicates
        assertEquals(asList(a1, a2, a1), mA.getRegisteredElements());

        final ObjectB b1 = new ObjectB();
        manager.register(b1);
        assertEquals(asList(b1), mB.getRegisteredElements());

        // subclass of B is registerd in all general models and its subclass
        // model
        final SubB s1 = new SubB();
        manager.register(s1);
        assertEquals(asList(b1, s1), mB.getRegisteredElements());

        assertTrue(mC.getRegisteredElements().isEmpty());

        // unregister not registered object
        final ObjectA a4 = new ObjectA();
        manager.unregister(a4);
        assertEquals(asList(a4), mA.getUnregisteredElements());

        // try again, this time with models rejecting unregister
        mA.setUnregisterAction(REJECT);
        try {
            manager.unregister(a4);
            fail();
        } catch (final IllegalArgumentException e) {}
        assertEquals(asList(a4, a4), mA.getUnregisteredElements());

        manager.unregister(b1);
        assertEquals(asList(b1), mB.getUnregisteredElements());

        manager.unregister(s1);
        assertEquals(asList(b1, s1), mB.getUnregisteredElements());

    }

    /**
     * Models should always define at least one link.
     */
    @Test(expected = IllegalArgumentException.class)
    public void noLinkModel() {
        ModelManager.builder().add(new FaultyModel());
    }

    /**
     * Anonymous models should work.
     */
    @Test
    public void anonymousModelTest() {
        final ModelManager manager = ModelManager
                .build(new SimpleModel<InnerObject>(InnerObject.class) {

                    @Override
                    public boolean register(InnerObject element) {
                        return false;
                    }

                    @Override
                    public boolean unregister(InnerObject element) {
                        return false;
                    }

                });

        try {
            manager.register(new InnerObject());
            fail();
        } catch (final IllegalArgumentException iae) {

        }
    }

    class InnerObject {}

    class BrokenRoadModel extends GraphRoadModel {
        public BrokenRoadModel(Graph<? extends ConnectionData> pGraph) {
            super(pGraph);
        }

        @Override
        public boolean register(RoadUser obj) {
            throw new IntendedException();
        }

        @Override
        public boolean unregister(RoadUser obj) {
            throw new IntendedException();
        }
    }

    class IntendedException extends RuntimeException {
        private static final long serialVersionUID = 2007153590961451509L;

        public IntendedException() {
            super("intended failure");
        }
    }

    class MultiModel implements Model {

        protected final List<? extends ModelLink<?>> modelLinks;

        public MultiModel(ModelLink<?>... links) {
            modelLinks = asList(links);
        }

        @Override
        public List<? extends ModelLink<?>> getModelLinks() {
            return modelLinks;
        }

    }

    class ModelDependsOnA extends MultiModel implements UserA {
        public ModelDependsOnA(ModelLink<?>... links) {
            super(links);
        }
    }

    class ModelDependsOnB extends MultiModel implements UserB {
        public ModelDependsOnB(ModelLink<?>... links) {
            super(links);
        }
    }

    class ModelDependsOnC extends MultiModel implements UserC {
        public ModelDependsOnC(ModelLink<?>... links) {
            super(links);
        }
    }

    class ModelDependsOnAB extends MultiModel implements UserA, UserB {}

    class DebugModelLink<T> implements ModelLink<T> {

        protected final Class<T> type;
        protected final List<T> registered;
        protected final List<T> unregistered;

        public DebugModelLink(Class<T> clazz) {
            type = clazz;
            registered = newArrayList();
            unregistered = newArrayList();
        }

        @Override
        public boolean register(T element) {
            registered.add(element);
            return true;
        }

        @Override
        public boolean unregister(T element) {
            unregistered.add(element);
            return true;
        }

        @Override
        public Class<T> getSupportedType() {
            return type;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + " " + type.getSimpleName();
        }
    }

    //
    class OtherFooModel extends SimpleModel<Foo> {

        int calledRegister;
        int callUnregister;

        public OtherFooModel() {
            super(Foo.class);
        }

        @Override
        public boolean register(Foo element) {
            calledRegister += 1;
            return true;
        }

        @Override
        public boolean unregister(Foo element) {
            callUnregister += 1;
            return true;
        }
    }

    class BarModel extends SimpleModel<Bar> {
        int calledRegister;

        protected BarModel() {
            super(Bar.class);
        }

        @Override
        public boolean register(Bar element) {
            calledRegister += 1;
            return true;
        }

        @Override
        public boolean unregister(Bar element) {
            return false;
        }
    }

    interface UserA {}

    interface UserB {}

    interface UserC {}

    class ObjectA implements UserA {}

    class ObjectB implements UserB {}

    class ObjectC implements UserC {}

    class SubB extends ObjectB {}

    class ObjectAB implements UserA, UserB {}

    class ObjectAC implements UserA, UserC {}

    class ObjectBC implements UserB, UserC {}

    class ObjectABC implements UserA, UserB, UserC {}

    class SubABC extends ObjectABC {}

    class ModelA extends DebugModel<UserA> {
        ModelA() {
            super(UserA.class);
        }
    }

    class ModelB extends DebugModel<UserB> {
        ModelB() {
            super(UserB.class);
        }
    }

    class ModelC extends DebugModel<UserC> {
        ModelC() {
            super(UserC.class);
        }
    }

    class FaultyModel implements Model, ModelLink<Object> {

        @Override
        public boolean register(Object element) {
            return false;
        }

        @Override
        public boolean unregister(Object element) {
            return false;
        }

        @Override
        public Class<Object> getSupportedType() {
            return Object.class;
        }

        @Override
        public List<? extends ModelLink<?>> getModelLinks() {
            return ImmutableList.of();
        }

    }

    public static class DebugModel<T> extends SimpleModel<T> {

        public enum Action {
            ALLOW, REJECT, FAIL
        }

        private Action registerAction;
        private Action unregisterAction;
        private final List<T> registeredElements;
        private final List<T> unregisteredElements;

        public DebugModel(Class<T> type) {
            super(type);
            registeredElements = new ArrayList<T>();
            unregisteredElements = new ArrayList<T>();
            registerAction = ALLOW;
            unregisterAction = ALLOW;
        }

        public void setRegisterAction(Action a) {
            registerAction = a;
        }

        public void setUnregisterAction(Action a) {
            unregisterAction = a;
        }

        @Override
        public boolean register(T element) {
            registeredElements.add(element);
            return actionResponse(registerAction);
        }

        @Override
        public boolean unregister(T element) {
            unregisteredElements.add(element);
            return actionResponse(unregisterAction);
        }

        public List<T> getRegisteredElements() {
            return Collections.unmodifiableList(registeredElements);
        }

        public List<T> getUnregisteredElements() {
            return Collections.unmodifiableList(unregisteredElements);
        }

        private boolean actionResponse(Action a) {
            switch (a) {
            case ALLOW:
                return true;
            case REJECT:
                return false;
            case FAIL:
                throw new RuntimeException("this is an intentional failure");
            default:
                throw new IllegalStateException();
            }
        }
    }

}

//
class Foo {}

class Bar {}
