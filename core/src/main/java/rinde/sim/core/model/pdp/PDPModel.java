/**
 * 
 */
package rinde.sim.core.model.pdp;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Maps.filterEntries;
import static com.google.common.collect.Maps.newHashMap;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableSet;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import rinde.sim.core.TimeLapse;
import rinde.sim.core.model.Model;
import rinde.sim.core.model.road.RoadModel;

import com.google.common.base.Predicate;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Assumptions of the model, any vehicle can pickup any (kind of) parcel (as
 * long as size constraints are met).
 * 
 * Currently supports three kinds of objects:
 * <ul>
 * <li> {@link Parcel}</li>
 * <li> {@link Vehicle}</li>
 * <li> {@link Depot}</li>
 * </ul>
 * 
 * A parcel must be in one of three locations: on a vehicle, in a depot or on a
 * road (roadmodel).
 * 
 * TODO write more about assumptions in model <br/>
 * TODO write about extensibility
 * 
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class PDPModel implements Model<PDPObject> {
    /**
     * Reference to the {@link RoadModel} on which the pdp objects are situated.
     */
    protected final RoadModel roadModel;
    /**
     * Multimap for keeping references to the contents of {@link Container}s.
     */
    protected final Multimap<Container, Parcel> containerContents;
    /**
     * Map for keeping the size of the contents of {@link Container}s.
     */
    protected final Map<Container, Double> containerContentsSize;
    /**
     * Map for keeping the capacity of {@link Container}s.
     */
    protected final Map<Container, Double> containerCapacities;
    /**
     * Map that stores the state of {@link Vehicle}s.
     */
    protected final Map<Vehicle, VehicleState> vehicleState;
    /**
     * Map that stores the state of {@link Parcel}s.
     */
    protected final Map<Parcel, ParcelState> parcelState;
    /**
     * Map that stores any pending {@link Action}s of {@link Vehicle}s.
     */
    protected final Map<Vehicle, Action> vehicleActions;

    enum ParcelState {
        LOADING, UNLOADING, AVAILABLE, IN_CARGO, DELIVERED
    }

    enum VehicleState {
        IDLE, LOADING, UNLOADING
    }

    enum PDPEvent {
        START_PICKUP, END_PICKUP, START_DELIVERY, END_DELIVERY
    }

    /**
     * Initializes the PDPModel.
     * @param rm The {@link RoadModel} which is associated to this model.
     */
    public PDPModel(RoadModel rm) {
        roadModel = rm;
        containerContents = HashMultimap.create();
        containerContentsSize = newHashMap();
        containerCapacities = newHashMap();
        vehicleActions = newHashMap();
        vehicleState = newHashMap();
        parcelState = newHashMap();
    }

    /**
     * Returns an unmodifiable view on the contents of the specified container.
     * @param container The container to inspect.
     * @return An unmodifiable collection.
     */
    public Collection<Parcel> getContents(Container container) {
        checkArgument(containerCapacities.containsKey(container));
        return unmodifiableCollection(containerContents.get(container));
    }

    /**
     * Returns the size of the contents of the specified container.
     * @param container The container to inspect.
     * @return A <code>double</code> indicating the size of the contents.
     */
    public double getContentsSize(Container container) {
        return containerContentsSize.get(container);
    }

    // public void getPack

    /**
     * Attempts to perform a pickup operation using the specified vehicle and
     * parcel.
     * 
     * @param vehicle
     * @param parcel
     * @param time
     */
    public void pickup(Vehicle vehicle, Parcel parcel, TimeLapse time) {

        // TODO add event

        /* 1 */checkArgument(roadModel.containsObject(vehicle), "vehicle does not exist in RoadModel");
        /* 2 */checkArgument(roadModel.containsObject(parcel), "parcel does not exist in RoadModel");
        /* 3 */checkArgument(parcelState.get(parcel) == ParcelState.AVAILABLE, "parcel must be registered and must be available");
        /* 4 */checkArgument(vehicleState.get(vehicle) == VehicleState.IDLE, "vehicle must be registered and must be available");
        /* 5 */checkArgument(roadModel.equalPosition(vehicle, parcel), "vehicle must be at the same location as the parcel it wishes to pickup");
        final double newSize = containerContentsSize.get(vehicle)
                + parcel.getMagnitude();
        /* 6 */checkArgument(newSize <= containerCapacities.get(vehicle), "parcel does not fit in vehicle");

        // remove the parcel such that it can no longer be attempted to be
        // picked up by anyone else
        roadModel.removeObject(parcel);
        // in this case we know we cannot finish this action with the available
        // time. We must continue in the next tick.
        if (time.getTimeLeft() < parcel.getLoadingDuration()) {
            vehicleState.put(vehicle, VehicleState.LOADING);
            parcelState.put(parcel, ParcelState.LOADING);

            vehicleActions.put(vehicle, new PickupAction(this, vehicle, parcel,
                    parcel.getLoadingDuration() - time.getTimeLeft()));
            time.consumeAll();
        } else {
            time.consume(parcel.getLoadingDuration());
            doPickup(vehicle, parcel);
        }
    }

    protected void doPickup(Vehicle vehicle, Parcel parcel) {
        containerContents.put(vehicle, parcel);
        containerContentsSize.put(vehicle, containerContentsSize.get(vehicle)
                + parcel.getMagnitude());

        parcelState.put(parcel, ParcelState.IN_CARGO);
    }

    // should deliver (put down) the parcel p that vehicle t is carrying.
    // TODO check for constraints, can a parcel just be put down everywhere? or
    // only at Depots? is this a global constraint? or only at its predefined
    // destination?
    // public void deliver(Truck t, Package p, TimeLapse time) {

    // TODO package.isDeliveryAllowedAt(currentPos)
    // TODO truck.canDeliverAt(currentPos / currentDepot)
    // TODO some of these constraints might be too much for a basic
    // implementation, if needed they should be implemented in an overriding
    // subclass
    // }

    public void deliver(Vehicle vehicle, Parcel parcel, TimeLapse time) {
        /* 1 */checkArgument(roadModel.containsObject(vehicle), "vehicle does not exist in RoadModel");
        /* 2 */checkArgument(vehicleState.get(vehicle)
                .equals(VehicleState.IDLE), "vehicle must be idle");
        /* 3 */checkArgument(containerContents.get(vehicle).contains(parcel), "vehicle does not contain parcel");
        /* 4 */checkArgument(parcel.getDestination()
                .equals(roadModel.getPosition(vehicle)), "parcel must be delivered at its destination, vehicle should move there first");

        if (time.getTimeLeft() < parcel.getUnloadingDuration()) {
            vehicleState.put(vehicle, VehicleState.UNLOADING);
            parcelState.put(parcel, ParcelState.UNLOADING);
            vehicleActions
                    .put(vehicle, new DeliverAction(this, vehicle, parcel,
                            parcel.getUnloadingDuration() - time.getTimeLeft()));
            time.consumeAll();
        } else {
            time.consume(parcel.getUnloadingDuration());
            doDeliver(vehicle, parcel);
        }
    }

    /**
     * @param vehicle
     * @param parcel
     */
    protected void doDeliver(Vehicle vehicle, Parcel parcel) {
        containerContents.remove(vehicle, parcel);
        containerContentsSize.put(vehicle, containerContentsSize.get(vehicle)
                - parcel.getMagnitude());

        parcelState.put(parcel, ParcelState.DELIVERED);
    }

    /**
     * This method is intended for {@link Parcel}s that wish to add themselves
     * to either a {@link Vehicle} or a {@link Depot}.
     * @param container
     * @param parcel
     */
    public void addParcelIn(Container container, Parcel parcel) {
        /* 1 */checkArgument(!roadModel.containsObject(parcel), "this parcel is already added to the roadmodel");
        /* 2 */checkArgument(parcelState.get(parcel) == ParcelState.AVAILABLE, "parcel must be registered and in AVAILABLE state");
        /* 3 */checkArgument(containerCapacities.containsKey(container), "the parcel container is not registered");
        /* 4 */checkArgument(roadModel.containsObject(container), "the parcel container is not on the roadmodel");
        final double newSize = containerContentsSize.get(container)
                + parcel.getMagnitude();
        /* 5 */checkArgument(newSize <= containerCapacities.get(container), "parcel does not fit in container");

        containerContents.put(container, parcel);
        containerContentsSize.put(container, newSize);
    }

    /**
     * @return An unmodifiable view on the the parcels which are in
     *         <code>AVAILABLE</code> state. Note that parcels which are
     *         available are not neccesarily already at a position.
     */
    public Set<Parcel> getAvailableParcels() {
        return unmodifiableSet(filterEntries(parcelState, new Predicate<Map.Entry<Parcel, ParcelState>>() {
            @Override
            public boolean apply(Map.Entry<Parcel, ParcelState> input) {
                return input.getValue() == ParcelState.AVAILABLE;
            }
        }).keySet());
    }

    public ParcelState getParcelState(Parcel p) {
        return parcelState.get(p);
    }

    public VehicleState getVehicleState(Vehicle t) {
        return vehicleState.get(t);
    }

    @Override
    public boolean register(PDPObject element) {
        if (element.getType() == PDPType.PARCEL) {
            checkArgument(!parcelState.containsKey(element));
            parcelState.put((Parcel) element, ParcelState.AVAILABLE);
        } else if (element.getType() == PDPType.VEHICLE
                || element.getType() == PDPType.DEPOT) {
            final Container pc = (Container) element;
            containerCapacities.put(pc, pc.getCapacity());
            containerContentsSize.put(pc, 0d);

            if (element.getType() == PDPType.VEHICLE) {
                vehicleState.put((Vehicle) element, VehicleState.IDLE);
            }
        }
        element.initPDPObject(this);

        return true;
    }

    @Override
    public boolean unregister(PDPObject element) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    @Override
    public Class<PDPObject> getSupportedType() {
        return PDPObject.class;
    }

    /**
     * Inspects the contents of the specified {@link Container} for existence of
     * the specified {@link Parcel} object.
     * @param container The container which is inspected.
     * @param parcel The parcel which we are checking.
     * @return <code>true</code> if the {@link Parcel} is contained in the
     *         {@link Container}, <code>false</code> otherwise.
     */
    public boolean containerContains(Container container, Parcel parcel) {
        return containerContents.containsEntry(container, parcel);
    }

    /**
     * @param vehicle
     * @param time
     */
    protected void continuePreviousActions(Vehicle vehicle, TimeLapse time) {
        if (vehicleActions.containsKey(vehicle)) {
            final Action action = vehicleActions.get(vehicle);
            action.perform(time);
            if (action.isDone()) {
                vehicleActions.remove(vehicle);
            }
        }
    }

    /**
     * Represents an action that takes time. This is used for actions that can
     * not be done at once (since there is not enough time available), using
     * this interface actions can be performed in steps.
     * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
     */
    protected interface Action {
        /**
         * Performs the action using the specified amount of time.
         * @param time
         */
        void perform(TimeLapse time);

        /**
         * @return <code>true</code> when this action is completed,
         *         <code>false</code> otherwise.
         */
        boolean isDone();
    }

    abstract class VehicleParcelAction implements Action {
        protected final PDPModel modelRef;
        protected final Vehicle vehicle;
        protected final Parcel parcel;
        protected long timeNeeded;

        public VehicleParcelAction(PDPModel model, Vehicle v, Parcel p,
                long pTimeNeeded) {
            modelRef = model;
            vehicle = v;
            parcel = p;
            timeNeeded = pTimeNeeded;
        }

        @Override
        public void perform(TimeLapse time) {
            // there is enough time to finish action
            if (time.getTimeLeft() >= timeNeeded) {
                time.consume(timeNeeded);
                timeNeeded = 0;
                finish(time);
            } else { // there is not enough time to finish action in this step
                timeNeeded -= time.getTimeLeft();
                time.consumeAll();
            }
        }

        abstract protected void finish(TimeLapse time);

        @Override
        public boolean isDone() {
            return timeNeeded == 0;
        }

        public long timeNeeded() {
            return timeNeeded;
        }
    }

    class PickupAction extends VehicleParcelAction {

        public PickupAction(PDPModel model, Vehicle v, Parcel p,
                long pTimeNeeded) {
            super(model, v, p, pTimeNeeded);
        }

        @Override
        public void finish(TimeLapse time) {
            modelRef.vehicleState.put(vehicle, VehicleState.IDLE);
            modelRef.doPickup(vehicle, parcel);

        }
    }

    class DeliverAction extends VehicleParcelAction {

        public DeliverAction(PDPModel model, Vehicle v, Parcel p,
                long pTimeNeeded) {
            super(model, v, p, pTimeNeeded);
        }

        @Override
        public void finish(TimeLapse time) {
            modelRef.vehicleState.put(vehicle, VehicleState.IDLE);
            modelRef.doDeliver(vehicle, parcel);
        }
    }

}
