package rinde.sim.ui;

import org.eclipse.swt.SWT;

import com.google.common.collect.ImmutableMap;

/**
 * The set of menu items used in the GUI.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public enum MenuItems {
  /**
   * The play menu item, starts/stops the simulation.
   */
  PLAY,

  /**
   * Advances the simulation with one tick.
   */
  NEXT_TICK,

  /**
   * Increases the speed of the simulation.
   */
  INCREASE_SPEED,

  /**
   * Decreases the speed of the simulation.
   */
  DECREASE_SPEED,

  /**
   * Zoom in.
   */
  ZOOM_IN,

  /**
   * Zooms out.
   */
  ZOOM_OUT;

  /**
   * The default accelerators, designed for keyboards with a QWERTY layout.
   */
  public static final ImmutableMap<MenuItems, Integer> QWERTY_ACCELERATORS = ImmutableMap
      .<MenuItems, Integer> builder().put(PLAY, SWT.MOD1 + 'P')
      .put(NEXT_TICK, SWT.MOD1 + SWT.SHIFT + ']')
      .put(INCREASE_SPEED, SWT.MOD1 + ']').put(DECREASE_SPEED, SWT.MOD1 + '[')
      .put(ZOOM_IN, SWT.MOD1 + '+').put(ZOOM_OUT, SWT.MOD1 + '-').build();

}
