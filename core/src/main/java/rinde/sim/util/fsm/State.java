package rinde.sim.util.fsm;

public interface State<E, C> {

    String name();

    E handle(E event, C context);

    void onEntry(E event, C context);

    void onExit(E event, C context);
}
