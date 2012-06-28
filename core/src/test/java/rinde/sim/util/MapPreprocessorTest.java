/**
 * 
 */
package rinde.sim.util;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import rinde.sim.core.graph.ConnectionData;
import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.Graphs;
import rinde.sim.core.graph.LengthData;
import rinde.sim.core.graph.MultiAttributeData;
import rinde.sim.core.graph.MultimapGraph;
import rinde.sim.core.graph.Point;
import rinde.sim.core.graph.TableGraph;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
@RunWith(Parameterized.class)
public class MapPreprocessorTest<E extends ConnectionData> {

	final static boolean WRITE_TO_FILE = false;

	protected Class<? extends Graph<E>> graphType;
	protected E empty;

	public Graph<E> createGraph() {
		try {
			if (graphType.equals(TableGraph.class)) {
				return new TableGraph<E>(empty);
			}
			return graphType.newInstance();

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public E newEdgeData(double length) {
		try {
			return (E) empty.getClass().getConstructor(Double.TYPE).newInstance(length);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public MapPreprocessorTest(Class<? extends Graph<E>> c, E empty) {
		graphType = c;
		this.empty = empty;
	}

	@Parameters
	public static Collection<Object[]> configs() {
		return Arrays.asList(new Object[][] { { MultimapGraph.class, LengthData.EMPTY }, { MultimapGraph.class, MultiAttributeData.EMPTY },
				{ TableGraph.class, LengthData.EMPTY }, { TableGraph.class, MultiAttributeData.EMPTY } });
	}

	static class GraphParameterSetting<E extends ConnectionData> {
		private final Class<? extends Graph<E>> g;
		private final E empty;

		public GraphParameterSetting(Class<Graph<E>> g, E empty) {
			this.g = g;
			this.empty = empty;
		}

		public Graph<E> newInstance() throws InstantiationException, IllegalAccessException {
			if (g.equals(TableGraph.class)) {
				return new TableGraph<E>(empty);
			}
			return g.newInstance();
		}
	}

	@Test
	public void testFindNotFullyConnectedNodes1() {
		Point a, b, c, d, e;
		Graph<E> graph = createGraph();
		a = new Point(0, 0);
		b = new Point(10, 0);
		c = new Point(0, 10);
		d = new Point(10, 10);
		e = new Point(15, 5);

		graph.addConnection(a, b);
		graph.addConnection(a, c);
		graph.addConnection(b, c);
		graph.addConnection(c, a);
		graph.addConnection(c, d);
		graph.addConnection(b, e);
		graph.addConnection(d, e);
		graph.addConnection(e, d);
		for (Point root : asList(a, b, c)) {
			Set<Point> unconnectedSet = MapPreprocessor.findNotFullyConnectedNodes(graph, root).get(0);
			assertTrue(unconnectedSet.size() == 2);
			assertTrue(unconnectedSet.containsAll(asList(e, d)));
		}

		for (Point root : asList(d, e)) {
			Set<Point> unconnectedSet = MapPreprocessor.findNotFullyConnectedNodes(graph, root).get(0);
			assertTrue(unconnectedSet.size() == 3);
			assertTrue(unconnectedSet.containsAll(asList(a, b, c)));
		}

	}

	@Test
	public void testFindNotFullyConnectedNodes2() {
		Point a, b, c, d, e;
		Graph<E> graph = createGraph();
		a = new Point(0, 0);
		b = new Point(10, 0);
		c = new Point(0, 10);
		d = new Point(10, 10);
		e = new Point(15, 5);

		graph.addConnection(a, b);
		graph.addConnection(a, c);
		graph.addConnection(b, c);
		graph.addConnection(c, a);
		graph.addConnection(c, d);
		graph.addConnection(b, e);
		graph.addConnection(d, e);

		for (Point root : asList(a, b, c)) {
			Set<Point> unconnectedSet = MapPreprocessor.findNotFullyConnectedNodes(graph, root).get(0);
			assertTrue(unconnectedSet.size() == 2);
			assertTrue(unconnectedSet.containsAll(asList(e, d)));
		}

		Set<Point> unconnectedSet = MapPreprocessor.findNotFullyConnectedNodes(graph, d).get(0);
		assertTrue(unconnectedSet.size() == 4);
		assertTrue(unconnectedSet.containsAll(asList(a, b, c, e)));

		unconnectedSet = MapPreprocessor.findNotFullyConnectedNodes(graph, e).get(0);
		assertTrue(unconnectedSet.size() == 4);
		assertTrue(unconnectedSet.containsAll(asList(a, b, c, d)));
	}

	@Test
	public void testConnect() {
		Point a, b, c, d, e;
		Graph<E> graph = createGraph();
		a = new Point(0, 0);
		b = new Point(10, 0);
		c = new Point(0, 10);
		d = new Point(10, 10);
		e = new Point(15, 5);

		graph.addConnection(a, b);
		graph.addConnection(a, c);
		graph.addConnection(b, c);
		graph.addConnection(c, a);
		graph.addConnection(c, d);
		graph.addConnection(b, e);
		graph.addConnection(d, e);
		graph.addConnection(e, d);

		Graph<E> result = MapPreprocessor.connect(graph);
		if (WRITE_TO_FILE) {
			DotUtils.saveToDot(graph, "files/test/input", true);
		}
		if (WRITE_TO_FILE) {
			DotUtils.saveToDot(result, "files/test/fixed", true);
		}
	}

	@Test
	public void testConnect2() {
		Point a, b, c, d, e;
		Graph<E> graph = createGraph();
		a = new Point(0, 0);
		b = new Point(10, 0);
		c = new Point(0, 10);
		d = new Point(10, 10);
		e = new Point(15, 5);

		graph.addConnection(a, b);
		graph.addConnection(a, c);
		graph.addConnection(b, c);
		graph.addConnection(c, a);
		graph.addConnection(c, d);
		graph.addConnection(b, e);
		graph.addConnection(d, e);
		graph.addConnection(e, d);

		Graph<E> result = MapPreprocessor.connect2(graph);
		if (WRITE_TO_FILE) {
			DotUtils.saveToDot(graph, "files/test/input2", true);
		}
		if (WRITE_TO_FILE) {
			DotUtils.saveToDot(result, "files/test/fixed2", true);
		}
	}

	// TODO extend these tests, also check all EdgeData stuff!
	@Test
	public void testSimplify1() {
		Point a, b, c;
		a = new Point(0, 0);
		b = new Point(1, 0);
		c = new Point(2, 0);
		Graph<E> graph = createGraph();

		Graphs.addPath(graph, a, b, c, a);
		//		System.out.println(graph.getConnections());
		if (WRITE_TO_FILE) {
			DotUtils.saveToDot(graph, "files/test/mapfixer/simplify-1-in", true);
		}
		Graph<E> out = MapPreprocessor.simplify(graph, empty);

		//		System.out.println(out.getConnections());

		if (WRITE_TO_FILE) {
			DotUtils.saveToDot(out, "files/test/mapfixer/simplify-1-out", true);
		}

		graph.addConnection(b, a);
		if (WRITE_TO_FILE) {
			DotUtils.saveToDot(graph, "files/test/mapfixer/simplify-2-in", true);
		}
		out = MapPreprocessor.simplify(graph, empty);
		if (WRITE_TO_FILE) {
			DotUtils.saveToDot(out, "files/test/mapfixer/simplify-2-out", true);
		}

		assertEquals(graph, out);

		graph = createGraph();
		Graphs.addBiPath(graph, a, b, c, a);
		assertEquals(graph, MapPreprocessor.simplify(graph, empty));
	}

	@Test
	public void testSimplify3() {
		Graph<E> graph = createGraph();
		Point a, b, c, d, e, f;
		a = new Point(0, 0);
		b = new Point(1, 0);
		c = new Point(2, 0);
		d = new Point(6, 0);
		e = new Point(6, 2);
		f = new Point(2, 2);
		Graph<E> out;
		//		Graphs.addPath(graph, a, b, c, d, e, f);
		//		DotUtils.saveToDot(graph, "files/test/mapfixer/simplify-3-in");
		//		Graph out = MapFixer.simplify(graph);
		//		DotUtils.saveToDot(out, "files/test/mapfixer/simplify-3-out");
		//
		//		assertEquals(1, out.getNumberOfConnections());
		//		assertEquals(12.0, out.connectionLength(a, f), 0.0002);

		graph = createGraph();
		Graphs.addBiPath(graph, a, b, c, d, e, f);
		if (WRITE_TO_FILE) {
			DotUtils.saveToDot(graph, "files/test/mapfixer/simplify-4-in", true);
		}

		assertEquals(MapPreprocessor.ContractType.RIGHT, MapPreprocessor.isContractable(graph, a, b));

		out = MapPreprocessor.simplify(graph, empty);
		if (WRITE_TO_FILE) {
			DotUtils.saveToDot(out, "files/test/mapfixer/simplify-4-out", true);
		}
		assertEquals(2, out.getNumberOfNodes());
		assertEquals(2, out.getNumberOfConnections());
		assertEquals(12.0, out.connectionLength(a, f), 0.0002);
	}

	@Test
	public void testIsContractableZero() {
		Point a, b, c;
		Graph<E> graph = createGraph();
		a = new Point(0, 0);
		b = new Point(1, 0);
		c = new Point(2, 0);

		Graphs.addPath(graph, a, b, c);
		if (WRITE_TO_FILE) {
			DotUtils.saveToDot(graph, "files/test/mapfixer/contractable-zero-1", true);
		}
		assertEquals(MapPreprocessor.ContractType.RIGHT, MapPreprocessor.isContractable(graph, a, b));
		assertEquals(MapPreprocessor.ContractType.LEFT, MapPreprocessor.isContractable(graph, b, a));
		assertEquals(MapPreprocessor.ContractType.LEFT, MapPreprocessor.isContractable(graph, b, c));
		assertEquals(MapPreprocessor.ContractType.RIGHT, MapPreprocessor.isContractable(graph, c, b));

		Graphs.addPath(graph, c, b, a);
		if (WRITE_TO_FILE) {
			DotUtils.saveToDot(graph, "files/test/mapfixer/contractable-zero-2", true);
		}
		assertEquals(MapPreprocessor.ContractType.RIGHT, MapPreprocessor.isContractable(graph, a, b));
		assertEquals(MapPreprocessor.ContractType.LEFT, MapPreprocessor.isContractable(graph, b, a));
		assertEquals(MapPreprocessor.ContractType.LEFT, MapPreprocessor.isContractable(graph, b, c));
		assertEquals(MapPreprocessor.ContractType.RIGHT, MapPreprocessor.isContractable(graph, c, b));

		Point d = new Point(3, 0);
		graph.addConnection(b, d);
		if (WRITE_TO_FILE) {
			DotUtils.saveToDot(graph, "files/test/mapfixer/contractable-zero-3", true);
		}
		assertEquals(MapPreprocessor.ContractType.NO, MapPreprocessor.isContractable(graph, a, b));
		assertEquals(MapPreprocessor.ContractType.NO, MapPreprocessor.isContractable(graph, b, a));

		graph = createGraph();
		Graphs.addPath(graph, a, b);
		if (WRITE_TO_FILE) {
			DotUtils.saveToDot(graph, "files/test/mapfixer/contractable-zero-4", true);
		}
		assertEquals(MapPreprocessor.ContractType.NO, MapPreprocessor.isContractable(graph, a, b));
		assertEquals(MapPreprocessor.ContractType.NO, MapPreprocessor.isContractable(graph, b, a));

		Graphs.addPath(graph, b, a);
		if (WRITE_TO_FILE) {
			DotUtils.saveToDot(graph, "files/test/mapfixer/contractable-zero-5", true);
		}
		assertEquals(MapPreprocessor.ContractType.NO, MapPreprocessor.isContractable(graph, a, b));
		assertEquals(MapPreprocessor.ContractType.NO, MapPreprocessor.isContractable(graph, b, a));

		graph = createGraph();
		graph.addConnection(a, b);
		graph.addConnection(b, a);
		if (WRITE_TO_FILE) {
			DotUtils.saveToDot(graph, "files/test/mapfixer/contractable-zero-6", true);
		}
		assertEquals(MapPreprocessor.ContractType.NO, MapPreprocessor.isContractable(graph, a, b));
		assertEquals(MapPreprocessor.ContractType.NO, MapPreprocessor.isContractable(graph, b, a));

	}

	@Test(expected = IllegalArgumentException.class)
	public void testIsContractableIllegal() {
		Point a, b, c;
		Graph<E> graph = createGraph();
		a = new Point(0, 0);
		b = new Point(1, 0);
		c = new Point(2, 0);

		Graphs.addPath(graph, a, b, c);
		assertEquals(MapPreprocessor.ContractType.NO, MapPreprocessor.isContractable(graph, a, c));
	}

	@Test
	public void testIsContractableOneAndOne() {
		Point a, b, c, d;
		Graph<E> graph = createGraph();
		a = new Point(0, 0);
		b = new Point(1, 0);
		c = new Point(2, 0);
		d = new Point(3, 0);

		Graphs.addPath(graph, a, b, c, d);
		if (WRITE_TO_FILE) {
			DotUtils.saveToDot(graph, "files/test/mapfixer/contractable-one-and-one-1", true);
		}
		assertEquals(MapPreprocessor.ContractType.BOTH, MapPreprocessor.isContractable(graph, b, c));
		assertEquals(MapPreprocessor.ContractType.BOTH, MapPreprocessor.isContractable(graph, c, b));

		graph.addConnection(b, a);
		if (WRITE_TO_FILE) {
			DotUtils.saveToDot(graph, "files/test/mapfixer/contractable-one-and-one-2", true);
		}
		assertEquals(MapPreprocessor.ContractType.RIGHT, MapPreprocessor.isContractable(graph, b, c));
		assertEquals(MapPreprocessor.ContractType.LEFT, MapPreprocessor.isContractable(graph, c, b));

		graph.addConnection(c, b);
		if (WRITE_TO_FILE) {
			DotUtils.saveToDot(graph, "files/test/mapfixer/contractable-one-and-one-3", true);
		}
		assertEquals(MapPreprocessor.ContractType.LEFT, MapPreprocessor.isContractable(graph, b, c));
		assertEquals(MapPreprocessor.ContractType.RIGHT, MapPreprocessor.isContractable(graph, c, b));

		graph.addConnection(d, c);
		if (WRITE_TO_FILE) {
			DotUtils.saveToDot(graph, "files/test/mapfixer/contractable-one-and-one-4", true);
		}
		assertEquals(MapPreprocessor.ContractType.BOTH, MapPreprocessor.isContractable(graph, b, c));
		assertEquals(MapPreprocessor.ContractType.BOTH, MapPreprocessor.isContractable(graph, c, b));

		graph.removeConnection(b, c);
		if (WRITE_TO_FILE) {
			DotUtils.saveToDot(graph, "files/test/mapfixer/contractable-one-and-one-5", true);
		}
		assertEquals(MapPreprocessor.ContractType.NO, MapPreprocessor.isContractable(graph, b, c));
		assertEquals(MapPreprocessor.ContractType.NO, MapPreprocessor.isContractable(graph, c, b));

		graph = createGraph();
		Graphs.addPath(graph, a, b, c, d);
		graph.addConnection(c, b);
		if (WRITE_TO_FILE) {
			DotUtils.saveToDot(graph, "files/test/mapfixer/contractable-one-and-one-6", true);
		}
		assertEquals(MapPreprocessor.ContractType.NO, MapPreprocessor.isContractable(graph, b, c));
		assertEquals(MapPreprocessor.ContractType.NO, MapPreprocessor.isContractable(graph, c, b));

	}

	@Test
	public void testIsContractableMoreThanOne() {
		Point a, b, c, d, e, f;
		Graph<E> graph = createGraph();
		a = new Point(0, 0);
		b = new Point(1, 0);
		c = new Point(2, 0);
		d = new Point(3, 0);
		e = new Point(4, 0);
		f = new Point(5, 0);

		Graphs.addPath(graph, a, c, d, f);
		Graphs.addPath(graph, a, b, c);
		if (WRITE_TO_FILE) {
			DotUtils.saveToDot(graph, "files/test/mapfixer/contractable-more-than-one-1", true);
		}
		assertEquals(MapPreprocessor.ContractType.RIGHT, MapPreprocessor.isContractable(graph, c, d));
		assertEquals(MapPreprocessor.ContractType.LEFT, MapPreprocessor.isContractable(graph, d, c));

		Graphs.addPath(graph, f, e, d);
		if (WRITE_TO_FILE) {
			DotUtils.saveToDot(graph, "files/test/mapfixer/contractable-more-than-one-2", true);
		}
		assertEquals(MapPreprocessor.ContractType.NO, MapPreprocessor.isContractable(graph, c, d));
		assertEquals(MapPreprocessor.ContractType.NO, MapPreprocessor.isContractable(graph, d, c));
	}

	@Test
	public void testIsContractableSameNeigh() {
		Point a, b, c;
		Graph<E> graph = createGraph();
		a = new Point(0, 0);
		b = new Point(1, 0);
		c = new Point(2, 0);

		Graphs.addPath(graph, a, b, c, a);
		if (WRITE_TO_FILE) {
			DotUtils.saveToDot(graph, "files/test/mapfixer/contractable-same-neigh-1", true);
		}
		assertEquals(MapPreprocessor.ContractType.BOTH, MapPreprocessor.isContractable(graph, a, b));
		assertEquals(MapPreprocessor.ContractType.BOTH, MapPreprocessor.isContractable(graph, b, a));
	}
}
