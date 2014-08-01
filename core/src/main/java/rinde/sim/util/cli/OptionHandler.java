package rinde.sim.util.cli;

public interface OptionHandler<T, U> {

  boolean execute(T ref, Value<U> value);
}
