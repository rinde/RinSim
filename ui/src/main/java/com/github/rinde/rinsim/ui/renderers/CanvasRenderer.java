package com.github.rinde.rinsim.ui.renderers;

import javax.annotation.Nullable;

import org.eclipse.swt.graphics.GC;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public interface CanvasRenderer extends Renderer {

  // FIXME documentation!

  void renderStatic(GC gc, ViewPort vp);

  void renderDynamic(GC gc, ViewPort vp, long time);

  @Nullable
  ViewRect getViewRect();

}
