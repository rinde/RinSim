/**
 *
 */
package rinde.sim.parsers.montemanni05

import rinde.sim.scenario.ScenarioBuilder
import rinde.sim.core.graph.Graph
import rinde.sim.scenario.Scenario
import rinde.sim.core.model.pdp.PDPScenarioEvent._
import rinde.sim.scenario.TimedEvent
import rinde.sim.core.graph.Point
import rinde.sim.scenario.ScenarioBuilder.EventCreator

import scala.io.Source
import scala.util.parsing.combinator.RegexParsers
import scala.collection.mutable.HashMap
import scala.collection.mutable.MultiMap
import scala.collection.mutable.HashSet
import scala.collection.{ mutable => mut }

import scala.math._

import scala.collection.immutable.Set

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 *
 */
object MontemanniParser extends RegexParsers {

  def parseVRP(vrpDefinition: String): Scenario = apply(vrpDefinition)
  def parseVRPfromFile(file: String): Scenario = apply(Source.fromFile(file).getLines().mkString("\n"))

  def apply(input: String): Scenario =
    parseAll(expr, input) match {
      case Success(result, _) => result
      case failure: NoSuccess => scala.sys.error(failure.msg)
    }

  /**
   * HELPER PARSERS
   */
  protected def variableName: Parser[String] = """[A-Z_]*:\s""".r ^^ { _.split(":")(0) }
  protected def varInt: Parser[Int] = """(?m)\d+$""".r ^^ { _.toInt }
  protected def varString: Parser[String] = """.+""".r

  protected def int: Parser[Int] = """\s*-?\d+""".r ^^ { _.toInt }
  protected def intEOL: Parser[Int] = """(?m)\s*-?\d+$""".r ^^ { _.toInt }

  protected def double: Parser[Double] = """\s*-?\d+\.?\d*""".r ^^ { _.toDouble }
  protected def doubleEOL: Parser[Double] = """(?m)\s*-?\d+\.?\d*$""".r ^^ { _.toDouble }

  protected def idMapping: Parser[Map[Int, Int]] = rep(int ~ intEOL) ^^ {
    case list => list.map(item => (item._1, item._2)).toMap
  }

  implicit def func2creator(func: Long => TimedEvent): EventCreator[TimedEvent] = {
    return new EventCreator[TimedEvent]() {
      def apply(time: java.lang.Long): TimedEvent = func(time)
    }
  }

  /**
   * SPECIFICATION
   */
  protected case class Specification(name: String, numDepots: Int, numCapacities: Int, numOrders: Int, numLocations: Int, numVehicles: Int, capacities: Int, comments: Set[String])
  protected val NAME = "NAME"
  protected val NUM_DEPOTS = "NUM_DEPOTS"
  protected val NUM_CAPACITIES = "NUM_CAPACITIES"
  protected val NUM_VISITS = "NUM_VISITS"
  protected val NUM_LOCATIONS = "NUM_LOCATIONS"
  protected val NUM_VEHICLES = "NUM_VEHICLES"
  protected val CAPACITIES = "CAPACITIES"
  protected val COMMENT = "COMMENT"

  protected def formatId: Parser[String] = """(?m)VRPTEST 1\.0$""".r
  protected def specSection: Parser[Specification] = rep(variableName ~ (varInt | varString)) ^^ {
    case list =>
      val vars: MultiMap[String, Any] = new HashMap[String, mut.Set[Any]]() with MultiMap[String, Any]
      list.map(item => (item._1, item._2)) foreach (it => vars.addBinding(it._1, it._2))

      val name = vars.get(NAME).get.head.asInstanceOf[String]
      val numDepots = vars.get(NUM_DEPOTS).get.head.asInstanceOf[Int]
      val numCapacities = vars.get(NUM_CAPACITIES).get.head.asInstanceOf[Int]
      val numVisits = vars.get(NUM_VISITS).get.head.asInstanceOf[Int]
      val numLocations = vars.get(NUM_LOCATIONS).get.head.asInstanceOf[Int]
      val numVehicles = vars.get(NUM_VEHICLES).get.head.asInstanceOf[Int]
      val capacities = vars.get(CAPACITIES).get.head.asInstanceOf[Int]
      val comments = vars.get(COMMENT).get.asInstanceOf[mut.Set[String]].toSet
      Specification(name, numDepots, numCapacities, numVisits, numLocations, numVehicles, capacities, comments)
  }

  /**
   * DATA
   */
  protected def dataSection: Parser[String] = """(?m)DATA_SECTION$""".r
  protected def depots: Parser[Set[Int]] = """(?m)DEPOTS$""".r ~> rep1(intEOL) ^^ { _.toSet }
  protected def demandSection: Parser[Map[Int, Int]] = """(?m)^DEMAND_SECTION$""".r ~> idMapping
  protected def locationSection: Parser[Map[Int, Point]] = """(?m)LOCATION_COORD_SECTION$""".r ~> rep(int ~ int ~ intEOL) ^^ {
    case list => list.map(item => (item._1._1, new Point(item._1._2, item._2))).toMap
  }

