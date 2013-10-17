/**
 * 
 */
package rinde.sim.util.fsm;

import javax.annotation.Nullable;

/**
 * Default empty implementation of state. Subclasses only need to implement the
 * {@link AbstractState#handle(Object, Object)} method.
 * @param <E> The event type, see {@link StateMachine} for more information.
 * @param <C> The context type, see {@link StateMachine} for more information.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public abstract class AbstractState<E, C> implements State<E, C> {

  @Override
  public String name() {
    return getClass().getName();
  }

  @Nullable
  @Override
  public abstract E handle(@Nullable E event, C context);

  @Override
  public void onEntry(E event, C context) {}

  @Override
  public void onExit(E event, C context) {}

}
