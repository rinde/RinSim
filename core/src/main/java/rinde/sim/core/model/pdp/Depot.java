/**
 * 
 */
package rinde.sim.core.model.pdp;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public abstract class Depot implements PackageContainer {

    @Override
    public final PDPType getType() {
        return PDPType.DEPOT;
    }
}
