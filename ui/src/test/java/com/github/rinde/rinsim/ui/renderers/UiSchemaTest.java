/*
 * Copyright (C) 2011-2015 Rinde van Lon, iMinds-DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rinde.rinsim.ui.renderers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.github.rinde.rinsim.testutil.GuiTests;

/**
 * @author Rinde van Lon
 * 
 */
@Category(GuiTests.class)
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
