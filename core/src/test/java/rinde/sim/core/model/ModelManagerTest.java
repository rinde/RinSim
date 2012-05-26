package rinde.sim.core.model;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import rinde.sim.core.graph.EdgeData;
import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.LengthEdgeData;
import rinde.sim.core.graph.MultimapGraph;

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

	@Test(expected = IllegalArgumentException.class)
	public void registerNull() {
		manager.register(null);
	}

	@Test(expected = IllegalStateException.class)
	public void registerModelTooLate() {
		manager.configure();
		manager.register(new RoadModel(new MultimapGraph<LengthEdgeData>()));
	}

	@Test(expected = IllegalStateException.class)
	public void addModelTooLate() {
		manager.configure();
		manager.add(new RoadModel(new MultimapGraph<LengthEdgeData>()));
	}

	@Test(expected = RuntimeException.class)
	public void registerWithBrokenModel() {
		manager.add(new RoadModel(new MultimapGraph<LengthEdgeData>()));
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
		manager.unregister(new RoadModel(new MultimapGraph<LengthEdgeData>()));
	}

	@Test(expected = IllegalStateException.class)
	public void unregisterWhenNotConfigured() {
		manager.unregister(new Object());
	}

	@Test
	public void unregister() {
		manager.add(new RoadModel(new MultimapGraph<LengthEdgeData>()));
		manager.add(new RoadModel(new MultimapGraph<LengthEdgeData>()));
		manager.configure();
		manager.unregister(new RoadUser() {
			@Override
			public void initRoadUser(RoadModel model) {}
		});
	}

	@Test(expected = RuntimeException.class)
	public void unregisterWithBrokenModel() {
		manager.add(new RoadModel(new MultimapGraph<LengthEdgeData>()));
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
}

class BrokenRoadModel extends RoadModel {
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

class Foo {}

class Bar {}