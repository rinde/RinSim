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
package com.github.rinde.rinsim.testutil;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Arrays.asList;
import static org.junit.Assert.fail;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * @author Rinde van Lon
 *
 */
public class TestUtil {
  public static <T> void testPrivateConstructor(Class<T> clazz) {
    try {
      final Constructor<T> c = clazz.getDeclaredConstructor();
      c.setAccessible(true);
      c.newInstance();
    } catch (final Exception e) {
      fail(e.getMessage());
    }
  }

  public static <T extends Enum<T>> void testEnum(Class<T> en) {
    checkArgument(en.isEnum(),
        "The specified class must be an enum, found %s.", en);

    final List<T> enums = asList(en.getEnumConstants());
    checkArgument(!enums.isEmpty(),
        "At least one enum constant must be defined in %s.", en);
    try {
      final Method m = en.getDeclaredMethod("valueOf", String.class);
      m.setAccessible(true);
      m.invoke(null, enums.get(0).toString());

    } catch (NoSuchMethodException | SecurityException | IllegalAccessException
        | IllegalArgumentException | InvocationTargetException e) {
      fail("unexpected error " + e.getMessage());
    }
  }
}
