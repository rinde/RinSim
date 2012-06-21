package rinde.sim.core.model.communication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Queue;

import org.junit.Before;
import org.junit.Test;

import rinde.sim.core.graph.Point;

public class MailboxTest {

	private Mailbox mBox;

	@Before
	public void setUp() throws Exception {
		mBox = new Mailbox();
		assertTrue(mBox.box.isEmpty());
	}

	@Test
	public void testReceive() {
		Message msg = new Message(new TestUser()) {};

		assertEquals(0, mBox.box.size());
		mBox.receive(msg);
		assertEquals(1, mBox.box.size());
		mBox.receive(msg);
		assertEquals(1, mBox.box.size());

	}

	@Test(expected = NullPointerException.class)
	public void addingNull() {
		mBox.receive(null);
	}

	@Test
	public void testGetMessages() {
		Message msg = new Message(new TestUser()) {};

		assertEquals(0, mBox.box.size());
		mBox.receive(msg);
		assertEquals(1, mBox.box.size());
		mBox.receive(msg);
		assertEquals(1, mBox.box.size());

		Queue<Message> messages = mBox.getMessages();
		assertEquals(0, mBox.box.size());
		assertEquals(1, messages.size());
		assertEquals(msg, messages.iterator().next());
	}

	class TestUser implements CommunicationUser {

		@Override
		public void setCommunicationAPI(CommunicationAPI api) {}

		@Override
		public Point getPosition() {
			return null;
		}

		@Override
		public double getRadius() {
			return 0;
		}

		@Override
		public double getReliability() {
			return 0;
		}

		@Override
		public void receive(Message message) {}
	}
}
