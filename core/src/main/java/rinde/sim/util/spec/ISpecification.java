/**
 * 
 */
package rinde.sim.util.spec;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public interface ISpecification<T, U extends ISpecification<T, U>> {

    boolean isSatisfiedBy(T context);

    U and(U other);

    U or(U other);

    U not();

}
