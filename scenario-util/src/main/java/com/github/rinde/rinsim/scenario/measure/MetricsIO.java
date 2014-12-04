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
package com.github.rinde.rinsim.scenario.measure;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.github.rinde.rinsim.geom.Point;
import com.google.common.annotations.Beta;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.io.Files;

/**
 * Provides some methods to write some common properties of scenarios to a file.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
@Beta
public final class MetricsIO {

  private MetricsIO() {}

  /**
   * Writes the specified list of {@link Point}s to the specified file. Each
   * point is printed on a separate line using the following format:
   * 
   * <pre>
   * {@code x y }
   * </pre>
   * 
   * i.e. the coordinates are space-separated.
   * @param locations The locations to write to file.
   * @param f The file to write to, non-existing parent directories will be
   *          created.
   */
  public static void writeLocationList(List<Point> locations, File f) {
    final StringBuilder sb = new StringBuilder();
    for (final Point p : locations) {
      sb.append(p.x).append(" ").append(p.y).append("\n");
    }
    try {
      Files.createParentDirs(f);
      Files.write(sb.toString(), f, Charsets.UTF_8);
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Writes the specified list of numbers to a file. The list is interpreted as
   * the y-values of a graph, the indices of the list are used as the x-values
   * of the graph. A <code>0</code> is always appended. For example, if the
   * provided list is <code>[10, 20, 30, 40]</code>, the file will contain:
   * 
   * <pre>
   * {@code 0 10}
   * {@code 1 20}
   * {@code 2 30}
   * {@code 3 40}
   * {@code 4 0}
   * </pre>
   * @param list The list of numbers to write to a file.
   * @param file The file to write to.
   */
  public static void writeLoads(List<? extends Number> list, File file) {
    final StringBuilder sb = new StringBuilder();
    int i = 0;
    for (; i < list.size(); i++) {
      sb.append(i).append(" ").append(list.get(i)).append("\n");
    }
    sb.append(i).append(" ").append(0).append("\n");
    try {
      Files.createParentDirs(file);
      Files.write(sb.toString(), file, Charsets.UTF_8);
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Writes the provided list of times to a file. The output format is as
   * follows:
   * 
   * <pre>
   * {@code length}
   * {@code time 1}
   * {@code time 2}
   * ..
   * {@code time n}
   * </pre>
   * @param length The length of the scenario: [0,length)
   * @param times The arrival times of events, for each time it holds that 0
   *          &#8804; time &lt; length.
   * @param f The file to write to.
   */
  public static void writeTimes(double length, List<? extends Number> times,
      File f) {
    try {
      Files.createParentDirs(f);
      Files.write(
          new StringBuilder().append(length).append("\n")
              .append(Joiner.on("\n").join(times)).append("\n"), f,
          Charsets.UTF_8);
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
