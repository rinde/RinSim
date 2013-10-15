package rinde.sim.util;

import com.google.common.reflect.TypeToken;

public interface SupplierRng<T> {

  T get(long seed);

  public static abstract class DefaultSupplierRng<T> implements SupplierRng<T> {

    @Override
    public String toString() {
      return new TypeToken<T>(getClass()) {}.getRawType().getSimpleName();
    }

  }
}
