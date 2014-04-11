package rinde.sim.util;

import rinde.sim.util.SupplierRng.DefaultSupplierRng;

public final class SupplierRngs {

  private SupplierRngs() {}

  public static <T> SupplierRng<T> constant(T value) {
    return new ConstantSupplierRng<T>(value);
  }

  private static class ConstantSupplierRng<T> extends DefaultSupplierRng<T> {
    private final T value;

    ConstantSupplierRng(T v) {
      value = v;
    }

    @Override
    public T get(long seed) {
      return value;
    }
  }

}
