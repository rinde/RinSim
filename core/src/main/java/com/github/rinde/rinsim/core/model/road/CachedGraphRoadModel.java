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

import static com.google.common.base.Preconditions.checkArgument;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;

/**
 * Special {@link GraphRoadModelImpl} that caches all
 * {@link #getShortestPathTo(Point, Point)} invocations. Further, it keeps track
 * of all {@link RoadUser}s and their types, such that
 * {@link #getObjectsOfType(Class)} is now O(1).
 *
 * @author Rinde van Lon
 */
public class CachedGraphRoadModel extends GraphRoadModelImpl {

  // TODO add cache specific unit tests

  private Table<Point, Point, List<Point>> pathTable;
  private final Multimap<Class<?>, RoadUser> classObjectMap;

  CachedGraphRoadModel(Graph<?> g, RoadModelBuilders.CachedGraphRMB b) {
    super(g, b);
    pathTable = HashBasedTable.create();
    classObjectMap = LinkedHashMultimap.create();
  }

  /**
   * Sets the path cache.
   * @param pPathTable The new path cache to use.
   */
  public void setPathCache(Table<Point, Point, List<Point>> pPathTable) {
    pathTable = pPathTable;
  }

  /**
   * @return An unmodifiable view on the cache that is kept in this model.
   */
  public Table<Point, Point, List<Point>> getPathCache() {
    return Tables.unmodifiableTable(pathTable);
  }

  // overrides internal func to add caching
  @Override
  protected List<Point> doGetShortestPathTo(Point from, Point to) {
    if (pathTable.contains(from, to)) {
      return pathTable.get(from, to);
    }
    final List<Point> path = super.doGetShortestPathTo(from, to);
    pathTable.put(from, to, path);
    return path;
  }

  @Override
  public void addObjectAt(RoadUser newObj, Point pos) {
    super.addObjectAt(newObj, pos);
    classObjectMap.put(newObj.getClass(), newObj);
  }

  @Override
  public void addObjectAtSamePosition(RoadUser newObj, RoadUser existingObj) {
    super.addObjectAtSamePosition(newObj, existingObj);
    classObjectMap.put(newObj.getClass(), newObj);
  }

  @Override
  public void clear() {
    super.clear();
    classObjectMap.clear();
  }

  /**
   * O(1) using a direct lookup. {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public <Y extends RoadUser> Set<Y> getObjectsOfType(final Class<Y> type) {
    checkArgument(type != null, "type can not be null");
    final Set<Y> set = new LinkedHashSet<>();
    set.addAll((Set<Y>) classObjectMap.get(type));
    return set;
  }

  @Override
  public void removeObject(RoadUser o) {
    super.removeObject(o);
    classObjectMap.remove(o.getClass(), o);
  }
}
