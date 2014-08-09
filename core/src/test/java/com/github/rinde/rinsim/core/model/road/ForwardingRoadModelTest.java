/**
 * 
 */
package com.github.rinde.rinsim.core.model.road;

import java.util.Arrays;
import java.util.Collection;

import javax.measure.Measure;
import javax.measure.unit.SI;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.github.rinde.rinsim.core.graph.Graph;
import com.github.rinde.rinsim.core.graph.Point;
import com.github.rinde.rinsim.core.graph.TestMultimapGraph;
import com.github.rinde.rinsim.core.model.road.ForwardingRoadModel;
import com.github.rinde.rinsim.core.model.road.GenericRoadModel;
import com.github.rinde.rinsim.core.model.road.GraphRoadModel;
import com.github.rinde.rinsim.core.model.road.PlaneRoadModel;

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
