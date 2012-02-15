package rinde.sim.core.model;

import org.junit.Before;
import org.junit.Test;

import rinde.sim.core.model.AbstractModel;
import rinde.sim.core.model.Model;
import rinde.sim.core.model.ModelManager;


import static org.junit.Assert.*;
public class TestModelManager {

	protected ModelManager manager;
	
	@Before
	public void setUp() {
		manager = new ModelManager();
	}
	
	@Test(expected=IllegalStateException.class)
	public void notConfigured() {
		manager.register(new Object());
	}
	
	@Test
	public void addToEmpty() {
		manager.configure();
		assertFalse(manager.register(new Object()));
	}
	
	@Test
	public void addOtherFooModel() {
		OtherFooModel model = new OtherFooModel();
		manager.register(model);
		manager.configure();
		assertTrue(manager.register(new Foo()));
		assertFalse(manager.register(new Bar()));
		assertEquals(1, model.calledRegister);
		assertEquals(1, model.calledTypes);
	}
	
	@Test
	public void addWhenTwoModels() {
		OtherFooModel model = new OtherFooModel();
		BarModel model2 = new BarModel();
		manager.register(model);
		manager.register(model2);
		manager.configure();
		assertTrue(manager.register(new Foo()));
		assertTrue(manager.register(new Bar()));
		assertTrue(manager.register(new Foo()));
		assertEquals(2, model.calledRegister);
		assertEquals(1, model.calledTypes);
		assertEquals(1, model2.calledRegister);
		
		assertArrayEquals(new Model<?>[] {model, model2}, manager.getModels().toArray(new Model<?>[2]));
	}
}
 

class OtherFooModel implements Model<Foo> {

	int calledTypes;
	int calledRegister;
	
	@Override
	public boolean register(Foo element) {
		calledRegister += 1;
		return true;
	}

	@Override
	public Class<Foo> getSupportedType() {
		calledTypes += 1;
		return Foo.class;
	}
}

class BarModel extends AbstractModel<Bar> {
	int calledRegister;
	
	protected BarModel() {
		super(Bar.class);
	}

	@Override
	public boolean register(Bar element) {
		calledRegister += 1;
		return true;
	}
}

class Foo {}
class Bar {}