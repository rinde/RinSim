package rinde.sim.core.graph;

/**
 * Simple interface to represent data associated to a {@link Connection} in a
 * {@link Graph}.
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * @since 2.0
 */
public interface ConnectionData {

    /**
     * This method can be implemented to override the default length (euclidean
     * distance).
     * @return The length of the {@link Connection}.
     */
    double getLength();
}
