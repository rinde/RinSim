package rinde.sim.scenario;

/**
 * Configuration exception.
 * TODO add comments
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * @since 2.0
 */
public class ConfigurationException extends Exception {

	public ConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}

	public ConfigurationException(String message) {
		super(message);
	}

}
