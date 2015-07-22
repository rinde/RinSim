/*
 * Copyright (C) 2011-2015 Rinde van Lon, iMinds-DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rinde.rinsim.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.BiMap;
import com.google.common.collect.ForwardingSet;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;

/**
 * {@link BiMap} with key based insertion ordering. Note that all its collection
 * views are unmodifiable.
 *
 * @author Rinde van Lon
 * @param <K> Key type.
 * @param <V> Value type.
 */
public class LinkedHashBiMap<K, V> extends ForwardingBiMap<K, V> {

  final BiMap<K, V> delegateBiMap;
  final Set<K> set;
  @Nullable
  private BiMap<V, K> inverse;

  LinkedHashBiMap() {
    delegateBiMap = HashBiMap.create();
    set = new LinkedHashSet<>();
  }

  // views are unmodifiable

  @Override
  public Set<V> values() {
    return new OrderedValuesSet<>(set, delegateBiMap);
  }

  @Override
  public Set<K> keySet() {
    return Collections.unmodifiableSet(set);
  }

  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    return new EntrySet<>(set, delegateBiMap);
  }

  // modifications

  @Override
  public V put(@Nullable K key, @Nullable V value) {
    final V v = delegate().put(key, value);
    set.add(key);
    return v;
  }

  @Override
  public V forcePut(@Nullable K key, @Nullable V value) {
    final V v = delegate().forcePut(key, value);
    set.add(key);
    return v;
  }

  @Override
  public void putAll(@Nullable Map<? extends K, ? extends V> map) {
    if (map == null) {
      return;
    }
    for (final Entry<? extends K, ? extends V> entry : map.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public V remove(@Nullable Object key) {
    final V val = delegate().remove(key);
    set.remove(key);
    return val;
  }

  @Override
  public void clear() {
    delegate().clear();
    set.clear();
  }

  @Override
  public BiMap<V, K> inverse() {
    final BiMap<V, K> inv = inverse;
    return inv == null ? inverse = new Inverse() : inv;
  }

  @Override
  protected BiMap<K, V> delegate() {
    return delegateBiMap;
  }

  public static <K, V> LinkedHashBiMap<K, V> create() {
    return new LinkedHashBiMap<>();
  }

  private final class Inverse extends ForwardingBiMap<V, K> {

    Inverse() {}

    BiMap<K, V> forward() {
      return LinkedHashBiMap.this;
    }

    @Override
    public BiMap<K, V> inverse() {
      return forward();
    }

    // views are unmodifiable

    @Override
    public Set<K> values() {
      return forward().keySet();
    }

    @Override
    public Set<V> keySet() {
      return forward().values();
    }

    @Override
    @Deprecated
    public Set<Map.Entry<V, K>> entrySet() {
      throw new UnsupportedOperationException(
          "Use inverse().entrySet() instead.");
    }

    // modifications

    @Override
    public K put(@Nullable V key, @Nullable K value) {
      final K val = get(key);
      forward().put(value, key);
      return val;
    }

    @Override
    public K forcePut(@Nullable V key, @Nullable K value) {
      final K val = get(key);
      forward().forcePut(value, key);
      return val;
    }

    @Override
    public void putAll(@Nullable Map<? extends V, ? extends K> map) {
      if (map == null) {
        return;
      }
      for (final Entry<? extends V, ? extends K> entry : map.entrySet()) {
        put(entry.getKey(), entry.getValue());
      }
    }

    @Override
    protected BiMap<V, K> delegate() {
      return delegateBiMap.inverse();
    }
  }

  private static final class OrderedValuesSet<K, V> extends UnmodifiableSet<V> {
    final Set<K> ordering;
    final BiMap<K, V> delegateMap;

    OrderedValuesSet(Set<K> order, BiMap<K, V> delegate) {
      ordering = order;
      delegateMap = delegate;
    }

    @Override
    public Iterator<V> iterator() {
      final Iterator<K> it = ordering.iterator();
      return new AbstractIterator<V>() {
        @Override
        protected V computeNext() {
          if (it.hasNext()) {
            return delegateMap.get(it.next());
          }
          return super.endOfData();
        }
      };
    }

    @Override
    public Object[] toArray() {
      return Iterators.toArray(iterator(), Object.class);
    }

    @Override
    public <T> T[] toArray(@Nullable T[] a) {
      final List<V> list = new ArrayList<>(size());
      Iterators.addAll(list, iterator());
      return list.toArray(a);
    }

    @Override
    protected Set<V> delegate() {
      return delegateMap.values();
    }

  }

  private static final class EntrySet<A, B> extends
      UnmodifiableSet<Map.Entry<A, B>> {

    final Map<A, B> delegateSet;
    private final Set<A> ordering;

    EntrySet(Set<A> order, Map<A, B> map) {
      ordering = order;
      delegateSet = map;
    }

    @Override
    protected Set<Entry<A, B>> delegate() {
      return delegateSet.entrySet();
    }

    @Override
    public Iterator<Map.Entry<A, B>> iterator() {
      final Iterator<A> it = ordering.iterator();

      return new AbstractIterator<Map.Entry<A, B>>() {

        @Override
        protected Map.Entry<A, B> computeNext() {
          if (it.hasNext()) {
            final A key = it.next();
            return Maps.immutableEntry(key, delegateSet.get(key));
          }
          return super.endOfData();
        }
      };

    }

    @Override
    public Object[] toArray() {
      return Iterators.toArray(iterator(), Entry.class);
    }

    @Override
    public <T> T[] toArray(@Nullable T[] a) {
      final List<Map.Entry<A, B>> list = new ArrayList<>(size());
      Iterators.addAll(list, iterator());
      return list.toArray(a);
    }
  }

  private abstract static class UnmodifiableSet<T> extends ForwardingSet<T> {

    UnmodifiableSet() {}

    @Override
    @Deprecated
    public boolean add(@Nullable T e) {
      throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public boolean remove(@Nullable Object o) {
      throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public boolean addAll(@Nullable Collection<? extends T> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public boolean retainAll(@Nullable Collection<?> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public boolean removeAll(@Nullable Collection<?> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public void clear() {
      throw new UnsupportedOperationException();
    }
  }
}
