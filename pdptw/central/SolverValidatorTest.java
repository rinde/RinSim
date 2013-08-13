/**
 * 
 */
package rinde.sim.pdptw.central;

import static rinde.sim.pdptw.central.ConverterTest.createGlobalStateObject;
import static rinde.sim.pdptw.central.ConverterTest.createVehicleState;

import org.junit.Test;

import rinde.sim.core.graph.Point;
import rinde.sim.pdptw.central.GlobalStateObject.VehicleState;
import rinde.sim.problem.common.ParcelDTO;
import rinde.sim.problem.common.VehicleDTO;
import rinde.sim.util.TestUtil;
import rinde.sim.util.TimeWindow;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class SolverValidatorTest {

    @Test(expected = IllegalArgumentException.class)
    public void validateNegativeTime() {
        @SuppressWarnings("null")
        final GlobalStateObject state = new GlobalStateObject(null, null, -1,
                null, null, null);
        SolverValidator.validateInputs(state);
    }

    @Test(expected = IllegalArgumentException.class)
    @SuppressWarnings("null")
    public void validateNegativeRemainingTime() {
        final VehicleDTO dto1 = new VehicleDTO(null, 1, 1, null);
        final VehicleState vs1 = new VehicleState(dto1, null, null, -1);
        final GlobalStateObject state = new GlobalStateObject(null,
                ImmutableList.of(vs1), 0, null, null, null);
        SolverValidator.validateInputs(state);
    }

    @Test(expected = IllegalArgumentException.class)
    @SuppressWarnings("null")
    public void validateZeroSpeed() {
        final VehicleDTO dto1 = new VehicleDTO(null, 0, 1, null);
        final VehicleState vs1 = new VehicleState(dto1, null, null, 0);
        final GlobalStateObject state = new GlobalStateObject(null,
                ImmutableList.of(vs1), 0, null, null, null);
        SolverValidator.validateInputs(state);
    }

    @SuppressWarnings("null")
    @Test(expected = IllegalArgumentException.class)
    public void validateParcelAvailableAndInInventory() {
        final VehicleDTO dto1 = new VehicleDTO(null, 1, 1, null);
        final ParcelDTO p1 = new ParcelDTO(new Point(0, 0), new Point(1, 1),
                TimeWindow.ALWAYS, TimeWindow.ALWAYS, 0, 0, 0, 0);
        final VehicleState vs1 = new VehicleState(dto1, null,
                ImmutableSet.of(p1), 0);
        final GlobalStateObject state = new GlobalStateObject(
                ImmutableSet.of(p1), ImmutableList.of(vs1), 0, null, null, null);
        SolverValidator.validateInputs(state);
    }

    @SuppressWarnings("null")
    @Test(expected = IllegalArgumentException.class)
    public void validateParcelInTwoInventories() {
        final VehicleDTO dto1 = new VehicleDTO(null, 1, 1, null);
        final VehicleDTO dto2 = new VehicleDTO(null, 1, 1, null);
        final ParcelDTO p1 = new ParcelDTO(new Point(0, 0), new Point(1, 1),
                TimeWindow.ALWAYS, TimeWindow.ALWAYS, 0, 0, 0, 0);
        final VehicleState vs1 = new VehicleState(dto1, null,
                ImmutableSet.of(p1), 0);
        final VehicleState vs2 = new VehicleState(dto2, null,
                ImmutableSet.of(p1), 0);
        final ImmutableSet<ParcelDTO> empty = ImmutableSet.of();
        final GlobalStateObject state = new GlobalStateObject(empty,
                ImmutableList.of(vs1, vs2), 0, null, null, null);
        SolverValidator.validateInputs(state);
    }

    @Test
    public void validateCorrectInputs() {
        final VehicleDTO dto1 = new VehicleDTO(null, 1, 1, null);
        final VehicleDTO dto2 = new VehicleDTO(null, 1, 1, null);
        final ParcelDTO p1 = new ParcelDTO(new Point(0, 0), new Point(1, 1),
                TimeWindow.ALWAYS, TimeWindow.ALWAYS, 0, 0, 0, 0);
        final ParcelDTO p2 = new ParcelDTO(new Point(0, 0), new Point(1, 1),
                TimeWindow.ALWAYS, TimeWindow.ALWAYS, 0, 0, 0, 0);
        final VehicleState vs1 = new VehicleState(dto1, null,
                ImmutableSet.of(p1), 0);
        final VehicleState vs2 = new VehicleState(dto2, null,
                ImmutableSet.of(p2), 0);
        final ImmutableSet<ParcelDTO> empty = ImmutableSet.of();
        final GlobalStateObject state = new GlobalStateObject(empty,
                ImmutableList.of(vs1, vs2), 0, null, null, null);
        SolverValidator.validateInputs(state);
    }

    @SuppressWarnings("null")
    @Test(expected = IllegalArgumentException.class)
    public void validateInvalidNumberOfRoutes() {
        final VehicleState vs1 = createVehicleState(new VehicleDTO(new Point(0,
                0), 1, 1, TimeWindow.ALWAYS), new Point(0, 0), null, 0);
        final ImmutableList<ImmutableList<ParcelDTO>> routes = ImmutableList
                .of();
        final GlobalStateObject state = createGlobalStateObject(null, ImmutableList
                .of(vs1), 0, null, null, null);
        SolverValidator.validateOutputs(routes, state);
    }

    @SuppressWarnings("null")
    @Test(expected = IllegalArgumentException.class)
    public void validateParcelInTwoRoutes() {
        final ImmutableSet<ParcelDTO> empty = ImmutableSet.of();
        final VehicleState vs1 = createVehicleState(new VehicleDTO(new Point(0,
                0), 1, 1, TimeWindow.ALWAYS), new Point(0, 0), empty, 0);
        final VehicleState vs2 = createVehicleState(new VehicleDTO(new Point(0,
                0), 1, 1, TimeWindow.ALWAYS), new Point(0, 0), empty, 0);
        final ParcelDTO p1 = new ParcelDTO(new Point(0, 0), new Point(0, 0),
                TimeWindow.ALWAYS, TimeWindow.ALWAYS, 0, 0, 0, 0);

        final ImmutableList<ImmutableList<ParcelDTO>> routes = ImmutableList
                .of(ImmutableList.of(p1, p1), ImmutableList.of(p1, p1));
        final ImmutableSet<ParcelDTO> availableParcels = ImmutableSet.of(p1);
        final ImmutableList<VehicleState> vehicles = ImmutableList.of(vs1, vs2);
        final GlobalStateObject state = createGlobalStateObject(availableParcels, vehicles, 0, null, null, null);
        SolverValidator.validateOutputs(routes, state);
    }

    @SuppressWarnings("null")
    @Test(expected = IllegalArgumentException.class)
    public void validateParcelTooManyTimes1() {
        final ImmutableSet<ParcelDTO> empty = ImmutableSet.of();
        final VehicleState vs1 = createVehicleState(new VehicleDTO(new Point(0,
                0), 1, 1, TimeWindow.ALWAYS), new Point(0, 0), empty, 0);
        final ParcelDTO p1 = new ParcelDTO(new Point(0, 0), new Point(0, 0),
                TimeWindow.ALWAYS, TimeWindow.ALWAYS, 0, 0, 0, 0);

        final ImmutableList<ImmutableList<ParcelDTO>> routes = ImmutableList
                .of(ImmutableList.of(p1, p1, p1));
        final ImmutableSet<ParcelDTO> availableParcels = ImmutableSet.of(p1);
        final ImmutableList<VehicleState> vehicles = ImmutableList.of(vs1);
        final GlobalStateObject state = createGlobalStateObject(availableParcels, vehicles, 0, null, null, null);
        SolverValidator.validateOutputs(routes, state);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateParcelTooManyTimes2() {
        final ParcelDTO p1 = new ParcelDTO(new Point(0, 0), new Point(0, 0),
                TimeWindow.ALWAYS, TimeWindow.ALWAYS, 0, 0, 0, 0);
        final VehicleState vs1 = createVehicleState(new VehicleDTO(new Point(0,
                0), 1, 1, TimeWindow.ALWAYS), new Point(0, 0), ImmutableSet.of(p1), 0);

        final ImmutableList<ImmutableList<ParcelDTO>> routes = ImmutableList
                .of(ImmutableList.of(p1, p1));
        final ImmutableSet<ParcelDTO> availableParcels = ImmutableSet.of();
        final ImmutableList<VehicleState> vehicles = ImmutableList.of(vs1);
        final GlobalStateObject state = createGlobalStateObject(availableParcels, vehicles, 0, null, null, null);
        SolverValidator.validateOutputs(routes, state);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateParcelNotInCargo() {
        final ImmutableSet<ParcelDTO> empty = ImmutableSet.of();
        final ParcelDTO p1 = new ParcelDTO(new Point(0, 0), new Point(0, 0),
                TimeWindow.ALWAYS, TimeWindow.ALWAYS, 0, 0, 0, 0);
        final VehicleState vs1 = createVehicleState(new VehicleDTO(new Point(0,
                0), 1, 1, TimeWindow.ALWAYS), new Point(0, 0), empty, 0);

        final ImmutableList<ImmutableList<ParcelDTO>> routes = ImmutableList
                .of(ImmutableList.of(p1));
        final ImmutableSet<ParcelDTO> availableParcels = ImmutableSet.of();
        final ImmutableList<VehicleState> vehicles = ImmutableList.of(vs1);
        final GlobalStateObject state = createGlobalStateObject(availableParcels, vehicles, 0, null, null, null);
        SolverValidator.validateOutputs(routes, state);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateUnknownParcelInRoute() {
        final ImmutableSet<ParcelDTO> empty = ImmutableSet.of();
        final ParcelDTO p1 = new ParcelDTO(new Point(0, 0), new Point(0, 0),
                new TimeWindow(1, 1), new TimeWindow(1, 1), 0, 0, 0, 0);
        final ParcelDTO p2 = new ParcelDTO(new Point(0, 0), new Point(0, 0),
                new TimeWindow(2, 2), new TimeWindow(2, 2), 0, 0, 0, 0);
        final ParcelDTO p3 = new ParcelDTO(new Point(0, 0), new Point(0, 0),
                new TimeWindow(3, 3), new TimeWindow(3, 3), 0, 0, 0, 0);
        final VehicleState vs1 = createVehicleState(new VehicleDTO(new Point(0,
                0), 1, 1, TimeWindow.ALWAYS), new Point(0, 0), empty, 0);
        final VehicleState vs2 = createVehicleState(new VehicleDTO(new Point(0,
                0), 1, 1, TimeWindow.ALWAYS), new Point(0, 0), empty, 0);

        final ImmutableList<ImmutableList<ParcelDTO>> routes = ImmutableList
                .of(ImmutableList.of(p1, p1), ImmutableList.of(p2, p3, p3, p2));
        final ImmutableSet<ParcelDTO> availableParcels = ImmutableSet
                .of(p1, p2);
        final ImmutableList<VehicleState> vehicles = ImmutableList.of(vs1, vs2);
        final GlobalStateObject state = createGlobalStateObject(availableParcels, vehicles, 0, null, null, null);
        SolverValidator.validateOutputs(routes, state);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateIncompleteRoute1() {
        final ImmutableSet<ParcelDTO> empty = ImmutableSet.of();
        final ParcelDTO p1 = new ParcelDTO(new Point(0, 0), new Point(0, 0),
                TimeWindow.ALWAYS, TimeWindow.ALWAYS, 0, 0, 0, 0);
        final ParcelDTO p2 = new ParcelDTO(new Point(0, 0), new Point(0, 0),
                TimeWindow.ALWAYS, TimeWindow.ALWAYS, 0, 0, 0, 0);
        final ParcelDTO p3 = new ParcelDTO(new Point(0, 0), new Point(0, 0),
                TimeWindow.ALWAYS, TimeWindow.ALWAYS, 0, 0, 0, 0);
        final VehicleState vs1 = createVehicleState(new VehicleDTO(new Point(0,
                0), 1, 1, TimeWindow.ALWAYS), new Point(0, 0), empty, 0);
        final VehicleState vs2 = createVehicleState(new VehicleDTO(new Point(0,
                0), 1, 1, TimeWindow.ALWAYS), new Point(0, 0), empty, 0);

        final ImmutableList<ImmutableList<ParcelDTO>> routes = ImmutableList
                .of(ImmutableList.of(p1, p1), ImmutableList.of(p2, p2));
        final ImmutableSet<ParcelDTO> availableParcels = ImmutableSet
                .of(p1, p2, p3);
        final ImmutableList<VehicleState> vehicles = ImmutableList.of(vs1, vs2);
        final GlobalStateObject state = createGlobalStateObject(availableParcels, vehicles, 0, null, null, null);
        SolverValidator.validateOutputs(routes, state);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateIncompleteRouteForVehicle() {
        final ParcelDTO p1 = new ParcelDTO(new Point(0, 0), new Point(0, 0),
                TimeWindow.ALWAYS, TimeWindow.ALWAYS, 0, 0, 0, 0);
        final VehicleState vs1 = createVehicleState(new VehicleDTO(new Point(0,
                0), 1, 1, TimeWindow.ALWAYS), new Point(0, 0), ImmutableSet.of(p1), 0);

        final ImmutableList<ParcelDTO> empty = ImmutableList.of();
        final ImmutableList<ImmutableList<ParcelDTO>> routes = ImmutableList
                .of(empty);
        final ImmutableSet<ParcelDTO> availableParcels = ImmutableSet.of();
        final ImmutableList<VehicleState> vehicles = ImmutableList.of(vs1);
        final GlobalStateObject state = createGlobalStateObject(availableParcels, vehicles, 0, null, null, null);
        SolverValidator.validateOutputs(routes, state);
    }

    @Test
    public void validateCorrectOutput() {
        final ImmutableSet<ParcelDTO> empty = ImmutableSet.of();
        final ParcelDTO p1 = new ParcelDTO(new Point(0, 0), new Point(0, 0),
                TimeWindow.ALWAYS, TimeWindow.ALWAYS, 0, 0, 0, 0);
        final ParcelDTO p2 = new ParcelDTO(new Point(0, 0), new Point(0, 0),
                TimeWindow.ALWAYS, TimeWindow.ALWAYS, 0, 0, 0, 0);
        final ParcelDTO p3 = new ParcelDTO(new Point(0, 0), new Point(0, 0),
                TimeWindow.ALWAYS, TimeWindow.ALWAYS, 0, 0, 0, 0);
        final VehicleState vs1 = createVehicleState(new VehicleDTO(new Point(0,
                0), 1, 1, TimeWindow.ALWAYS), new Point(0, 0), empty, 0);
        final VehicleState vs2 = createVehicleState(new VehicleDTO(new Point(0,
                0), 1, 1, TimeWindow.ALWAYS), new Point(0, 0), ImmutableSet.of(p3), 0);

        final ImmutableList<ImmutableList<ParcelDTO>> routes = ImmutableList
                .of(ImmutableList.of(p1, p1), ImmutableList.of(p2, p3, p2));
        final ImmutableSet<ParcelDTO> availableParcels = ImmutableSet
                .of(p1, p2);
        final ImmutableList<VehicleState> vehicles = ImmutableList.of(vs1, vs2);
        final GlobalStateObject state = createGlobalStateObject(availableParcels, vehicles, 0, null, null, null);
        SolverValidator.validateOutputs(routes, state);
    }

    @Test
    public void testWrap() {
        TestUtil.testPrivateConstructor(SolverValidator.class);
        final ImmutableSet<ParcelDTO> empty = ImmutableSet.of();
        final ParcelDTO p1 = new ParcelDTO(new Point(0, 0), new Point(0, 0),
                TimeWindow.ALWAYS, TimeWindow.ALWAYS, 0, 0, 0, 0);
        final ParcelDTO p2 = new ParcelDTO(new Point(0, 0), new Point(0, 0),
                TimeWindow.ALWAYS, TimeWindow.ALWAYS, 0, 0, 0, 0);
        final ParcelDTO p3 = new ParcelDTO(new Point(0, 0), new Point(0, 0),
                TimeWindow.ALWAYS, TimeWindow.ALWAYS, 0, 0, 0, 0);
        final VehicleState vs1 = createVehicleState(new VehicleDTO(new Point(0,
                0), 1, 1, TimeWindow.ALWAYS), new Point(0, 0), empty, 0);
        final VehicleState vs2 = createVehicleState(new VehicleDTO(new Point(0,
                0), 1, 1, TimeWindow.ALWAYS), new Point(0, 0), ImmutableSet.of(p3), 0);

        final ImmutableList<ImmutableList<ParcelDTO>> routes = ImmutableList
                .of(ImmutableList.of(p1, p1), ImmutableList.of(p2, p3, p2));
        final ImmutableSet<ParcelDTO> availableParcels = ImmutableSet
                .of(p1, p2);
        final ImmutableList<VehicleState> vehicles = ImmutableList.of(vs1, vs2);
        final GlobalStateObject state = createGlobalStateObject(availableParcels, vehicles, 0, null, null, null);
        final Solver solver = SolverValidator.wrap(new FakeSolver(routes));
        solver.solve(state);
    }

    class FakeSolver implements Solver {
        ImmutableList<ImmutableList<ParcelDTO>> answer;

        FakeSolver(ImmutableList<ImmutableList<ParcelDTO>> answer) {
            this.answer = answer;
        }

        public ImmutableList<ImmutableList<ParcelDTO>> solve(
                GlobalStateObject state) {
            return answer;
        }
    }

}
