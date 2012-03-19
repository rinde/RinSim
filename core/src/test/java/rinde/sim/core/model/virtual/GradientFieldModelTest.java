package rinde.sim.core.model.virtual;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.apache.commons.math.random.MersenneTwister;
import org.apache.commons.math.random.RandomGenerator;
import org.junit.Before;
import org.junit.Test;

import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.Graphs;
import rinde.sim.core.graph.MultiAttributeEdgeData;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.RoadModel;
import rinde.sim.serializers.DotGraphSerializer;
import rinde.sim.serializers.SelfCycleFilter;


public class GradientFieldModelTest {
	
	GradientFieldModel gradientFieldModel;
	RoadModel roadModel;
	RandomGenerator rand;
	
	@Before
	public void setup() throws Exception{
		String MAP_DIR = "../core/files/maps/";
		rand = new MersenneTwister(1235);
		Graph<MultiAttributeEdgeData> graph = DotGraphSerializer.getMultiAttributeGraphSerializer(new SelfCycleFilter()).read(MAP_DIR + "leuven-simple.dot");
		roadModel = new RoadModel(graph);
		gradientFieldModel = new GradientFieldModel(roadModel);
	}
	
	@Test
	public void registerAndUnregisterVirtualEntity(){
		VirtualEntity entity = new SimpleVirtualEntity();
		assertTrue(gradientFieldModel.register(entity));
		assertFalse(gradientFieldModel.register(entity));
		assertTrue(gradientFieldModel.unregister(entity));
		assertFalse(gradientFieldModel.unregister(entity));
	}
	
	@Test
	public void singleSimpleField(){
		VirtualEntity entity = new SimpleVirtualEntity(new Point(0,0));
		gradientFieldModel.register(entity);
		Collection<Field> fields = gradientFieldModel.getSimpleFields(new Point(0,0));
		assertEquals(1,fields.size());
		Field field = (Field) fields.toArray()[0];
		assertEquals(0,field.getDistance(),0);

		fields = gradientFieldModel.getSimpleFields(new Point(5,0));
		assertEquals(1,fields.size());
		field = (Field) fields.toArray()[0];
		assertEquals(5,field.getDistance(),0);
	}
	
	@Test
	public void multipleSimpleFields(){
		VirtualEntity entity1 = new SimpleVirtualEntity(new Point(0,0));
		VirtualEntity entity2 = new SimpleVirtualEntity(new Point(10,0));
		gradientFieldModel.register(entity1);
		gradientFieldModel.register(entity2);
		Collection<Field> fields = gradientFieldModel.getSimpleFields(new Point(5,0));
		assertEquals(2,fields.size());
	}
	
	@Test
	public void singleField(){
		Point p1 = roadModel.getGraph().getRandomNode(rand);
		Point p2 = roadModel.getGraph().getRandomNode(rand);
		double distance = Graphs.pathLength(roadModel.getShortestPathTo(p2, p1));
		VirtualEntity entity = new SimpleVirtualEntity(p1);
		gradientFieldModel.register(entity);
		Collection<Field> fields = gradientFieldModel.getFields(p2);
		assertEquals(1, fields.size(),0);
		Field field = (Field) fields.toArray()[0];
		assertEquals(distance, field.getDistance(),0);
	}

	@Test
	public void MultipleField(){
		Point p1 = roadModel.getGraph().getRandomNode(rand);
		Point p2 = roadModel.getGraph().getRandomNode(rand);
		Point p3 = roadModel.getGraph().getRandomNode(rand);
		VirtualEntity entity1 = new SimpleVirtualEntity(p1);
		VirtualEntity entity2 = new SimpleVirtualEntity(p2);
		gradientFieldModel.register(entity1);
		gradientFieldModel.register(entity2);
		Collection<Field> fields = gradientFieldModel.getFields(p3);
		assertEquals(2,fields.size());
	}
	
	class SimpleVirtualEntity implements VirtualEntity{
		private Point position;
		private boolean isEmitting;

		public SimpleVirtualEntity(){
			this(new Point(0,0));
		}
		
		public SimpleVirtualEntity(Point position){
			this.position = position;
			this.isEmitting = true;
		}
		
		public void setEmitting(boolean emitting){
			this.isEmitting = emitting;
		}
		
		@Override
		public void init(GradientFieldAPI api) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean isEmitting() {
			return isEmitting;
		}

		@Override
		public Point getPosition() {
			return position;
		}

		@Override
		public FieldData getFieldData() {
			return new FieldData() {
				
			};		
		}
		
	}

}
