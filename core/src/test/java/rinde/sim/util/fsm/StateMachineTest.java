/**
 * 
 */
package rinde.sim.util.fsm;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static rinde.sim.util.fsm.StateMachineTest.States.PAUSED;
import static rinde.sim.util.fsm.StateMachineTest.States.SPECIAL;
import static rinde.sim.util.fsm.StateMachineTest.States.STARTED;
import static rinde.sim.util.fsm.StateMachineTest.States.STOPPED;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import rinde.sim.event.ListenerEventHistory;
import rinde.sim.util.fsm.StateMachine.StateMachineEvent;
import rinde.sim.util.fsm.StateMachine.StateTransitionEvent;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class StateMachineTest {

    enum Events {
        START, STOP, PAUSE, SPEZIAL
    }

    enum States implements State<Events, Context> {

        STARTED {},
        STOPPED {},
        PAUSED {},
        SPECIAL {
            @Override
            public Events handle(Events event, Context context) {
                super.handle(event, context);
                if (context != null) {
                    return Events.START;
                }
                return null;
            }
        };

        protected List<Events> history;

        private States() {
            history = newArrayList();
        }

        @Override
        public void onEntry(Events event, Context context) {}

        @Override
        public void onExit(Events event, Context context) {}

        @Override
        public Events handle(Events event, Context context) {
            history.add(event);
            return null;
        }

    }

    class Context {}

    /**
     * The state machine under test.
     */
    protected StateMachine<Events, Context> fsm;

    /**
     * Setup the state machine used in the tests.
     */
    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        fsm = StateMachine.create(STOPPED)
                .addTransition(STARTED, Events.STOP, STOPPED)
                .addTransition(STARTED, Events.SPEZIAL, SPECIAL)
                .addTransition(SPECIAL, Events.START, STARTED)
                .addTransition(SPECIAL, Events.STOP, SPECIAL)
                .addTransition(STOPPED, Events.START, STARTED)
                .addTransition(STOPPED, Events.STOP, STOPPED)
                .addTransition(STARTED, Events.PAUSE, PAUSED)
                .addTransition(PAUSED, Events.STOP, STOPPED)
                .addTransition(PAUSED, Events.START, STARTED).build();

        assertTrue(fsm.stateIsOneOf(States.values()));
        assertFalse(fsm.stateIsOneOf(STARTED, SPECIAL));

        fsm.toDot();
        StateMachineEvent.valueOf("STATE_TRANSITION");
    }

    /**
     * Tests transitions.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testTransition() {
        final ListenerEventHistory history = new ListenerEventHistory();
        fsm.getEventAPI()
                .addListener(history, StateMachine.StateMachineEvent.values());

        // start in STOPPED state
        assertEquals(STOPPED, fsm.getCurrentState());
        fsm.handle(Events.START, null);
        assertEquals(STARTED, fsm.getCurrentState());
        // history.getHistory()

        // nothing should happen
        fsm.handle(null);
        assertEquals(STARTED, fsm.getCurrentState());

        // should go to SPECIAL and back to STARTED immediately
        history.clear();
        fsm.handle(Events.SPEZIAL, new Context());
        assertEquals(STARTED, fsm.getCurrentState());
        assertEquals(2, history.getHistory().size());
        assertTrue(((StateTransitionEvent) history.getHistory().get(0))
                .equalTo(STARTED, Events.SPEZIAL, SPECIAL));
        assertTrue(((StateTransitionEvent) history.getHistory().get(1))
                .equalTo(SPECIAL, Events.START, STARTED));

        // testing the equalTo method
        assertFalse(((StateTransitionEvent) history.getHistory().get(1))
                .equalTo(STARTED, Events.START, STARTED));
        assertFalse(((StateTransitionEvent) history.getHistory().get(1))
                .equalTo(SPECIAL, Events.PAUSE, STARTED));
        assertFalse(((StateTransitionEvent) history.getHistory().get(1))
                .equalTo(SPECIAL, Events.START, SPECIAL));

        // go to SPECIAL
        fsm.handle(Events.SPEZIAL, null);
        assertEquals(SPECIAL, fsm.getCurrentState());
        // should remain in SPECIAL
        fsm.handle(Events.STOP, null);
        assertEquals(SPECIAL, fsm.getCurrentState());

        fsm.handle(Events.START, null);
        assertEquals(STARTED, fsm.getCurrentState());

    }

    /**
     * Test transition that is not allowed.
     */
    @Test(expected = IllegalArgumentException.class)
    public void impossibleTransition() {
        fsm.handle(Events.START, null);
        fsm.handle(Events.START, null);
    }

    /**
     * Tests correct behavior for events which are not equal.
     */
    @Test
    public void eventNotEqualBehavior() {

        final TestState state1 = new TestState("state1");
        state1.name();
        final TestState state2 = new TestState("state2");
        final Object event1 = "event1";
        final Object event2 = new Object();

        final StateMachine<Object, Object> sm = StateMachine.create(state1)/* */
        .addTransition(state1, event1, state2)/* */
        .addTransition(state2, event2, state1)/* */
        .build();

        assertTrue(sm.isSupported(event1));
        assertTrue(sm.isSupported("event1"));
        assertTrue(sm.isSupported(new StringBuilder("event").append(1)
                .toString()));

        assertFalse(sm.isSupported(event2));

        sm.handle("event1", null);
        assertTrue(sm.stateIs(state2));

        assertTrue(sm.isSupported(event2));
        assertFalse(sm.isSupported(new Object()));
    }

    static class TestState extends AbstractState<Object, Object> {
        private final String name;

        public TestState(String pName) {
            name = pName;
        }

        @Override
        public String name() {
            return super.name() + name;
        }

        @Override
        public Object handle(Object event, Object context) {
            return null;
        }
    }
}
