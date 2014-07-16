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
import rinde.sim.pdptw.central.Solvers.SimulationSolver;
import rinde.sim.pdptw.central.Solvers.SolveArgs;
import rinde.sim.pdptw.common.AddVehicleEvent;
import rinde.sim.pdptw.common.DefaultParcel;
import rinde.sim.pdptw.common.DynamicPDPTWProblem.Creator;
import rinde.sim.pdptw.common.PDPRoadModel;
import rinde.sim.pdptw.common.RouteFollowingVehicle;
import rinde.sim.pdptw.experiment.DefaultMASConfiguration;
import rinde.sim.pdptw.experiment.MASConfiguration;
import rinde.sim.util.StochasticSupplier;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

// FIXME test this class thoroughly
/**
 * A facade for RinSim which provides a centralized interface such that
 * {@link Solver} instances can solve
 * {@link rinde.sim.pdptw.scenario.PDPScenario}s.
 * <p>
 * TODO update this comment
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public final class Central {

  private Central() {}

  /**
   * Provides a {@link MASConfiguration} that configures a MAS that is
   * controlled centrally by a {@link Solver}.
   * @param solverCreator The solver creator to use for instantiating solvers.
   * @return A new configuration.
   */
  public static MASConfiguration solverConfiguration(
      StochasticSupplier<? extends Solver> solverCreator) {
    return new CentralConfiguration(solverCreator, "");
  }

  /**
   * Provides a {@link MASConfiguration} that configures a MAS that is
   * controlled centrally by a {@link Solver}.
   * @param solverCreator The solver creator to use for instantiating solvers.
   * @param nameSuffix A string which is append to the toString() for the
   *          configuration.
   * @return A new configuration.
   */
  public static MASConfiguration solverConfiguration(
      StochasticSupplier<? extends Solver> solverCreator, String nameSuffix) {
    return new CentralConfiguration(solverCreator, nameSuffix);
  }

  private static final class CentralConfiguration extends
      DefaultMASConfiguration {
    private static final long serialVersionUID = 8906291887010954854L;
    final StochasticSupplier<? extends Solver> solverCreator;
    private final String nameSuffix;

    CentralConfiguration(StochasticSupplier<? extends Solver> solverCreator,
        String name) {
      this.solverCreator = solverCreator;
      nameSuffix = name;
    }

    @Override
    public Creator<AddVehicleEvent> getVehicleCreator() {
      return new VehicleCreator();
    }

    @Override
    public ImmutableList<? extends StochasticSupplier<? extends Model<?>>> getModels() {
      return ImmutableList.of(new CentralModelSupplier(solverCreator));
    }

    @Override
    public String toString() {
      return "Central-" + solverCreator.toString() + nameSuffix;
    }
  }

  private static final class VehicleCreator implements Creator<AddVehicleEvent> {
    VehicleCreator() {}

    @Override
    public boolean create(Simulator sim, AddVehicleEvent event) {
      return sim.register(new RouteFollowingVehicle(event.vehicleDTO, false));
    }
  }

  private static class CentralModelSupplier implements
      StochasticSupplier<CentralModel> {
    private final StochasticSupplier<? extends Solver> solverSupplier;

    CentralModelSupplier(StochasticSupplier<? extends Solver> solverSupplier) {
      this.solverSupplier = solverSupplier;
    }

    @Override
    public CentralModel get(long seed) {
      return new CentralModel(solverSupplier.get(seed));
    }
  }

  private static final class CentralModel implements Model<DefaultParcel>,
      TickListener, ModelReceiver, SimulatorUser {
    private boolean hasChanged;
    private Optional<ModelProvider> modelProvider;
    private Optional<PDPRoadModel> roadModel;
    private Optional<SimulationSolver> solverAdapter;
    private final Solver solver;
    private Optional<SimulatorAPI> simulatorAPI;

    CentralModel(Solver solver) {
      modelProvider = Optional.absent();
      roadModel = Optional.absent();
      solverAdapter = Optional.absent();
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

        final Iterator<Queue<DefaultParcel>> routes = solverAdapter
            .get()
            .solve(
                SolveArgs.create().useAllParcels()
                    .useCurrentRoutes(currentRouteBuilder.build())).iterator();

        for (final RouteFollowingVehicle vehicle : vehicles) {
          vehicle.setRoute(routes.next());
        }
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

        solverAdapter = Optional.of(Solvers.solverBuilder(solver)
            .with(modelProvider.get()).with(simulatorAPI.get()).build());
      }
    }
  }
}
