package rinde.sim.core.model.pdp;

import java.util.Collection;
import java.util.Set;

import rinde.sim.core.TimeLapse;
import rinde.sim.core.model.ModelProvider;
import rinde.sim.core.model.pdp.Parcel;
import rinde.sim.core.model.pdp.Vehicle;
import rinde.sim.event.EventAPI;

import com.google.common.collect.ImmutableSet;

/**
 * A {@link PDPModel} which forwards all its method calls to another
 * {@link PDPModel}. Subclasses should override one or more methods to modify
 * the behavior of the backing model as desired per the <a
 * href="http://en.wikipedia.org/wiki/Decorator_pattern">decorator pattern</a>.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class ForwardingPDPModel extends PDPModel {
  /**
   * The {@link PDPModel} to which all calls are delegated.
   */
  protected final PDPModel delegate;

  /**
   * Initializes a new instance that delegates all calls to the specified
   * {@link PDPModel}.
   * @param deleg The instance to which all calls are delegated.
   */
  protected ForwardingPDPModel(PDPModel deleg) {
    delegate = deleg;
    delegate.setSelf(this);
  }

  @Override
  protected final void setSelf(PDPModel pm) {
    super.setSelf(pm);
    delegate.setSelf(pm);
  }

  @Override
  protected boolean doRegister(PDPObject element) {
    return delegate.register(element);
  }

  @Override
  protected void continuePreviousActions(Vehicle vehicle, TimeLapse time) {
    delegate.continuePreviousActions(vehicle, time);
  }

  @Override
  public boolean unregister(PDPObject element) {
    return delegate.unregister(element);
  }

  @Override
  public ImmutableSet<Parcel> getContents(Container container) {
    return delegate.getContents(container);
  }

  @Override
  public double getContentsSize(Container container) {
    return delegate.getContentsSize(container);
  }

  @Override
  public double getContainerCapacity(Container container) {
    return delegate.getContainerCapacity(container);
  }

  @Override
  public void pickup(Vehicle vehicle, Parcel parcel, TimeLapse time) {
    delegate.pickup(vehicle, parcel, time);
  }

  @Override
  public void deliver(Vehicle vehicle, Parcel parcel, TimeLapse time) {
    delegate.deliver(vehicle, parcel, time);
  }

  @Override
  public void addParcelIn(Container container, Parcel parcel) {
    delegate.addParcelIn(container, parcel);
  }

  @Override
  public Collection<Parcel> getParcels(ParcelState state) {
    return delegate.getParcels(state);
  }

  @Override
  public Collection<Parcel> getParcels(ParcelState... states) {
    return delegate.getParcels(states);
  }

  @Override
  public Set<Vehicle> getVehicles() {
    return delegate.getVehicles();
  }

  @Override
  public ParcelState getParcelState(Parcel parcel) {
    return delegate.getParcelState(parcel);
  }

  @Override
  public VehicleState getVehicleState(Vehicle vehicle) {
    return delegate.getVehicleState(vehicle);
  }

  @Override
  public PDPModel.VehicleParcelActionInfo getVehicleActionInfo(Vehicle vehicle) {
    return delegate.getVehicleActionInfo(vehicle);
  }

  @Override
  public EventAPI getEventAPI() {
    return delegate.getEventAPI();
  }

  @Override
  public boolean containerContains(Container container, Parcel parcel) {
    return delegate.containerContains(container, parcel);
  }

  @Override
  public TimeWindowPolicy getTimeWindowPolicy() {
    return delegate.getTimeWindowPolicy();
  }

  @Override
  public void service(Vehicle vehicle, Parcel parcel, TimeLapse time) {
    delegate.service(vehicle, parcel, time);
  }

  @Override
  public void tick(TimeLapse timeLapse) {
    delegate.tick(timeLapse);
  }

  @Override
  public void afterTick(TimeLapse timeLapse) {
    delegate.afterTick(timeLapse);
  }

  @Override
  public void registerModelProvider(ModelProvider mp) {
    delegate.registerModelProvider(mp);
  }
  @Override
  public void drop(Vehicle vehicle, Parcel parcel, TimeLapse time) {
  	delegate.drop(vehicle, parcel, time);
  	
  }
}
