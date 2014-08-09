/**
 * 
 */
package com.github.rinde.rinsim.core.model.pdp;

/**
 * Implementors of this interface can contain 'things', typically {@link Parcel}
 * objects. This interface is typically not used directly, two often used
 * implementations are {@link Vehicle} and {@link Depot}.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public interface Container extends PDPObject {

  /**
   * The returned value is treated as a constant (i.e. it is read only once).
   * @return The maximum capacity of the container.
   */
  double getCapacity();
}
