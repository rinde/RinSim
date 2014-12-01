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
package com.github.rinde.rinsim.examples.pdptw.gradientfield;

import com.github.rinde.rinsim.core.model.pdp.PDPModel.ParcelState;
import com.github.rinde.rinsim.core.pdptw.DefaultParcel;
import com.github.rinde.rinsim.core.pdptw.ParcelDTO;
import com.github.rinde.rinsim.geom.Point;

public class GFParcel extends DefaultParcel implements FieldEmitter {
  private Point pos;

  public GFParcel(ParcelDTO pDto) {
    super(pDto);
    this.pos = pDto.pickupLocation;
  }

  @Override
  public void setModel(GradientModel model) {}

  @Override
  public Point getPosition() {
    return this.pos;
  }

  @Override
  public float getStrength() {
    return getPDPModel().getParcelState(this) == ParcelState.AVAILABLE ? 3.0f
        : 0.0f;
  }
}
