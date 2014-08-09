/**
 * 
 */
package com.github.rinde.rinsim.core.model;

/**
 * Implementors of this interface will receive a reference to a
 * {@link ModelProvider} which can be used to gain access to available
 * {@link Model}s.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public interface ModelReceiver {

  /**
   * Via this method the {@link ModelProvider} is injected.
   * @param mp The {@link ModelProvider} to inject.
   */
  void registerModelProvider(ModelProvider mp);
}
