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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import com.github.rinde.rinsim.geom.ConnectionData;
import com.github.rinde.rinsim.geom.Graph;
import com.google.common.base.Charsets;

/**
 * Common abstract class for graph input and output operations.
 * @param <E> Type of connection data.
 * @author Bartosz Michalik
 * @author Rinde van Lon
 */
public abstract class AbstractGraphIO<E extends ConnectionData> {
  /**
   * Reads a graph from the specified reader.
   * @param reader The reader to user for reading.
   * @return A {@link Graph} instance.
   * @throws IOException If something goes wrong while reading.
   */
  abstract public Graph<E> read(Reader reader) throws IOException;

  /**
   * Reads a graph from the specified path.
   * @param path The path to read from.
   * @return A {@link Graph} instance.
   * @throws IOException If something goes wrong while reading.
   */
  public Graph<E> read(Path path) throws IOException {
    return read(Files.newInputStream(path));
  }

  /**
   * Reads a graph from the specified stream.
   * @param stream The stream to read from.
   * @return A {@link Graph} instance.
   * @throws IOException If something goes wrong while reading.
   */
  public Graph<E> read(InputStream stream) throws IOException {
    return readReader(new InputStreamReader(stream));
  }

  /**
   * Reads a graph from the specified file.
   * @param filePath A path to a file containing a graph.
   * @return A {@link Graph} instance.
   * @throws IOException If something goes wrong while reading.
   */
  public Graph<E> read(String filePath) throws IOException {
    return read(FileSystems.getDefault().getPath(filePath));
  }

  /**
   * Writes a graph to the specified writer.
   * @param graph The graph to write.
   * @param writer The writer to use for writing to.
   * @throws IOException If something goes wrong while writing.
   */
  abstract public void write(Graph<E> graph, Writer writer)
      throws IOException;

  /**
   * Writes a graph to the specified path.
   * @param graph The graph to write.
   * @param path The path to write to.
   * @throws IOException If something goes wrong while writing.
   */
  public void write(Graph<E> graph, Path path) throws IOException {
    final BufferedWriter writer = Files.newBufferedWriter(path, Charsets.UTF_8);
    write(graph, writer);
    writer.close();
  }

  /**
   * Writes a graph to the specified path.
   * @param graph The graph to write.
   * @param filePath The path to write to.
   * @throws IOException If something goes wrong while writing.
   */
  public void write(Graph<E> graph, String filePath) throws IOException {
    write(graph, FileSystems.getDefault().getPath(filePath));
  }

  Graph<E> readReader(Reader r) throws IOException {
    final Graph<E> graph = read(r);
    r.close();
    return graph;
  }
}
