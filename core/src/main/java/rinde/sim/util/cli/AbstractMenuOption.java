package rinde.sim.util.cli;


public abstract class AbstractMenuOption<T> implements MenuOption<T> {

  private final String shortName;
  private final String longName;

  protected AbstractMenuOption(String sn, String ln) {
    shortName = sn;
    longName = ln;
  }

  @Override
  public String getShortName() {
    return shortName;
  }

  @Override
  public String getLongName() {
    return longName;
  }
}
