package rinde.sim.examples.fabrirecht.gradientField;

import java.util.ArrayList;
import java.util.List;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.Model;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.Parcel;

public class GradientModel implements Model<FieldEmitter>{

	private List<FieldEmitter> emitters = new ArrayList<FieldEmitter>();
	private int minX;
	private int maxX;
	private int minY;
	private int maxY;
	private PDPModel pdpModel;
	
	public GradientModel(int minX, int maxX, int minY, int maxY, PDPModel pdpModel){
		this.minX = minX;
		this.maxX = maxX;
		this.minY = minY;
		this.maxY = maxY;
		this.pdpModel = pdpModel;
	}
	
	/**
	 * Possibilities
	 * (-1,1)	(0,1)	(1,1)
	 * (-1,0)			(1,0
	 * (-1,-1)	(0,-1)	(1,-1)
	 */
	private final int[] x = {	-1,	0, 	1, 	1, 	1, 	0, 	-1,	-1	};
	private final int[] y = {	1,	1,	1,	0,	-1,	-1,	-1,	0	};
	
	
	public Point getTargetFor(Truck element){
		float maxField = Float.NEGATIVE_INFINITY;
		Point maxFieldPoint = null;
		
		
		for(int i = 0;i < x.length;i++){
			Point p = new Point(element.getPosition().x + x[i], element.getPosition().y + y[i]);
			
			if( p.x < minX || p.x > maxX || p.y < minY || p.y > maxY)
				continue;
			
			float field = getField(p, element);
			if(field >= maxField){
				maxField = field;
				maxFieldPoint = p;
			}
		}
		
		return maxFieldPoint;
	}
	
	public float getField(Point in, Truck truck){
		float field = 0.0f;
		for(FieldEmitter emitter:emitters){
			field += emitter.getStrength() / Point.distance(emitter.getPosition(),in);
		}

		for(Parcel p:pdpModel.getContents(truck)){
			field += 2 / Point.distance(p.getDestination(), in);
		}
		return field;
	}

	@Override
	public boolean register(FieldEmitter element) {
		emitters.add(element);
		element.setModel(this);
		return true;
	}

	@Override
	public boolean unregister(FieldEmitter element) {
		emitters.remove(element);
		return false;
	}

	@Override
	public Class<FieldEmitter> getSupportedType() {
		return FieldEmitter.class;
	}
}
