/*
 * Copyright (C) 2011-2018 Rinde R.S. van Lon
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
package com.github.rinde.rinsim.core.model.time;

/**
 * Allows an implementor to receive updates when time progresses in the
 * simulator.
 *
 * @author Rinde van Lon
 * @author Bartosz Michalik
 */
public interface TickListener {

  /**
   * Is called when time has progressed a single 'tick' (time step). The
   * provided {@link TimeLapse} object provides information about the current
   * time. Further, an implementor can 'use' the provided time to perform
   * actions. Actions are methods that specify an operation (usually on a model)
   * that takes time. The {@link TimeLapse} reference that is received via this
   * method can be used to spent on these time consuming actions. <br>
   * <br>
   * Note:<b> a reference to the {@link TimeLapse} object should never be
   * kept</b>. The time lapse object will be consumed by default after this
   * method is finished.
   * @param timeLapse The time lapse that is handed to this object.
   */
  void tick(TimeLapse timeLapse);

  /**
   * Is called after all {@link TickListener}s have received their call to
   * {@link #tick(TimeLapse)}. This can be used to specify operations which must
   * be explicitly executed after the regular tick. Note that the received
   * {@link TimeLapse} object will be entirely consumed. This means that no time
   * consuming actions can be done in the implementation of this method.
   * @param timeLapse The time lapse that is handed to this object.
   */
  void afterTick(TimeLapse timeLapse);
}
