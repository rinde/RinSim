package rinde.sim.core.model;

import com.google.common.reflect.TypeToken;

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
   */
  @SuppressWarnings({ "serial", "unchecked" })
  protected AbstractModel() {
    this.clazz = (Class<T>) new TypeToken<T>(getClass()) {}.getRawType();
  }

  @Override
  public final Class<T> getSupportedType() {
    return clazz;
  }

}
