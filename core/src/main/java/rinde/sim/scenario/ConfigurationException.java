package rinde.sim.scenario;

/**
 * Configuration exception. Used to signal problems with setting up an
 * experiment (via {@link ScenarioController})
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * @since 2.0
 */
public class ConfigurationException extends Exception {

  private static final long serialVersionUID = -7811526104730249976L;

  public ConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }

  public ConfigurationException(String message) {
    super(message);
  }

}
