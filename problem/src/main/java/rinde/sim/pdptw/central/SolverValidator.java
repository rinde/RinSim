/**
 * 
 */
package rinde.sim.pdptw.central;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Sets.newHashSet;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import rinde.sim.pdptw.central.GlobalStateObject.VehicleState;
import rinde.sim.pdptw.common.ParcelDTO;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * Provides methods for validating input to and output from {@link Solver}s.
 * Also provides {@link #wrap(Solver)} method which decorates any solver such
 * that both inputs and outputs are validated every time it is is called.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public final class SolverValidator {

    private SolverValidator() {}

    /**
     * Decorates the original {@link Solver} such that both the inputs to the
     * solver and the outputs from the solver are validated. When an invalid
     * input or output is detected a {@link IllegalArgumentException is thrown}.
     * @param delegate The {@link Solver} that will be used for the actual
     *            solving.
     * @return The wrapped solver.
     */
    public static Solver wrap(Solver delegate) {
        return new SolverValidator.Validator(delegate);
    }

    /**
     * Validates the inputs for {@link Solver#solve(GlobalStateObject)} method.
     * If the input is not correct an {@link IllegalArgumentException} is
     * thrown.
     * @param state The state to validate.
     * @return The state.
     */
    public static GlobalStateObject validateInputs(GlobalStateObject state) {
        checkArgument(state.time >= 0, "Time must be >= 0, is %s.", state.time);
        final Set<ParcelDTO> inventoryParcels = newHashSet();
        for (final VehicleState vs : state.vehicles) {
            checkArgument(vs.remainingServiceTime >= 0,
                "Remaining service time must be >= 0, is %s.",
                vs.remainingServiceTime);
            checkArgument(vs.speed > 0, "Speed must be positive, is %s.",
                vs.speed);
            final Set<ParcelDTO> intersection =
                    Sets.intersection(state.availableParcels, vs.contents);
            checkArgument(
                intersection.isEmpty(),
                "Parcels can not be available AND in the inventory of a vehicle, found: %s.",
                intersection);
            final Set<ParcelDTO> inventoryIntersection =
                    Sets.intersection(inventoryParcels, vs.contents);
            checkArgument(
                inventoryIntersection.isEmpty(),
                "Parcels can not be in the inventory of two vehicles at the same time, found: %s.",
                inventoryIntersection);
            inventoryParcels.addAll(vs.contents);

            if (vs.destination != null) {
                // if the destination parcel is not available, it must be in the
                // cargo of the vehicle
                if (!state.availableParcels.contains(vs.destination)) {
                    checkArgument(
                        vs.contents.contains(vs.destination),
                        "The current destination is not available therefore it must be in the contents of the vehicle.");
                }
            }
        }
        return state;
    }

    /**
     * Validates the routes that are produced by a {@link Solver}. If one of the
     * routes is infeasible an {@link IllegalArgumentException} is thrown.
     * @param routes The routes that are validated.
     * @param state Parameter as specified by
     *            {@link Solver#solve(GlobalStateObject)}.
     * @return The routes.
     */
    public static ImmutableList<ImmutableList<ParcelDTO>> validateOutputs(
            ImmutableList<ImmutableList<ParcelDTO>> routes,
            GlobalStateObject state) {

        checkArgument(
            routes.size() == state.vehicles.size(),
            "There must be exactly one route for every vehicle, found %s routes with %s vehicles.",
            routes.size(), state.vehicles.size());

        final Set<ParcelDTO> inputParcels = newHashSet(state.availableParcels);

        final Set<ParcelDTO> outputParcels = newHashSet();
        for (int i = 0; i < routes.size(); i++) {
            final List<ParcelDTO> route = routes.get(i);
            final Set<ParcelDTO> routeSet = ImmutableSet.copyOf(route);
            checkArgument(
                routeSet.containsAll(state.vehicles.get(i).contents),
                "The route of vehicle %s doesn't visit all parcels in its cargo.",
                i);
            inputParcels.addAll(state.vehicles.get(i).contents);

            if (state.vehicles.get(i).destination != null) {
                checkArgument(
                    route.get(0) == state.vehicles.get(i).destination,
                    "The route of vehicle %s should start with its current destination: %s.",
                    i, state.vehicles.get(i).destination);
            }

            for (final ParcelDTO p : route) {
                checkArgument(!outputParcels.contains(p),
                    "Found a parcel which is already in another route: %s.", p);
                final int frequency = Collections.frequency(route, p);
                if (state.availableParcels.contains(p)) {
                    // if the parcel is available, it needs to occur twice in
                    // the route (once for pickup, once for delivery).
                    checkArgument(
                        frequency == 2,
                        "A parcel that is picked up needs to be delivered as well, so it should occur twice in the route, found %s occurence(s) of parcel %s.",
                        frequency, p);
                } else {
                    checkArgument(
                        state.vehicles.get(i).contents.contains(p),
                        "The parcel in this route is not available, which means it should be in the contents of this vehicle. Parcel: %s.",
                        p);
                    checkArgument(
                        frequency == 1,
                        "A parcel that is already in cargo should occur once in the route, found %s occurences of parcel %s.",
                        frequency, p);
                }
            }
            outputParcels.addAll(route);
        }
        checkArgument(
            inputParcels.equals(outputParcels),
            "The number of distinct parcels in the routes should equal the number of parcels in the input, parcels that should be added in routes: %s, parcels that should be removed from routes: %s.",
            Sets.difference(inputParcels, outputParcels),
            Sets.difference(outputParcels, inputParcels));
        return routes;
    }

    private static class Validator implements Solver {
        private final Solver delegateSolver;

        private Validator(Solver delegate) {
            delegateSolver = delegate;
        }

        public ImmutableList<ImmutableList<ParcelDTO>> solve(
                GlobalStateObject state) {
            return validateOutputs(delegateSolver.solve(validateInputs(state)),
                state);
        }
    }

}
