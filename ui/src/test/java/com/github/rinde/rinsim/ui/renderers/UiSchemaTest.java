/**
 * 
 */
package com.github.rinde.rinsim.ui.renderers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.rinde.rinsim.ui.renderers.UiSchema;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class UiSchemaTest {

	UiSchema schema1, schema2;

	RGB rgbA, rgbB;
	Display display;

	@Before
	public void setup() {
		rgbA = new RGB(255, 0, 0);
		rgbB = new RGB(0, 255, 0);
		schema1 = new UiSchema(false);
		schema1.add(A.class, rgbA);
		schema1.add(B.class, rgbB);
		display = new Display();
		schema1.initialize(display);
		schema2 = new UiSchema(false);
	}

	@After
	public void tearDown() {
		display.dispose();
	}

	@Test(expected = IllegalStateException.class)
	public void getColorFail() {
		schema2.getColor(A.class);
	}

	@Test
	public void getColor() {
		assertEquals(rgbA, schema1.getColor(A.class).getRGB());
		assertNull(schema1.colorRegistry.get(AA.class.getName()));
		assertEquals(rgbA, schema1.getColor(AA.class).getRGB());
		assertEquals(rgbA, schema1.colorRegistry.get(AA.class.getName()).getRGB());

		assertEquals(rgbB, schema1.getColor(B.class).getRGB());
		assertNull(schema1.colorRegistry.get(BB.class.getName()));
		assertNull(schema1.colorRegistry.get(BBB.class.getName()));

		assertEquals(rgbB, schema1.getColor(BBB.class).getRGB());
		assertEquals(rgbB, schema1.colorRegistry.get(BBB.class.getName()).getRGB());
		assertEquals(rgbB, schema1.colorRegistry.get(BB.class.getName()).getRGB());

	}

	class A {}

	class AA extends A {}

	class B {}

	class BB extends B {}

	class BBB extends BB {}

}
