package rinde.sim.core.model;

/**
 * Basic implementation that have a getSupportedType method implemented
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 *
 * @param <T>
 */
public abstract class AbstractModel<T> implements Model<T> {

	private Class<T> clazz;
	
	protected AbstractModel(Class<T> clazz) {
		this.clazz = clazz;
	}

	@Override
	final public Class<T> getSupportedType() {
		return clazz;
	}

}
