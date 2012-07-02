/**
 * 
 */
package rinde.sim.core.graph;

/**
 * Exception that indicates that a path could not be found.
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 */
public class PathNotFoundException extends RuntimeException {

    private static final long serialVersionUID = -1605717570711159457L;

    /**
     * Create new exeception with specified error message.
     * @param string the error message.
     */
    public PathNotFoundException(String string) {
        super(string);
    }

}
