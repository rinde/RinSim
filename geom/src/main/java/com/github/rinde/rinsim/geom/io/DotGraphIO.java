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
package com.github.rinde.rinsim.geom.io;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.ConnectionData;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.MultiAttributeData;
import com.github.rinde.rinsim.geom.MultiAttributeData.Builder;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.geom.TableGraph;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;

/**
 * Provides input (read) and output (write) operations for {@link Graph}
 * instances in the dot format. Instances can be obtained via any of the
 * following methods:
 * <ul>
 * <li>{@link #getLengthGraphIO()}</li>
 * <li>{@link #getLengthGraphIO(Predicate)}</li>
 * <li>{@link #getMultiAttributeGraphIO()}</li>
 * <li>{@link #getMultiAttributeGraphIO(Predicate)}</li>
 * </ul>
 * @author Bartosz Michalik
 * @author Rinde van Lon
 * @param <E> The type of {@link ConnectionData}.
 */
public class DotGraphIO<E extends ConnectionData> extends
    AbstractGraphIO<E> {

  static final String POS = "p=";
  static final char NODE_PREFIX = 'n';
  static final String DISTANCE = "d";
  static final String MAX_SPEED = "s";
  static final char DATA_START = '[';
  static final char DATA_END = ']';
  static final String CONN_SEPARATOR = "->";
  static final char LIST_ITEM_SEPARATOR = ',';
  static final char KEY_VAL_SEPARATOR = '=';
  static final String QUOTE = "\"";
  static final String SPACE = " ";

  static final Splitter KEY_VAL_SPLITTER = Splitter.on(KEY_VAL_SEPARATOR)
      .trimResults();
  static final Splitter LIST_SPLITTER = Splitter.on(LIST_ITEM_SEPARATOR)
      .trimResults();
  static final Splitter CONN_SPLITTER = Splitter.on(CONN_SEPARATOR)
      .trimResults();
  static final Splitter DATA_SPLITTER = Splitter.on(DATA_START).limit(2);

  private final Predicate<Connection<?>> filter;
  private final ConnectionDataIO<E> dataIO;

  DotGraphIO(ConnectionDataIO<E> connectionSerializer,
      Predicate<Connection<?>> predicate) {
    filter = predicate;
    this.dataIO = connectionSerializer;
  }

  @Override
  public Graph<E> read(Reader r) throws IOException {
    final BufferedReader reader = new BufferedReader(r);
    final Graph<E> graph = new TableGraph<>();
    final Map<String, Point> nodeMapping = new HashMap<>();
    String line;
    while ((line = reader.readLine()) != null) {
      if (line.contains(POS)) {
        final String nodeName = line.substring(0, line.indexOf(DATA_START))
            .trim();
        final String[] position = line.split(QUOTE)[1].split(",");
        final Point p = new Point(Double.parseDouble(position[0]),
            Double.parseDouble(position[1]));
        nodeMapping.put(nodeName, p);
      } else if (line.contains(CONN_SEPARATOR)) {
        // example:
        // node1004 -> node820[label="163.3"]

        final List<String> parts = DATA_SPLITTER.splitToList(line);
        final List<String> names = CONN_SPLITTER.splitToList(parts.get(0));

        final Point from = nodeMapping.get(names.get(0));
        final Point to = nodeMapping.get(names.get(1));

        final Optional<E> data;
        if (parts.size() > 1) {
          checkArgument(
              parts.get(1).charAt(parts.get(1).length() - 1) == DATA_END,
              "Data block of a connection must be closed by a ']'");
          data = dataIO.read(parts.get(1).substring(0,
              parts.get(1).length() - 1));
        } else {
          data = Optional.absent();
        }

        final Connection<E> conn = Connection.create(from, to, data);
        if (filter.apply(conn)) {
          graph.addConnection(conn);
        }
      }
    }
    return graph;
  }

  @Override
  public void write(Graph<E> graph, Writer writer) throws IOException {
    try (final BufferedWriter out = new BufferedWriter(writer)) {
      final StringBuilder string = new StringBuilder();
      string.append("digraph mapgraph {\n");

      int nodeId = 0;
      final Map<Point, Integer> idMap = new HashMap<>();
      for (final Point p : graph.getNodes()) {
        string.append(NODE_PREFIX)
            .append(nodeId)
            .append(DATA_START)
            .append(POS)
            .append(QUOTE)
            .append(p.x)
            .append(LIST_ITEM_SEPARATOR)
            .append(p.y)
            .append(QUOTE)
            .append(DATA_END)
            .append(System.lineSeparator());

        idMap.put(p, nodeId);
        nodeId++;
      }

      for (final Connection<E> entry : graph.getConnections()) {
        string.append(NODE_PREFIX)
            .append(idMap.get(entry.from()))
            .append(SPACE)
            .append(CONN_SEPARATOR)
            .append(SPACE)
            .append(NODE_PREFIX)
            .append(idMap.get(entry.to()));
        if (entry.data().isPresent()) {
          dataIO.write(string, entry.data().get());
        }
        string.append(System.lineSeparator());
      }
      string.append('}');
      out.append(string);
    }
  }

  /**
   * Get instance of a {@link DotGraphIO} for {@link Graph} instances
   * {@link Connection}s with {@link LengthData}.
   * @return A new instance.
   */
  public static DotGraphIO<LengthData> getLengthGraphIO() {
    return new DotGraphIO<>(LengthDataIO.INSTANCE, Filters.noFilter());
  }

  /**
   * Get instance of a {@link DotGraphIO} for {@link Graph} instances
   * {@link Connection}s with {@link LengthData}.
   * @param filter A filter that specifies which {@link Connection} should not
   *          be ignored when reading and writing graphs. See {@link Filters}
   *          for some common implementations.
   * @return A new instance.
   */
  public static DotGraphIO<LengthData> getLengthGraphIO(
      Predicate<Connection<?>> filter) {
    return new DotGraphIO<>(LengthDataIO.INSTANCE, filter);
  }

  /**
   * Get instance of a {@link DotGraphIO} for {@link Graph} instances with
   * {@link Connection}s with {@link MultiAttributeData}.
   * @return A new instance.
   */
  public static DotGraphIO<MultiAttributeData> getMultiAttributeGraphIO() {
    return new DotGraphIO<>(MultiAttributeDataIO.INSTANCE, Filters.noFilter());
  }

  /**
   * Get instance of a {@link DotGraphIO} for {@link Graph} instances
   * {@link Connection}s with {@link MultiAttributeData}.
   * @param filter A filter that specifies which {@link Connection} should not
   *          be ignored when reading and writing graphs. See {@link Filters}
   *          for some common implementations.
   * @return A new instance.
   */
  public static DotGraphIO<MultiAttributeData> getMultiAttributeGraphIO(
      Predicate<Connection<?>> filter) {
    return new DotGraphIO<>(MultiAttributeDataIO.INSTANCE, filter);
  }

  static Map<String, String> parseDataAsMap(String line) {
    final List<String> parts = LIST_SPLITTER.trimResults().splitToList(line);
    final Map<String, String> map = new LinkedHashMap<>();
    for (final String part : parts) {
      final List<String> keyVal = KEY_VAL_SPLITTER.splitToList(part);
      final String key = keyVal.get(0).replaceAll(QUOTE, "");
      checkArgument(!map.containsKey(key),
          "Found a duplicate key in data '%s'.", line);
      final String val = keyVal.get(1).replaceAll(QUOTE, "");
      map.put(key, val);
    }
    return map;
  }

  interface ConnectionDataIO<E extends ConnectionData> {
    void write(StringBuilder sb, E data);

    Optional<E> read(String data);
  }

  enum LengthDataIO implements ConnectionDataIO<LengthData> {
    INSTANCE {
      @Override
      public void write(StringBuilder sb, LengthData data) {
        if (data.getLength().isPresent()) {
          sb.append(DATA_START)
              .append(DISTANCE)
              .append(KEY_VAL_SEPARATOR)
              .append(data.getLength().get())
              .append(DATA_END);
        }
      }

      @Override
      public Optional<LengthData> read(String data) {
        final Map<String, String> map = parseDataAsMap(data);
        if (map.containsKey(DISTANCE)) {
          final double len = Double.parseDouble(map.get(DISTANCE)
              .replaceAll(QUOTE, ""));
          return Optional.of(LengthData.create(len));
        }
        return Optional.absent();
      }
    }
  }

  enum MultiAttributeDataIO implements ConnectionDataIO<MultiAttributeData> {
    INSTANCE {
      @Override
      public void write(StringBuilder sb, MultiAttributeData data) {
        final Map<String, String> map = new LinkedHashMap<>();
        if (data.getLength().isPresent()) {
          map.put(DISTANCE, Double.toString(data.getLength().get()));
        }
        if (data.getMaxSpeed().isPresent()) {
          map.put(MAX_SPEED, Double.toString(data.getMaxSpeed().get()));
        }
        for (final Entry<String, Object> entry : data.getAttributes()
            .entrySet()) {
          checkArgument(!entry.getKey().equals(DISTANCE)
              && !entry.getKey().equals(MAX_SPEED),
              "Attribute key: '%s' is reserved and should not be used.",
              entry.getKey());
          map.put(entry.getKey(), entry.getValue().toString());
        }

        if (!map.isEmpty()) {
          sb.append(DATA_START);
          Joiner.on(LIST_ITEM_SEPARATOR).withKeyValueSeparator("=")
              .appendTo(sb, map);
          sb.append(DATA_END);
        }
      }

      @Override
      public Optional<MultiAttributeData> read(String data) {
        final Map<String, String> map = parseDataAsMap(data);
        final Builder b = MultiAttributeData.builder();
        if (map.containsKey(DISTANCE)) {
          b.setLength(Double.parseDouble(map.get(DISTANCE)));
          map.remove(DISTANCE);
        }
        if (map.containsKey(MAX_SPEED)) {
          b.setMaxSpeed(Double.parseDouble(map.get(MAX_SPEED)));
          map.remove(MAX_SPEED);
        }
        b.addAllAttributes(map);

        if (b.getAttributes().isEmpty() && !b.getLength().isPresent()
            && !b.getMaxSpeed().isPresent()) {
          return Optional.absent();
        }
        return Optional.of(b.build());
      }
    }
  }
}
