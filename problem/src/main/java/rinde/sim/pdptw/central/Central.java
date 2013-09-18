/**
 * 
 */
package rinde.sim.pdptw.central;

import java.util.Iterator;
import java.util.Queue;
import java.util.Set;

import rinde.sim.core.Simulator;
import rinde.sim.core.SimulatorAPI;
import rinde.sim.core.SimulatorUser;
import rinde.sim.core.TickListener;
import rinde.sim.core.TimeLapse;
import rinde.sim.core.model.Model;
import rinde.sim.core.model.ModelProvider;
import rinde.sim.core.model.ModelReceiver;
import rinde.sim.pdptw.central.Solvers.SolverHandle;
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
      TickListener, ModelReceiver, SimulatorUser {
    private boolean hasChanged;
    private Optional<ModelProvider> modelProvider;
    private Optional<PDPRoadModel> roadModel;
    private Optional<SolverHandle> solverHandle;
    private final Solver solver;
    private Optional<SimulatorAPI> simulatorAPI;

    CentralModel(Solver solver) {
      modelProvider = Optional.absent();
      roadModel = Optional.absent();
      solverHandle = Optional.absent();
      simulatorAPI = Optional.absent();
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

        final Set<RouteFollowingVehicle> vehicles = roadModel.get()
            .getObjectsOfType(RouteFollowingVehicle.class);

        // gather current routes
        final ImmutableList.Builder<ImmutableList<DefaultParcel>> currentRouteBuilder = ImmutableList
            .builder();
        for (final RouteFollowingVehicle vehicle : vehicles) {
          final ImmutableList<DefaultParcel> l = ImmutableList.copyOf(vehicle
              .getRoute());
          currentRouteBuilder.add(l);
        }

        final Iterator<Queue<DefaultParcel>> routes = solverHandle.get()
            .solve(currentRouteBuilder.build()).iterator();

        for (final RouteFollowingVehicle vehicle : vehicles) {
          final Queue<DefaultParcel> route = routes.next();
          vehicle.setRoute(route);
        }

        final ImmutableList.Builder<ImmutableList<DefaultParcel>> currentRouteBuilder2 = ImmutableList
            .builder();
        for (final RouteFollowingVehicle vehicle : vehicles) {
          final ImmutableList<DefaultParcel> l = ImmutableList.copyOf(vehicle
              .getRoute());
          currentRouteBuilder2.add(l);
        }

        final StateContext sc = solverHandle.get().convert(
            currentRouteBuilder2.build());
        SolverValidator.validateInputs(sc.state);
      }
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {}

    @Override
    public void registerModelProvider(ModelProvider mp) {
      modelProvider = Optional.of(mp);
      roadModel = Optional.fromNullable(mp.getModel(PDPRoadModel.class));
      initSolver();
    }

    @Override
    public void setSimulator(SimulatorAPI api) {
      simulatorAPI = Optional.of(api);
      initSolver();
    }

    void initSolver() {
      if (modelProvider.isPresent() && simulatorAPI.isPresent()) {
        solverHandle = Optional.of(Solvers.solver(solver, modelProvider.get(),
            simulatorAPI.get()));
      }
    }
  }
}
