/**
 * 
 */
package rinde.sim.core.model.pdp;

/**
 * Abstract base class for depot concept: a stationary {@link Container}.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public abstract class Depot extends ContainerImpl {

    @Override
    public final PDPType getType() {
        return PDPType.DEPOT;
    }
}
