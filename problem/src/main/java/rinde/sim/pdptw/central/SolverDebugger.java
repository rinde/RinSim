/**
 * 
 */
package rinde.sim.pdptw.central;

import rinde.sim.pdptw.common.ParcelDTO;

import com.google.common.collect.ImmutableList;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class SolverDebugger implements Solver {

    private final Solver delegate;

    private SolverDebugger(Solver delegate) {
        this.delegate = delegate;
    }

    @Override
    public ImmutableList<ImmutableList<ParcelDTO>> solve(GlobalStateObject state) {
        System.out.println(state);
        final ImmutableList<ImmutableList<ParcelDTO>> result =
                delegate.solve(state);
        System.out.println(result);
        return result;
    }

    public static Solver wrap(Solver s) {
        return new SolverDebugger(s);
    }

}
