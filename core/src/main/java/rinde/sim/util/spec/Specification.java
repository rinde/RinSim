/**
 * 
 */
package rinde.sim.util.spec;

/**
 * A specification implementation <i>inspired</i> by the <a
 * href="https://en.wikipedia.org/wiki/Specification_pattern">specification
 * pattern</a>. It can be used to easily recombine specifications (i.e.
 * conditions) into arbitrary complex specifications.
 * <p>
 * <b>Example:</b>
 * 
 * <pre>
 * ISpecification&lt;Object&gt; TRUE = new ISpecification&lt;Object&gt;() {
 *   &#064;Override
 *   public boolean isSatisfiedBy(Object context) {
 *     return true;
 *   }
 * };
 * ISpecification&lt;Object&gt; IS_NULL = new ISpecification&lt;Object&gt;() {
 *   &#064;Override
 *   public boolean isSatisfiedBy(Object context) {
 *     return context == null;
 *   }
 * };
 * Specification.of(TRUE).not().orNot(IS_NULL).and(TRUE).build()
 *     .isSatisfiedBy(null);
 * </pre>
 * 
 * The above example only returns <code>true</code> when
 * {@link ISpecification#isSatisfiedBy(Object)} is supplied with a non-null
 * object.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public final class Specification {

  private Specification() {}

  /**
   * Start creating a new {@link ISpecification} instance using the specified
   * instance as a base.
   * @param base The {@link ISpecification} to use as base.
   * @return A reference to a builder for creating {@link ISpecification}
   *         instances.
   */
  public static <T> Builder<T> of(ISpecification<T> base) {
    return new Builder<T>(base);
  }

  /**
   * A specification or condition.
   * @param <T> The type that is used in the {@link #isSatisfiedBy(Object)}
   *          method.
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  public interface ISpecification<T> {
    /**
     * @param context The context to inspect.
     * @return <code>true</code> if this specification is satisfied by the
     *         specified context object, <code>false</code> otherwise.
     */
    boolean isSatisfiedBy(T context);
  }

  /**
   * A builder for recombining {@link ISpecification}s.
   * @param <T> The type of the {@link ISpecification}.
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  public static final class Builder<T> {
    private ISpecification<T> specification;

    Builder(ISpecification<T> spec) {
      this.specification = spec;
    }

    /**
     * Recombines the current specification with the specified spec using the
     * logical AND operator.
     * @param spec The specification to add.
     * @return This builder.
     */
    public Builder<T> and(ISpecification<T> spec) {
      specification = new AndSpecification<T>(specification, spec);
      return this;
    }

    /**
     * First applies the NOT operator to the specified spec and then recombines
     * it with the current spec using the logical AND operator.
     * @param spec The specification to add.
     * @return This builder.
     */
    public Builder<T> andNot(ISpecification<T> spec) {
      return and(new NotSpecification<T>(spec));
    }

    /**
     * Recombines the current specification with the specified spec using the
     * logical OR operator.
     * @param spec The specification to add.
     * @return This builder.
     */
    public Builder<T> or(ISpecification<T> spec) {
      specification = new OrSpecification<T>(specification, spec);
      return this;
    }

    /**
     * First applies the NOT operator to the specified spec and then recombines
     * it with the current spec using the logical OR operator.
     * @param spec The specification to add.
     * @return This builder.
     */
    public Builder<T> orNot(ISpecification<T> spec) {
      return or(new NotSpecification<T>(spec));
    }

    /**
     * Recombines the current specification with the specified spec using the
     * logical XOR operator.
     * @param spec The specification to add.
     * @return This builder.
     */
    public Builder<T> xor(ISpecification<T> spec) {
      specification = new XorSpecification<T>(specification, spec);
      return this;
    }

    /**
     * Negates the current specification.
     * @return This builder.
     */
    public Builder<T> not() {
      specification = new NotSpecification<T>(specification);
      return this;
    }

    /**
     * @return The new specification.
     */
    public ISpecification<T> build() {
      return specification;
    }
  }

  final static class AndSpecification<T> implements ISpecification<T> {
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

  final static class OrSpecification<T> implements ISpecification<T> {
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

  final static class NotSpecification<T> implements ISpecification<T> {
    private final ISpecification<T> spec;

    NotSpecification(ISpecification<T> specification) {
      spec = specification;
    }

    @Override
    public boolean isSatisfiedBy(T context) {
      return !spec.isSatisfiedBy(context);
    }
  }

  final static class XorSpecification<T> implements ISpecification<T> {
    private final ISpecification<T> spec1;
    private final ISpecification<T> spec2;

    XorSpecification(ISpecification<T> one, ISpecification<T> other) {
      spec1 = one;
      spec2 = other;
    }

    @Override
    public boolean isSatisfiedBy(T context) {
      return spec1.isSatisfiedBy(context) != spec2.isSatisfiedBy(context);
    }
  }

}
