/**
 * 
 */
package rinde.sim.core.graph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class ConnectionTest {

	private static final double DELTA = 0.0001;

	@SuppressWarnings("unused")
	@Test(expected = IllegalArgumentException.class)
	public void invalidConnection1() {
		new Connection<LengthEdgeData>(null, null, null);
	}

	@SuppressWarnings("unused")
	@Test(expected = IllegalArgumentException.class)
	public void invalidConnection2() {
		new Connection<LengthEdgeData>(new Point(1, 1), null, null);
	}

	@Test
	public void unmodifiableConnection() {
		Connection<LengthEdgeData> original = new Connection<LengthEdgeData>(new Point(2, 2), new Point(3, 3), null);
		Connection<LengthEdgeData> unmod = Graphs.unmodifiableConnection(original);

		assertEquals(unmod, original);
		assertEquals(original, unmod);
		original.setEdgeData(new LengthEdgeData(300));

		assertFalse(original.getEdgeData().equals(null));

		assertEquals(original.getEdgeData(), unmod.getEdgeData());
		assertEquals(original.getEdgeData().hashCode(), unmod.getEdgeData().hashCode());
		assertEquals(original.getEdgeData().getLength(), unmod.getEdgeData().getLength(), DELTA);
		assertEquals(original.hashCode(), unmod.hashCode());
		assertEquals(original.toString(), unmod.toString());
		assertEquals(unmod, original);
		assertEquals(original, unmod);

		original.setEdgeData(null);
		assertEquals(unmod, original);
		assertEquals(original, unmod);
	}

	@Test
	public void unmodifiableMultiAttributeEdgeData() {
		Connection<MultiAttributeEdgeData> original = new Connection<MultiAttributeEdgeData>(new Point(2, 2),
				new Point(3, 3), null);
		Connection<MultiAttributeEdgeData> unmod = Graphs.unmodifiableConnection(original);

		assertEquals(unmod, original);
		assertEquals(original, unmod);
		original.setEdgeData(new MultiAttributeEdgeData(10, 20));
		assertEquals(unmod, original);
		assertEquals(original, unmod);

		original.getEdgeData().put("test", Arrays.asList(1, 2, 3));
		assertEquals(unmod, original);
		assertEquals(original, unmod);
		assertEquals(Arrays.asList(1, 2, 3), unmod.getEdgeData().get("test", Object.class));
		assertEquals(Arrays.asList(1, 2, 3), unmod.getEdgeData().get("test", List.class));
		assertNull(unmod.getEdgeData().get("test", Map.class));
		assertNull(unmod.getEdgeData().get("test2", Map.class));

		assertEquals(original.getEdgeData().getLength(), unmod.getEdgeData().getLength(), DELTA);
		assertEquals(original.getEdgeData().getMaxSpeed(), unmod.getEdgeData().getMaxSpeed(), DELTA);

		assertTrue(original.getEdgeData().equals(unmod.getEdgeData()));
		assertTrue(unmod.getEdgeData().equals(unmod.getEdgeData()));
		assertEquals(unmod.getEdgeData().hashCode(), unmod.getEdgeData().hashCode());
		assertEquals(original.hashCode(), unmod.hashCode());

		original.getEdgeData().put(MultiAttributeEdgeData.KEY_LENGTH, new Object());
		assertEquals(original, unmod);
		original.getEdgeData().put(MultiAttributeEdgeData.KEY_MAX_SPEED, new Object());
		assertEquals(original, unmod);

		assertTrue(Double.isNaN(original.getEdgeData().getMaxSpeed()));
		assertTrue(Double.isNaN(original.getEdgeData().getLength()));

		assertTrue(Double.isNaN(original.getEdgeData().setMaxSpeed(100)));
		assertEquals(100, original.getEdgeData().setMaxSpeed(200), DELTA);

		assertEquals(original, unmod);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void unmodMultiAttED() {
		Graphs.unmodifiableEdgeData(new MultiAttributeEdgeData(10, 20)).setMaxSpeed(-1);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void unmodMultiAttED2() {
		Graphs.unmodifiableEdgeData(new MultiAttributeEdgeData(10, 20)).put("", null);
	}

	@Test
	public void equalsTest() {
		Connection<LengthEdgeData> c1 = new Connection<LengthEdgeData>(new Point(2, 2), new Point(3, 3), null);
		Connection<LengthEdgeData> c2 = new Connection<LengthEdgeData>(new Point(2, 2), new Point(2, 3), null);
		Connection<LengthEdgeData> c3 = new Connection<LengthEdgeData>(new Point(3, 2), new Point(2, 3), null);
		Connection<LengthEdgeData> c4 = new Connection<LengthEdgeData>(new Point(2, 2), new Point(3, 3),
				new LengthEdgeData(30));
		assertFalse(c1.equals(new Object()));
		assertFalse(c1.equals(c2));
		assertFalse(c1.equals(c3));
		assertFalse(c1.equals(c4));
	}

	@Test(expected = UnsupportedOperationException.class)
	public void unmodifiableConnSetEdgeData() {
		Graphs.unmodifiableConnection(new Connection<LengthEdgeData>(new Point(2, 2), new Point(3, 3), null))
				.setEdgeData(null);
	}
}
