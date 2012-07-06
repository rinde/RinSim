/**
 * 
 */
package rinde.sim.core.model.pdp;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.unmodifiableCollection;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import rinde.sim.core.TimeLapse;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.Model;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.core.model.road.RoadUser;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * 
 * 
 * Assumptions of the model, any truck can pickup any (kind of) package (as long
 * as size constraints are met).
 * 
 * Currently supports three kinds of objects:
 * <ul>
 * <li> {@link Package}</li>
 * <li> {@link Truck}</li>
 * <li> {@link Depot}</li>
 * </ul>
 * 
 * A package must be in one of three locations: on a truck, in a depot or on a
 * road (roadmodel).
 * 
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class PDPModel implements Model<PDPObject> {
    protected final RoadModel roadModel;
    protected final Multimap<PackageContainer, Package> containerContents;
    protected final Map<PackageContainer, Double> containerContentsSize;
    protected final Map<PackageContainer, Double> containerCapacities;

    protected final Map<Truck, Action> truckActions;

    protected final Set<Package> knownPackages;

    // TODO what if we give all model actions default visibility? and let them
    // be accesible only through the abstract class

    // TODO where to store pickup and delivery processing times?

    // a Package must always be either in the PDPModel or in the RoadModel not
    // both! if it exists in neither it doesn't exist at all.

    public PDPModel(RoadModel rm) {
        roadModel = rm;
        containerContents = HashMultimap.create();
        containerContentsSize = newHashMap();
        containerCapacities = newHashMap();
        knownPackages = newHashSet();
        truckActions = newHashMap();
    }

    // protected Table<Truck>

    public Collection<Package> getContents(PackageContainer truck) {
        checkArgument(containerCapacities.containsKey(truck));
        return unmodifiableCollection(containerContents.get(truck));
    }

    public double getContentsSize(PackageContainer truck) {
        return containerContentsSize.get(truck);
    }

    // public void getPack

    /**
     * {@link Truck} <code>t</code> attempts to pickup {@link Package}
     * <code>p</code>.
     * @param t
     * @param p
     */
    public void pickup(Truck t, Package p, TimeLapse time) {

        // TODO add event
        // TODO package.isPickupAllowedBy(truck)
        // TODO truck.canPickup(package)

        checkArgument(time.hasTimeLeft(), "there must be time available to perform the action");
        checkArgument(roadModel.containsObject(t), "truck does not exist in RoadModel");
        checkArgument(roadModel.containsObject(p), "package does not exist in RoadModel");
        checkArgument(knownPackages.contains(p), "package must be registered in PDPModel");
        checkArgument(roadModel.equalPosition(t, p), "truck must be at the same location as the package it wishes to pickup");

        // FIXME this should be made impossible, if so then this check will
        // become redundant
        checkArgument(!containerContents.containsEntry(t, p), "truck is already carrying the package");
        final Double newSize = containerContentsSize.get(t) + p.getMagnitude();
        checkArgument(newSize <= containerCapacities.get(t), "package does not fit in truck");

        // remove the package such that it can no longer be attempted to be
        // picked up by anyone else
        roadModel.removeObject(p);
        // in this case we know we cannot finish this action with the available
        // time. We must continue in the next tick.
        if (time.getTimeLeft() < p.getLoadingDuration()) {
            truckActions.put(t, new PickupAction(this, t, p, p
                    .getLoadingDuration() - time.getTimeLeft()));
            time.consumeAll();
        } else {
            time.consume(p.getLoadingDuration());
            doPickup(t, p);

        }
    }

    protected void doPickup(Truck t, Package p) {
        containerContents.put(t, p);
        containerContentsSize.put(t, containerContentsSize.get(t)
                + p.getMagnitude());
    }

    // should deliver (put down) the package p that truck t is carrying.
    // TODO what to do with the time that is needed for delivery?
    // TODO check for constraints, can a package just be put down everywhere? or
    // only at Depots? is this a global constraint? or only at its predefined
    // destination?
    // public void deliver(Truck t, Package p, TimeLapse time) {

    // partial delivery?
    // what if we didn't have time to completely deliver? we should enforce
    // continuing the delivery in the next tick
    // how to avoid that the truck moves in the mean time? listening to move
    // events in the RoadModel? intercepting TimeLapses in the Truck? (could
    // be done by making an abstract class). the state should be saved
    // somewhere, in the model? in the truck?

    // TODO package.isDeliveryAllowedAt(currentPos)
    // TODO truck.canDeliverAt(currentPos / currentDepot)
    // TODO some of these constraints might be too much for a basic
    // implementation, if needed they should be implemented in an overriding
    // subclass
    // }

    public void deliver(Truck t, Package p, Point position, TimeLapse time) {

    }

    public void deliver(Truck t, Package p, RoadUser ru, TimeLapse time) {

    }

    /**
     * This method is intended for Packages that wish to add themselves to
     * either a Truck or a Depot.
     * @param container
     * @param p
     */
    // TODO can this method be misused for hacking the model? try to avoid!
    public void addPackageIn(PackageContainer container, Package p) {
        checkArgument(!roadModel.containsObject(p));
        checkArgument(knownPackages.contains(p));
        checkArgument(containerContents.containsKey(container));
        // TODO implement
    }

    @Override
    public boolean register(PDPObject element) {
        if (element.getType() == PDPType.PACKAGE) {
            checkArgument(!knownPackages.contains(element));
            knownPackages.add((Package) element);
        } else if (element.getType() == PDPType.TRUCK
                || element.getType() == PDPType.DEPOT) {
            final PackageContainer pc = (PackageContainer) element;
            containerCapacities.put(pc, pc.getCapacity());
            containerContentsSize.put(pc, 0d);
        }
        element.initPDPObject(this);

        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean unregister(PDPObject element) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Class<PDPObject> getSupportedType() {
        return PDPObject.class;
    }

    /**
     * @param p
     * @return
     */
    public boolean truckContains(Truck t, Package p) {
        return containerContents.containsEntry(t, p);
    }

    public boolean depotContains(Depot d, Package p) {
        return containerContents.containsEntry(d, p);
    }

    /**
     * @param truck
     * @param time
     */
    protected void continuePreviousActions(Truck truck, TimeLapse time) {
        if (truckActions.containsKey(truck)) {
            final Action action = truckActions.get(truck);
            action.perform(time);
            if (action.isDone()) {
                truckActions.remove(truck);
            }
        }
    }

    interface Action {
        void perform(TimeLapse time);

        boolean isDone();
    }

    class PickupAction implements Action {

        private final PDPModel modelRef;
        private final Truck truck;
        private final Package pack;
        private long timeNeeded;

        public PickupAction(PDPModel model, Truck t, Package p, long pTimeNeeded) {
            modelRef = model;
            truck = t;
            pack = p;
            timeNeeded = pTimeNeeded;
        }

        @Override
        public void perform(TimeLapse time) {
            // there is enough time to finish action
            if (time.getTimeLeft() >= timeNeeded) {
                timeNeeded = 0;
                time.consume(timeNeeded);
                modelRef.doPickup(truck, pack);
            } else { // there is not enough time to finish action in this step
                timeNeeded -= time.getTimeLeft();
                time.consumeAll();
            }
        }

        @Override
        public boolean isDone() {
            return timeNeeded == 0;
        }
    }

}
