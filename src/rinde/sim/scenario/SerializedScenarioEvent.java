package rinde.sim.scenario;

import java.lang.reflect.Field;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class SerializedScenarioEvent implements Comparable<SerializedScenarioEvent> {
	public final long time;

	public SerializedScenarioEvent(long time) {
		this.time = time;
	}

	@Override
	public int compareTo(SerializedScenarioEvent other) {
		return time == other.time ? 0 : time < other.time ? -1 : 1;
	}

	@Override
	public String toString() {
		return this.getClass().getName() + "|" + time;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof SerializedScenarioEvent) {
			for (Field f : this.getClass().getFields()) {
				try {
					if (!f.get(this).equals(f.get(other))) {
						return false;
					}
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
					return false;
				} catch (IllegalAccessException e) {
					e.printStackTrace();
					return false;
				}
			}
			return true;
		}
		return false;
	}
}