package rinde.sim.core.model;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;


/**
 * Models manager keeps track of all models used in the simulator.
 * It is responsible for adding a simulation object to the appropriate models
 *  
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * 
 */
public class ModelManager {
	
	private Multimap<Class<? extends Object>, Model<? extends Object>> registry;
	private LinkedList<Model<? extends Object>> models;
	private boolean configured;
	
	
	public ModelManager() {
		registry = LinkedListMultimap.create();
		models = new LinkedList<Model<? extends Object>>();
	}
	
	/**
	 * Add model to the manager.
	 * 
	 * @param model
	 * @return whether the addition was sucessful
	 * @throws IllegalStateException
	 *             when method called after calling configure
	 */
	public boolean add(Model<?> model) {
		if (configured)
			throw new IllegalStateException(
					"model cannot be registered after configure()");
		boolean result = models.add(model);
		if (!result)
			return false;
		result &= registry.put(model.getSupportedType(), model);
		if (!result) {
			models.remove(model);
		}
		return result;
	}
	
	/**
	 * Method that allows for initialization of the manager (e.g., resolution of the dependencies between models)
	 * Should be called after all models were registered in the manager. 
	 */
	public void configure() {
		configured = true;
	}
	
	/**
	 * Add object to all models that support a given object
	 * @param o object to register
	 * @return <code>true</code> if object was added to at least one model
	 */
	public boolean register(Object o) {
		assert o != null : "NPE later in code otherwise";
		if(o instanceof Model) {
			if(configured) throw new IllegalStateException("model cannot be registered after configure()");
			return add((Model<?>)o);
		}
		
		if(!configured) throw new IllegalStateException("call configure()");
		boolean result = false;
		Set<Class<?>> keys = registry.keySet();
		for (Class<?> k : keys) {
			if(k.isAssignableFrom(o.getClass())) {
				Collection<Model<?>> modelsByType = registry.get(k);
				for (Model<?> m : modelsByType) {
					try {
						Method method = m.getClass().getMethod("register", k);
						if(!Modifier.isPublic(method.getModifiers())) continue;
						method.invoke(m, o);
						result = true;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		return result;
	}
	
	
	/**
	 * Unregister an object from all models it was attached to
	 * @param o object to unregister
	 * @return <code>true</code> when the unregistration succeeded in at least on model
	 * @throws IllegalAccessException if an object is a model
	 * @throws IllegalStateException if the method is called before simulator is configured
	 */
	public boolean unregister(Object o) {
		assert o != null : "NPE later in code otherwise";
		if(o instanceof Model) throw new IllegalArgumentException("cannot unregister an model");
		if(!configured) throw new IllegalStateException("call configure()");
		
		boolean result = false;
		Set<Class<?>> keys = registry.keySet();
		for (Class<?> k : keys) {
			if(k.isAssignableFrom(o.getClass())) {
				Collection<Model<?>> modelsByType = registry.get(k);
				for (Model<?> m : modelsByType) {
					try {
						Method method = m.getClass().getMethod("unregister", k);
						if(!Modifier.isPublic(method.getModifiers())) continue;
						method.invoke(m, o);
						result = true;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		return result;
	}
	
	public List<Model<?>> getModels() {
		return Collections.unmodifiableList(models);
	}
}
