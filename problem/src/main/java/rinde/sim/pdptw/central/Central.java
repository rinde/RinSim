/**
 * 
 */
package rinde.sim.pdptw.central;

import java.util.Iterator;
import java.util.Queue;
import java.util.Set;

import javax.measure.Measure;

import rinde.sim.core.Simulator;
import rinde.sim.core.TickListener;
import rinde.sim.core.TimeLapse;
import rinde.sim.core.model.Model;
import rinde.sim.core.model.ModelProvider;
import rinde.sim.core.model.ModelReceiver;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.pdptw.central.Solvers.StateContext;
import rinde.sim.pdptw.common.AddVehicleEvent;
import rinde.sim.pdptw.common.DefaultParcel;
import rinde.sim.pdptw.common.DynamicPDPTWProblem.Creator;
import rinde.sim.pdptw.common.PDPRoadModel;
import rinde.sim.pdptw.common.RouteFollowingVehicle;
import rinde.sim.pdptw.experiment.DefaultMASConfiguration;
import rinde.sim.pdptw.experiment.MASConfiguration;
import rinde.sim.pdptw.experiment.MASConfigurator;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

// FIXME test this class thoroughly
/**
 * A facade for RinSim which provides a centralized interface such that
 * {@link Solver} instances can solve
 * {@link rinde.sim.pdptw.common.DynamicPDPTWScenario}s.
 * <p>
 * TODO update this comment
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public final class Central {

  private Central() {}

  /**
   * Provides a {@link MASConfigurator} instance that creates
   * {@link MASConfiguration}s that are controlled centrally by a {@link Solver}
   * .
   * @param solverCreator The solver creator to use for instantiating solvers.
   * @return A new configurator.
   */
  public static MASConfigurator solverConfigurator(SolverCreator solverCreator) {
    return new CentralConfigurator(solverCreator,
        CentralConfigurator.class.getSimpleName());
  }

  /**
   * Provides a {@link MASConfigurator} instance that creates
   * {@link MASConfiguration}s that are controlled centrally by a {@link Solver}
   * .
   * @param solverCreator The solver creator to use for instantiating solvers.
   * @param nameSuffix A string which is append to the toString() for the
   *          configurator.
   * @return A new configurator.
   */
  public static MASConfigurator solverConfigurator(SolverCreator solverCreator,
      String nameSuffix) {
    return new CentralConfigurator(solverCreator, nameSuffix);
  }

  /**
   * Implementations should create new {@link Solver} instances.
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  public interface SolverCreator {

    /**
     * Each time this method is called a new instance should be created.
     * @param seed The seed which to use for creating the instance.
     * @return A new {@link Solver} instance.
     */
    Solver create(long seed);
  }

  private static final class CentralConfigurator implements MASConfigurator {
    final SolverCreator solverCreator;
    private final String nameSuffix;

    CentralConfigurator(SolverCreator solverCreator, String name) {
      this.solverCreator = solverCreator;
      nameSuffix = name;
    }

    @Override
    public String toString() {
      return "Central-" + solverCreator.toString() + nameSuffix;
    }

    @Override
    public MASConfiguration configure(final long seed) {
      return new DefaultMASConfiguration() {

        @Override
        public Creator<AddVehicleEvent> getVehicleCreator() {
          return new VehicleCreator();
        }

        @Override
        public ImmutableList<? extends Model<?>> getModels() {
          return ImmutableList.of(new CentralModel(solverCreator.create(seed)));
        }
      };
    }

    private static final class VehicleCreator implements
        Creator<AddVehicleEvent> {

      VehicleCreator() {}

      @Override
      public boolean create(Simulator sim, AddVehicleEvent event) {
        return sim.register(new RouteFollowingVehicle(event.vehicleDTO, false));
      }
    }
  }

  private static final class CentralModel implements Model<DefaultParcel>,
      TickListener, ModelReceiver {
    private boolean hasChanged;
    private Optional<PDPRoadModel> rm;
    private Optional<PDPModel> pm;
    private final Solver solver;

    CentralModel(Solver solver) {
      rm = Optional.absent();
      pm = Optional.absent();
      this.solver = solver;
    }

    @Override
    public boolean register(DefaultParcel element) {
      hasChanged = true;
      return false;
    }

    @Override
    public boolean unregister(DefaultParcel element) {
      return false;
    }

    @Override
    public Class<DefaultParcel> getSupportedType() {
      return DefaultParcel.class;
    }

    @Override
    public void tick(TimeLapse timeLapse) {
      if (hasChanged) {
        hasChanged = false;
        // TODO check to see that this is called the first possible moment after
        // the add parcel event was dispatched

        // TODO it must be checked whether the calculated routes end up in the
        // correct vehicles

        final Set<RouteFollowingVehicle> vehicles = rm.get().getObjectsOfType(
            RouteFollowingVehicle.class);

        System.out.println(vehicles.getClass());

        // gather current routes
        final ImmutableList.Builder<ImmutableList<DefaultParcel>> currentRouteBuilder = ImmutableList
            .builder();
        System.out.println(".......in vehicle");
        for (final RouteFollowingVehicle vehicle : vehicles) {
          final ImmutableList<DefaultParcel> l = ImmutableList.copyOf(vehicle
              .getRoute());
          System.out.println(l);
          currentRouteBuilder.add(l);
        }

        final Iterator<Queue<DefaultParcel>> routes = Solvers.solve(solver,
            rm.get(), pm.get(),
            Measure.valueOf(timeLapse.getTime(), timeLapse.getTimeUnit()),
            currentRouteBuilder.build()).iterator();
        System.out.println("....... set route");
        for (final RouteFollowingVehicle vehicle : vehicles) {
          final Queue<DefaultParcel> route = routes.next();
          System.out.println(route);
          vehicle.setRoute(route);
        }

        final ImmutableList.Builder<ImmutableList<DefaultParcel>> currentRouteBuilder2 = ImmutableList
            .builder();
        for (final RouteFollowingVehicle vehicle : vehicles) {
          final ImmutableList<DefaultParcel> l = ImmutableList.copyOf(vehicle
              .getRoute());
          currentRouteBuilder2.add(l);
        }

        final StateContext sc = Solvers.convert(rm.get(), pm.get(),
            Measure.valueOf(timeLapse.getTime(), timeLapse.getTimeUnit()),
            currentRouteBuilder2.build());

        SolverValidator.validateInputs(sc.state);

      }
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {}

    @Override
    public void registerModelProvider(ModelProvider mp) {
      pm = Optional.fromNullable(mp.getModel(PDPModel.class));
      rm = Optional.fromNullable(mp.getModel(PDPRoadModel.class));
    }
  }
}
