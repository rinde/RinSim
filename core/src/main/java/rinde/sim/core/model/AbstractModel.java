package rinde.sim.core.model;

/**
 * Basic implementation that provides a getSupportedType method implementation.
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * 
 * @param <T> The type that is supported by this model.
 */
public abstract class AbstractModel<T> implements Model<T> {

	private final Class<T> clazz;

	/**
	 * Create a new model.
	 * @param pClazz The class that represents the supported type of this model.
	 */
	protected AbstractModel(Class<T> pClazz) {
		this.clazz = pClazz;
	}

	@Override
	final public Class<T> getSupportedType() {
		return clazz;
	}

}
