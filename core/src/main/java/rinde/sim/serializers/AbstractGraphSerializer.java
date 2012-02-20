/**
 * 
 */
package rinde.sim.serializers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import rinde.sim.core.graph.EdgeData;
import rinde.sim.core.graph.Graph;

/**
 * Common interface for graph serialization deserialization
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 *
 */
public abstract class AbstractGraphSerializer<E extends EdgeData> {
	abstract public Graph<E> read(Reader reader) throws IOException;
	abstract public void write(Graph<? extends E> graph, Writer writer) throws IOException;
	
	public Graph<E> read(File file) throws FileNotFoundException, IOException {
		FileReader reader = new FileReader(file);
		Graph<E> graph = read(reader);
		reader.close();
		return graph;
	}
	
	public Graph<E> read(String filePath) throws FileNotFoundException, IOException {
		return read(new File(filePath));
	}
	
	public void write(Graph<? extends E> graph, File file) throws IOException {
		FileWriter writer = new FileWriter(file);
		write(graph, writer);
		writer.close();
	}
	
	public void write(Graph<? extends E> graph, String filePath) throws IOException {
		write(graph, new File(filePath));
	}
}
