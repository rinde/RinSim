package rinde.sim.examples.fabrirecht.gradientField;

import rinde.sim.core.graph.Point;

public interface FieldEmitter {

  public void setModel(GradientModel model);

  public Point getPosition();

  public float getStrength();
}
