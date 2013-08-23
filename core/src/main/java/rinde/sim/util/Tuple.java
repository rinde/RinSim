package rinde.sim.util;

import java.util.Map.Entry;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 */
public class Tuple<K, V> implements Entry<K, V> {
  protected K key;
  protected V value;

  public Tuple(K key, V value) {
    this.key = key;
    this.value = value;
  }

  @Override
  public K getKey() {
    return key;
  }

  @Override
  public V getValue() {
    return value;
  }

  @Override
  public V setValue(V value) {
    return this.value = value;
  }

  public static <K, V> Entry<K, V> copy(Entry<K, V> entry) {
    return new Tuple<K, V>(entry.getKey(), entry.getValue());
  }
}
