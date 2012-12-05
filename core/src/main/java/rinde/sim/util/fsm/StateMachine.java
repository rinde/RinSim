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
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class StateMachine<T> {

    protected Table<State<T>, T, State<T>> transitionTable;
    protected State<T> currentState;
    protected final State<T> startState;

    StateMachine(State<T> start, Table<State<T>, T, State<T>> table) {
        startState = start;
        currentState = start;
        transitionTable = unmodifiableTable(table);
    }

    // transition
    public void fire(T event) {
        checkArgument(transitionTable.contains(currentState, event), "The event "
                + event + " is not supported when in state " + currentState);
        final State<T> newState = transitionTable.get(currentState, event);
        if (newState != currentState) {
            currentState.onExit(event);
            currentState = newState;
            currentState.onEntry(event);
        } else {
            currentState.onEvent(event);
        }
    }

    public String toDot() {
        int id = 0;
        final StringBuilder string = new StringBuilder();
        string.append("digraph stategraph {\n");
        final Set<State<T>> allStates = newHashSet();
        allStates.addAll(transitionTable.rowKeySet());
        allStates.addAll(transitionTable.values());

        final Map<State<T>, Integer> idMap = newHashMap();
        for (final State<T> s : allStates) {
            string.append("node").append(id).append("[label=\"")
                    .append(s.getClass().getSimpleName()).append("\"]\n");
            idMap.put(s, id);
            id++;
        }

        string.append("node").append(id).append("[label=\"\",shape=point]\n");
        string.append("node").append(id).append(" -> ").append("node")
                .append(idMap.get(startState)).append("\n");

        for (final Cell<State<T>, T, State<T>> cell : transitionTable.cellSet()) {
            final int id1 = idMap.get(cell.getRowKey());
            final int id2 = idMap.get(cell.getValue());
            string.append("node").append(id1).append(" -> ").append("node")
                    .append(id2).append("[label=\"")
                    .append(cell.getColumnKey()).append("\"]\n");
        }

        string.append("}");
        return string.toString();
    }

    public static <T> StateMachineBuilder<T> create(Class<T> eventType,
            State<T> initialState) {
        return new StateMachineBuilder<T>(initialState);
    }

    public interface State<E> {
        void onEntry(E event);

        void onEvent(E event);

        void onExit(E event);
    }

    public static class DefaultState<E> implements State<E> {
        @Override
        public void onEntry(E event) {}

        @Override
        public void onEvent(E event) {}

        @Override
        public void onExit(E event) {}
    }

    public final static class StateMachineBuilder<T> {
        protected Table<State<T>, T, State<T>> table;
        protected State<T> start;

        private StateMachineBuilder(State<T> initialState) {
            table = HashBasedTable.create();
            start = initialState;
        }

        public StateMachineBuilder<T> addTransition(State<T> from, State<T> to,
                T event) {
            table.put(from, event, to);
            return this;
        }

        public StateMachineBuilder<T> addSelfTransitionToAll(T event) {
            final Set<State<T>> union = newHashSet(table.rowKeySet());
            union.addAll(table.values());
            for (final State<T> s : union) {
                table.put(s, event, s);
            }
            return this;
        }

        public StateMachine<T> build() {
            return new StateMachine<T>(start, table);
        }
    }

}
