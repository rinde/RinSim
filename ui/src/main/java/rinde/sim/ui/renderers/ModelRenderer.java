/**
 * 
 */
package rinde.sim.ui.renderers;

import rinde.sim.core.model.Model;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public interface ModelRenderer<T extends Model<?>> extends Renderer {

	// FIXME documentation

	void register(T model);

	Class<T> getSupportedModelType();
}
