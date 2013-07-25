/**
 * 
 */
package rinde.sim.core;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public interface IBuilder<T> {

    T build();

    Class<T> getType();
}
