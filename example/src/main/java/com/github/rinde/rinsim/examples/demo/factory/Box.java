/*
 * Copyright (C) 2011-2016 Rinde van Lon, iMinds-DistriNet, KU Leuven
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
package com.github.rinde.rinsim.examples.demo.factory;

import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.examples.demo.factory.AgvModel.BoxHandle;
import com.github.rinde.rinsim.geom.Point;

class Box extends Parcel {
  final BoxHandle boxHandle;

  Box(Point o, Point d, long duration, BoxHandle bh) {
    super(
        Parcel.builder(o, d)
            .serviceDuration(duration)
            .neededCapacity(1d)
            .buildDTO());
    boxHandle = bh;
  }

}
