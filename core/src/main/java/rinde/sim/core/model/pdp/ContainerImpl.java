/**
 * 
 */
package rinde.sim.core.model.pdp;

import static com.google.common.base.Preconditions.checkState;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public abstract class ContainerImpl extends PDPObjectImpl implements Container {

    private double capacity;

    protected final void setCapacity(double pCapacity) {
        checkState(!isRegistered(), "capacity must be set before object is registered");
        capacity = pCapacity;
    }

    @Override
    public final double getCapacity() {
        return capacity;
    }
}
