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
package com.github.rinde.rinsim.core.model.communication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Queue;

import org.junit.Before;
import org.junit.Test;

import com.github.rinde.rinsim.core.model.communication.CommunicationAPI;
import com.github.rinde.rinsim.core.model.communication.CommunicationUser;
import com.github.rinde.rinsim.core.model.communication.Mailbox;
import com.github.rinde.rinsim.core.model.communication.Message;
import com.github.rinde.rinsim.geom.Point;

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
