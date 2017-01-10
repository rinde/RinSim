/*
 * Copyright (C) 2011-2017 Rinde van Lon, imec-DistriNet, KU Leuven
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
package com.github.rinde.rinsim.core.model.comm;

import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

/**
 * A communication user. A communication user is an object that uses a
 * {@link CommDevice} for communicating with other instances.
 * @author Rinde van Lon
 */
public interface CommUser {
  /**
   * Should return the current position of the user or {@link Optional#absent()}
   * if it has none. This method should ideally be implemented efficiently as it
   * may be called many times (for example for broadcasts in a certain range).
   * Note that when {@link Optional#absent()} is returned the {@link CommDevice}
   * of this {@link CommUser} can not send message if the device has a range nor
   * can it receive messages that are send by devices with a range.
   * @return The current position.
   */
  Optional<Point> getPosition();

  /**
   * This method is called to inject a {@link CommDevice} into the user. The
   * specific device can be configured via {@link CommDeviceBuilder}. Note that
   * implementors of this method are required to create exactly one
   * {@link CommDevice}, failure to do so will result in an
   * {@link IllegalStateException}.
   * @param builder The builder for creating a {@link CommDevice}.
   */
  void setCommDevice(CommDeviceBuilder builder);
}
