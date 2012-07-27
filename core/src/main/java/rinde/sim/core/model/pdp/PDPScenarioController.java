/**
 * 
 */
package rinde.sim.core.model.pdp;

import rinde.sim.scenario.Scenario;
import rinde.sim.scenario.ScenarioController;
import rinde.sim.scenario.TimedEvent;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public abstract class PDPScenarioController extends ScenarioController {

    /**
     * @param scen
     * @param numberOfTicks
     */
    public PDPScenarioController(Scenario scen, int numberOfTicks) {
        super(scen, numberOfTicks);
    }

    @Override
    protected boolean handleTimedEvent(TimedEvent event) {
        if (event.getEventType() instanceof PDPScenarioEvent) {
            boolean isHandled = false;
            switch ((PDPScenarioEvent) event.getEventType()) {
            case ADD_VEHICLE:
                isHandled = handleAddVehicleEvent(event);
                break;
            case REMOVE_VEHICLE:
                isHandled = handleRemoveVehicleEvent(event);
                break;
            case ADD_PARCEL:
                isHandled = handleAddParcelEvent(event);
                break;
            case REMOVE_PARCEL:
                isHandled = handleRemoveParcelEvent(event);
                break;
            case ADD_DEPOT:
                isHandled = handleAddDepotEvent(event);
                break;
            case REMOVE_DEPOT:
                isHandled = handleRemoveDepotEvent(event);
                break;
            }
            return isHandled;
        }
        return false;
    }

    protected boolean handleAddVehicleEvent(TimedEvent event) {
        return false;
    }

    protected boolean handleRemoveVehicleEvent(TimedEvent event) {
        return false;
    }

    protected boolean handleAddParcelEvent(TimedEvent event) {
        return false;
    }

    protected boolean handleRemoveParcelEvent(TimedEvent event) {
        return false;
    }

    protected boolean handleAddDepotEvent(TimedEvent event) {
        return false;
    }

    protected boolean handleRemoveDepotEvent(TimedEvent event) {
        return false;
    }

}
