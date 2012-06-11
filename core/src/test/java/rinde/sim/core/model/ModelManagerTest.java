package rinde.sim.core.model;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static rinde.sim.core.model.DebugModel.Action.ALLOW;
import static rinde.sim.core.model.DebugModel.Action.REJECT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import rinde.sim.core.graph.EdgeData;
import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.LengthEdgeData;
import rinde.sim.core.graph.MultimapGraph;
import rinde.sim.core.model.road.GraphRoadModel;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.core.model.road.RoadUser;

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

	@Test
	public void addToEmpty() {
		manager.configure();
		assertFalse(manager.register(new Object()));
	}

	@Test
	public void addOtherFooModel() {
		OtherFooModel model = new OtherFooModel();
		manager.register(model);
		manager.configure();
		assertTrue(manager.register(new Foo()));
		assertFalse(manager.register(new Bar()));
		assertEquals(1, model.calledRegister);
		assertEquals(1, model.calledTypes);
	}

	@Test
	public void addWhenTwoModels() {
		OtherFooModel model = new OtherFooModel();
		BarModel model2 = new BarModel();
		manager.register(model);
		manager.register(model2);
		manager.configure();
		assertTrue(manager.register(new Foo()));
		assertTrue(manager.register(new Bar()));
		assertTrue(manager.register(new Foo()));
		assertEquals(2, model.calledRegister);
		assertEquals(1, model.calledTypes);
		assertEquals(1, model2.calledRegister);

		assertArrayEquals(new Model<?>[] { model, model2 }, manager.getModels().toArray(new Model<?>[2]));
	}

	@Test
	public void addDuplicateModel() {
		OtherFooModel model = new OtherFooModel();
		assertTrue(manager.add(model));
		assertFalse(manager.add(model));
	}

	@Test(expected = IllegalArgumentException.class)
	public void addNull() {
		manager.add(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void addFaultyModel() {
		ModelA model = new ModelA();
		model.setSupportedType(null);
		manager.add(model);
	}

	@Test(expected = IllegalArgumentException.class)
	public void registerNull() {
		manager.register(null);
	}

	@Test(expected = IllegalStateException.class)
	public void registerModelTooLate() {
		manager.configure();
		manager.register(new GraphRoadModel(new MultimapGraph<LengthEdgeData>()));
	}

	@Test(expected = IllegalStateException.class)
	public void addModelTooLate() {
		manager.configure();
		manager.add(new GraphRoadModel(new MultimapGraph<LengthEdgeData>()));
	}

	@Test(expected = RuntimeException.class)
	public void registerWithBrokenModel() {
		manager.add(new GraphRoadModel(new MultimapGraph<LengthEdgeData>()));
		manager.add(new BrokenRoadModel(new MultimapGraph<LengthEdgeData>()));
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
		assertFalse(manager.unregister(new Object()));
	}

	@Test(expected = IllegalArgumentException.class)
	public void unregisterNull() {
		manager.unregister(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void unregisterModel() {
		manager.unregister(new GraphRoadModel(new MultimapGraph<LengthEdgeData>()));
	}

	@Test(expected = IllegalStateException.class)
	public void unregisterWhenNotConfigured() {
		manager.unregister(new Object());
	}

	@Test
	public void unregister() {
		manager.add(new GraphRoadModel(new MultimapGraph<LengthEdgeData>()));
		manager.add(new GraphRoadModel(new MultimapGraph<LengthEdgeData>()));
		manager.configure();
		manager.unregister(new RoadUser() {
			@Override
			public void initRoadUser(RoadModel model) {}
		});
	}

	@Test(expected = RuntimeException.class)
	public void unregisterWithBrokenModel() {
		manager.add(new GraphRoadModel(new MultimapGraph<LengthEdgeData>()));
		manager.add(new BrokenRoadModel(new MultimapGraph<LengthEdgeData>()));
		manager.configure();
		manager.unregister(new RoadUser() {
			@Override
			public void initRoadUser(RoadModel model) {}
		});
	}

	@Test
	public void unregisterUnregistered() {
		OtherFooModel model = new OtherFooModel();
		manager.register(model);
		manager.configure();
		Object o = new Object();
		assertFalse(manager.unregister(o));
		// it wont be register
		assertFalse(manager.register(o));
		assertFalse(manager.unregister(o));
	}

	public void unregisterRegistered() {
		OtherFooModel model = new OtherFooModel();
		BarModel model2 = new BarModel();
		manager.register(model);
		manager.register(model2);
		manager.configure();

		final Foo foo = new Foo();
		final Bar bar = new Bar();

		assertTrue(manager.register(foo));
		assertTrue(manager.register(bar));

		assertTrue(manager.unregister(foo));

		assertEquals(1, model.calledRegister);
		assertEquals(1, model2.calledRegister);
		assertEquals(1, model.callUnregister);
	}

	@Test
	public void manyModelsTest() {
		ModelA mA = new ModelA();
		ModelAA mAA = new ModelAA();
		ModelB mB = new ModelB();
		ModelB mB2 = new ModelB();
		ModelBB mBB = new ModelBB();
		ModelBBB mBBB = new ModelBBB();
		SpecialModelB mSB = new SpecialModelB();
		ModelC mC = new ModelC();

		manager.register(mA);
		manager.register(mAA);
		manager.register(mB);
		manager.register(mB2);
		manager.register(mBB);
		manager.register(mBBB);
		manager.register(mSB);
		manager.register(mC);

		manager.configure();

		ObjectA a1 = new ObjectA();
		assertTrue(manager.register(a1));
		assertEquals(asList(a1), mA.getRegisteredElements());
		assertEquals(asList(a1), mAA.getRegisteredElements());

		mA.setRegisterAction(REJECT);
		ObjectA a2 = new ObjectA();
		assertTrue(manager.register(a2));
		assertEquals(asList(a1, a2), mA.getRegisteredElements());
		assertEquals(asList(a1, a2), mAA.getRegisteredElements());

		mAA.setRegisterAction(REJECT);
		ObjectA a3 = new ObjectA();
		assertFalse(manager.register(a3));
		assertEquals(asList(a1, a2, a3), mA.getRegisteredElements());
		assertEquals(asList(a1, a2, a3), mAA.getRegisteredElements());

		mA.setRegisterAction(ALLOW);
		mAA.setRegisterAction(ALLOW);
		assertTrue(manager.register(a1));// allow duplicates
		assertEquals(asList(a1, a2, a3, a1), mA.getRegisteredElements());
		assertEquals(asList(a1, a2, a3, a1), mAA.getRegisteredElements());

		ObjectB b1 = new ObjectB();
		assertTrue(manager.register(b1));
		assertEquals(asList(b1), mB.getRegisteredElements());
		assertEquals(asList(b1), mB2.getRegisteredElements());
		assertEquals(asList(b1), mBB.getRegisteredElements());
		assertEquals(asList(b1), mBBB.getRegisteredElements());
		assertEquals(asList(), mSB.getRegisteredElements());

		// subclass of B is registerd in all general models and its subclass
		// model
		SpecialB s1 = new SpecialB();
		assertTrue(manager.register(s1));
		assertEquals(asList(b1, s1), mB.getRegisteredElements());
		assertEquals(asList(b1, s1), mB2.getRegisteredElements());
		assertEquals(asList(b1, s1), mBB.getRegisteredElements());
		assertEquals(asList(b1, s1), mBBB.getRegisteredElements());
		assertEquals(asList(s1), mSB.getRegisteredElements());

		assertTrue(mC.getRegisteredElements().isEmpty());

		// unregister not registered object
		ObjectA a4 = new ObjectA();
		assertTrue(manager.unregister(a4));
		assertEquals(asList(a4), mA.getUnregisteredElements());
		assertEquals(asList(a4), mAA.getUnregisteredElements());

		// try again, this time with models rejecting unregister
		mA.setUnregisterAction(REJECT);
		mAA.setUnregisterAction(REJECT);
		assertFalse(manager.unregister(a4));
		assertEquals(asList(a4, a4), mA.getUnregisteredElements());
		assertEquals(asList(a4, a4), mAA.getUnregisteredElements());

		assertTrue(manager.unregister(b1));
		assertEquals(asList(b1), mB.getUnregisteredElements());
		assertEquals(asList(b1), mB2.getUnregisteredElements());
		assertEquals(asList(b1), mBB.getUnregisteredElements());
		assertEquals(asList(b1), mBBB.getUnregisteredElements());
		assertEquals(asList(), mSB.getUnregisteredElements());

		assertTrue(manager.unregister(s1));
		assertEquals(asList(b1, s1), mB.getUnregisteredElements());
		assertEquals(asList(b1, s1), mB2.getUnregisteredElements());
		assertEquals(asList(b1, s1), mBB.getUnregisteredElements());
		assertEquals(asList(b1, s1), mBBB.getUnregisteredElements());
		assertEquals(asList(s1), mSB.getUnregisteredElements());

	}

	@Test
	public void anonymousModelTest() {
		manager.add(new Model<InnerObject>() {
			@Override
			public boolean register(InnerObject element) {
				return false;
			}

			@Override
			public boolean unregister(InnerObject element) {
				return false;
			}

			@Override
			public Class<InnerObject> getSupportedType() {
				return InnerObject.class;
			}
		});

		manager.configure();

		assertFalse(manager.register(new InnerObject()));
	}

	class InnerObject {}
}

class BrokenRoadModel extends GraphRoadModel {
	public BrokenRoadModel(Graph<? extends EdgeData> pGraph) {
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

class OtherFooModel implements Model<Foo> {

	int calledTypes;
	int calledRegister;
	int callUnregister;

	@Override
	public boolean register(Foo element) {
		calledRegister += 1;
		return true;
	}

	@Override
	public Class<Foo> getSupportedType() {
		calledTypes += 1;
		return Foo.class;
	}

	@Override
	public boolean unregister(Foo element) {
		callUnregister += 1;
		return true;
	}
}

class BarModel extends AbstractModel<Bar> {
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

class ObjectA {}

class ObjectB {}

class SpecialB extends ObjectB {}

class ObjectC {}

class ModelA extends DebugModel<ObjectA> {
	ModelA() {
		super(ObjectA.class);
	}
}

class ModelAA extends DebugModel<ObjectA> {
	ModelAA() {
		super(ObjectA.class);
	}
}

class ModelB extends DebugModel<ObjectB> {
	ModelB() {
		super(ObjectB.class);
	}
}

class ModelBB extends DebugModel<ObjectB> {
	ModelBB() {
		super(ObjectB.class);
	}
}

class ModelBBB extends DebugModel<ObjectB> {
	ModelBBB() {
		super(ObjectB.class);
	}
}

class SpecialModelB extends DebugModel<SpecialB> {
	SpecialModelB() {
		super(SpecialB.class);
	}
}

class ModelC extends DebugModel<ObjectC> {
	ModelC() {
		super(ObjectC.class);
	}
}

class DebugModel<T> implements Model<T> {

	enum Action {
		ALLOW, REJECT, FAIL
	}

	private Action registerAction;
	private Action unregisterAction;
	private Class<T> supportedType;
	private final List<T> registeredElements;
	private final List<T> unregisteredElements;

	public DebugModel(Class<T> type) {
		supportedType = type;
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

	public void setSupportedType(Class<T> type) {
		supportedType = type;
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

	@Override
	public Class<T> getSupportedType() {
		return supportedType;
	}

}

class Foo {}

class Bar {}