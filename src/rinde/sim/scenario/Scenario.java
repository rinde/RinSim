/**
 * 
 */
package rinde.sim.scenario;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rinde.sim.core.Simulator;
import rinde.sim.event.Listener;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class Scenario {

	public final List<SerializedScenarioEvent> events;

	private ScenarioController controller;

	public Scenario(List<SerializedScenarioEvent> events) {
		this.events = Collections.unmodifiableList(events);
	}

	public void attachToSimulator(Simulator<?> simulator, Listener listener) {
		controller = new ScenarioController(this, simulator, listener);
	}

	public void deattachToSimulator() {
		controller.stop();
		controller = null;
	}

	public boolean isAttachedToSimulator() {
		return controller != null;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Scenario && events.size() == ((Scenario) other).events.size()) {
			return events.equals(((Scenario) other).events);
		}
		return false;
	}

	// Class: String name, Long time, List<Class<?>> types,
	// Instance: name, time, values

	//	public static Scenario randomScenario(RandomGenerator gen, int numTrucks, int numPackages, long lastPackageDispatchTime, List<Point> positions) {
	//		List<SerializedScenarioEvent> events = new ArrayList<SerializedScenarioEvent>();
	//
	//		for (int i = 0; i < numTrucks; i++) {
	//			events.add(new AddTruckEvent(0L, positions.get(gen.nextInt(positions.size()))));
	//		}
	//		for (int i = 0; i < numPackages; i++) {
	//			long time = i == 0 ? 0L : (long) (gen.nextDouble() * lastPackageDispatchTime);
	//			events.add(new AddPackageEvent(time, positions.get(gen.nextInt(positions.size())), positions.get(gen.nextInt(positions.size()))));
	//		}
	//		Collections.sort(events);
	//		return new Scenario(events);
	//	}

	public static void toFile(Scenario s, String file) {

		try {
			BufferedWriter w = new BufferedWriter(new FileWriter(file));

			for (SerializedScenarioEvent e : s.events) {
				w.append(e.toString() + "\n");
			}
			w.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static Scenario parseFile(String file) {
		try {
			BufferedReader r = new BufferedReader(new FileReader(file));
			String line;
			List<SerializedScenarioEvent> events = new ArrayList<SerializedScenarioEvent>();

			while ((line = r.readLine()) != null) {
				if (!line.startsWith("#")) {
					String[] parts = line.split("\\|");

					//				for (Class<?> c : classes) {
					//					if (c.getName().equals(parts[0])) {
					//						events.add((SerializedScenarioEvent) c.getConstructor(String[].class).newInstance((Object) parts));
					//						break;
					//					}
					//				}

					events.add((SerializedScenarioEvent) Class.forName(parts[0]).getConstructor(String[].class).newInstance((Object) parts));

					//				if (parts[0].equals("AddPackageEvent")) {
					//					events.add(new AddPackageEvent(Long.parseLong(parts[1]), Point.parsePoint(parts[2]), Point.parsePoint(parts[3])));
					//				} else if (parts[0].equals("AddTruckEvent")) {
					//					events.add(new AddTruckEvent(Long.parseLong(parts[1]), Point.parsePoint(parts[2])));
					//				}
				}
			}
			return new Scenario(events);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}
}
