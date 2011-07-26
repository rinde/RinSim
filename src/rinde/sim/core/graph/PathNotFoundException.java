/**
 * 
 */
package rinde.sim.core.graph;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class PathNotFoundException extends RuntimeException {

	private static final long serialVersionUID = -1605717570711159457L;

	public PathNotFoundException(String string) {
		super(string);
	}

}
