package rinde.sim.core.model;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static rinde.sim.core.model.DebugModel.Action.ALLOW;
import static rinde.sim.core.model.DebugModel.Action.REJECT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import rinde.sim.core.graph.ConnectionData;
import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.LengthData;
import rinde.sim.core.graph.MultimapGraph;
import rinde.sim.core.model.road.GraphRoadModel;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.core.model.road.RoadUser;

import com.google.common.collect.ImmutableList;

public class ModelManagerTest {

    protected ModelManager manager;

    @Before
    public void setUp() {
        manager = new ModelManager();
    }

    @Test(expected = IllegalStateException.class)
    public void notConfigured() {
        manager.register(new Object());
    }

    @Test(expected = IllegalArgumentException.class)
    public void addToEmpty() {
        manager.configure();
        manager.register(new Object());
    }

    @Test
    public void addOtherFooModel() {
        final OtherFooModel model = new OtherFooModel();
        manager.register(model);
        manager.configure();
        manager.register(new Foo());
        try {
            manager.register(new Bar());
            fail();
        } catch (final IllegalArgumentException e) {}
        assertEquals(1, model.calledRegister);
        // assertEquals(1, model.calledTypes);
    }

    @Test
    public void addWhenTwoModels() {
        final OtherFooModel model = new OtherFooModel();
        final BarModel model2 = new BarModel();
        manager.register(model);
        manager.register(model2);
        manager.configure();
        manager.register(new Foo());
        manager.register(new Bar());
        manager.register(new Foo());
        assertEquals(2, model.calledRegister);
        // assertEquals(1, model.calledTypes);
        assertEquals(1, model2.calledRegister);

        assertArrayEquals(new Model[] { model, model2 }, manager.getModels()
                .toArray(new Model[2]));
    }

    @Test
    public void addDuplicateModel() {
        final OtherFooModel model = new OtherFooModel();
        manager.add(model);
        try {
            manager.add(model);
            fail();
        } catch (final IllegalArgumentException e) {}
    }

    @Test(expected = IllegalArgumentException.class)
    public void addNull() {
        manager.add(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addFaultyModel() {
        final DebugModel<ObjectA> model = new DebugModel<ObjectA>(null);
        manager.add(model);
    }

    @Test(expected = IllegalArgumentException.class)
    public void registerNull() {
        manager.register(null);
    }

    @Test(expected = IllegalStateException.class)
    public void registerModelTooLate() {
        manager.configure();
        manager.register(new GraphRoadModel(new MultimapGraph<LengthData>()));
    }

    @Test(expected = IllegalStateException.class)
    public void addModelTooLate() {
        manager.configure();
        manager.add(new GraphRoadModel(new MultimapGraph<LengthData>()));
    }

    @Test(expected = RuntimeException.class)
    public void registerWithBrokenModel() {
        manager.add(new GraphRoadModel(new MultimapGraph<LengthData>()));
        manager.add(new BrokenRoadModel(new MultimapGraph<LengthData>()));
        manager.configure();
        manager.register(new RoadUser() {
            @Override
            public void initRoadUser(RoadModel model) {}
        });
    }

    @Test
    public void unregisterWithoutModels() {
        manager.configure();
        assertEquals(0, manager.getModels().size());
        try {
            manager.unregister(new Object());
            fail();
        } catch (final IllegalArgumentException e) {}
    }

    @Test(expected = IllegalArgumentException.class)
    public void unregisterNull() {
        manager.unregister(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void unregisterModel() {
        manager.unregister(new GraphRoadModel(new MultimapGraph<LengthData>()));
    }

    @Test(expected = IllegalStateException.class)
    public void unregisterWhenNotConfigured() {
        manager.unregister(new Object());
    }

    @Test(expected = IllegalArgumentException.class)
    public void duplicateModel() {
        manager.add(new GraphRoadModel(new MultimapGraph<LengthData>()));
        manager.add(new GraphRoadModel(new MultimapGraph<LengthData>()));

    }

    @Test(expected = IllegalArgumentException.class)
    public void unregister() {
        manager.add(new GraphRoadModel(new MultimapGraph<LengthData>()));
        manager.configure();
        manager.unregister(new RoadUser() {
            @Override
            public void initRoadUser(RoadModel model) {}
        });
    }

    @Test(expected = RuntimeException.class)
    public void unregisterWithBrokenModel() {
        manager.add(new GraphRoadModel(new MultimapGraph<LengthData>()));
        manager.add(new BrokenRoadModel(new MultimapGraph<LengthData>()));
        manager.configure();
        manager.unregister(new RoadUser() {
            @Override
            public void initRoadUser(RoadModel model) {}
        });
    }

    @Test
    public void unregisterUnregistered() {
        final OtherFooModel model = new OtherFooModel();
        manager.register(model);
        manager.configure();
        final Object o = new Object();
        try {
            manager.unregister(o);
            fail();
        } catch (final IllegalArgumentException e) {}
        try {
            // it wont be register
            manager.register(o);
            fail();
        } catch (final IllegalArgumentException e) {}
        try {
            manager.unregister(o);
            fail();
        } catch (final IllegalArgumentException e) {}
    }

    public void unregisterRegistered() {
        final OtherFooModel model = new OtherFooModel();
        final BarModel model2 = new BarModel();
        manager.register(model);
        manager.register(model2);
        manager.configure();

        final Foo foo = new Foo();
        final Bar bar = new Bar();

        manager.register(foo);
        manager.register(bar);

        manager.unregister(foo);

        assertEquals(1, model.calledRegister);
        assertEquals(1, model2.calledRegister);
        assertEquals(1, model.callUnregister);
    }

    // TODO add multi model test

    @Test
    public void manyModelsTest() {
        final ModelA mA = new ModelA();
        final ModelB mB = new ModelB();
        final ModelC mC = new ModelC();

        try {
            manager.getModel(ModelA.class);
            fail();
        } catch (final IllegalArgumentException e) {}

        manager.register(mA);
        manager.register(mB);
        manager.register(mC);

        manager.configure();

        assertTrue(mA == manager.getModel(ModelA.class));
        assertTrue(mB == manager.getModel(ModelB.class));
        assertTrue(mC == manager.getModel(ModelC.class));

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

    @Test(expected = IllegalArgumentException.class)
    public void noLinkModel() {
        manager.add(new FaultyModel());
    }

    @Test
    public void anonymousModelTest() {
        manager.add(new SimpleModel<InnerObject>(InnerObject.class) {

            @Override
            public boolean register(InnerObject element) {
                return false;
            }

            @Override
            public boolean unregister(InnerObject element) {
                return false;
            }

        });

        manager.configure();

        try {
            manager.register(new InnerObject());
            fail();
        } catch (final IllegalArgumentException iae) {

        }
    }

    class InnerObject {}
}

class BrokenRoadModel extends GraphRoadModel {
    public BrokenRoadModel(Graph<? extends ConnectionData> pGraph) {
        super(pGraph);
    }

    @Override
    public boolean register(RoadUser obj) {
        throw new RuntimeException("intended failure");
    }

    @Override
    public boolean unregister(RoadUser obj) {
        throw new RuntimeException("intended failure");
    }
}

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
        return null;
    }

    @Override
    public List<? extends ModelLink<?>> getModelLinks() {
        return ImmutableList.of();
    }

}

class DebugModel<T> extends SimpleModel<T> {

    enum Action {
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
        setRegisterAction(ALLOW);
        setUnregisterAction(ALLOW);
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

class Foo {}

class Bar {}
