/*
 * Copyright (C) 2011-2018 Rinde R.S. van Lon
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

import java.awt.im.InputContext;
import java.util.LinkedHashSet;
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
import com.google.auto.value.AutoValue;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * The view class is the main GUI class. For creating a view, see
 * {@link #builder()}.
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * @author Bartosz Michalik
 * @since 2.0
 */
public final class View
    extends AbstractModel<Void>
    implements TickListener, UserInterface, MainView {

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
    final Display d = builder.display().isPresent()
      ? builder.display().get()
      : Display.getCurrent();
    final boolean isDisplayOwner = d == null;
    display = isDisplayOwner ? new Display() : Display.getCurrent();

    int shellArgs = SWT.TITLE | SWT.CLOSE;
    if (!builder.viewOptions().contains(ViewOption.DISALLOW_RESIZE)) {
      shellArgs = shellArgs | SWT.RESIZE;
    }

    shell = new Shell(display, shellArgs);

    if (builder.monitor().isPresent()) {
      final Monitor m = builder.monitor().get();
      shell.setLocation(m.getBounds().x, m.getBounds().y);
    }

    shell.setText("RinSim - " + builder.title());
    if (builder.viewOptions().contains(ViewOption.FULL_SCREEN)) {
      shell.setFullScreen(true);
      shell.setMaximized(true);
    } else {
      shell.setSize(builder.screenSize());
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
        if (builder.viewOptions().contains(ViewOption.AUTO_CLOSE)) {
          close();
        }
      }
    }, Clock.ClockEventType.STOPPED);
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
    if (builder.callback().isPresent()) {
      builder.callback().get()
        .handleEvent(new Event(Clock.ClockEventType.STOPPED, null));
    }
  }

  @Override
  public void show() {
    shell.open();
    for (final Listener l : listeners) {
      l.handleEvent(new Event(EventType.SHOW, this));
    }
    if (!builder.viewOptions().contains(ViewOption.ASYNC)) {
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
    if (builder.stopTime() > 0 && time.getTime() >= builder.stopTime()) {
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
  public static Builder builder() {
    return Builder.create();
  }

  enum ViewOption {
    AUTO_PLAY, AUTO_CLOSE, DISALLOW_RESIZE, FULL_SCREEN, ASYNC;
  }

  /**
   * A builder that creates a visualization for {@link Simulator} instances.
   * @author Rinde van Lon
   */
  @AutoValue
  public abstract static class Builder extends AbstractModelBuilder<View, Void>
      implements CompositeModelBuilder<View, Void> {
    /**
     * The default window size: 800x600.
     */
    public static final Point DEFAULT_WINDOW_SIZE = new Point(800, 600);
    private static final long serialVersionUID = -955386603340399937L;

    Builder() {
      setDependencies(ClockController.class);
      setProvidingTypes(Shell.class, Device.class, Display.class,
        MainView.class);
    }

    abstract ImmutableSet<ModelBuilder<? extends Renderer, ?>> renderers();

    abstract ImmutableSet<ViewOption> viewOptions();

    abstract ImmutableMap<MenuItems, Integer> accelerators();

    abstract int speedUp();

    abstract long stopTime();

    abstract String title();

    abstract Point screenSize();

    abstract Optional<Listener> callback();

    abstract Optional<Monitor> monitor();

    abstract Optional<Display> display();

    /**
     * Adds the specified builder of a {@link Renderer}.
     * @param builder The builder to add.
     * @return A new builder instance.
     */
    @CheckReturnValue
    public Builder with(ModelBuilder<? extends Renderer, ?> builder) {
      return create(ImmutableSet
        .<ModelBuilder<? extends Renderer, ?>>builder()
        .addAll(renderers())
        .add(builder)
        .build(),
        viewOptions(), accelerators(), speedUp(), stopTime(), title(),
        screenSize(), callback(), monitor(), display());
    }

    /**
     * When <i>auto play</i> is enabled the {@link Simulator} will be started
     * directly when {@link #show()} is called. Default: <code>disabled</code>.
     * @return A new builder instance.
     */
    @CheckReturnValue
    public Builder withAutoPlay() {
      return create(
        renderers(),
        Sets.immutableEnumSet(ViewOption.AUTO_PLAY,
          viewOptions().toArray(new ViewOption[] {})),
        accelerators(), speedUp(), stopTime(), title(), screenSize(),
        callback(), monitor(), display());
    }

    /**
     * When <i>auto close</i> is enabled the view will be closed as soon as the
     * {@link Simulator} is stopped. This is useful for creating automated GUIs.
     * Default: <code>disabled</code>.
     * @return A new builder instance.
     */
    @CheckReturnValue
    public Builder withAutoClose() {
      return create(
        renderers(),
        Sets.immutableEnumSet(ViewOption.AUTO_CLOSE,
          viewOptions().toArray(new ViewOption[] {})),
        accelerators(), speedUp(), stopTime(), title(), screenSize(),
        callback(), monitor(), display());
    }

    /**
     * Stops the simulator at the specified time.
     * @param simulationTime The time to stop, must be positive.
     * @return A new builder instance.
     */
    @CheckReturnValue
    public Builder withSimulatorEndTime(long simulationTime) {
      checkArgument(simulationTime > 0);
      return create(renderers(), viewOptions(), accelerators(), speedUp(),
        simulationTime,
        title(), screenSize(), callback(), monitor(), display());
    }

    /**
     * Speed up defines the simulation time between two respective GUI draw
     * operations. Default: <code>1</code>.
     * @param speed The speed to use.
     * @return A new builder instance.
     */
    @CheckReturnValue
    public Builder withSpeedUp(int speed) {
      return create(renderers(), viewOptions(), accelerators(), speed,
        stopTime(), title(), screenSize(), callback(), monitor(), display());
    }

    /**
     * Should be used in case there is already an SWT application running that
     * was launched from the same VM as the current GUI that is created.
     * @param d The existing {@link Display} to use as display for the view.
     * @return A new builder instance.
     */
    @CheckReturnValue
    public Builder withDisplay(Display d) {
      return create(renderers(), viewOptions(), accelerators(), speedUp(),
        stopTime(), title(), screenSize(), callback(), monitor(),
        Optional.of(d));
    }

    /**
     * Changes the title appendix of the view. The default title is <i>RinSim -
     * Simulator</i>, the title appendix is everything after the dash.
     * @param titleAppendix The new appendix to use.
     * @return A new builder instance.
     */
    @CheckReturnValue
    public Builder withTitleAppendix(String titleAppendix) {
      return create(renderers(), viewOptions(), accelerators(), speedUp(),
        stopTime(), titleAppendix, screenSize(), callback(), monitor(),
        display());
    }

    /**
     * Don't allow the user to resize the application window. Default:
     * <i>allowed</i>.
     * @return A new builder instance.
     */
    @CheckReturnValue
    public Builder withNoResizing() {
      return create(
        renderers(),
        Sets.immutableEnumSet(ViewOption.DISALLOW_RESIZE,
          viewOptions().toArray(new ViewOption[] {})),
        accelerators(), speedUp(), stopTime(), title(), screenSize(),
        callback(), monitor(), display());
    }

    /**
     * This takes precedence over any calls to {@link #withResolution(int, int)}
     * .
     * @return A new builder instance.
     */
    @CheckReturnValue
    public Builder withFullScreen() {
      return create(
        renderers(),
        Sets.immutableEnumSet(ViewOption.FULL_SCREEN,
          viewOptions().toArray(new ViewOption[] {})),
        accelerators(), speedUp(), stopTime(), title(), screenSize(),
        callback(), monitor(), display());
    }

    /**
     * Change the resolution of the window. Default resolution:
     * {@link #DEFAULT_WINDOW_SIZE}.
     * @param width The new width to use.
     * @param height The new height to use.
     * @return A new builder instance.
     */
    @CheckReturnValue
    public Builder withResolution(int width, int height) {
      checkArgument(width > 0 && height > 0,
        "Only positive dimensions are allowed, input: %s x %s.", width,
        height);
      return create(renderers(), viewOptions(), accelerators(), speedUp(),
        stopTime(), title(), new Point(width, height), callback(), monitor(),
        display());
    }

    /**
     * Specify on which monitor the application should be positioned. If this
     * method is not called SWT decides where the screen is positioned, usually
     * on the primary monitor.
     * @param m The monitor.
     * @return A new builder instance.
     */
    @CheckReturnValue
    public Builder withMonitor(Monitor m) {
      return create(renderers(), viewOptions(), accelerators(), speedUp(),
        stopTime(), title(), screenSize(), callback(), Optional.of(m),
        display());
    }

    /**
     * Allows to change the accelerators (aka shortcuts) of the menu items. Each
     * accelerator is set to its respective menu item via
     * {@link org.eclipse.swt.widgets.MenuItem#setAccelerator(int)}. By default
     * the accelerators are set to {@link MenuItems#QWERTY_ACCELERATORS} unless
     * a keyboard for the French language is detected (probably using an AZERTY
     * layout), then {@link MenuItems#AZERTY_ACCELERATORS} is used.
     * @param acc The accelerators to set.
     * @return A new builder instance.
     */
    @CheckReturnValue
    public Builder withAccelerators(Map<MenuItems, Integer> acc) {
      return create(renderers(), viewOptions(), ImmutableMap.copyOf(acc),
        speedUp(), stopTime(), title(), screenSize(), callback(), monitor(),
        display());
    }

    /**
     * Sets the view into asynchronous mode. This means that the call to
     * {@link #show()} is non-blocking and will return immediately. By default
     * the {@link #show()} is synchronous.
     * @return A new builder instance.
     */
    @CheckReturnValue
    public Builder withAsync() {
      return create(
        renderers(),
        Sets.immutableEnumSet(ViewOption.ASYNC,
          viewOptions().toArray(new ViewOption[] {})),
        accelerators(), speedUp(), stopTime(), title(), screenSize(),
        callback(), monitor(), display());
    }

    /**
     * Allows to register a {@link Listener} for stop events of the simulator.
     * @param l The listener to register, overwrites previous listeners if any.
     * @return A new builder instance.
     */
    @CheckReturnValue
    public Builder withCallback(Listener l) {
      return create(renderers(), viewOptions(), accelerators(), speedUp(),
        stopTime(), title(), screenSize(), Optional.of(l), monitor(),
        display());
    }

    @CheckReturnValue
    @Override
    public View build(DependencyProvider dependencyProvider) {
      checkArgument(!renderers().isEmpty(),
        "At least one renderer needs to be defined.");

      final ClockController cc = dependencyProvider.get(ClockController.class);
      return new View(this, cc);
    }

    @Override
    public ImmutableSet<ModelBuilder<?, ?>> getChildren() {
      return ImmutableSet.<ModelBuilder<?, ?>>builder()
        .add(SimulationViewer.builder(this))
        .addAll(renderers())
        .build();
    }

    static Builder create() {
      final ImmutableMap<MenuItems, Integer> accelerators;
      @Nullable
      final Locale loc = InputContext.getInstance().getLocale();
      if (loc != null
        && loc.getLanguage().equals(Locale.FRENCH.getLanguage())) {
        accelerators = MenuItems.AZERTY_ACCELERATORS;
      } else {
        accelerators = MenuItems.QWERTY_ACCELERATORS;
      }

      return create(ImmutableSet.<ModelBuilder<? extends Renderer, ?>>of(),
        ImmutableSet.<ViewOption>of(), accelerators, 1, -1L, "Simulator",
        DEFAULT_WINDOW_SIZE,
        Optional.<Listener>absent(),
        Optional.<Monitor>absent(),
        Optional.<Display>absent());
    }

    static Builder create(
        ImmutableSet<ModelBuilder<? extends Renderer, ?>> renderers,
        ImmutableSet<ViewOption> viewOptions,
        ImmutableMap<MenuItems, Integer> accelerators,
        int speedUp,
        long stopTime,
        String title,
        Point screenSize,
        Optional<Listener> callback,
        Optional<Monitor> monitor,
        Optional<Display> display) {
      return new AutoValue_View_Builder(renderers, viewOptions, accelerators,
        speedUp, stopTime, title, screenSize, callback, monitor, display);
    }
  }
}
