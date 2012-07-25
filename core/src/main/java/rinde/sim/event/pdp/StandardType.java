package rinde.sim.event.pdp;

import rinde.sim.scenario.ScenarioController;

/**
 * Types supported by the {@link ScenarioController} out of the box
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 */
public enum StandardType {
    // FIXME this should be moved to pdp package, uncoupled from scenario
    // controller!
    ADD_TRUCK, ADD_PACKAGE, REMOVE_TRUCK, REMOVE_PACKAGE;
}
