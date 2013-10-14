package rinde.sim.util;

public interface SupplierRng<T> {

  T get(long seed);
}
