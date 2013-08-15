/**
 * 
 */
package rinde.sim.pdptw.central;

import rinde.sim.pdptw.central.arrays.ArraysSolverValidator;
import rinde.sim.pdptw.common.ParcelDTO;

import com.google.common.collect.ImmutableList;

/**
 * Interface for solvers of the pickup-and-delivery problem with time windows
 * (PDPTW).
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public interface Solver {

    /**
     * Computes a solution for the PDPTW as specified by the
     * {@link GlobalStateObject}. The returned solution does not necessarily
     * need to be optimal but it needs to be feasible. The
     * {@link ArraysSolverValidator} can check whether a {@link Solver} produces a
     * valid solution and it can check whether the input parameters of the
     * {@link Solver} are valid.
     * @param state The state of the world, or problem instance.
     * @return A list of routes, one for every vehicle in the
     *         {@link GlobalStateObject}.
     */
    ImmutableList<ImmutableList<ParcelDTO>> solve(GlobalStateObject state);
}