  protected def depotLocationSection: Parser[Map[Int, Int]] = """(?m)DEPOT_LOCATION_SECTION$""".r ~> idMapping
  protected def visitLocationSection: Parser[Map[Int, Int]] = """(?m)VISIT_LOCATION_SECTION$""".r ~> idMapping

  protected def durationSection: Parser[Map[Int, Double]] = """(?m)DURATION_SECTION$""".r ~> rep(int ~ doubleEOL) ^^ {
    case list => list.map(item => (item._1, item._2)).toMap
  }

  protected def depotTimeWindowSection: Parser[Map[Int, (Double, Double)]] = """(?m)DEPOT_TIME_WINDOW_SECTION$""".r ~> rep(int ~ double ~ doubleEOL) ^^ {
    case list => list.map(item => (item._1._1, (item._1._2, item._2))).toMap
  }

  protected def timeAvailSection: Parser[Map[Int, Double]] = """(?m)TIME_AVAIL_SECTION$""".r ~> rep(int ~ doubleEOL) ^^ {
    case list => list.map(item => (item._1, item._2)).toMap
  }

  protected def expr: Parser[Scenario] = formatId ~>
    specSection ~
    dataSection ~
    depots ~
    demandSection ~
    locationSection ~
    depotLocationSection ~
    visitLocationSection ~
    durationSection ~
    depotTimeWindowSection ~
    rep(variableName ~ (varString | varInt)) ~
    timeAvailSection <~
    "EOF" ^^ {
      case spec ~ dataSection ~ depots ~ orders ~ locations ~ depotLocations ~ orderLocations ~ orderDurations ~ depotTimeWindows ~ comment ~ orderAvailableTimes =>

        require(spec.numDepots > 0, "numDepots must be > 0")
        require(spec.numLocations == (spec.numOrders + spec.numDepots), "numLocations must equals numVisits + numDepots")
        require(locations.size == spec.numLocations)
        require(orderLocations.size == spec.numOrders)
        require(depotLocations.size == spec.numDepots)

        require(locations.size == (depotLocations.size + orderLocations.size))
        require(depots.size == spec.numDepots, "the number of depots in the specification section must equal the number of depots in the data section")
        require(depots.size == 1, "currently this parser supports only one depot")
        require(depots.size == depotTimeWindows.size, "each depot needs to have time windows")
        require(spec.numVehicles > 0, "at least one vehicle must be specified")
        require(orders.size == orderLocations.size, "each order needs to have a location and vice versa")
        require(orders.size == orderDurations.size, "each order needs to have a duration and vice versa")
        require(orders.size == orderAvailableTimes.size, "each order needs to have an available time and vice versa")
        require(orders.size == spec.numOrders, "the specification must match the specified number of orders")

        val builder = new ScenarioBuilder(ADD_DEPOT, ADD_VEHICLE, ADD_PARCEL, REMOVE_DEPOT)

        val timeFactor = 1000L

        /*
         * depots
         */
        val depotId = depots.head
        val depotLocation = locations.get(depotLocations.get(depotId).get).get
        val depotTimeWindow = depotTimeWindows.get(depotId).get
        val earliestDepartureTime = (timeFactor * depotTimeWindow._1).toLong
        val latestReturnTime = (timeFactor * depotTimeWindow._2).toLong

        builder.addEvent(AddDepotEvent(earliestDepartureTime, DepotDTO(depotId, depotLocation, earliestDepartureTime, latestReturnTime)))
        builder.addEvent(CloseDepotEvent(latestReturnTime, depotId))

        /*
         * vehicles
         */
        // vehicle ids range from 1 to NUM_VEHICLES
        // default vehicle speed = 1
        var vehicleId = 0;
        val func: (Long => TimedEvent) = {
          time =>
            vehicleId += 1
            AddVehicleEvent(timeFactor * time, VehicleDTO(vehicleId, depotLocation, spec.capacities, 1))
        }
        builder.addMultipleEvents(0, spec.numVehicles, func)

        /*
         * orders AKA demands AKA visits AKA packages AKA parcels
         */
        orders foreach {
          item =>
            // TODO time units?
            val availableTime = orderAvailableTimes.get(item._1).get
            val quantity = item._2;
            val typez = if (quantity < 0) Delivery else Pickup

            // the format describes times as real values, however, we require discrete time values
            require(round(orderDurations.get(item._1).get) == orderDurations.get(item._1).get)

            val dto = OrderDTO(item._1, locations.get(orderLocations.get(item._1).get).get, typez, quantity, (timeFactor * orderDurations.get(item._1).get).toLong)
            builder.addEvent(AddOrderEvent((timeFactor * availableTime).toLong, dto))
        }

        builder.build()

    }

}