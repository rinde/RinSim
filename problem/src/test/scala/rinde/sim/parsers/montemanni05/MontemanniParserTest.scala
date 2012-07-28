/**
 *
 */
package rinde.sim.parsers.montemanni05
import scala.io.Source

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 *
 */
object MontemanniParserTest {
  def main(args: Array[String]): Unit = {
    val file = "/data/montemanni05/f71.vrp"
    val source = Source.fromInputStream(getClass.getResourceAsStream(file))
    println(MontemanniParser.apply(source.getLines().mkString("\n")).asList())
  }
}