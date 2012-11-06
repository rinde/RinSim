/**
 * 
 */
package rinde.sim.util.spec;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public interface ISpecification<T> {

    boolean isSatisfiedBy(T context);

    ISpecification<T> and(ISpecification<T> other);

    ISpecification<T> or(ISpecification<T> other);

    ISpecification<T> not();

}
