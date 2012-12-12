/**
 * 
 */
package rinde.sim.ui.renderers;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.ModelProvider;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.PDPModel.ParcelState;
import rinde.sim.core.model.pdp.PDPModel.VehicleState;
import rinde.sim.core.model.pdp.Parcel;
import rinde.sim.core.model.pdp.Vehicle;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.core.model.road.RoadUser;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class PDPModelRenderer implements ModelRenderer {
	protected static final RGB BLACK = new RGB(0, 0, 0);
	protected static final RGB WHITE = new RGB(255, 255, 255);
	protected static final RGB GRAY = new RGB(80, 80, 80);
	protected static final RGB LIGHT_GRAY = new RGB(205, 201, 201);
	protected static final RGB DARK_GREEN = new RGB(0, 200, 0);
	protected static final RGB GREEN = new RGB(0, 255, 0);
	protected static final RGB ORANGE = new RGB(255, 160, 0);
	protected static final RGB BLUE = new RGB(0, 0, 255);
	protected static final RGB FOREGROUND_INFO = WHITE;
	protected static final RGB BACKGROUND_INFO = BLUE;

	protected PDPModel pdpModel;
	protected RoadModel roadModel;

	@Override
	public void renderStatic(GC gc, ViewPort vp) {}

	@Override
	public void renderDynamic(GC gc, ViewPort vp, long time) {

		synchronized (pdpModel) {
			final Map<RoadUser, Point> posMap = roadModel.getObjectsAndPositions();
			final Set<Vehicle> vehicles = pdpModel.getVehicles();

			for (final Vehicle v : vehicles) {
				if (posMap.containsKey(v)) {
					final Point p = posMap.get(v);
					final double size = pdpModel.getContentsSize(v);

					final Collection<Parcel> contents = pdpModel.getContents(v);
					final int x = vp.toCoordX(p.x);
					final int y = vp.toCoordY(p.y);

					gc.setForeground(new Color(gc.getDevice(), BLACK));

					for (final Parcel parcel : contents) {

						final Point po = parcel.getDestination();
						final int xd = vp.toCoordX(po.x);
						final int yd = vp.toCoordY(po.y);
						if (parcel.getDeliveryTimeWindow().isIn(time)) {
							gc.setBackground(new Color(gc.getDevice(), DARK_GREEN));
						} else {
							gc.setBackground(new Color(gc.getDevice(), ORANGE));
						}

						gc.drawLine(x, y, xd, yd);
						gc.fillOval(xd - 5, yd - 5, 10, 10);
						gc.drawOval(xd - 5, yd - 5, 10, 10);
					}
					gc.setBackground(new Color(gc.getDevice(), BACKGROUND_INFO));
					gc.setForeground(new Color(gc.getDevice(), FOREGROUND_INFO));
					final VehicleState state = pdpModel.getVehicleState(v);
					// FIXME, investigate why the second check is
					// neccesary..
					if (state != VehicleState.IDLE && pdpModel.getVehicleActionInfo(v) != null) {
						gc.drawText(state.toString() + " " + pdpModel.getVehicleActionInfo(v).timeNeeded(), x, y - 20);
					}
					gc.drawText("" + size, x, y);
					drawMore(gc, vp, time, v, p);
				}
			}

			final Collection<Parcel> parcels = pdpModel.getParcels(ParcelState.AVAILABLE, ParcelState.ANNOUNCED);
			for (final Parcel parcel : parcels) {

				final Point p = posMap.get(parcel);
				if (posMap.containsKey(parcel)) {
					final int x = vp.toCoordX(p.x);
					final int y = vp.toCoordY(p.y);
					gc.setForeground(new Color(gc.getDevice(), LIGHT_GRAY));
					gc.drawLine(x, y, vp.toCoordX(parcel.getDestination().x), vp.toCoordY(parcel.getDestination().y));

					RGB color = null;
					if (pdpModel.getParcelState(parcel) == ParcelState.ANNOUNCED) {
						color = GRAY;
					} else if (parcel.getPickupTimeWindow().isIn(time)) {
						color = GREEN;
					} else {
						color = ORANGE;
					}
					gc.setForeground(new Color(gc.getDevice(), BLACK));
					gc.setBackground(new Color(gc.getDevice(), color));
					gc.fillOval(x - 5, y - 5, 10, 10);
				}
			}
		}
	}

	protected void drawMore(GC gc, ViewPort vp, long time, Vehicle v, Point p) {}

	@Override
	public ViewRect getViewRect() {
		return null;
	}

	@Override
	public void registerModelProvider(ModelProvider mp) {
		pdpModel = mp.getModel(PDPModel.class);
		roadModel = mp.getModel(RoadModel.class);
	}

	public Class<PDPModel> getSupportedModelType() {
		return PDPModel.class;
	}

}
