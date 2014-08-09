package com.github.rinde.rinsim.examples.pdptw.gradientfield;

import com.github.rinde.rinsim.core.graph.Point;

public interface FieldEmitter {

  public void setModel(GradientModel model);

  public Point getPosition();

  public float getStrength();
}
