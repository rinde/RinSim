/**
 * 
 */
package rinde.sim.ui.renderers;

import rinde.sim.core.model.Model;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public interface ModelProvider {

	<T extends Model<?>> T getModel(Class<T> clazz);

}
