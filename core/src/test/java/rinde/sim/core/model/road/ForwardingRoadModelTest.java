/**
 * 
 */
package rinde.sim.core.model.road;

import java.util.Arrays;
import java.util.Collection;

import javax.measure.Measure;
import javax.measure.unit.SI;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.Point;
import rinde.sim.core.graph.TestMultimapGraph;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
@RunWith(Parameterized.class)
public class ForwardingRoadModelTest extends
    AbstractRoadModelTest<GenericRoadModel> {
  @Parameters
  public static Collection<Object[]> configs() {
    return Arrays.asList(new Object[][] //
        { { new Creator() {
          @Override
          public GenericRoadModel create(ForwardingRoadModelTest testClass) {
            return new ForwardingRoadModel(new PlaneRoadModel(new Point(0, 0),
                new Point(10, 10), SI.METER, Measure.valueOf(10d,
                    SI.METERS_PER_SECOND)));
          }
        } }, { new Creator() {
          @Override
          public GenericRoadModel create(ForwardingRoadModelTest testClass) {
            return new ForwardingRoadModel(new GraphRoadModel(testClass
                .createGraph(), SI.METER, SI.METERS_PER_SECOND));
          }
        } }, { new Creator() {
          @Override
          public GenericRoadModel create(ForwardingRoadModelTest testClass) {
            return new ForwardingRoadModel(new ForwardingRoadModel(
                new ForwardingRoadModel(new GraphRoadModel(testClass
                    .createGraph(), SI.METER, SI.METERS_PER_SECOND))));
          }
        } } });
  }

  Graph<?> createGraph() {
    final Graph<?> g = new TestMultimapGraph();
    g.addConnection(SW, SE);
    g.addConnection(SE, NE);
    g.addConnection(NE, NW);
    return g;
  }

  interface Creator {
    GenericRoadModel create(ForwardingRoadModelTest testClass);
  }

  final Creator creator;

  public ForwardingRoadModelTest(Creator c) {
    creator = c;
  }

  @Override
  public void setUp() throws Exception {
    model = creator.create(this);
  }
}
