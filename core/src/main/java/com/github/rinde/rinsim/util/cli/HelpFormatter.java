package com.github.rinde.rinsim.util.cli;

/**
 * Implementations should create a formatted string containing all information
 * about a menu.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public interface HelpFormatter {

  /**
   * Creates a formatted help string.
   * @param menu The menu to create the help for.
   * @return A formatted string containing all help information of the specified
   *         menu.
   */
  String format(Menu menu);

}
