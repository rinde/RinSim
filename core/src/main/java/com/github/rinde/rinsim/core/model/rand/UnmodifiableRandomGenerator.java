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
package com.github.rinde.rinsim.core.model.rand;

import javax.annotation.Nullable;

import org.apache.commons.math3.random.RandomGenerator;

/**
 * @author Rinde van Lon
 */
class UnmodifiableRandomGenerator implements RandomGenerator {

  private final RandomGenerator delegateRng;

  UnmodifiableRandomGenerator(RandomGenerator delegate) {
    delegateRng = delegate;
  }

  @Deprecated
  @Override
  public void setSeed(int seed) {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  @Override
  public void setSeed(@Nullable int[] seed) {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  @Override
  public void setSeed(long seed) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void nextBytes(@Nullable byte[] bytes) {
    delegateRng.nextBytes(bytes);
  }

  @Override
  public int nextInt() {
    return delegateRng.nextInt();
  }

  @Override
  public int nextInt(int n) {
    return delegateRng.nextInt(n);
  }

  @Override
  public long nextLong() {
    return delegateRng.nextLong();
  }

  @Override
  public boolean nextBoolean() {
    return delegateRng.nextBoolean();
  }

  @Override
  public float nextFloat() {
    return delegateRng.nextFloat();
  }

  @Override
  public double nextDouble() {
    return delegateRng.nextDouble();
  }

  @Override
  public double nextGaussian() {
    return delegateRng.nextGaussian();
  }
}
