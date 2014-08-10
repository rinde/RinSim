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
