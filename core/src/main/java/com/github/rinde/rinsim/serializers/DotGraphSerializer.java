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
package com.github.rinde.rinsim.serializers;

import static java.util.Arrays.asList;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;

import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.ConnectionData;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.MultiAttributeData;
import com.github.rinde.rinsim.geom.MultimapGraph;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

/**
 * Dot format serializer for a road model graph. Allows for reading storing maps
 * in dot format. The default implementation of the serializer for graphs with
 * edge length information can be obtained via calling
 * {@link DotGraphSerializer#getLengthGraphSerializer(SerializerFilter...)}
 * @param <E>
 *
 * @author Bartosz Michalik
 * @author Rinde van Lon
 */
public class DotGraphSerializer<E extends ConnectionData> extends
    AbstractGraphSerializer<E> {

  static final String POS = "p=";
  static final String NODE_PREFIX = "n";
  static final String DISTANCE = "d";
  static final String MAX_SPEED = "s";

  private final Predicate<Connection<?>> filter;
  private final ConnectionSerializer<E> serializer;

  DotGraphSerializer(ConnectionSerializer<E> connectionSerializer,
      Iterable<? extends Predicate<Connection<?>>> predicates) {
    filter = Predicates.or(predicates);
    this.serializer = connectionSerializer;
  }

  @Override
  public Graph<E> read(Reader r) throws IOException {
    final BufferedReader reader = new BufferedReader(r);
    final MultimapGraph<E> graph = new MultimapGraph<>();
    final HashMap<String, Point> nodeMapping = new HashMap<>();
    String line;
    while ((line = reader.readLine()) != null) {
      if (line.contains(POS)) {
        final String nodeName = line.substring(0, line.indexOf("[")).trim();
        final String[] position = line.split("\"")[1].split(",");
        final Point p = new Point(Double.parseDouble(position[0]),
            Double.parseDouble(position[1]));
        nodeMapping.put(nodeName, p);
      } else if (line.contains("->")) {
        // example:
        // node1004 -> node820[label="163.3"]
        final String[] names = line.split("->");
        final String fromStr = names[0].trim();
        final String toStr = names[1].substring(0, names[1].indexOf("["))
            .trim();
        final Point from = nodeMapping.get(fromStr);
        final Point to = nodeMapping.get(toStr);
        final E data = serializer.deserialize(line);

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
    final BufferedWriter out = new BufferedWriter(writer);

    final StringBuilder string = new StringBuilder();
    string.append("digraph mapgraph {\n");

    int nodeId = 0;
    final HashMap<Point, Integer> idMap = new HashMap<>();
    for (final Point p : graph.getNodes()) {
      string.append(NODE_PREFIX).append(nodeId).append('[').append(POS)
          .append("\"").append(p.x).append(',').append(p.y).append("\"]\n");
      idMap.put(p, nodeId);
      nodeId++;
    }

    for (final Connection<E> entry : graph.getConnections()) {
      string.append(serializer.serializeConnection(idMap.get(entry.from()),
          idMap
              .get(entry.to()), entry));
    }
    string.append('}');
    out.append(string);
    out.close(); // it is important to close the BufferedWriter! otherwise
                 // there is no guarantee that it has reached the end..
  }

  /**
   * Used to serialize graphs
   * @author Bartosz Michalik
   *
   * @since 2.0
   */
  interface ConnectionSerializer<E extends ConnectionData> {
    String serializeConnection(int idFrom, int idTo, Connection<E> conn);

    E deserialize(String connection);
  }

  private static class LengthConnectionSerializer implements
      ConnectionSerializer<LengthData> {

    public LengthConnectionSerializer() {}

    @Override
    public String serializeConnection(int idFrom, int idTo,
        Connection<LengthData> conn) {
      final StringBuilder sb = new StringBuilder();
      sb.append(NODE_PREFIX)
          .append(idFrom)
          .append(" -> ")
          .append(NODE_PREFIX)
          .append(idTo)
          .append('[')
          .append(DISTANCE)
          .append("=\"")
          .append(Math.round(conn.data().get().getLength().get()) / 10d)
          .append("\"]\n");
      return sb.toString();
    }

    @Override
    public LengthData deserialize(String connection) {
      final double distance = Double.parseDouble(connection.split("\"")[1]);
      return LengthData.create(distance);
    }
  }

  private static class MultiAttributeConnectionSerializer implements
      ConnectionSerializer<MultiAttributeData> {
    public MultiAttributeConnectionSerializer() {}

    @Override
    public String serializeConnection(int idFrom, int idTo,
        Connection<MultiAttributeData> conn) {
      final StringBuilder sb = new StringBuilder();
      sb.append(NODE_PREFIX)
          .append(idFrom)
          .append(" -> ")
          .append(NODE_PREFIX)
          .append(idTo)
          .append('[')
          .append(DISTANCE)
          .append("=\"")
          .append(Math.round(conn.data().get().getLength().get()) / 10d);
      if (conn.data().get().getMaxSpeed().isPresent()) {
        sb.append("\", ")
            .append(MAX_SPEED)
            .append("=\"")
            .append(conn.data().get().getMaxSpeed().get());
      }
      sb.append("\"]\n");
      return sb.toString();
    }

    @Override
    public MultiAttributeData deserialize(String connection) {
      final double distance = Double.parseDouble(connection.split("\"")[1]);
      try {
        final double maxSpeed = Double.parseDouble(connection.split("\"")[3]);
        return MultiAttributeData.builder()
            .setLength(distance)
            .setMaxSpeed(maxSpeed)
            .build();
      } catch (final Exception e) {
        return MultiAttributeData.builder()
            .setLength(distance)
            .build();
      }
    }
  }

  /**
   * Get instance of the serializer that can read write graph with the edges
   * length information
   * @param filters
   * @return
   */
  public static DotGraphSerializer<LengthData> getLengthGraphSerializer(
      Iterable<? extends Predicate<Connection<?>>> filters) {
    return new DotGraphSerializer<>(new LengthConnectionSerializer(),
        filters);
  }

  public static DotGraphSerializer<LengthData> getLengthGraphSerializer(
      Predicate<Connection<?>> filters) {
    return new DotGraphSerializer<>(new LengthConnectionSerializer(),
        asList(filters));
  }

  public static DotGraphSerializer<MultiAttributeData> getMultiAttributeGraphSerializer(
      Predicate<Connection<?>> filter) {
    return new DotGraphSerializer<>(new MultiAttributeConnectionSerializer(),
        asList(filter));
  }

  /**
   * @param filters
   * @return
   */
  public static DotGraphSerializer<MultiAttributeData> getMultiAttributeGraphSerializer(
      Iterable<? extends Predicate<Connection<?>>> filters) {
    return new DotGraphSerializer<>(new MultiAttributeConnectionSerializer(),
        filters);
  }
}
