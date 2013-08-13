/**
 * 
 */
package rinde.sim.pdptw.central.solver;

import static org.junit.Assert.fail;
import static rinde.sim.pdptw.central.ConverterTest.createGlobalStateObject;
import static rinde.sim.pdptw.central.ConverterTest.createVehicleState;
import static rinde.sim.pdptw.central.solver.SolverValidator.validateInputs;
import static rinde.sim.pdptw.central.solver.SolverValidator.validateOutputs;
import static rinde.sim.pdptw.central.solver.SolverValidator.wrap;

import java.lang.reflect.Constructor;

import org.junit.Test;

import rinde.sim.core.graph.Point;
import rinde.sim.pdptw.central.ConverterTest;
import rinde.sim.pdptw.central.GlobalStateObject;
import rinde.sim.pdptw.central.GlobalStateObject.VehicleState;
import rinde.sim.pdptw.central.solver.MultiVehicleArraysSolver;
import rinde.sim.pdptw.central.solver.SingleVehicleArraysSolver;
import rinde.sim.pdptw.central.solver.SolutionObject;
import rinde.sim.pdptw.central.solver.Solver;
import rinde.sim.pdptw.central.solver.SolverValidator;
import rinde.sim.problem.common.ParcelDTO;
import rinde.sim.problem.common.VehicleDTO;
import rinde.sim.util.TimeWindow;
import rinde.solver.pdptw.MipTest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class SolverValidatorTest {

    /*
     * VALIDATE SOLVER INPUTS
     */

    @Test(expected = IllegalArgumentException.class)
    public void validateNegativeTime() {
        @SuppressWarnings("null")
        final GlobalStateObject state = ConverterTest
                .createGlobalStateObject(null, null, -1, null, null, null);
        validateInputs(state);
    }

    @Test(expected = IllegalArgumentException.class)
    @SuppressWarnings("null")
    public void validateNegativeRemainingTime() {
        final VehicleDTO dto1 = new VehicleDTO(null, 1, 1, null);
        final VehicleState vs1 = createVehicleState(dto1, null, null, -1);
        final GlobalStateObject state = createGlobalStateObject(null, ImmutableList
                .of(vs1), 0, null, null, null);
        validateInputs(state);
    }

    @Test(expected = IllegalArgumentException.class)
    @SuppressWarnings("null")
    public void validateZeroSpeed() {
        final VehicleDTO dto1 = new VehicleDTO(null, 0, 1, null);
        final VehicleState vs1 = createVehicleState(dto1, null, null, 0);
        final GlobalStateObject state = createGlobalStateObject(null, ImmutableList
                .of(vs1), 0, null, null, null);
        validateInputs(state);
    }

    @SuppressWarnings("null")
    @Test(expected = IllegalArgumentException.class)
    public void validateParcelAvailableAndInInventory() {
        final VehicleDTO dto1 = new VehicleDTO(null, 1, 1, null);
        final ParcelDTO p1 = new ParcelDTO(new Point(0, 0), new Point(1, 1),
                TimeWindow.ALWAYS, TimeWindow.ALWAYS, 0, 0, 0, 0);
        final VehicleState vs1 = createVehicleState(dto1, null, ImmutableSet.of(p1), 0);
        final GlobalStateObject state = createGlobalStateObject(ImmutableSet.of(p1), ImmutableList
                .of(vs1), 0, null, null, null);
        validateInputs(state);
    }

    @SuppressWarnings("null")
    @Test(expected = IllegalArgumentException.class)
    public void validateParcelInTwoInventories() {
        final VehicleDTO dto1 = new VehicleDTO(null, 1, 1, null);
        final VehicleDTO dto2 = new VehicleDTO(null, 1, 1, null);
        final ParcelDTO p1 = new ParcelDTO(new Point(0, 0), new Point(1, 1),
                TimeWindow.ALWAYS, TimeWindow.ALWAYS, 0, 0, 0, 0);
        final VehicleState vs1 = createVehicleState(dto1, null, ImmutableSet.of(p1), 0);
        final VehicleState vs2 = createVehicleState(dto2, null, ImmutableSet.of(p1), 0);
        final ImmutableSet<ParcelDTO> empty = ImmutableSet.of();
        final GlobalStateObject state = createGlobalStateObject(empty, ImmutableList
                .of(vs1, vs2), 0, null, null, null);
        validateInputs(state);
    }

    @Test
    public void validateCorrectInputs() {
        final VehicleDTO dto1 = new VehicleDTO(null, 1, 1, null);
        final VehicleDTO dto2 = new VehicleDTO(null, 1, 1, null);
        final ParcelDTO p1 = new ParcelDTO(new Point(0, 0), new Point(1, 1),
                TimeWindow.ALWAYS, TimeWindow.ALWAYS, 0, 0, 0, 0);
        final ParcelDTO p2 = new ParcelDTO(new Point(0, 0), new Point(1, 1),
                TimeWindow.ALWAYS, TimeWindow.ALWAYS, 0, 0, 0, 0);
        final VehicleState vs1 = createVehicleState(dto1, null, ImmutableSet.of(p1), 0);
        final VehicleState vs2 = createVehicleState(dto2, null, ImmutableSet.of(p2), 0);
        final ImmutableSet<ParcelDTO> empty = ImmutableSet.of();
        final GlobalStateObject state = createGlobalStateObject(empty, ImmutableList
                .of(vs1, vs2), 0, null, null, null);
        validateInputs(state);
    }

    /*
     * VALIDATE SINGLE VEHICLE INPUTS
     */

    @Test(expected = IllegalArgumentException.class)
    public void validateInputsEmptyTravelTimeMatrix() {
        validateInputs(new int[][] {}, new int[] {}, new int[] {}, new int[][] {}, new int[] {});
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateInputsInvalidTravelTimeMatrix1() {
        validateInputs(new int[7][6], new int[] {}, new int[] {}, new int[][] {}, new int[] {});
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateInputsInvalidTravelTimeMatrix2() {
        validateInputs(new int[][] { new int[3], new int[3], new int[4] }, new int[] {}, new int[] {}, new int[][] {}, new int[] {});
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateInputsInvalidReleaseDatesLength() {
        validateInputs(new int[4][4], new int[5], new int[] {}, new int[][] {}, new int[] {});
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateInputsInvalidDueDatesLength() {
        validateInputs(new int[4][4], new int[4], new int[2], new int[][] {}, new int[] {});
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateInputsInvalidServiceTime() {
        validateInputs(new int[7][7], new int[7], new int[7], new int[][] {}, new int[3]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateInputsInvalidTW() {
        validateInputs(new int[3][3], new int[] { 1, 2, 3 }, new int[] { 1, 3,
                2 }, new int[][] {}, new int[3]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateInputsInvalidTWstart1() {
        validateInputs(new int[3][3], new int[] { 1, 2, 3 }, new int[] { 1, 3,
                6 }, new int[][] {}, new int[3]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateInputsInvalidTWstart2() {
        validateInputs(new int[3][3], new int[] { 0, 2, 3 }, new int[] { 1, 3,
                6 }, new int[][] {}, new int[3]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateInputsInvalidTWdepot() {
        validateInputs(new int[3][3], new int[] { 0, 2, 1 }, new int[] { 0, 3,
                23 }, new int[][] {}, new int[3]);
    }

    @Test
    public void validateInputsValidEmpty() {
        validateInputs(new int[3][3], new int[3], new int[3], new int[][] {}, new int[3]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateInputsInvalidPairSize() {
        validateInputs(new int[4][4], new int[4], new int[4], new int[][] {
                new int[] { 1, 2 }, new int[3] }, new int[4]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateInputsInvalidPairLocation1() {
        validateInputs(new int[6][6], new int[6], new int[6], new int[][] {
                new int[] { 1, 2 }, new int[] { 3, 0 } }, new int[6]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateInputsInvalidPairLocation2() {
        validateInputs(new int[6][6], new int[6], new int[6], new int[][] {
                new int[] { 1, 2 }, new int[] { 3, 913 } }, new int[6]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateInputsDuplicatePairLocation() {
        validateInputs(new int[6][6], new int[6], new int[6], new int[][] {
                new int[] { 1, 2 }, new int[] { 2, 0 } }, new int[6]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateInputsInvalidArrivalTimes() {
        validateInputs(new int[6][6], new int[6], new int[6], new int[][] {
                new int[] { 4, 2 }, new int[] { 3, 1 } }, new int[] { -1, -1,
                0, 0, 0, 0 });
    }

    @Test
    public void validateInputsValid() {
        validateInputs(new int[6][6], new int[6], new int[6], new int[][] {
                new int[] { 4, 2 }, new int[] { 3, 1 } }, new int[6]);
    }

    /*
     * VALIDATE MULTI VEHICLE INPUTS
     */

    @Test(expected = IllegalArgumentException.class)
    public void validateInputsInvalidNoVehicles() {
        validateInputs(new int[6][6], new int[6], new int[6], new int[][] {
                new int[] { 4, 2 }, new int[] { 3, 1 } }, new int[6],//
                new int[0][0], new int[0][0], new int[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateInputsInvalidVehicleTravelTimes1() {
        validateInputs(new int[6][6], new int[6], new int[6], new int[][] {
                new int[] { 4, 2 }, new int[] { 3, 1 } }, new int[6], //
                new int[2][0], new int[0][0], new int[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateInputsInvalidVehicleTravelTimes2() {
        validateInputs(new int[6][6], new int[6], new int[6], new int[][] {
                new int[] { 4, 2 }, new int[] { 3, 1 } }, new int[6], //
                new int[][] { { 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, -1, 0 } }, //
                new int[0][0], new int[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateInputsInvalidInventories1() {
        // inventory too big
        validateInputs(new int[6][6], new int[6], new int[6], new int[][] {
                new int[] { 4, 2 }, new int[] { 3, 1 } }, new int[6], //
                new int[][] { { 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0 } }, //
                new int[][] { { 0 } }, new int[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateInputsInvalidInventories2() {
        // inventory too small
        validateInputs(new int[6][6], new int[6], new int[6], new int[][] { new int[] {
                3, 1 } }, new int[6], //
                new int[][] { { 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0 } }, //
                new int[][] { { 0 } }, new int[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateInputsInvalidInventories3() {
        // invalid inventory dimensions
        validateInputs(new int[6][6], new int[6], new int[6], new int[][] { new int[] {
                3, 1 } }, new int[6], //
                new int[][] { { 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0 } }, //
                new int[][] { { 0 }, { 0 } }, new int[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateInputsInvalidInventories4a() {
        // non existing vehicle
        validateInputs(new int[6][6], new int[6], new int[6], new int[][] { new int[] {
                3, 1 } }, new int[6], //
                new int[][] { { 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0 } }, //
                new int[][] { { 2, 0 }, { 2, 0 } }, new int[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateInputsInvalidInventories4b() {
        // non existing vehicle
        validateInputs(new int[6][6], new int[6], new int[6], new int[][] { new int[] {
                3, 1 } }, new int[6], //
                new int[][] { { 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0 } }, //
                new int[][] { { -1, 0 }, { 2, 0 } }, new int[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateInputsInvalidInventories5a() {
        // non existing location
        validateInputs(new int[6][6], new int[6], new int[6], new int[][] { new int[] {
                3, 1 } }, new int[6], //
                new int[][] { { 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0 } }, //
                new int[][] { { 1, 0 }, { 1, 0 } }, new int[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateInputsInvalidInventories5b() {
        // non existing location
        validateInputs(new int[6][6], new int[6], new int[6], new int[][] { new int[] {
                3, 1 } }, new int[6], //
                new int[][] { { 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0 } }, //
                new int[][] { { 1, 5 }, { 1, 5 } }, new int[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateInputsInvalidInventories6() {
        // reference to still available location in inventory
        validateInputs(new int[6][6], new int[6], new int[6], new int[][] { new int[] {
                3, 1 } }, new int[6], //
                new int[][] { { 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0 } }, //
                new int[][] { { 1, 1 }, { 1, 1 } }, new int[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateInputsInvalidInventories7() {
        // duplicate inventory entry
        validateInputs(new int[6][6], new int[6], new int[6], new int[][] { new int[] {
                4, 2 } }, new int[6], //
                new int[][] { { 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0 } }, //
                new int[][] { { 1, 1 }, { 1, 1 } }, new int[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateInputsInvalidRemainingServiceTimes1() {
        // incorrect size of array
        validateInputs(new int[6][6], new int[6], new int[6], new int[][] { new int[] {
                4, 2 } }, new int[6], //
                new int[][] { { 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0 } }, //
                new int[][] { { 1, 1 }, { 1, 3 } }, new int[] { 1 });
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateInputsInvalidRemainingServiceTimes2() {
        // incorrect time value
        validateInputs(new int[6][6], new int[6], new int[6], new int[][] { new int[] {
                4, 2 } }, new int[6], //
                new int[][] { { 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0 } }, //
                new int[][] { { 1, 1 }, { 1, 3 } }, new int[] { 300, -1 });
    }

    @Test
    public void validateInputsValidMulti() {
        // incorrect time value
        validateInputs(new int[6][6], new int[6], new int[6], new int[][] { new int[] {
                4, 2 } }, new int[6], //
                new int[][] { { 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0 } }, //
                new int[][] { { 1, 1 }, { 1, 3 } }, new int[] { 300, 0 });
    }

    /*
     * VALIDATE SOLUTION
     */

    @SuppressWarnings("null")
    @Test(expected = IllegalArgumentException.class)
    public void validateInvalidNumberOfRoutes() {
        final VehicleState vs1 = createVehicleState(new VehicleDTO(new Point(0,
                0), 1, 1, TimeWindow.ALWAYS), new Point(0, 0), null, 0);
        final ImmutableList<ImmutableList<ParcelDTO>> routes = ImmutableList
                .of();
        final GlobalStateObject state = createGlobalStateObject(null, ImmutableList
                .of(vs1), 0, null, null, null);
        validateOutputs(routes, state);
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
        validateOutputs(routes, state);
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
        validateOutputs(routes, state);
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
        validateOutputs(routes, state);
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
        validateOutputs(routes, state);
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
        validateOutputs(routes, state);
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
        validateOutputs(routes, state);
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
        validateOutputs(routes, state);
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
        validateOutputs(routes, state);
    }

    /**
     * Route length must be equal to number of locations (4).
     */
    @Test(expected = IllegalArgumentException.class)
    public void validateInvalidRouteLength() {
        validateOutputs(new SolutionObject(new int[3], new int[1], 0), new int[4][4], new int[4], new int[4], new int[][] {}, new int[4]);
    }

    /**
     * A location may only be visited once, duplicates are not allowed.
     */
    @Test(expected = IllegalArgumentException.class)
    public void validateDuplicatesInRouteStart() {
        validateOutputs(new SolutionObject(new int[] { 1, 1, 1, 1 },
                new int[1], 0), new int[4][4], new int[4], new int[4], new int[][] {}, new int[4]);
    }

    /**
     * The first location visited must always be the vehicle start location (0).
     */
    @Test(expected = IllegalArgumentException.class)
    public void validateInvalidRouteDuplicates() {
        validateOutputs(new SolutionObject(new int[] { 1, 2, 0, 3 },
                new int[1], 0), new int[4][4], new int[4], new int[4], new int[][] {}, new int[4]);
    }

    /**
     * The last location visited must always be the depot (n-1).
     */
    @Test(expected = IllegalArgumentException.class)
    public void validateInvalidRouteDepot() {
        validateOutputs(new SolutionObject(new int[] { 0, 1, 3, 2 },
                new int[1], 0), new int[4][4], new int[4], new int[4], new int[][] {}, new int[4]);
    }

    /**
     * All locations must be visited, can not visit non-existing locations.
     */
    @Test(expected = IllegalArgumentException.class)
    public void validateInvalidRouteNonExisting() {
        validateOutputs(new SolutionObject(new int[] { 0, 1, 9, 3 },
                new int[1], 0), new int[4][4], new int[4], new int[4], new int[][] {}, new int[4]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateInvalidArrivalTimesLength() {
        validateOutputs(new SolutionObject(new int[] { 0, 1, 2, 3 },
                new int[1], 0), new int[4][4], new int[4], new int[4], new int[][] {}, new int[4]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateInvalidArrivalTimesFirst() {
        validateOutputs(new SolutionObject(new int[] { 0, 1, 2, 3 }, new int[] {
                1, -1, 0, 1 }, 0), new int[4][4], new int[4], new int[4], new int[][] {}, new int[4]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateInvalidArrivalTimes1() {
        validateOutputs(new SolutionObject(new int[] { 0, 1, 2, 3 }, new int[] {
                0, -1, 0, 1 }, 0), new int[4][4], new int[4], new int[4], new int[][] {}, new int[4]);
    }

    final int[][] travelTimes = new int[][] {
    /* */new int[] { 0, 10, 999, 999 },
    /* */new int[] { 999, 0, 3, 999 },
    /* */new int[] { 999, 999, 0, 7 },
    /* */new int[] { 999, 999, 999, 0 } };

    @Test(expected = IllegalArgumentException.class)
    public void validateInvalidArrivalTimes2() {
        validateOutputs(new SolutionObject(new int[] { 0, 1, 2, 3 }, new int[] {
                0, 0, 0, 1 }, 0), travelTimes, new int[4], new int[4], new int[][] {}, new int[4]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateInvalidArrivalTimes3() {
        validateOutputs(new SolutionObject(new int[] { 0, 1, 2, 3 }, new int[] {
                0, 10, 0, 1 }, 0), travelTimes, new int[4], new int[4], new int[][] {}, new int[4]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateInvalidObjective() {
        validateOutputs(new SolutionObject(new int[] { 0, 1, 2, 3 }, new int[] {
                0, 10, 100, 108 }, 0), travelTimes, new int[4], new int[4], new int[][] {}, new int[4]);
    }

    /*
     * VALIDATE MULTI VEHICLE
     */

    @Test(expected = IllegalArgumentException.class)
    public void validateMissingLocation2Vehicles() {
        final SolutionObject sol1 = new SolutionObject(new int[] { 0, 2, 3 },
                new int[] { 0, 10, 100, 108 }, 238);
        final SolutionObject sol2 = new SolutionObject(new int[] { 0, 2, 3 },
                new int[] { 0, 10, 100, 108 }, 238);
        validateOutputs(new SolutionObject[] { sol1, sol2 }, travelTimes, new int[4], new int[4], new int[][] {}, new int[4], new int[2][4], new int[2][2], new int[2]);
    }

    @Test
    public void validateMultiVehicleCorrect() {
        final int[][] travelTime = new int[][] {
        /* */new int[] { 999, 999, 999, 999 },
        /* */new int[] { 999, 0, 999, 3 },
        /* */new int[] { 999, 999, 0, 7 },
        /* */new int[] { 999, 999, 999, 0 } };

        final int[] releaseDates = { 0, 7, 8, 0 };
        final int[] dueDates = { 0, 9, 100, 40 };

        final int[][] vehicleTravelTime = new int[][] {
        /* */new int[] { 999, 9, 999, 999 },
        /* */new int[] { 999, 999, 2, 999 } };

        // travel time for this route: 9 + 3
        // tardiness: 1 + 60
        final SolutionObject sol1 = new SolutionObject(new int[] { 0, 1, 3 },
                new int[] { 0, 10, 100 }, 73);
        // travel time for this route: 2 + 7
        // tardiness: 0 + 50
        final SolutionObject sol2 = new SolutionObject(new int[] { 0, 2, 3 },
                new int[] { 0, 15, 90 }, 59);

        validateOutputs(new SolutionObject[] { sol1, sol2 }, travelTime, releaseDates, dueDates, new int[][] {}, new int[4], vehicleTravelTime, new int[2][2], new int[2]);
    }

    /**
     * In this test the vehicles attempt to deliver parcels which are not in
     * their own inventory.
     */
    @Test(expected = IllegalArgumentException.class)
    public void validateRouteWithoutInventoryLocations() {
        final SolutionObject sol1 = new SolutionObject(new int[] { 0, 1, 3 },
                new int[] { 0, 10, 100 }, 73);
        final SolutionObject sol2 = new SolutionObject(new int[] { 0, 2, 3 },
                new int[] { 0, 15, 90 }, 59);
        // vehicle 0 has location 2
        // vehicle 1 has location 1
        final int[][] inventories = new int[][] { { 0, 2 }, { 1, 1 } };

        validateOutputs(new SolutionObject[] { sol1, sol2 }, new int[4][4], new int[4], new int[4], new int[][] {}, new int[4], new int[2][4], inventories, new int[2]);
    }

    /**
     * The first arrival time should reflect the remainingServiceTime for each
     * vehicle.
     */
    @Test(expected = IllegalArgumentException.class)
    public void validateRemainingServiceTime() {
        final SolutionObject sol1 = new SolutionObject(new int[] { 0, 1, 3 },
                new int[] { 0, 10, 100 }, 73);
        final SolutionObject sol2 = new SolutionObject(new int[] { 0, 2, 3 },
                new int[] { 0, 15, 90 }, 59);
        final int[] remainingServiceTimes = new int[] { 3, 700 };
        validateOutputs(new SolutionObject[] { sol1, sol2 }, new int[4][4], new int[4], new int[4], new int[][] {}, new int[4], new int[2][4], new int[0][0], remainingServiceTimes);
    }

    @Test
    public void validateCorrect() {
        validateOutputs(new SolutionObject(new int[] { 0, 1, 2, 3 }, new int[] {
                0, 10, 100, 108 }, 238), travelTimes, new int[4], new int[4], new int[][] {}, new int[4])
                .toString();
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateBig() {
        final int[][] travelTime = new int[][] {
                { 0, 132, 233, 491, 644, 513, 284, 447, 435, 255, 140, 246 },
                { 132, 0, 168, 380, 548, 508, 228, 399, 394, 301, 161, 163 },
                { 233, 168, 0, 276, 414, 351, 61, 232, 227, 216, 127, 35 },
                { 491, 380, 276, 0, 185, 439, 256, 263, 282, 463, 402, 250 },
                { 644, 548, 414, 185, 0, 427, 373, 283, 307, 555, 534, 399 },
                { 513, 508, 351, 439, 427, 0, 297, 177, 160, 280, 376, 373 },
                { 284, 228, 61, 256, 373, 297, 0, 172, 167, 210, 162, 77 },
                { 447, 399, 232, 263, 283, 177, 172, 0, 25, 286, 312, 242 },
                { 435, 394, 227, 282, 307, 160, 167, 25, 0, 265, 298, 240 },
                { 255, 301, 216, 463, 555, 280, 210, 286, 265, 0, 141, 250 },
                { 140, 161, 127, 402, 534, 376, 162, 312, 298, 141, 0, 155 },
                { 246, 163, 35, 250, 399, 373, 77, 242, 240, 250, 155, 0 } };
        final int[] releaseDates = { 0, 1245143, 1755843, 63643, 1330543,
                711843, 1811843, 212643, 1453443, 0, 188143, 0 };
        final int[] dueDates = { 0, 2139143, 2606143, 1758143, 2242143,
                1968143, 2564143, 1395843, 2401143, 1843643, 2030043, 2940143 };
        final int[][] servicePairs = { { 1, 2 }, { 3, 4 }, { 5, 6 }, { 7, 8 } };
        final int[] serviceTimes = MipTest
                .createServiceTimes(300, releaseDates.length);

        // Route: [0, 1, 10, 9, 5, 8, 7, 4, 3, 6, 2, 11]
        // Arrival times: [0, 132, 4384, 3467, 2982, 1614, 4023, 2399, 2074,
        // 1034, 593, 4719]
        // Objective: 1719

        final SolutionObject sol = new SolutionObject(//
                new int[] { 0, 1, 10, 9, 5, 8, 7, 4, 3, 6, 2, 11 }, //
                new int[] { 0, 132, 4384, 3467, 2982, 1614, 4023, 2399, 2074,
                        1034, 593, 4719 }, //
                1719);

        validateOutputs(sol, travelTime, releaseDates, dueDates, servicePairs, serviceTimes);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateInvalidArrivalOrder() {
        validateOutputs(new SolutionObject(//
                new int[] { 0, 2, 1, 3 }, // route
                new int[] { 0, 1998, 999, 2997 }, // arrival times
                238), // obj. value
                travelTimes, new int[4], new int[4], new int[][] { { 1, 2 } }, new int[4]);

    }

    @Test
    public void testWrap() {

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
        final Solver solver = wrap(new FakeSolver(routes));
        solver.solve(state);
    }

    @Test
    public void testWrapSingle() {
        final SingleVehicleArraysSolver s = wrap(new FakeSingleSolver(
                new SolutionObject(new int[] { 0, 1, 2, 3 }, new int[] { 0, 10,
                        100, 108 }, 238)));
        s.solve(travelTimes, new int[4], new int[4], new int[][] {}, new int[4]);
    }

    public static <T> void testPrivateConstructor(Class<T> clazz) {
        try {
            final Constructor<T> c = clazz.getDeclaredConstructor();
            c.setAccessible(true);
            c.newInstance();
        } catch (final Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testWrapMulti() {
        testPrivateConstructor(SolverValidator.class);

        final MultiVehicleArraysSolver s = wrap(new FakeMultiSolver(
                new SolutionObject[] { new SolutionObject(new int[] { 0, 1, 2,
                        3 }, new int[] { 0, 10, 100, 108 }, 228) }));

        s.solve(travelTimes, new int[4], new int[4], new int[][] {}, new int[4], new int[2][4], new int[][] {
                { 0, 1 }, { 0, 2 } }, new int[2]);
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

    class FakeSingleSolver implements SingleVehicleArraysSolver {
        SolutionObject answer;

        FakeSingleSolver(SolutionObject answer) {
            this.answer = answer;
        }

        public SolutionObject solve(int[][] travelTime, int[] releaseDates,
                int[] dueDates, int[][] servicePairs, int serviceTimes[]) {
            return answer;
        }
    }

    class FakeMultiSolver implements MultiVehicleArraysSolver {
        SolutionObject[] answer;

        FakeMultiSolver(SolutionObject[] answer) {
            this.answer = answer;
        }

        public SolutionObject[] solve(int[][] travelTime, int[] releaseDates,
                int[] dueDates, int[][] servicePairs, int[] serviceTimes,
                int[][] vehicleTravelTimes, int[][] inventories,
                int[] remainingServiceTimes) {
            return answer;
        }
    }
}
