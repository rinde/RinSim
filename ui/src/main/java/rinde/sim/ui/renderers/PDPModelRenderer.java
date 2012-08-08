/**
 * 
 */
package rinde.sim.ui.renderers;

import java.util.Collection;
import java.util.Set;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.PDPModel.ParcelState;
import rinde.sim.core.model.pdp.PDPModel.VehicleState;
import rinde.sim.core.model.pdp.Parcel;
import rinde.sim.core.model.pdp.Vehicle;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class PDPModelRenderer implements ModelRenderer<PDPModel> {

	protected final static RGB GREEN = new RGB(0, 255, 0);
	protected final static RGB ORANGE = new RGB(255, 160, 0);

	protected PDPModel pdpModel;

	@Override
	public void renderStatic(GC gc, ViewPort vp) {}

	@Override
	public void renderDynamic(GC gc, ViewPort vp) {
		final Set<Vehicle> vehicles = pdpModel.getVehicles();
		synchronized (vehicles) {
			for (final Vehicle v : vehicles) {
				final Point p = pdpModel.getPosition(v);
				final double size = pdpModel.getContentsSize(v);

				final Collection<Parcel> contents = pdpModel.getContents(v);
				final int x = vp.toCoordX(p.x);
				final int y = vp.toCoordY(p.y);
				gc.drawText("" + size, x, y);
				for (final Parcel parcel : contents) {
					gc.drawLine(x, y, vp.toCoordX(parcel.getDestination().x), vp.toCoordY(parcel.getDestination().y));
				}
				final VehicleState state = pdpModel.getVehicleState(v);
				if (state != VehicleState.IDLE) {
					gc.drawText(state.toString(), x, y - 20);
				}
			}
		}

		final Collection<Parcel> parcels = pdpModel.getParcels(ParcelState.AVAILABLE, ParcelState.ANNOUNCED);
		synchronized (parcels) {
			for (final Parcel parcel : parcels) {
				final Point p = pdpModel.getPosition(parcel);
				final int x = vp.toCoordX(p.x);
				final int y = vp.toCoordY(p.y);
				gc.drawLine(x, y, vp.toCoordX(parcel.getDestination().x), vp.toCoordY(parcel.getDestination().y));
				gc.setBackground(new Color(gc.getDevice(),
						pdpModel.getParcelState(parcel) == ParcelState.AVAILABLE ? GREEN : ORANGE));
				gc.fillOval(x - 5, y - 5, 10, 10);

			}
		}

	}

	@Override
	public ViewRect getViewRect() {
		return null;
	}

	@Override
	public void register(PDPModel model) {
		pdpModel = model;
	}

	@Override
	public Class<PDPModel> getSupportedModelType() {
		return PDPModel.class;
	}

}
