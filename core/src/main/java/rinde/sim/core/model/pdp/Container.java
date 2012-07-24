/**
 * 
 */
package rinde.sim.core.model.pdp;

/**
 * Implementors of this interface can contain 'things', typically {@link Parcel}
 * objects.
 * 
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public interface Container extends PDPObject {

    /**
     * The returned value is treated as a constant (i.e. it is read only once).
     * @return The maximum capacity of the container.
     */
    double getCapacity();
}
