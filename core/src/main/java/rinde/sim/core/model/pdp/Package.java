/**
 * 
 */
package rinde.sim.core.model.pdp;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public abstract class Package implements PDPObject {

	@Override
	public final PDPType getType() {
		return PDPType.PACKAGE;
	}

	// indicates 'size'/heaviness/etc
	abstract double getMagnitude();

}
