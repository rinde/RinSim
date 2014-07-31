package rinde.sim.util.cli;

import org.apache.commons.cli.Option;

public class ForwardingMenuOption<T> implements MenuOption<T> {
  protected final MenuOption<T> delegate;

  public ForwardingMenuOption(MenuOption<T> deleg) {
    delegate = deleg;
  }

  @Override
  public String getShortName() {
    return delegate.getShortName();
  }

  @Override
  public String getLongName() {
    return delegate.getLongName();
  }

  @Override
  public Option createOption(T builder) {
    return delegate.createOption(builder);
  }

  @Override
  public boolean execute(T builder, Value value) {
    return delegate.execute(builder, value);
  }
}
