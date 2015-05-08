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
package com.github.rinde.rinsim.ui;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Arrays.asList;

import java.awt.im.InputContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.CompositeModelBuilder;
import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.Model.AbstractModel;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.model.UserInterface;
import com.github.rinde.rinsim.core.model.time.Clock;
import com.github.rinde.rinsim.core.model.time.ClockController;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.Listener;
import com.github.rinde.rinsim.ui.renderers.Renderer;
import com.google.common.collect.ImmutableSet;

/**
 * The view class is the main GUI class. For creating a view, see
 * {@link #create()}.
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * @author Bartosz Michalik
 * @since 2.0
 */
public final class View extends AbstractModel<Void> implements
  TickListener, UserInterface, MainView {

  final Builder builder;
  final ClockController clockController;
  final Shell shell;
  final Display display;
  final Set<Listener> listeners;

  View(Builder b, ClockController cc) {
    builder = b;
    clockController = cc;

    listeners = new LinkedHashSet<>();
    Display.setAppName("RinSim");
    final Display d = builder.display != null ? builder.display : Display
      .getCurrent();
    final boolean isDisplayOwner = d == null;
    display = isDisplayOwner ? new Display() : Display.getCurrent();

    int shellArgs = SWT.TITLE | SWT.CLOSE;
    if (builder.allowResize) {
      shellArgs = shellArgs | SWT.RESIZE;
    }

    shell = new Shell(display, shellArgs);

    if (builder.monitor != null) {
      final Monitor m = builder.monitor;
      shell.setLocation(m.getBounds().x, m.getBounds().y);
    }

    shell.setText("RinSim - " + builder.title);
    if (builder.fullScreen) {
      shell.setFullScreen(true);
      shell.setMaximized(true);
    } else {
      shell.setSize(builder.screenSize);
    }

    shell.addListener(SWT.Close, new org.eclipse.swt.widgets.Listener() {
      @Override
      public void handleEvent(@Nullable org.eclipse.swt.widgets.Event event) {
        clockController.stop();
        while (clockController.isTicking()) {
          // wait until clock actually stops (it finishes its
          // current tick first).
        }
        if (isDisplayOwner && !display.isDisposed()) {
          display.dispose();
        } else if (!isDisplayOwner && !shell.isDisposed()) {
          shell.dispose();
        }
      }
    });
    clockController.getEventAPI().addListener(new Listener() {
      @Override
      public void handleEvent(Event e) {
        if (e.getEventType() == Clock.ClockEventType.STARTED) {
          // show();
        } else if (builder.autoClose) {
          close();
        }
      }
    },
      Clock.ClockEventType.STARTED, Clock.ClockEventType.STOPPED);
  }

  void close() {
    if (!shell.isDisposed()) {
      display.asyncExec(new Runnable() {
        @Override
        public void run() {
          shell.close();
        }
      });
    }
    if (builder.callback != null) {
      builder.callback
        .handleEvent(new Event(Clock.ClockEventType.STOPPED, null));
    }
  }

  @Override
  public void show() {
    shell.open();
    for (final Listener l : listeners) {
      l.handleEvent(new Event(EventType.SHOW, this));
    }
    if (!builder.async) {
      while (!shell.isDisposed()) {
        if (!display.readAndDispatch()) {
          display.sleep();
        }
      }
      if (shell.isDisposed()) {
        clockController.stop();
      }
    }

  }

  @Override
  public void tick(TimeLapse time) {}

  @Override
  public void afterTick(TimeLapse time) {
    if (builder.stopTime > 0 && time.getTime() >= builder.stopTime) {
      clockController.stop();
    }
  }

  @Override
  public boolean register(Void element) {
    return false;
  }

  @Override
  public boolean unregister(Void element) {
    return false;
  }

  @Override
  public void addListener(Listener l) {
    listeners.add(l);
  }

  @Override
  public <U> U get(Class<U> clazz) {
    if (clazz == Shell.class) {
      return clazz.cast(shell);
    }
    if (clazz == Device.class || clazz == Display.class) {
      return clazz.cast(shell.getDisplay());
    }
    if (clazz == MainView.class) {
      return clazz.cast(this);
    }
    throw new IllegalArgumentException("Unknown type: " + clazz);
  }

  /**
   * Creates a {@link View.Builder}. The returned builder allows to configure
   * the visualization.
   * @return The {@link View.Builder}.
   */
  @CheckReturnValue
  public static Builder create() {
    return new Builder();
  }

  /**
   * A builder that creates a visualization for {@link Simulator} instances.
   * @author Rinde van Lon
   */
  public static class Builder extends AbstractModelBuilder<View, Void>
    implements CompositeModelBuilder<View, Void> {
    /**
     * The default window size: 800x600.
     */
    public static final Point DEFAULT_WINDOW_SIZE = new Point(800, 600);

    boolean autoPlay;
    boolean autoClose;
    boolean allowResize;
    boolean fullScreen;
    boolean async;
    int speedUp;
    long stopTime;
    @Nullable
    Display display;
    String title;
    Point screenSize;
    @Nullable
    Monitor monitor;
    final List<Object> rendererList;
    Set<ModelBuilder<?, ?>> renderers;
    Map<MenuItems, Integer> accelerators;
    @Nullable
    Listener callback;

    Builder() {
      setDependencies(ClockController.class);
      setProvidingTypes(Shell.class, Device.class, Display.class,
        MainView.class);
      autoPlay = false;
      autoClose = false;
      allowResize = true;
      fullScreen = false;
      title = "Simulator";
      speedUp = 1;
      stopTime = -1;
      screenSize = DEFAULT_WINDOW_SIZE;
      rendererList = new ArrayList<>();
      renderers = new LinkedHashSet<>();
      accelerators = new HashMap<>();

      @Nullable
      final Locale loc = InputContext.getInstance().getLocale();
      if (loc != null && loc.getLanguage().equals(Locale.FRENCH.getLanguage())) {
        accelerators.putAll(MenuItems.AZERTY_ACCELERATORS);
      } else {
        accelerators.putAll(MenuItems.QWERTY_ACCELERATORS);
      }
    }

    /**
     * Adds the specified renderers.
     * @param renderers The {@link Renderer}s to add to the view.
     * @return This, as per the builder pattern.
     */
    @CheckReturnValue
    public Builder with(Renderer... renderers) {
      rendererList.addAll(asList(renderers));
      return this;
    }

    /**
     * Adds the specified builder of a {@link Renderer}.
     * @param builder The builder to add.
     * @return This, as per the builder pattern.
     */
    @CheckReturnValue
    public Builder with(ModelBuilder<?, ?> builder) {
      renderers.add(builder);
      return this;
    }

    /**
     * When <i>auto play</i> is enabled the {@link Simulator} will be started
     * directly when {@link #show()} is called. Default: <code>disabled</code>.
     * @return This as per the builder pattern.
     */
    @CheckReturnValue
    public Builder enableAutoPlay() {
      autoPlay = true;
      return this;
    }

    /**
     * When <i>auto close</i> is enabled the view will be closed as soon as the
     * {@link Simulator} is stopped. This is useful for creating automated GUIs.
     * Default: <code>disabled</code>.
     * @return This as per the builder pattern.
     */
    @CheckReturnValue
    public Builder enableAutoClose() {
      autoClose = true;
      return this;
    }

    /**
     * Stops the simulator at the specified time.
     * @param simulationTime The time to stop, must be positive.
     * @return This as per the builder pattern.
     */
    @CheckReturnValue
    public Builder stopSimulatorAtTime(long simulationTime) {
      checkArgument(simulationTime > 0);
      stopTime = simulationTime;
      return this;
    }

    /**
     * Speed up defines the simulation time between two respective GUI draw
     * operations. Default: <code>1</code>.
     * @param speed The speed to use.
     * @return This as per the builder pattern.
     */
    @CheckReturnValue
    public Builder setSpeedUp(int speed) {
      speedUp = speed;
      return this;
    }

    /**
     * Should be used in case there is already an SWT application running that
     * was launched from the same VM as the current GUI that is created.
     * @param d The existing {@link Display} to use as display for the view.
     * @return This as per the builder pattern.
     */
    @CheckReturnValue
    public Builder setDisplay(Display d) {
      display = d;
      return this;
    }

    /**
     * Changes the title appendix of the view. The default title is <i>RinSim -
     * Simulator</i>, the title appendix is everything after the dash.
     * @param titleAppendix The new appendix to use.
     * @return This as per the builder pattern.
     */
    @CheckReturnValue
    public Builder setTitleAppendix(String titleAppendix) {
      title = titleAppendix;
      return this;
    }

    /**
     * Don't allow the user to resize the application window. Default:
     * <i>allowed</i>.
     * @return This as per the builder pattern.
     */
    @CheckReturnValue
    public Builder disallowResizing() {
      allowResize = false;
      return this;
    }

    /**
     * This takes precedence over any calls to {@link #setResolution(int, int)}.
     * @return This, as per the builder pattern.
     */
    @CheckReturnValue
    public Builder setFullScreen() {
      fullScreen = true;
      return this;
    }

    /**
     * Change the resolution of the window. Default resolution:
     * {@link #DEFAULT_WINDOW_SIZE}.
     * @param width The new width to use.
     * @param height The new height to use.
     * @return This, as per the builder pattern.
     */
    @CheckReturnValue
    public Builder setResolution(int width, int height) {
      checkArgument(width > 0 && height > 0,
        "Only positive dimensions are allowed, input: %s x %s.", width,
        height);
      screenSize = new Point(width, height);
      return this;
    }

    /**
     * Specify on which monitor the application should be positioned. If this
     * method is not called SWT decides where the screen is positioned, usually
     * on the primary monitor.
     * @param m The monitor.
     * @return This, as per the builder pattern.
     */
    @CheckReturnValue
    public Builder displayOnMonitor(Monitor m) {
      monitor = m;
      return this;
    }

    /**
     * Allows to change the accelerators (aka shortcuts) of the menu items. Each
     * accelerator is set to its respective menu item via
     * {@link org.eclipse.swt.widgets.MenuItem#setAccelerator(int)}. By default
     * the accelerators are set to {@link MenuItems#QWERTY_ACCELERATORS} unless
     * a keyboard for the French language is detected (probably using an AZERTY
     * layout), then {@link MenuItems#AZERTY_ACCELERATORS} is used.
     * @param acc The accelerators to set.
     * @return This, as per the builder pattern.
     */
    @CheckReturnValue
    public Builder setAccelerators(Map<MenuItems, Integer> acc) {
      accelerators.putAll(acc);
      return this;
    }

    /**
     * Sets the view into asynchronous mode. This means that the call to
     * {@link #show()} is non-blocking and will return immediately. By default
     * the {@link #show()} is synchronous.
     * @return This, as per the builder pattern.
     */
    @CheckReturnValue
    public Builder setAsync() {
      async = true;
      return this;
    }

    /**
     * Allows to register a {@link Listener} for stop events of the simulator.
     * @param l The listener to register, overwrites previous listeners if any.
     * @return This, as per the builder pattern.
     */
    @CheckReturnValue
    public Builder setCallback(Listener l) {
      callback = l;
      return this;
    }

    @CheckReturnValue
    @Override
    public View build(DependencyProvider dependencyProvider) {
      checkArgument(!(rendererList.isEmpty() && renderers.isEmpty()),
        "At least one renderer needs to be defined.");

      final ClockController cc = dependencyProvider.get(ClockController.class);
      return new View(this, cc);
    }

    @Override
    public ImmutableSet<ModelBuilder<?, ?>> getChildren() {
      return ImmutableSet.<ModelBuilder<?, ?>> builder()
        .add(new SimulationViewer.Builder(this))
        .addAll(renderers)
        .build();
    }
  }

}

interface MainView {
  enum EventType {
    SHOW;
  }

  void addListener(Listener l);
}
