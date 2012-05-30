package rinde.sim.core.model;

/**
 * Basic implementation that have a getSupportedType method implemented
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * 
 * @param <T> The type of the supported type of the model.
 */
public abstract class AbstractModel<T> implements Model<T> {

	private final Class<T> clazz;

	protected AbstractModel(Class<T> pClazz) {
		this.clazz = pClazz;
	}

	@Override
	final public Class<T> getSupportedType() {
		return clazz;
	}

}
