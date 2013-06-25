/**
 * 
 */
package rinde.sim.core.model;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public interface ModelProvider {

    <T extends Model> T getModel(Class<T> clazz);

}
