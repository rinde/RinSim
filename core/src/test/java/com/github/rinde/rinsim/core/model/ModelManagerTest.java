/*
 * Copyright (C) 2011-2016 Rinde van Lon, iMinds-DistriNet, KU Leuven
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
package com.github.rinde.rinsim.core.model;

import static com.github.rinde.rinsim.core.model.DebugModel.Action.ALLOW;
import static com.github.rinde.rinsim.core.model.DebugModel.Action.REJECT;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import org.junit.Before;
import org.junit.Test;

import com.github.rinde.rinsim.core.model.Model.AbstractModel;
import com.github.rinde.rinsim.core.model.road.GraphRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.ConnectionData;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.MultimapGraph;
import com.google.common.collect.ImmutableSet;

/**
 * Tests the model manager.
 * @author Rinde van Lon
 */
public class ModelManagerTest {

  @SuppressWarnings("null")
  ModelManager emptyManager;

  /**
   * Creates an empty model manager.
   */
  @Before
  public void setUp() {
    emptyManager = new ModelManager(ImmutableSet.<Model<?>>of());
  }

  /**
   * Tests that registering an object with a type not associated to any model
   * fails.
   */
  @Test
  public void registerFail() {
    boolean fail = false;
    try {
      emptyManager.register(new Object());
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertTrue(fail);
  }

  /**
   * Tests that only known types can be registered successfully.
   */
  @Test
  public void addOtherFooModel() {
    final OtherFooModel model = new OtherFooModel();

    final ModelManager mm = new ModelManager(ImmutableSet.of(model));
    mm.register(new Foo());
    boolean fail = false;
    try {
      mm.register(new Bar());
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertTrue(fail);
    assertEquals(1, model.calledRegister);
    assertEquals(1, model.calledTypes);
  }

  /**
   * Test correct working with two models.
   */
  @Test
  public void addWhenTwoModels() {
    final OtherFooModel model = new OtherFooModel();
    final BarModel model2 = new BarModel();
    final ModelManager mm = new ModelManager(ImmutableSet.of(model, model2));

    mm.register(new Foo());
    mm.register(new Bar());
    mm.register(new Foo());
    assertThat(model.calledRegister).isEqualTo(2);
    assertThat(model.calledTypes).isEqualTo(1);
    assertThat(model2.calledRegister).isEqualTo(1);
    assertThat((Iterable<?>) mm.getModels()).containsAllOf(model, model2);
  }

  /**
   * Checks if a faulty model is detected and rejected.
   */
  @SuppressWarnings("unused")
  @Test
  public void addFaultyModel() {
    final ModelA model = new ModelA();
    model.setSupportedType(null);
    boolean fail = false;
    try {
      new ModelManager(ImmutableSet.of(model));
    } catch (final NullPointerException e) {
      fail = true;
    }
    assertThat(fail).isTrue();
  }

  /**
   * A model can not be registered.
   */
  @Test(expected = IllegalArgumentException.class)
  public void canNotRegisterModel() {
    emptyManager.register(RoadModelBuilders.staticGraph(
        new MultimapGraph<LengthData>()).build(mock(DependencyProvider.class)));
  }

  /**
   * Tests that exception thrown by the broken model bubbles up.
   */
  @Test(expected = RuntimeException.class)
  public void registerWithBrokenModel() {
    final ModelManager mm = new ModelManager(ImmutableSet.of(
        RoadModelBuilders.staticGraph(
            new MultimapGraph<LengthData>())
            .build(mock(DependencyProvider.class)),
        new BrokenRoadModel(new MultimapGraph<LengthData>())));

    mm.register(new RoadUser() {
      @Override
      public void initRoadUser(RoadModel model) {}
    });
  }

  /**
   * Tests that unregistering an object with a type for which no model is
   * responsible yields an exception.
   */
  @Test(expected = IllegalArgumentException.class)
  public void unregisterFail() {
    emptyManager.unregister(new Object());
  }

  /**
   * Tests that unregistering a model is not possible.
   */
  @Test(expected = IllegalArgumentException.class)
  public void unregisterFailModel() {
    emptyManager.unregister(RoadModelBuilders.staticGraph(
        new MultimapGraph<LengthData>()).build(mock(DependencyProvider.class)));
  }

  /**
   * Tests that unregistering an not registered object fails.
   */
  @Test
  public void unregisterFailNotRegisteredObject() {
    final ModelManager mm = new ModelManager(
        ImmutableSet.of(
            RoadModelBuilders.staticGraph(new MultimapGraph<LengthData>())
                .build(mock(DependencyProvider.class)),
            RoadModelBuilders.staticGraph(new MultimapGraph<LengthData>())
                .build(mock(DependencyProvider.class))));

    boolean fail = false;
    try {
      mm.unregister(new RoadUser() {
        @Override
        public void initRoadUser(RoadModel model) {}
      });
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertTrue(fail);
  }

  /**
   * Tests that an exception thrown by a model during unregister bubbles up.
   */
  @Test
  public void unregisterWithBrokenModel() {
    final ModelManager mm = new ModelManager(
        ImmutableSet.of(RoadModelBuilders.staticGraph(
            new MultimapGraph<LengthData>())
            .build(mock(DependencyProvider.class)),
            new BrokenRoadModel(new MultimapGraph<LengthData>())));

    boolean fail = false;
    try {
      mm.unregister(new RoadUser() {
        @Override
        public void initRoadUser(RoadModel model) {}
      });
    } catch (final RuntimeException e) {
      fail = true;
    }
    assertThat(fail).isTrue();
  }

  /**
   * Tests that registering and unregistering an object of a type not associated
   * to any model fails.
   */
  @Test
  public void registerAndUnregisterFail() {
    final OtherFooModel model = new OtherFooModel();
    final ModelManager mm = new ModelManager(ImmutableSet.of(model));
    final Object o = new Object();

    boolean fail = false;
    try {
      mm.unregister(o);
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertTrue(fail);

    // it wont be registered
    fail = false;
    try {
      mm.register(o);
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertTrue(fail);

    fail = false;
    try {
      mm.unregister(o);
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertTrue(fail);
  }

  /**
   * Tests for unregistering a previously registered object.
   */
  public void unregisterRegistered() {
    final OtherFooModel model = new OtherFooModel();
    final BarModel model2 = new BarModel();
    emptyManager.register(model);
    emptyManager.register(model2);

    final Foo foo = new Foo();
    final Bar bar = new Bar();

    emptyManager.register(foo);
    emptyManager.register(bar);

    emptyManager.unregister(foo);

    assertEquals(1, model.calledRegister);
    assertEquals(1, model2.calledRegister);
    assertEquals(1, model.callUnregister);
  }

  /**
   * Test with a lot of different models.
   */
  @Test
  public void manyModelsTest() {
    final ModelA mA = new ModelA();
    final ModelAA mAA = new ModelAA();
    final ModelB mB = new ModelB();
    final ModelB mB2 = new ModelB();
    final ModelBB mBB = new ModelBB();
    final ModelBBB mBBB = new ModelBBB();
    final SpecialModelB mSB = new SpecialModelB();
    final ModelC mC = new ModelC();

    final ModelManager mm = new ModelManager(ImmutableSet.<Model<?>>of(
        mA, mAA, mB, mB2, mBB, mBBB, mSB, mC));

    final ObjectA a1 = new ObjectA();
    mm.register(a1);
    assertEquals(asList(a1), mA.getRegisteredElements());
    assertEquals(asList(a1), mAA.getRegisteredElements());

    mA.setRegisterAction(REJECT);
    final ObjectA a2 = new ObjectA();
    mm.register(a2);
    assertEquals(asList(a1, a2), mA.getRegisteredElements());
    assertEquals(asList(a1, a2), mAA.getRegisteredElements());

    mAA.setRegisterAction(REJECT);
    final ObjectA a3 = new ObjectA();
    boolean fail = false;
    try {
      mm.register(a3);
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertTrue(fail);
    assertEquals(asList(a1, a2, a3), mA.getRegisteredElements());
    assertEquals(asList(a1, a2, a3), mAA.getRegisteredElements());

    mA.setRegisterAction(ALLOW);
    mAA.setRegisterAction(ALLOW);
    // allow duplicates
    mm.register(a1);
    assertEquals(asList(a1, a2, a3, a1), mA.getRegisteredElements());
    assertEquals(asList(a1, a2, a3, a1), mAA.getRegisteredElements());

    final ObjectB b1 = new ObjectB();
    mm.register(b1);
    assertEquals(asList(b1), mB.getRegisteredElements());
    assertEquals(asList(b1), mB2.getRegisteredElements());
    assertEquals(asList(b1), mBB.getRegisteredElements());
    assertEquals(asList(b1), mBBB.getRegisteredElements());
    assertEquals(asList(), mSB.getRegisteredElements());

    // subclass of B is registered in all general models and its subclass
    // model
    final SpecialB s1 = new SpecialB();
    mm.register(s1);
    assertEquals(asList(b1, s1), mB.getRegisteredElements());
    assertEquals(asList(b1, s1), mB2.getRegisteredElements());
    assertEquals(asList(b1, s1), mBB.getRegisteredElements());
    assertEquals(asList(b1, s1), mBBB.getRegisteredElements());
    assertEquals(asList(s1), mSB.getRegisteredElements());

    assertTrue(mC.getRegisteredElements().isEmpty());

    // unregister not registered object
    final ObjectA a4 = new ObjectA();
    mm.unregister(a4);
    assertEquals(asList(a4), mA.getUnregisteredElements());
    assertEquals(asList(a4), mAA.getUnregisteredElements());

    // try again, this time with models rejecting unregister
    mA.setUnregisterAction(REJECT);
    mAA.setUnregisterAction(REJECT);
    fail = false;
    try {
      mm.unregister(a4);
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertTrue(fail);
    assertEquals(asList(a4, a4), mA.getUnregisteredElements());
    assertEquals(asList(a4, a4), mAA.getUnregisteredElements());

    mm.unregister(b1);
    assertEquals(asList(b1), mB.getUnregisteredElements());
    assertEquals(asList(b1), mB2.getUnregisteredElements());
    assertEquals(asList(b1), mBB.getUnregisteredElements());
    assertEquals(asList(b1), mBBB.getUnregisteredElements());
    assertEquals(asList(), mSB.getUnregisteredElements());

    mm.unregister(s1);
    assertEquals(asList(b1, s1), mB.getUnregisteredElements());
    assertEquals(asList(b1, s1), mB2.getUnregisteredElements());
    assertEquals(asList(b1, s1), mBB.getUnregisteredElements());
    assertEquals(asList(b1, s1), mBBB.getUnregisteredElements());
    assertEquals(asList(s1), mSB.getUnregisteredElements());

  }

  /**
   * Test for anonymous model.
   */
  @Test
  public void anonymousModelTest() {
    final ModelManager mm = new ModelManager(ImmutableSet.of(
        new Model<InnerObject>() {
          @Override
          public boolean register(InnerObject element) {
            return false;
          }

          @Override
          public boolean unregister(InnerObject element) {
            return false;
          }

          @Override
          public Class<InnerObject> getSupportedType() {
            return InnerObject.class;
          }

          @Override
          public <T> T get(Class<T> clazz) {
            throw new UnsupportedOperationException();
          }
        }));

    boolean fail = false;
    try {
      mm.register(new InnerObject());
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertTrue(fail);
  }

  class InnerObject {}
}

class BrokenRoadModel extends GraphRoadModel {
  public BrokenRoadModel(Graph<? extends ConnectionData> pGraph) {
    super(pGraph, RoadModelBuilders.staticGraph(pGraph));
  }

  @Override
  public boolean doRegister(RoadUser obj) {
    throw new RuntimeException("intended failure");
  }

  @Override
  public boolean unregister(RoadUser obj) {
    throw new RuntimeException("intended failure");
  }
}

class OtherFooModel implements Model<Foo> {

  int calledTypes;
  int calledRegister;
  int callUnregister;

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

  @Override
  public boolean unregister(Foo element) {
    callUnregister += 1;
    return true;
  }

  @Override
  public <T> T get(Class<T> clazz) {
    throw new UnsupportedOperationException();
  }
}

class BarModel extends AbstractModel<Bar> {
  int calledRegister;

  protected BarModel() {}

  @Override
  public boolean register(Bar element) {
    calledRegister += 1;
    return true;
  }

  @Override
  public boolean unregister(Bar element) {
    return false;
  }
}

class ObjectA {}

class ObjectB {}

class SpecialB extends ObjectB {}

class ObjectC {}

class ModelA extends DebugModel<ObjectA> {
  ModelA() {
    super(ObjectA.class);
  }
}

class ModelAA extends DebugModel<ObjectA> {
  ModelAA() {
    super(ObjectA.class);
  }
}

class ModelB extends DebugModel<ObjectB> {
  ModelB() {
    super(ObjectB.class);
  }
}

class ModelBB extends DebugModel<ObjectB> {
  ModelBB() {
    super(ObjectB.class);
  }
}

class ModelBBB extends DebugModel<ObjectB> {
  ModelBBB() {
    super(ObjectB.class);
  }
}

class SpecialModelB extends DebugModel<SpecialB> {
  SpecialModelB() {
    super(SpecialB.class);
  }
}

class ModelC extends DebugModel<ObjectC> {
  ModelC() {
    super(ObjectC.class);
  }
}

class DebugModel<T> implements Model<T> {

  enum Action {
    ALLOW, REJECT, FAIL
  }

  private Action registerAction;
  private Action unregisterAction;
  private Class<T> supportedType;
  private final List<T> registeredElements;
  private final List<T> unregisteredElements;

  DebugModel(Class<T> type) {
    supportedType = type;
    registeredElements = new ArrayList<>();
    unregisteredElements = new ArrayList<>();
    registerAction = ALLOW;
    unregisterAction = ALLOW;
  }

  public void setRegisterAction(Action a) {
    registerAction = a;
  }

  public void setUnregisterAction(Action a) {
    unregisterAction = a;
  }

  @SuppressWarnings("null")
  public void setSupportedType(@Nullable Class<T> type) {
    supportedType = type;
  }

  @Override
  public boolean register(T element) {
    registeredElements.add(element);
    return actionResponse(registerAction);
  }

  @Override
  public boolean unregister(T element) {
    unregisteredElements.add(element);
    return actionResponse(unregisterAction);
  }

  public List<T> getRegisteredElements() {
    return Collections.unmodifiableList(registeredElements);
  }

  public List<T> getUnregisteredElements() {
    return Collections.unmodifiableList(unregisteredElements);
  }

  private static boolean actionResponse(Action a) {
    switch (a) {
    case ALLOW:
      return true;
    case REJECT:
      return false;
    case FAIL:
      throw new RuntimeException("this is an intentional failure");
    default:
      throw new IllegalStateException();
    }
  }

  @Override
  public Class<T> getSupportedType() {
    return supportedType;
  }

  @Override
  public <T> T get(Class<T> clazz) {
    throw new UnsupportedOperationException();
  }
}

class Foo {}

class Bar {}
