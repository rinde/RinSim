package rinde.sim.core.model;

/**
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 *
 * @param <T> basic type of element supported by model
 */
public interface Model<T> {
	
	/**
	 * Register element in a model 
	 * @param element the <code>! null</code> should be imposed 
	 * @return true if the object was successfully registered
	 */
	boolean register(T element);
	
	Class<T> getSupportedType();
}
