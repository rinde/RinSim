/**
 *
 */
package rinde.sim.parsers

import rinde.sim.scenario.TimedEvent
import rinde.sim.core.graph.Point
import rinde.sim.core.model.pdp.PDPScenarioEvent._

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 *
 */
package object montemanni05 {

  case class AddDepotEvent(timestamp: Long, dto: DepotDTO) extends TimedEvent(ADD_DEPOT, timestamp)
  case class CloseDepotEvent(timestamp: Long, id: Int) extends TimedEvent(REMOVE_DEPOT, timestamp)
  case class DepotDTO(id: Int, position: Point, earliestDepartureTime: Long, latestReturnTime: Long)

  case class AddVehicleEvent(timestamp: Long, dto: VehicleDTO) extends TimedEvent(ADD_VEHICLE, timestamp)
  case class VehicleDTO(id: Int, startPosition: Point, capacity: Int, speed: Double)

  case class AddOrderEvent(timestamp: Long, dto: OrderDTO) extends TimedEvent(ADD_PARCEL, timestamp)
  case class OrderDTO(id: Int, position: Point, typez: OrderType, quantity: Int, duration: Long)

  sealed class OrderType
  case object Pickup extends OrderType
  case object Delivery extends OrderType

}