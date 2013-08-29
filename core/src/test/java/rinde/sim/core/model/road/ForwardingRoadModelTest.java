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
public class ForwardingRoadModelTest extends AbstractRoadModelTest<RoadModel> {
  @Parameters
  public static Collection<Object[]> configs() {
    return Arrays.asList(new Object[][] //
        { { new Creator() {
          @Override
          public RoadModel create(ForwardingRoadModelTest testClass) {
            return new TestForwardingRoadModel(new PlaneRoadModel(new Point(0,
                0), new Point(10, 10), SI.METER, Measure
                .valueOf(10d, SI.METERS_PER_SECOND)));
          }
        } }, { new Creator() {
          @Override
          public RoadModel create(ForwardingRoadModelTest testClass) {
            return new TestForwardingRoadModel(new GraphRoadModel(testClass
                .createGraph(), SI.METER));
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
    RoadModel create(ForwardingRoadModelTest testClass);
  }

  final Creator creator;

  public ForwardingRoadModelTest(Creator c) {
    creator = c;
  }

  @Override
  public void setUp() throws Exception {
    model = creator.create(this);
  }

  static class TestForwardingRoadModel extends ForwardingRoadModel {

    private final AbstractRoadModel<?> delegate;

    public TestForwardingRoadModel(AbstractRoadModel<?> deleg) {
      delegate = deleg;
    }

    @Override
    protected AbstractRoadModel<?> delegate() {
      return delegate;
    }

  }
}
