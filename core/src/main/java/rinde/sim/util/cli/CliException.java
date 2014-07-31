package rinde.sim.util.cli;

import javax.annotation.Nullable;

import com.google.common.base.Optional;

/**
 * Exception indicating a problem with the command-line interface.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class CliException extends RuntimeException {
  private static final long serialVersionUID = -7434606684541234080L;
  private final Optional<ICliOption<?>> menuOption;

  /**
   * Construct a new exception.
   * @param msg The message.
   * @param cause The cause.
   */
  public CliException(String msg, Throwable cause) {
    this(msg, cause, null);
  }

  /**
   * Construct a new exception with a {@link ICliOption}.
   * @param msg The message.
   * @param cause The cause.
   * @param opt The menu option.
   */
  public CliException(String msg, Throwable cause, @Nullable ICliOption<?> opt) {
    super(msg, cause);
    menuOption = Optional.<ICliOption<?>> fromNullable(opt);
  }

  /**
   * @return The {@link ICliOption} where the exception occurred.
   * @throws IllegalStateException If there is no {@link ICliOption} responsible
   *           for this exception.
   * @see #hasMenuOption()
   */
  public ICliOption<?> getMenuOption() throws IllegalStateException {
    return menuOption.get();
  }

  /**
   * @return <code>true</code> in case there is a {@link ICliOption} responsible
   *         for this exception, <code>false</code> otherwise.
   */
  public boolean hasMenuOption() {
    return menuOption.isPresent();
  }
}
