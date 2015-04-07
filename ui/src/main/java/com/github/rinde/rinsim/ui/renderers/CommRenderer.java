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

import static com.google.common.base.Verify.verify;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;

import com.github.rinde.rinsim.core.model.ModelProvider;
import com.github.rinde.rinsim.core.model.comm.CommDevice;
import com.github.rinde.rinsim.core.model.comm.CommModel;
import com.github.rinde.rinsim.core.model.comm.CommModel.CommModelEvent;
import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.Listener;
import com.github.rinde.rinsim.ui.renderers.CommRenderer.Builder.ViewOptions;
import com.google.common.base.Optional;
import com.google.common.collect.Sets;

/**
 * A renderer for {@link CommModel}. Draws dots for a device, a circle is drawn
 * around it if it has a maximum range. It has several options which can be
 * configured via {@link #builder()}.
 * @author Rinde van Lon
 */
public final class CommRenderer implements CanvasRenderer {
  static final int OPAQUE = 255;
  static final int SEMI_TRANSPARENT = 50;
  static final double DOT_RADIUS = .05;

  private final List<DeviceUI> uiObjects;
  final RenderHelper helper;
  final Set<ViewOptions> viewOptions;
  final RGB reliableColor;
  final RGB unreliableColor;

  CommRenderer(Builder b, CommModel model) {
    viewOptions = Sets.immutableEnumSet(b.viewOptions);
    reliableColor = b.reliableColor;
    unreliableColor = b.unreliableColor;
    helper = new RenderHelper();
    uiObjects = new ArrayList<>();
    for (final Entry<CommUser, CommDevice> entry : model.getUsersAndDevices()
      .entrySet()) {
      addUIObject(entry.getKey(), entry.getValue());

    }

    model.getEventAPI().addListener(new Listener() {
      @Override
      public void handleEvent(Event e) {
        verify(e instanceof CommModelEvent);
        final CommModelEvent event = (CommModelEvent) e;
        addUIObject(event.getUser(), event.getDevice());
      }
    }, CommModel.EventTypes.ADD_COMM_USER);
  }

  void addUIObject(CommUser u, CommDevice d) {
    uiObjects.add(new DeviceUI(u, d));
  }

  @Override
  public void renderStatic(GC gc, ViewPort vp) {}

  @Override
  public void renderDynamic(GC gc, ViewPort vp, long time) {
    helper.adapt(gc, vp);

    for (final DeviceUI ui : uiObjects) {
      ui.update(gc, vp, time);
    }
  }

  @Nullable
  @Override
  public ViewRect getViewRect() {
    return null;
  }

  /**
   * @return A new {@link Builder} for creating a {@link CommRenderer}.
   */
  public static Builder builder() {
    return new Builder();
  }

  class DeviceUI {
    final CommUser user;
    final CommDevice device;
    Optional<Color> color;

    DeviceUI(CommUser u, CommDevice d) {
      user = u;
      device = d;
      color = Optional.absent();
    }

    void update(GC gc, ViewPort vp, long time) {
      if (!color.isPresent()
        && viewOptions.contains(ViewOptions.RELIABILITY_COLOR)) {
        final RGB rgb = ColorUtil.interpolate(unreliableColor, reliableColor,
          device.getReliability());
        color = Optional.of(new Color(gc.getDevice(), rgb));
      }

      if (device.getMaxRange().isPresent()) {
        helper.drawCircle(user.getPosition().get(), device.getMaxRange().get());
        if (color.isPresent()) {
          gc.setBackground(color.get());
        } else {
          helper.setBackgroundSysCol(SWT.COLOR_DARK_BLUE);
        }
        gc.setAlpha(SEMI_TRANSPARENT);
        helper.fillCircle(user.getPosition().get(), device.getMaxRange().get());
      }

      gc.setAlpha(OPAQUE);
      helper.fillCircle(user.getPosition().get(), DOT_RADIUS);

      final StringBuilder sb = new StringBuilder();

      if (viewOptions.contains(ViewOptions.MSG_COUNT)) {
        sb.append(device.getReceivedCount())
          .append('(')
          .append(device.getUnreadCount())
          .append(')');
      }
      if (viewOptions.contains(ViewOptions.RELIABILITY_PERC)) {
        sb.append(" rel:")
          .append(String.format("%.2f", device.getReliability()));
      }
      if (sb.length() > 0) {
        helper.drawString(sb.toString(), user.getPosition().get(), true);
      }
    }

    void dispose() {
      if (color.isPresent()) {
        color.get().dispose();
      }
    }
  }

  /**
   * A builder for creating a {@link CommRenderer}.
   * @author Rinde van Lon
   */
  public static class Builder implements CanvasRendererBuilder {
    final Set<ViewOptions> viewOptions;
    RGB unreliableColor;
    RGB reliableColor;

    enum ViewOptions {
      RELIABILITY_COLOR, MSG_COUNT, RELIABILITY_PERC;
    }

    Builder() {
      viewOptions = EnumSet.noneOf(ViewOptions.class);
      reliableColor = defaultReliableColor();
      unreliableColor = defaultUnreliableColor();
    }

    /**
     * Shows the reliability as a percentage for every {@link CommDevice} on the
     * map.
     * @return This, as per the builder pattern.
     */
    public Builder showReliabilityPercentage() {
      viewOptions.add(ViewOptions.RELIABILITY_PERC);
      return this;
    }

    /**
     * Shows the message counts for every {@link CommDevice} on the map. The
     * format for display is as follows: <code>XX(Y)</code> where
     * <code>XX</code> is the total number of received messages and
     * <code>Y</code> is the number of unread messages.
     * @return This, as per the builder pattern.
     */
    public Builder showMessageCount() {
      viewOptions.add(ViewOptions.MSG_COUNT);
      return this;
    }

    /**
     * Shows the reliability of all {@link CommDevice}s by drawing them in color
     * ranging from red (0% reliability) to green (100% reliability). For using
     * different colors see {@link #showReliabilityColors(RGB, RGB)}.
     * @return This, as per the builder pattern.
     */
    public Builder showReliabilityColors() {
      viewOptions.add(ViewOptions.RELIABILITY_COLOR);
      return this;
    }

    /**
     * Shows the reliability of all {@link CommDevice}s by drawing them in color
     * ranging from unreliable color (0% reliability) to reliable color (100%
     * reliability). For using the default colors red and green see
     * {@link #showReliabilityColors()}.
     * @param unreliable The color that will be used as the negative extreme.
     * @param reliable The color that will be used as the positive extreme.
     * @return This, as per the builder pattern.
     */
    public Builder showReliabilityColors(RGB unreliable, RGB reliable) {
      reliableColor = reliable;
      unreliableColor = unreliable;
      return showReliabilityColors();
    }

    @Override
    public CanvasRenderer build(ModelProvider mp) {
      return new CommRenderer(this, mp.getModel(CommModel.class));
    }

    @Override
    public CanvasRendererBuilder copy() {
      final Builder copy = new Builder();
      copy.viewOptions.addAll(viewOptions);
      copy.unreliableColor = copy(unreliableColor);
      copy.reliableColor = copy(reliableColor);
      return copy;
    }

    static RGB defaultReliableColor() {
      return new RGB(0, OPAQUE, 0);
    }

    static RGB defaultUnreliableColor() {
      return new RGB(OPAQUE, 0, 0);
    }

    static RGB copy(RGB color) {
      return new RGB(color.red, color.green, color.blue);
    }
  }
}
