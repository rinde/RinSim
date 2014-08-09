package com.github.rinde.rinsim.pdptw.common;

import javax.annotation.Nullable;

import com.github.rinde.rinsim.core.pdptw.VehicleDTO;
import com.github.rinde.rinsim.pdptw.common.RouteFollowingVehicle;
import com.github.rinde.rinsim.util.fsm.StateMachine;

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
    final StateMachine<StateEvent, RouteFollowingVehicle> fsm = super
        .createStateMachine();

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
