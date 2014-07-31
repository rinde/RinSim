package rinde.sim.util.cli;

import org.apache.commons.cli.Option;

// TODO convert to abstract method to hide create() and maybe also execute()
// methods?
public interface ICliOption<T> {
  boolean execute(T ref, Value value);

  String getShortName();

  String getLongName();

  // should not be called by application code
  Option create();

}
