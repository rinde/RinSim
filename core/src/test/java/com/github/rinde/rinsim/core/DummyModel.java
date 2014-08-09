package com.github.rinde.rinsim.core;

import java.util.HashSet;

import com.github.rinde.rinsim.core.SimulatorTest.DummyObject;
import com.github.rinde.rinsim.core.model.Model;

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