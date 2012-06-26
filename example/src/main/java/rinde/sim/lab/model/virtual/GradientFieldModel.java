package rinde.sim.lab.model.virtual;

import java.util.Collection;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.Model;
import rinde.sim.core.model.road.RoadModel;

public class GradientFieldModel implements Model<VirtualEntity>, GradientFieldAPI {
	protected static final Logger LOGGER = LoggerFactory.getLogger(GradientFieldModel.class);

	private RoadModel rm;

	protected volatile Collection<VirtualEntity> entities;

	public GradientFieldModel() {
		entities = new HashSet<VirtualEntity>();
	}

	public GradientFieldModel(RoadModel rm) {
		this();
		// TODO exercise
	}

	@Override
	public Collection<Field> getFields(Point point) {
		// TODO exercise
		return null;
	}

	@Override
	public Collection<Field> getSimpleFields(Point point) {

		Collection<Field> fields = new HashSet<Field>();

		for (VirtualEntity entity : entities) {
			if (entity.isEmitting()) {
				double distance = Point.distance(point, entity.getPosition());
				fields.add(new Field(entity.getFieldData(), distance));
			}
		}

		return fields;
	}

	@Override
	public boolean register(VirtualEntity entity) {
		entity.init(this);
		return entities.add(entity);
	}

	@Override
	public boolean unregister(VirtualEntity entity) {
		if (entities.contains(entity)) {
			entities.remove(entity);
			return true;
		}
		return false;
	}

	@Override
	public Class<VirtualEntity> getSupportedType() {
		return VirtualEntity.class;
	}

}