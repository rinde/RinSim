/*
 * Copyright (C) 2011-2018 Rinde R.S. van Lon
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
package com.github.rinde.rinsim.core.model.road;

import com.github.rinde.rinsim.geom.Point;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * A spatial data structure for storing elements in a 2D space.
 * @author Rinde van Lon
 * @param <T> The type of element in this data structure.
 */
public interface SpatialRegistry<T> {

  /**
   * Check whether the specified object is contained in this registry.
   * @param object The object to check.
   * @return <code>true</code> if it is contained by the registry,
   *         <code>false</code> otherwise.
   */
  boolean containsObject(T object);

  /**
   * Removes the specified object from the registry.
   * @param object The object to remove.
   */
  void removeObject(T object);

  /**
   * Removes all objects from the registry.
   */
  void clear();

  /**
   * Looks up the position of the specified object.
   * @param object The object to lookup.
   * @return The position.
   */
  Point getPosition(T object);

  /**
   * Adds the specified object to the registry at the specified position.
   * @param object The object to add.
   * @param position The position.
   */
  void addAt(T object, Point position);

  ImmutableMap<T, Point> getObjectsAndPositions();

  ImmutableSet<T> getObjects();

  ImmutableSet<T> findObjectsWithinRadius(Point position, double radius);

  ImmutableSet<T> findObjectsInRect(Point min, Point max);

  ImmutableSet<T> findNearestObjects(Point position, int n);

}
