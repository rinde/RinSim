/**
 * 
 */
package com.github.rinde.rinsim.ui.renderers;

import org.eclipse.swt.widgets.Composite;

/**
 * Using {@link PanelRenderer} a user interface panel can be defined. This panel
 * can be positioned alongside the canvas (which is the main view of the
 * simulation) to show additional information.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public interface PanelRenderer extends Renderer {

  /**
   * Sets the parent {@link Composite} of this panel. Through this method
   * {@link org.eclipse.swt.widgets.Control}s can be added to the panel.
   * @param parent The {@link Composite} which acts as parent of the panel.
   */
  void initializePanel(Composite parent);

  /**
   * Returns the preferred size, in pixels, of this panel. In case the position
   * of this panel is either <code>SWT.TOP</code> or <code>SWT.BOTTOM</code>
   * this values is interpreted as the preferred <i>height</i>. Else, if the
   * position is either <code>SWT.LEFT</code> or <code>SWT.RIGHT</code> it is
   * interpreted as the preferred <i>width</i>.
   * @return The preferred size in pixels.
   */
  int preferredSize();

  /**
   * Returns the preferred position of this panel relative to the canvas.
   * @return Possible values:
   *         <ul>
   *         <li><code>SWT.TOP</code></li>
   *         <li><code>SWT.BOTTOM</code></li>
   *         <li><code>SWT.LEFT</code></li>
   *         <li><code>SWT.RIGHT</code></li>
   *         </ul>
   */
  int getPreferredPosition();

  /**
   * @return The name of the panel.
   */
  String getName();

  /**
   * Signals that the panel can updates its contents.
   */
  void render();

}
