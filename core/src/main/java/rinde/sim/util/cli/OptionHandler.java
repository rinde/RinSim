package rinde.sim.util.cli;

import com.google.common.base.Optional;

public interface OptionHandler<T, U> {

  void execute(T ref, Optional<U> value);
}
