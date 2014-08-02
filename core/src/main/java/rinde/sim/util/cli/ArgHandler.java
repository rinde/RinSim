package rinde.sim.util.cli;

import com.google.common.base.Optional;

public interface ArgHandler<S, V> {

  void execute(S subject, Optional<V> value);
}
