/**
 * 
 */
package rinde.sim.util.spec;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public abstract class CompositeSpecification<T> implements ISpecification<T> {

    @Override
    public abstract boolean isSatisfiedBy(T context);

    @Override
    public ISpecification<T> and(ISpecification<T> other) {
        return new AndSpecification<T>(this, other);
    }

    @Override
    public ISpecification<T> or(ISpecification<T> other) {
        return new OrSpecification<T>(this, other);
    }

    @Override
    public ISpecification<T> not() {
        return new NotSpecification<T>(this);
    }

    public final class AndSpecification<T> extends CompositeSpecification<T> {
        private final ISpecification<T> spec1;
        private final ISpecification<T> spec2;

        AndSpecification(ISpecification<T> one, ISpecification<T> other) {
            spec1 = one;
            spec2 = other;
        }

        @Override
        public boolean isSatisfiedBy(T context) {
            return spec1.isSatisfiedBy(context) && spec2.isSatisfiedBy(context);
        }
    }

    public final class OrSpecification<T> extends CompositeSpecification<T> {
        private final ISpecification<T> spec1;
        private final ISpecification<T> spec2;

        OrSpecification(ISpecification<T> one, ISpecification<T> other) {
            spec1 = one;
            spec2 = other;
        }

        @Override
        public boolean isSatisfiedBy(T context) {
            return spec1.isSatisfiedBy(context) || spec2.isSatisfiedBy(context);
        }
    }

    public final class NotSpecification<T> extends CompositeSpecification<T> {
        private final ISpecification<T> spec;

        NotSpecification(ISpecification<T> specification) {
            spec = specification;
        }

        @Override
        public boolean isSatisfiedBy(T context) {
            return !spec.isSatisfiedBy(context);
        }
    }

}
