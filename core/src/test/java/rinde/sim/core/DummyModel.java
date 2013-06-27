package rinde.sim.core;

import java.util.HashSet;

import rinde.sim.core.SimulatorTest.DummyObject;
import rinde.sim.core.model.SimpleModel;

public class DummyModel extends SimpleModel<DummyObject> {

    private final HashSet<DummyObject> objs;

    public DummyModel() {
        super(DummyObject.class);
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

}
