/**
 * 
 */
package rinde.sim.util.fsm;

import static com.google.common.collect.Lists.newArrayList;
import static rinde.sim.util.fsm.StateMachineTest.States.PAUSED;
import static rinde.sim.util.fsm.StateMachineTest.States.STARTED;
import static rinde.sim.util.fsm.StateMachineTest.States.STOPPED;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import rinde.sim.util.fsm.StateMachine.State;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class StateMachineTest {

    enum Events {
        START, STOP, PAUSE
    }

    enum States implements State<Events, Context> {

        STARTED {},
        STOPPED {},
        PAUSED {};

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

    class Context {

    }

    protected StateMachine<Events, Context> fsm;

    @Before
    public void setUp() {
        fsm = StateMachine.create(STOPPED)
                .addTransition(STARTED, Events.STOP, STOPPED)
                .addTransition(STOPPED, Events.START, STARTED)
                .addTransition(STOPPED, Events.STOP, STOPPED)
                .addTransition(STARTED, Events.PAUSE, PAUSED)
                .addTransition(PAUSED, Events.STOP, STOPPED)
                .addTransition(PAUSED, Events.START, STARTED).build();
    }

    @Test
    public void test() {
        System.out.println("Current state: " + fsm.currentState);
        fsm.handle(Events.START, null);
        System.out.println("Current state: " + fsm.currentState + " "
                + ((States) fsm.currentState).history);

    }

    @Test(expected = IllegalArgumentException.class)
    public void impossibleTransition() {
        fsm.handle(Events.START, null);
        fsm.handle(Events.START, null);
    }
}
