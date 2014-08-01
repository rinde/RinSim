package rinde.sim.util.cli;

import static com.google.common.base.Preconditions.checkState;

import javax.annotation.Nullable;

import com.google.common.base.Optional;

/**
 * Exception indicating a problem with the command-line interface.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class CliException extends RuntimeException {
  private static final long serialVersionUID = -7434606684541234080L;
  private final Optional<CliOption> menuOption;
  private final CauseType causeType;

  /**
   * Construct a new exception.
   * @param msg The message.
   * @param cause The cause.
   */
  public CliException(String msg, CauseType cause) {
    this(msg, null, cause, null);
  }

  public CliException(String msg, CauseType type, @Nullable CliOption opt) {
    this(msg, null, type, opt);
  }

  /**
   * Construct a new exception with a {@link ICliOption}.
   * @param msg The message.
   * @param cause The cause.
   * @param opt The menu option.
   */
  public CliException(String msg, @Nullable Throwable cause, CauseType type,
      @Nullable CliOption opt) {
    super(msg, cause);
    menuOption = Optional.<CliOption> fromNullable(opt);
    causeType = type;
  }

  /**
   * @return The {@link ICliOption} where the exception occurred.
   * @throws IllegalStateException If there is no {@link ICliOption} responsible
   *           for this exception.
   * @see #hasMenuOption()
   */
  public CliOption getMenuOption() throws IllegalStateException {
    checkState(menuOption.isPresent(), "'%s' has no reference to an option.",
        toString());
    return menuOption.get();
  }

  /**
   * @return <code>true</code> in case there is a {@link ICliOption} responsible
   *         for this exception, <code>false</code> otherwise.
   */
  public boolean hasMenuOption() {
    return menuOption.isPresent();
  }

  public CauseType getCauseType() {
    return causeType;
  }

  public enum CauseType {

    MISSING_ARG, ALREADY_SELECTED, PARSE_EXCEPTION, INVALID_NUMBER_FORMAT, INVALID;

  }
}
