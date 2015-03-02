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
package com.github.rinde.rinsim.geom;

import com.google.auto.value.AutoValue;
import com.google.common.base.Optional;

/**
 * Immutable value object representing a directed connection (link/edge) in a
 * graph.
 * @param <E> Type of {@link ConnectionData} that is used. This data object can
 *          be used to add additional information to the connection.
 * @since 2.0
 * @author Bartosz Michalik
 * @author Rinde van Lon
 */
@AutoValue
public abstract class Connection<E extends ConnectionData> {

  Connection() {}

  /**
   * @return The starting point of the connection.
   */
  public abstract Point from();

  /**
   * @return The end point of the connection.
   */
  public abstract Point to();

  /**
   * @return The data associated to this connection wrapped in an
   *         {@link Optional}.
   */
  public abstract Optional<E> data();

  /**
   * @return The length of this connection as specified by the data or
   *         alternatively by the euclidean distance between the two points.
   */
  public double getLength() {
    if (data().isPresent() && data().get().getLength().isPresent()) {
      return data().get().getLength().get();
    }
    return Point.distance(from(), to());
  }

  /**
   * Create a new connection without any connection data associated to it.
   * @param from The starting point of the connection.
   * @param to The end point of the connection.
   * @param <E> The type of {@link ConnectionData}.
   * @return A new {@link Connection} instance.
   */
  public static <E extends ConnectionData> Connection<E> create(Point from,
      Point to) {
    return create(from, to, Optional.<E> absent());
  }

  /**
   * Create a new connection.
   * @param from The starting point of the connection.
   * @param to The end point of the connection.
   * @param data The data associated to the connection.
   * @param <E> The type of {@link ConnectionData}.
   * @return A new {@link Connection} instance.
   */
  public static <E extends ConnectionData> Connection<E> create(Point from,
      Point to, E data) {
    return create(from, to, Optional.of(data));
  }

  /**
   * Create a new connection.
   * @param from The starting point of the connection.
   * @param to The end point of the connection.
   * @param data The data associated to the connection.
   * @param <E> The type of {@link ConnectionData}.
   * @return A new {@link Connection} instance.
   */
  public static <E extends ConnectionData> Connection<E> create(Point from,
      Point to, Optional<E> data) {
    return new AutoValue_Connection<>(from, to, data);
  }
}
