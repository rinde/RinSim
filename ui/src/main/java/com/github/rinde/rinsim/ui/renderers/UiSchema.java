/*
 * Copyright (C) 2011-2017 Rinde van Lon, imec-DistriNet, KU Leuven
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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Maps.newHashMap;

import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;

/**
 * Used to configure a graphical representation of the model elements.
 * @author Bartosz Michalik
 * @author Rinde van Lon
 */
public class UiSchema {
  /**
   * This is the key that is used for the default color.
   */
  public static final String DEFAULT = "default_color";

  @Nullable
  Map<String, Color> colorRegistry;
  @Nullable
  Map<String, Image> imageRegistry;

  private final Map<String, RGB> colorCache;
  private final Map<String, Integer> defaultColorCache;

  private final Map<String, String> imgCache;
  private final boolean useDefault;

  /**
   * Create the schema.
   * @param pUseDefault when use default is <code>true</code> the default color
   *          is used
   */
  public UiSchema(boolean pUseDefault) {
    colorCache = newHashMap();
    defaultColorCache = newHashMap();
    imgCache = newHashMap();
    useDefault = pUseDefault;
  }

  /**
   * Creates schema with default colors.
   */
  public UiSchema() {
    this(true);
  }

  /**
   * Associate a {@link RGB} to a {@link Class}. Note that this color
   * association works through super classes as well. An example: <br>
   * consider the following class hierarchy<br>
   * <code>class A{}</code><br>
   * <code>class AA extends A{}</code><br>
   * <code>class AAA extends AA{}</code><br>
   * When adding a color named <code>C1</code> to <code>AA</code>,
   * {@link #getColor(Class)} will return the following values: for
   * <code>A: null</code>, for <code>AA: C1</code>, for <code>AAA: C1</code>.
   * When adding another color named <code>C2</code> to <code>A</code>
   * {@link #getColor(Class)} will return: for <code>A: C2</code>, for
   * <code>AA: C1</code>, for <code>AAA: C1</code>.
   * @param type The {@link Class} used as identifier.
   * @param rgb The {@link RGB} instance used as color.
   */
  public void add(Class<?> type, RGB rgb) {
    colorCache.put(type.getName(), rgb);
  }

  public void add(Class<?> type, int defaultColor) {
    defaultColorCache.put(type.getName(), defaultColor);
  }

  public void add(String key, RGB rgb) {
    colorCache.put(key, rgb);
  }

  public void add(Class<?> type, String fileName) {
    imgCache.put(type.getName(), fileName);
  }

  @Nullable
  public Image getImage(Class<?> type) {
    checkInitialized();
    return imageRegistry.get(type.getName());
  }

  void checkInitialized() {
    checkState(colorRegistry != null,
      "UiSchema needs to be initialized before it can be used");
  }

  /**
   * Looks up the {@link Color} which is associated to the specified type. Note
   * that the {@link Color} instance is derived from the {@link RGB} instance
   * which was added through {@link #add(Class, RGB)}. If there is no
   * {@link Color} associated to the type, its superclass is checked instead.
   * This is continued recursively until a {@link Color} is found. If no
   * {@link Color} is found, either the default color is returned or
   * <code>null</code>.
   * @param type The {@link Class} which is checked for a {@link Color}
   *          association.
   * @return A {@link Color} instance if it exists, or <code>null</code>
   *         otherwise.
   * @see #add(Class, RGB)
   */
  @Nullable
  public Color getColor(Class<?> type) {
    checkInitialized();
    final Color color = colorRegistry.get(type.getName());
    if (color == null && type.getSuperclass() != null) {
      final Color tmp = getColor(type.getSuperclass());
      if (tmp != null) {
        colorRegistry.put(type.getName(), tmp);
        return tmp;
      }
    }
    if (color == null && useDefault) {
      return colorRegistry.get(DEFAULT);
    }
    return color;
  }

  public Color getDefaultColor() {
    return colorRegistry.get(DEFAULT);
  }

  public Color getColor(String key) {
    final Color color = colorRegistry.get(key);
    if (color == null && useDefault) {
      return colorRegistry.get(DEFAULT);
    }
    return color;
  }

  public void initialize(Device d) {
    flushCache(d);
  }

  private void flushCache(Device d) {
    if (colorRegistry != null) {
      return;
    }
    colorRegistry = newHashMap();
    imageRegistry = newHashMap();
    colorRegistry.put(DEFAULT, d.getSystemColor(SWT.COLOR_RED));
    for (final Entry<String, RGB> e : colorCache.entrySet()) {
      colorRegistry.put(e.getKey(), new Color(d, e.getValue()));
    }
    for (final Entry<String, Integer> e : defaultColorCache.entrySet()) {
      colorRegistry.put(e.getKey(), d.getSystemColor(e.getValue()));
    }

    for (final Entry<String, String> e : imgCache.entrySet()) {
      imageRegistry.put(e.getKey(), new Image(d, getClass()
        .getResourceAsStream(e.getValue())));
    }
  }
}
