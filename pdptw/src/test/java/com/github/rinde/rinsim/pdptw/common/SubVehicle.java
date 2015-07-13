/*
 * Copyright (C) 2011-2015 Rinde van Lon, iMinds-DistriNet, KU Leuven
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
package com.github.rinde.rinsim.pdptw.common;

import javax.annotation.Nullable;

import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.fsm.StateMachine;

class SubVehicle extends RouteFollowingVehicle {
  final ExtraState extraState;

  SubVehicle(VehicleDTO pDto, boolean allowDelayedRouteChanging) {
    super(pDto, allowDelayedRouteChanging);
    extraState = stateMachine.getStateOfType(ExtraState.class);
  }

  @Override
  protected StateMachine<StateEvent, RouteFollowingVehicle> createStateMachine() {
    // when overriding, please see doc of super method!

    // reuse super implementation
    final StateMachine<StateEvent, RouteFollowingVehicle> fsm =
        super.createStateMachine();

    // get ref to existing state
    final Wait wait = fsm.getStateOfType(Wait.class);

    // add new state
    final ExtraState extra = new ExtraState();

    // add two new transitions
    return StateMachine.create(wait)
        .addTransition(wait, ExtraEvent.TEST_EVENT, extra)
        .addTransition(extra, DefaultEvent.DONE, wait)
        .addTransitionsFrom(fsm)
        .build();
  }

  enum ExtraEvent implements StateEvent {
    TEST_EVENT;
  }

  class ExtraState extends AbstractTruckState {
    long startTime;

    @Override
    public void onEntry(StateEvent event, RouteFollowingVehicle context) {
      startTime = context.currentTime.get().getTime();
    }

    @Override
    @Nullable
    public StateEvent handle(@Nullable StateEvent event,
        RouteFollowingVehicle context) {
      // stay in this state for 1000ms then go back to wait
      currentTime.get().consumeAll();
      if (currentTime.get().getTime() - startTime > 1000) {
        return DefaultEvent.DONE;
      }
      return null;
    }
  }
}
