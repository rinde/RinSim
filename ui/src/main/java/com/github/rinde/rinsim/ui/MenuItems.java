/*
 * Copyright (C) 2011-2014 Rinde van Lon, iMinds DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rinde.rinsim.ui;

import org.eclipse.swt.SWT;

import com.google.common.collect.ImmutableMap;

/**
 * The set of menu items used in the GUI.
 * @author Rinde van Lon 
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
  public final static ImmutableMap<MenuItems, Integer> QWERTY_ACCELERATORS = ImmutableMap
      .<MenuItems, Integer> builder().put(PLAY, SWT.MOD1 + 'P')
      .put(NEXT_TICK, SWT.MOD1 + SWT.SHIFT + ']')
      .put(INCREASE_SPEED, SWT.MOD1 + ']').put(DECREASE_SPEED, SWT.MOD1 + '[')
      .put(ZOOM_IN, SWT.MOD1 + '+').put(ZOOM_OUT, SWT.MOD1 + '-').build();

  /**
   * Accelerators designed for keyboards with a AZERTY layout.
   */
  public final static ImmutableMap<MenuItems, Integer> AZERTY_ACCELERATORS = ImmutableMap
      .<MenuItems, Integer> builder().put(MenuItems.PLAY, SWT.MOD1 + 'P')
      .put(MenuItems.NEXT_TICK, SWT.MOD1 + SWT.SHIFT + '$')
      .put(MenuItems.INCREASE_SPEED, SWT.MOD1 + '$')
      .put(MenuItems.DECREASE_SPEED, SWT.MOD1 + '^')
      .put(MenuItems.ZOOM_IN, SWT.MOD1 + '+')
      .put(MenuItems.ZOOM_OUT, SWT.MOD1 + '-').build();
}
