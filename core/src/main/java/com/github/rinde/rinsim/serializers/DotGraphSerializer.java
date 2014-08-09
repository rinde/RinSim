package com.github.rinde.rinsim.serializers;

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

/**
 * Dot format serializer for a road model graph. Allows for reading storing maps
 * in dot format. The default implementation of the serializer for graphs with
 * edge length information can be obtained via calling
 * {@link DotGraphSerializer#getLengthGraphSerializer(SerializerFilter...)}
 * 
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * 
 */
public class DotGraphSerializer<E extends ConnectionData> extends
    AbstractGraphSerializer<E> {

  private SerializerFilter<? extends Object>[] filters;
  private ConnectionSerializer<E> serializer;

  public static final String POS = "p";
  public static final String NODE_PREFIX = "n";
  public static final String DISTANCE = "d";
  public static final String MAX_SPEED = "s";

  public DotGraphSerializer(ConnectionSerializer<E> connectionSerializer,
      SerializerFilter<?>... filters) {
    if (connectionSerializer == null) {
      throw new IllegalArgumentException("connectionSerializer cannot be null");
    }
    this.filters = filters;
    if (filters == null) {
      this.filters = new SerializerFilter<?>[0];
    }
    this.serializer = connectionSerializer;
  }

  public DotGraphSerializer(ConnectionSerializer<E> serializer) {
    this(serializer, new SerializerFilter[0]);
  }

  @Override
  public Graph<E> read(Reader r) throws IOException {

    final BufferedReader reader = new BufferedReader(r);

    final MultimapGraph<E> graph = new MultimapGraph<E>();

    final HashMap<String, Point> nodeMapping = new HashMap<String, Point>();
    String line;
    while ((line = reader.readLine()) != null) {
      if (line.contains(POS + "=")) {
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
        for (final SerializerFilter<?> f : filters) {
          if (f.filterOut(from, to)) {
            continue;
          }
        }
        final E data = serializer.deserialize(line);
        graph.addConnection(from, to, data);

      }
    }
    return graph;

  }

  @Override
  public void write(Graph<? extends E> graph, Writer writer) throws IOException {
    final BufferedWriter out = new BufferedWriter(writer);

    final StringBuilder string = new StringBuilder();
    string.append("digraph mapgraph {\n");

    int nodeId = 0;
    final HashMap<Point, Integer> idMap = new HashMap<Point, Integer>();
    for (final Point p : graph.getNodes()) {
      string.append(NODE_PREFIX).append(nodeId).append('[').append(POS)
          .append("=\"").append(p.x).append(',').append(p.y).append("\"]\n");
      idMap.put(p, nodeId);
      nodeId++;
    }

    for (final Connection<? extends E> entry : graph.getConnections()) {
      string.append(serializer.serializeConnection(idMap.get(entry.from), idMap
          .get(entry.to), entry));
    }
    string.append('}');
    out.append(string);
    out.close(); // it is important to close the BufferedWriter! otherwise
                 // there is no guarantee that it has reached the end..
  }

  /**
   * Used to serialize graphs
   * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
   * 
   * @param <E>
   * @since 2.0
   */
  public static abstract class ConnectionSerializer<E extends ConnectionData> {
    public abstract String serializeConnection(int idFrom, int idTo,
        Connection<? extends E> conn);

    public abstract E deserialize(String connection);
  }

  private static class LengthConnectionSerializer extends
      ConnectionSerializer<LengthData> {

    public LengthConnectionSerializer() {}

    @Override
    public String serializeConnection(int idFrom, int idTo,
        Connection<? extends LengthData> conn) {
      final StringBuffer buffer = new StringBuffer();
      buffer.append(NODE_PREFIX).append(idFrom).append(" -> ")
          .append(NODE_PREFIX).append(idTo);
      buffer.append('[').append(DISTANCE).append("=\"")
          .append(Math.round(conn.getData().getLength()) / 10d).append("\"]\n");
      return buffer.toString();
    }

    @Override
    public LengthData deserialize(String connection) {
      final double distance = Double.parseDouble(connection.split("\"")[1]);
      return new LengthData(distance);
    }
  }

  private static class MultiAttributeConnectionSerializer extends
      ConnectionSerializer<MultiAttributeData> {
    public MultiAttributeConnectionSerializer() {}

    @Override
    public String serializeConnection(int idFrom, int idTo,
        Connection<? extends MultiAttributeData> conn) {
      final StringBuffer buffer = new StringBuffer();
      buffer.append(NODE_PREFIX).append(idFrom).append(" -> ")
          .append(NODE_PREFIX).append(idTo);
      buffer.append('[').append(DISTANCE).append("=\"")
          .append(Math.round(conn.getData().getLength()) / 10d);
      if (!Double.isNaN(conn.getData().getMaxSpeed())
          && conn.getData().getMaxSpeed() > 0) {
        buffer.append("\", ").append(MAX_SPEED).append("=\"")
            .append(conn.getData().getMaxSpeed());
      }
      buffer.append("\"]\n");
      return buffer.toString();
    }

    @Override
    public MultiAttributeData deserialize(String connection) {
      final double distance = Double.parseDouble(connection.split("\"")[1]);
      try {
        final double maxSpeed = Double.parseDouble(connection.split("\"")[3]);
        return new MultiAttributeData(distance, maxSpeed);
      } catch (final Exception e) {
        return new MultiAttributeData(distance);
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
      SerializerFilter<?>... filters) {
    return new DotGraphSerializer<LengthData>(new LengthConnectionSerializer(),
        filters);
  }

  public static DotGraphSerializer<MultiAttributeData> getMultiAttributeGraphSerializer(
      SerializerFilter<?>... filters) {
    return new DotGraphSerializer<MultiAttributeData>(
        new MultiAttributeConnectionSerializer(), filters);
  }
}
