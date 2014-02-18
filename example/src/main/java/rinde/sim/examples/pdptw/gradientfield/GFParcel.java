package rinde.sim.examples.fabrirecht.gradientfield;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.PDPModel.ParcelState;
import rinde.sim.pdptw.common.DefaultParcel;
import rinde.sim.pdptw.common.ParcelDTO;

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
