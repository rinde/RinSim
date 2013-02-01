/**
 * 
 */
package rinde.sim.util.fsm;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Tables.unmodifiableTable;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;

/**
 * A simple state machine.
 * @param <E> The event type.
 * @param <C> The context type.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class StateMachine<E, C> {

    protected Table<State<E, C>, E, State<E, C>> transitionTable;
    protected State<E, C> currentState;
    protected final State<E, C> startState;

    StateMachine(State<E, C> start, Table<State<E, C>, E, State<E, C>> table) {
        startState = start;
        currentState = start;
        transitionTable = unmodifiableTable(table);
    }

    /**
     * Calls the {@link State#handle(Object, Object)} method to the current
     * {@link State}.
     * @param context
     */
    public void handle(C context) {
        handle(null, context);
    }

    public void handle(E event, C context) {
        if (event != null) {
            changeState(event, context);
        }
        final E newEvent = currentState.handle(event, context);
        if (newEvent != null) {
            handle(newEvent, context);
        }
    }

    protected void changeState(E event, C context) {
        checkArgument(transitionTable.contains(currentState, event), "The event "
                + event + " is not supported when in state " + currentState);
        final State<E, C> newState = transitionTable.get(currentState, event);
        if (newState != currentState) {
            // System.out.println(currentState + " + " + event + " = " +
            // newState);
            currentState.onExit(event, context);
            currentState = newState;
            currentState.onEntry(event, context);
        }
    }

    public State<E, C> getCurrentState() {
        return currentState;
    }

    public boolean stateIs(State<E, C> s) {
        return currentState == s;
    }

    public boolean stateIsOneOf(State<E, C>... states) {
        for (final State<E, C> s : states) {
            if (currentState == s) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the current state supports the event.
     * @param event
     * @return
     */
    public boolean isSupported(E event) {
        return transitionTable.contains(currentState, event);
    }

    public String toDot() {
        int id = 0;
        final StringBuilder string = new StringBuilder();
        string.append("digraph stategraph {\n");
        final Set<State<E, C>> allStates = newHashSet();
        allStates.addAll(transitionTable.rowKeySet());
        allStates.addAll(transitionTable.values());

        final Map<State<E, C>, Integer> idMap = newHashMap();
        for (final State<E, C> s : allStates) {
            string.append("node").append(id).append("[label=\"")
                    .append(s.name()).append("\"]\n");
            idMap.put(s, id);
            id++;
        }

        string.append("node").append(id).append("[label=\"\",shape=point]\n");
        string.append("node").append(id).append(" -> ").append("node")
                .append(idMap.get(startState)).append("\n");

        for (final Cell<State<E, C>, E, State<E, C>> cell : transitionTable
                .cellSet()) {
            final int id1 = idMap.get(cell.getRowKey());
            final int id2 = idMap.get(cell.getValue());
            string.append("node").append(id1).append(" -> ").append("node")
                    .append(id2).append("[label=\"")
                    .append(cell.getColumnKey()).append("\"]\n");
        }

        string.append("}");
        return string.toString();
    }

    public static <E, C> StateMachineBuilder<E, C> create(
            State<E, C> initialState) {
        return new StateMachineBuilder<E, C>(initialState);
    }

    public final static class StateMachineBuilder<E, C> {
        protected Table<State<E, C>, E, State<E, C>> table;
        protected State<E, C> start;

        private StateMachineBuilder(State<E, C> initialState) {
            table = HashBasedTable.create();
            start = initialState;
        }

        public StateMachineBuilder<E, C> addTransition(State<E, C> from,
                E event, State<E, C> to) {
            table.put(from, event, to);
            return this;
        }

        public StateMachine<E, C> build() {
            return new StateMachine<E, C>(start, table);
        }
    }

}
