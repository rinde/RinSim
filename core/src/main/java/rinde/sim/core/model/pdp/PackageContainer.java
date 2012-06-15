/**
 * 
 */
package rinde.sim.core.model.pdp;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 *         An object that can contain packages.
 */
interface PackageContainer extends PDPObject {

	/**
	 * The returned value is treated as a constant (i.e. it is read only once).
	 * @return
	 */
	public abstract double getCapacity();

}
