/**
 * 
 */
package rinde.sim.ui.renderers;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;

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

	protected Color black;
	protected Color white;
	protected Color gray;
	protected Color lightGray;
	protected Color darkGreen;
	protected Color green;
	protected Color orange;
	protected Color blue;
	protected Color foregroundInfo;
	protected Color backgroundInfo;

	protected PDPModel pdpModel;
	protected RoadModel roadModel;

	protected boolean isInitialized;

	public PDPModelRenderer() {}

	// TODO dispose colors on exit!
	protected void initialize(GC gc) {
		System.out.println();
		isInitialized = true;
		black = gc.getDevice().getSystemColor(SWT.COLOR_BLACK);
		white = gc.getDevice().getSystemColor(SWT.COLOR_WHITE);
		gray = gc.getDevice().getSystemColor(SWT.COLOR_GRAY);
		darkGreen = gc.getDevice().getSystemColor(SWT.COLOR_DARK_GREEN);
		green = gc.getDevice().getSystemColor(SWT.COLOR_GREEN);
		blue = gc.getDevice().getSystemColor(SWT.COLOR_BLUE);

		lightGray = new Color(gc.getDevice(), 205, 201, 201);
		orange = new Color(gc.getDevice(), 255, 160, 0);

		foregroundInfo = white;
		backgroundInfo = blue;

	}

	@Override
	public void renderStatic(GC gc, ViewPort vp) {}

	@Override
	public void renderDynamic(GC gc, ViewPort vp, long time) {
		if (!isInitialized) {
			initialize(gc);
		}

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

					gc.setForeground(black);

					for (final Parcel parcel : contents) {

						final Point po = parcel.getDestination();
						final int xd = vp.toCoordX(po.x);
						final int yd = vp.toCoordY(po.y);
						if (parcel.getDeliveryTimeWindow().isIn(time)) {
							gc.setBackground(darkGreen);
						} else {
							gc.setBackground(orange);
						}

						gc.drawLine(x, y, xd, yd);
						gc.fillOval(xd - 5, yd - 5, 10, 10);
						gc.drawOval(xd - 5, yd - 5, 10, 10);
					}
					gc.setBackground(backgroundInfo);
					gc.setForeground(foregroundInfo);
					final VehicleState state = pdpModel.getVehicleState(v);
					// FIXME, investigate why the second check is
					// neccesary..
					if (state != VehicleState.IDLE && pdpModel.getVehicleActionInfo(v) != null) {
						gc.drawText(state.toString() + " " + pdpModel.getVehicleActionInfo(v).timeNeeded(), x, y - 20);
					}
					gc.drawText("" + size, x, y);
					drawMore(gc, vp, time, v, p, posMap);
				}
			}

			final Collection<Parcel> parcels = pdpModel.getParcels(ParcelState.AVAILABLE, ParcelState.ANNOUNCED);
			for (final Parcel parcel : parcels) {

				final Point p = posMap.get(parcel);
				if (posMap.containsKey(parcel)) {
					final int x = vp.toCoordX(p.x);
					final int y = vp.toCoordY(p.y);
					gc.setForeground(lightGray);
					gc.drawLine(x, y, vp.toCoordX(parcel.getDestination().x), vp.toCoordY(parcel.getDestination().y));

					Color color = null;
					if (pdpModel.getParcelState(parcel) == ParcelState.ANNOUNCED) {
						color = gray;
					} else if (parcel.getPickupTimeWindow().isIn(time)) {
						color = green;
					} else {
						color = orange;
					}
					gc.setForeground(black);
					gc.setBackground(color);
					gc.fillOval(x - 5, y - 5, 10, 10);
				}
			}
		}
	}

	protected void drawMore(GC gc, ViewPort vp, long time, Vehicle v, Point p, Map<RoadUser, Point> posMap) {}

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
