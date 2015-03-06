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

import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.BiMap;
import com.google.common.collect.ForwardingMap;

/**
 * @author Rinde van Lon
 *
 */
public abstract class ForwardingBiMap<K, V> extends ForwardingMap<K, V>
    implements
    BiMap<K, V> {

  @Override
  public Set<V> values() {
    return delegate().values();
  }

  @Override
  public V forcePut(@Nullable K key, @Nullable V value) {
    return delegate().forcePut(key, value);
  }

  @Override
  public BiMap<V, K> inverse() {
    return delegate().inverse();
  }

  @Override
  protected abstract BiMap<K, V> delegate();
}
