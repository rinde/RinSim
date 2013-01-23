/**
 * 
 */
package rinde.sim.ui.renderers;

import org.eclipse.swt.widgets.Composite;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public interface PanelRenderer extends Renderer {

	void setParent(Composite c);

	int preferredSize();

	// preferred position: LEFT, RIGHT, TOP, BOTTOM,

	// name

	int getPreferredPosition();

	String getName();

}
