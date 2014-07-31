package rinde.sim.util.cli;

import javax.annotation.Nullable;

import com.google.common.base.Optional;

public class CliException extends RuntimeException {
  private static final long serialVersionUID = -7434606684541234080L;
  private final Optional<MenuOption<?>> menuOption;

  public CliException(String msg, Throwable cause) {
    this(msg, cause, null);
  }

  public CliException(String msg, Throwable cause, @Nullable MenuOption<?> opt) {
    super(msg, cause);
    menuOption = Optional.<MenuOption<?>> fromNullable(opt);
  }

  /**
   * @return The {@link MenuOption} where the exception occurred.
   */
  public MenuOption<?> getMenuOption() {
    return menuOption.get();
  }

  public boolean hasMenuOption() {
    return menuOption.isPresent();
  }
}
