/**
 * 
 */
package rinde.sim.util.fsm;

import static com.google.common.collect.Lists.newArrayList;

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

    protected StateMachine<Events> fsm;
    protected State<Events> started;
    protected State<Events> stopped;
    protected State<Events> paused;

    @Before
    public void setUp() {
        started = new TestState("started");
        stopped = new TestState("stopped");
        paused = new TestState("paused");
        fsm = StateMachine.create(Events.class, stopped)
                .addTransition(started, stopped, Events.STOP)
                .addTransition(stopped, started, Events.START)
                .addTransition(stopped, stopped, Events.STOP)
                .addTransition(started, paused, Events.PAUSE)
                .addTransition(paused, stopped, Events.STOP)
                .addTransition(paused, started, Events.START).build();
    }

    @Test
    public void test() {
        System.out.println("Current state: " + fsm.currentState);
        fsm.fire(Events.START);
        System.out.println("Current state: " + fsm.currentState + " "
                + ((TestState) fsm.currentState).history);

    }

    @Test(expected = IllegalArgumentException.class)
    public void impossibleTransition() {
        fsm.fire(Events.START);
        fsm.fire(Events.START);
    }

    class TestState implements State<Events> {

        List<String> history;
        String name;

        public TestState(String n) {
            name = n;
            history = newArrayList();
        }

        @Override
        public void onEntry(Events event) {
            history.add("onEntry(" + event + ")");
        }

        @Override
        public void onEvent(Events event) {
            history.add("body(" + event + ")");
        }

        @Override
        public void onExit(Events event) {
            history.add("onExit(" + event + ")");
        }

        @Override
        public String toString() {
            return "[" + name + "]";
        }

    }

}
