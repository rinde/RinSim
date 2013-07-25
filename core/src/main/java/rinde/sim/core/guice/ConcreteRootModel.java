package rinde.sim.core.guice;

import rinde.sim.core.IBuilder;
import rinde.sim.core.guice.ConcreteModelABC.NoArgBuilder;

// @Model(RootModel.class)

class ConcreteRootModel implements RootAPI, Model {

    private ConcreteRootModel() {}

    public void test() {}

    public static IBuilder<ConcreteRootModel> builder() {
        return new NoArgBuilder<ConcreteRootModel>() {};
    }

}

interface RootAPI {}
