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
  private final Optional<Option> menuOption;
  private final CauseType causeType;

  CliException(String msg, CauseType cause) {
    this(msg, null, cause, null);
  }

  CliException(String msg, CauseType type, @Nullable Option opt) {
    this(msg, null, type, opt);
  }

  CliException(String msg, @Nullable Throwable cause, CauseType type,
      @Nullable Option opt) {
    super(msg, cause);
    menuOption = Optional.<Option> fromNullable(opt);
    causeType = type;
  }

  /**
   * @return The {@link Option} where the exception occurred, or
   *         {@link Optional#absent()} if there is no {@link Option} responsible
   *         for this exception.
   */
  public Optional<Option> getMenuOption() {
    checkState(menuOption.isPresent(), "'%s' has no reference to an option.",
        toString());
    return menuOption;
  }

  /**
   * @return The {@link CauseType} of this exception.
   */
  public CauseType getCauseType() {
    return causeType;
  }

  /**
   * Collection of causes of command-line interface exceptions.
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  public enum CauseType {
    /**
     * An argument of an option is missing which should have been defined.
     */
    MISSING_ARG,

    /**
     * An argument of an option was found where none was expected.
     */
    UNEXPECTED_ARG,

    /**
     * This option has already been selected.
     */
    ALREADY_SELECTED,

    /**
     * Something went wrong during parsing.
     */
    PARSE_EXCEPTION,

    /**
     * The number format is invalid.
     */
    INVALID_ARG_FORMAT,

    /**
     * An error has occurred during execution of the {@link ArgHandler}.
     */
    INVALID;
  }
}
