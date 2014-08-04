package rinde.sim.util.cli;

/**
 * Implementations should handle the activation of an option.
 * @param <S> The type of subject this handler expects.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public interface NoArgHandler<S> {
  /**
   * Is called when an option is activated.
   * @param subject The subject of the handler.
   */
  void execute(S subject);
}
