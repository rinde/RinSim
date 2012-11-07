/**
 * 
 */
package rinde.sim.util.spec;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public abstract class CompositeSpecification<T, U extends ISpecification<T, U>>
        implements ISpecification<T, U> {

    @Override
    public abstract boolean isSatisfiedBy(T context);

    @Override
    public U and(U other) {
        return wrap(new AndSpecification<T, U>((U) this, other));
    }

    @Override
    public U or(U other) {
        return wrap(new OrSpecification<T, U>((U) this, other));
    }

    @Override
    public U not() {
        return wrap(new NotSpecification<T, U>((U) this));
    }

    protected abstract U wrap(ISpecification<T, U> spec);

    public final class AndSpecification<T, U extends ISpecification<T, U>>
            extends CompositeSpecification<T, U> {

        private final U spec1;
        private final U spec2;

        AndSpecification(U one, U other) {
            spec1 = one;
            spec2 = other;
        }

        @Override
        public boolean isSatisfiedBy(T context) {
            return spec1.isSatisfiedBy(context) && spec2.isSatisfiedBy(context);
        }

        @Override
        protected U wrap(ISpecification<T, U> spec) {
            return null;
        }
    }

    public final class OrSpecification<T, U extends ISpecification<T, U>>
            extends CompositeSpecification<T, U> {
        private final U spec1;
        private final U spec2;

        OrSpecification(U one, U other) {
            spec1 = one;
            spec2 = other;
        }

        @Override
        public boolean isSatisfiedBy(T context) {
            return spec1.isSatisfiedBy(context) || spec2.isSatisfiedBy(context);
        }

        @Override
        protected U wrap(ISpecification<T, U> spec) {
            return null;
        }
    }

    public final class NotSpecification<T, U extends ISpecification<T, U>>
            extends CompositeSpecification<T, U> {
        private final U spec;

        NotSpecification(U specification) {
            spec = specification;
        }

        @Override
        public boolean isSatisfiedBy(T context) {
            return !spec.isSatisfiedBy(context);
        }

        @Override
        protected U wrap(ISpecification<T, U> spec) {
            return null;
        }
    }

}
