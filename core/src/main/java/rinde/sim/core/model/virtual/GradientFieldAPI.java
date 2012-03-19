package rinde.sim.core.model.virtual;


import java.util.Collection;

import rinde.sim.core.graph.Point;

public interface GradientFieldAPI {

	public Collection<Field> getFields(Point point);
	
	public Collection<Field> getSimpleFields(Point point);
}
