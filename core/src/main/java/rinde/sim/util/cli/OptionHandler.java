package rinde.sim.util.cli;

public interface OptionHandler<T> {

  boolean execute(T ref, Value value);
}
