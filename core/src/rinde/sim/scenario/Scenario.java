/**
 * 
 */
package rinde.sim.scenario;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class Scenario implements Serializable {

	private static final long serialVersionUID = -2385766315100640657L;
	private final List<SerializedScenarioEvent> events;

	//	private transient ScenarioController controller;

	public Scenario(List<SerializedScenarioEvent> events) {
		this.events = Collections.unmodifiableList(events);
	}

	//	public void attachToSimulator(Simulator<?> simulator, Listener listener) {
	//		controller = new ScenarioController(this, simulator, listener);
	//	}

	public List<SerializedScenarioEvent> getEvents() {
		return Collections.unmodifiableList(events);
	}

	//	public void deattachToSimulator() {
	//		controller.stop();
	//		controller = null;
	//	}
	//
	//	public boolean isAttachedToSimulator() {
	//		return controller != null;
	//	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Scenario && events.size() == ((Scenario) other).events.size()) {
			return events.equals(((Scenario) other).events);
		}
		return false;
	}

	//	private static final RuntimeTypeAdapterFactory<SerializedScenarioEvent> eventAdapter = RuntimeTypeAdapterFactory.of(SerializedScenarioEvent.class, "type");
	//
	//	public static void registerScenarioEvent(Class<? extends SerializedScenarioEvent> clazz) {
	//		eventAdapter.registerSubtype(clazz, clazz.getName());
	//	}

	//	public static void toFile(Scenario s, String file) {
	//		Gson gson = new GsonBuilder().registerTypeAdapter(SerializedScenarioEvent.class, eventAdapter).create();
	//		try {
	//			BufferedWriter w = new BufferedWriter(new FileWriter(file));
	//			w.append(gson.toJson(s));
	//			w.close();
	//		} catch (IOException e) {
	//			throw new RuntimeException("Could not write scenario to file.", e);
	//		}
	//	}
	//
	//	public static Scenario parseFile(String file) {
	//		return parseFile(file, Scenario.class);
	//	}
	//
	//	public static <T extends Scenario> T parseFile(String file, Class<T> clazz) {
	//		try {
	//			Gson gson = new GsonBuilder().registerTypeAdapter(SerializedScenarioEvent.class, eventAdapter).create();
	//			return gson.fromJson(new BufferedReader(new FileReader(file)), clazz);
	//		} catch (Throwable e) {
	//			throw new RuntimeException("Failed to load scenario: " + file, e);
	//		}
	//	}
}
