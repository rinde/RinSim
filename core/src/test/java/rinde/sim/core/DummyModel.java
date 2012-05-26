package rinde.sim.core;

import java.util.HashSet;

import rinde.sim.core.SimulatorTest.DummyObject;
import rinde.sim.core.model.Model;

public class DummyModel implements Model<DummyObject> {

	private final HashSet<DummyObject> objs;

	public DummyModel() {
		objs = new HashSet<DummyObject>();
	}

	@Override
	public boolean register(DummyObject element) {
		return objs.add(element);
	}

	@Override
	public boolean unregister(DummyObject element) {
		return objs.remove(element);
	}

	@Override
	public Class<DummyObject> getSupportedType() {
		return DummyObject.class;
	}
}