package rinde.sim.examples.fabrirecht.gradientField;

import java.util.List;
import java.util.Map;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.ModelProvider;
import rinde.sim.ui.renderers.ModelRenderer;
import rinde.sim.ui.renderers.ViewPort;
import rinde.sim.ui.renderers.ViewRect;

public class GradientFieldRenderer implements ModelRenderer{
	
	protected final static RGB GREEN = new RGB(0, 255, 0);
	protected final static RGB RED = new RGB(255, 0, 0);

	protected GradientModel gradientModel;
	
	@Override
	public void renderStatic(GC gc, ViewPort vp) {}

	@Override
	public void renderDynamic(GC gc, ViewPort vp, long time) {
		final List<Truck> trucks = gradientModel.getTruckEmitters();
		
		synchronized (trucks) {
			for (final Truck t : trucks) {
				
				Point tp = t.getPosition();
				
				final Map<Point, Float> fields = t.getFields();
				
				float max = Float.NEGATIVE_INFINITY;
				float min = Float.POSITIVE_INFINITY;
				
				for(Point p:fields.keySet()){
					max = Math.max(max, fields.get(p));
					min = Math.min(min, fields.get(p));
				}
				
				int dia;
				RGB color = null;
				for(Point p:fields.keySet()){
					final int x = vp.toCoordX(tp.x + 6 * p.x);
					final int y = vp.toCoordY(tp.y + 6 * p.y);
					final float field = fields.get(p);
					
					if( field < 0 ){
						dia = (int) (field / -min * 6);
						color = RED;
					}
					else{
						dia = (int) (field / max * 6);
						color = GREEN;
					}
					
					gc.setBackground(new Color(gc.getDevice(), color));
					gc.fillOval(x, y, dia, dia);
				}
			}
		}
		
	}

	@Override
	public ViewRect getViewRect() {
		return null;
	}

	@Override
	public void registerModelProvider(ModelProvider mp) {
		gradientModel = mp.getModel(GradientModel.class);
	}

}
