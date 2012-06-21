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

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.Model;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.core.model.road.RoadUser;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * 
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
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 */
public class PDPModel implements Model<PDPObject> {
	protected final RoadModel roadModel;
	protected final Multimap<PackageContainer, Package> containerContents;
	protected final Map<PackageContainer, Double> containerContentsSize;
	protected final Map<PackageContainer, Double> containerCapacities;

	protected final Set<Package> knownPackages;

	// a Package must always be either in the PDPModel or in the RoadModel not
	// both! if it exists in neither it doesn't exist at all.

	public PDPModel(RoadModel rm) {
		roadModel = rm;
		containerContents = HashMultimap.create();
		containerContentsSize = newHashMap();
		containerCapacities = newHashMap();
		knownPackages = newHashSet();
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
	public void pickup(Truck t, Package p) {

		// TODO add event
		// TODO what to do with the time that is needed for pickup?
		// TODO package.isPickupAllowedBy(truck)
		// TODO truck.canPickup(package)

		checkArgument(roadModel.containsObject(t), "truck does not exist in RoadModel");
		checkArgument(roadModel.containsObject(p), "package does not exist in RoadModel");
		checkArgument(knownPackages.contains(p), "package must be registered in PDPModel");
		checkArgument(roadModel.equalPosition(t, p), "truck must be at the same location as the package it wishes to pickup");

		// FIXME this should be made impossible, if so then this check will
		// become redundant
		checkArgument(!containerContents.containsEntry(t, p), "truck is already carrying the package");
		Double newSize = containerContentsSize.get(t) + p.getMagnitude();
		checkArgument(newSize <= containerCapacities.get(t), "package does not fit in truck");

		roadModel.removeObject(p);
		containerContents.put(t, p);
		containerContentsSize.put(t, newSize);

	}

	// should deliver (put down) the package p that truck t is carrying.
	// TODO what to do with the time that is needed for delivery?
	// TODO check for constraints, can a package just be put down everywhere? or
	// only at Depots? is this a global constraint? or only at its predefined
	// destination?
	public void deliver(Truck t, Package p) {

		// TODO package.isDeliveryAllowedAt(currentPos)
		// TODO truck.canDeliverAt(currentPos / currentDepot)
		// TODO some of these constraints might be too much for a basic
		// implementation, if needed they should be implemented in an overriding
		// subclass
	}

	public void deliver(Truck t, Package p, Point position) {

	}

	public void deliver(Truck t, Package p, RoadUser ru) {

	}

	/**
	 * This method is intended for Packages that wish to initialize themselves
	 * in either a Truck or a Depot.
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
		} else if (element.getType() == PDPType.TRUCK || element.getType() == PDPType.DEPOT) {
			PackageContainer pc = ((PackageContainer) element);
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

}
