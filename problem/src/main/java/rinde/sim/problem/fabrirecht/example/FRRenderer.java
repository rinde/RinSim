/**
 * 
 */
package rinde.sim.problem.fabrirecht.example;

import java.util.Set;

import org.eclipse.swt.graphics.GC;

import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.Parcel;
import rinde.sim.problem.fabrirecht.FRParcel;
import rinde.sim.ui.renderers.ModelRenderer;
import rinde.sim.ui.renderers.ViewPort;
import rinde.sim.ui.renderers.ViewRect;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class FRRenderer implements ModelRenderer<PDPModel> {

	PDPModel pdpModel;

	@Override
	public void renderStatic(GC gc, ViewPort vp) {
		// TODO Auto-generated method stub

	}

	@Override
	public void renderDynamic(GC gc, ViewPort vp) {
		final Set<Parcel> parcels = pdpModel.getAvailableParcels();
		synchronized (parcels) {
			for (final Parcel parcel : parcels) {
				final FRParcel p = ((FRParcel) parcel);

				// pdpModel.
				//
				// p.dto.
				//
				// vp.

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
