/*
 * Copyright (C) 2011-2014 Rinde van Lon, iMinds DistriNet, KU Leuven
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
package com.github.rinde.rinsim.geom;

import javax.annotation.Nullable;

/**
 * Simple implementation of {@link ConnectionData}, allowing to specify the
 * length of a connection.
 * @author Bartosz Michalik 
 * @author Rinde van Lon 
 */
public class LengthData implements ConnectionData {

  /**
   * Represents an empty value for usage in a {@link TableGraph}.
   */
  public static final LengthData EMPTY = new LengthData(Double.NaN);

  private final double length;

  /**
   * Instantiate a new instance using the specified length.
   * @param pLength The length of the connection.
   */
  public LengthData(double pLength) {
    length = pLength;
  }

  @Override
  public double getLength() {
    return length;
  }

  @Override
  public int hashCode() {
    return Double.valueOf(length).hashCode();
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (obj instanceof LengthData) {
      return Double.compare(length, ((LengthData) obj).length) == 0;
    }
    return false;
  }

  @Override
  public String toString() {
    return Double.toString(length);
  }

}
