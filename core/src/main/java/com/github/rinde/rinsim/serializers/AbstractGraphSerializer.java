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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;

import com.github.rinde.rinsim.geom.ConnectionData;
import com.github.rinde.rinsim.geom.Graph;

/**
 * Common interface for graph serialization deserialization
 * @author Bartosz Michalik 
 * 
 */
public abstract class AbstractGraphSerializer<E extends ConnectionData> {
  abstract public Graph<E> read(Reader reader) throws IOException;

  abstract public void write(Graph<? extends E> graph, Writer writer)
      throws IOException;

  public Graph<E> read(File file) throws FileNotFoundException, IOException {
    return readReader(new FileReader(file));
  }

  public Graph<E> read(InputStream stream) throws IOException {
    return readReader(new InputStreamReader(stream));
  }

  Graph<E> readReader(Reader r) throws IOException {
    final Graph<E> graph = read(r);
    r.close();
    return graph;
  }

  public Graph<E> read(String filePath) throws FileNotFoundException,
      IOException {
    return read(new File(filePath));
  }

  public void write(Graph<? extends E> graph, File file) throws IOException {
    final FileWriter writer = new FileWriter(file);
    write(graph, writer);
    writer.close();
  }

  public void write(Graph<? extends E> graph, String filePath)
      throws IOException {
    write(graph, new File(filePath));
  }
}
