package rinde.sim.core.model.virtual;

import rinde.sim.core.graph.Point;

public interface VirtualEntity {
	
	public void init(GradientFieldAPI api);

	public boolean isEmitting();
	
	public Point getPosition();
	
	public FieldData getFieldData();
	
}
