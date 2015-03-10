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
package com.github.rinde.rinsim.core.model;

import static com.github.rinde.rinsim.core.model.DebugModel.Action.ALLOW;
import static com.github.rinde.rinsim.core.model.DebugModel.Action.REJECT;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.measure.unit.SI;

import org.junit.Before;
import org.junit.Test;

import com.github.rinde.rinsim.core.model.road.GraphRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.ConnectionData;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.MultimapGraph;

public class ModelManagerTest {

  protected ModelManager manager;

  @Before
  public void setUp() {
    manager = new ModelManager();
  }

  @Test(expected = IllegalStateException.class)
  public void notConfigured() {
    manager.register(new Object());
  }

  @Test
  public void addToEmpty() {
    manager.configure();
    boolean fail = false;
    try {
      manager.register(new Object());
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertTrue(fail);
  }

  @Test
  public void addOtherFooModel() {
    final OtherFooModel model = new OtherFooModel();
    manager.register(model);
    manager.configure();
    assertTrue(manager.register(new Foo()));
    boolean fail = false;
    try {
      manager.register(new Bar());
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertTrue(fail);
    assertEquals(1, model.calledRegister);
    assertEquals(1, model.calledTypes);
  }

  @Test
  public void addWhenTwoModels() {
    final OtherFooModel model = new OtherFooModel();
    final BarModel model2 = new BarModel();
    manager.register(model);
    manager.register(model2);
    manager.configure();
    assertTrue(manager.register(new Foo()));
    assertTrue(manager.register(new Bar()));
    assertTrue(manager.register(new Foo()));
    assertEquals(2, model.calledRegister);
    assertEquals(1, model.calledTypes);
    assertEquals(1, model2.calledRegister);

    assertArrayEquals(new Model<?>[] { model, model2 }, manager.getModels()
        .toArray(new Model<?>[2]));
  }

  @Test
  public void addDuplicateModel() {
    final OtherFooModel model = new OtherFooModel();
    assertTrue(manager.add(model));
    assertFalse(manager.add(model));
  }

  @Test(expected = IllegalArgumentException.class)
  public void addFaultyModel() {
    final ModelA model = new ModelA();
    model.setSupportedType(null);
    manager.add(model);
  }

  @Test(expected = IllegalStateException.class)
  public void registerModelTooLate() {
    manager.configure();
    manager.register(new GraphRoadModel(new MultimapGraph<LengthData>(),
        SI.METER, SI.METERS_PER_SECOND));
  }

  @Test(expected = IllegalStateException.class)
  public void addModelTooLate() {
    manager.configure();
    manager.add(new GraphRoadModel(new MultimapGraph<LengthData>(), SI.METER,
        SI.METERS_PER_SECOND));
  }

  @Test(expected = RuntimeException.class)
  public void registerWithBrokenModel() {
    manager.add(new GraphRoadModel(new MultimapGraph<LengthData>(), SI.METER,
        SI.METERS_PER_SECOND));
    manager.add(new BrokenRoadModel(new MultimapGraph<LengthData>()));
    manager.configure();
    manager.register(new RoadUser() {
      @Override
      public void initRoadUser(RoadModel model) {}
    });
  }

  @Test
  public void unregisterWithoutModels() {
    manager.configure();
    assertEquals(0, manager.getModels().size());
    boolean fail = false;
    try {
      manager.unregister(new Object());
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertTrue(fail);
  }

  @Test(expected = IllegalArgumentException.class)
  public void unregisterModel() {
    manager.unregister(new GraphRoadModel(new MultimapGraph<LengthData>(),
        SI.METER, SI.METERS_PER_SECOND));
  }

  @Test(expected = IllegalStateException.class)
  public void unregisterWhenNotConfigured() {
    manager.unregister(new Object());
  }

  @Test
  public void unregister() {
    manager.add(new GraphRoadModel(new MultimapGraph<LengthData>(), SI.METER,
        SI.METERS_PER_SECOND));
    manager.add(new GraphRoadModel(new MultimapGraph<LengthData>(), SI.METER,
        SI.METERS_PER_SECOND));
    manager.configure();
    boolean fail = false;
    try {
      manager.unregister(new RoadUser() {
        @Override
        public void initRoadUser(RoadModel model) {}
      });
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertTrue(fail);
  }

  @Test(expected = RuntimeException.class)
  public void unregisterWithBrokenModel() {
    manager.add(new GraphRoadModel(new MultimapGraph<LengthData>(), SI.METER,
        SI.METERS_PER_SECOND));
    manager.add(new BrokenRoadModel(new MultimapGraph<LengthData>()));
    manager.configure();
    manager.unregister(new RoadUser() {
      @Override
      public void initRoadUser(RoadModel model) {}
    });
  }

  @Test
  public void unregisterUnregistered() {
    final OtherFooModel model = new OtherFooModel();
    manager.register(model);
    manager.configure();
    final Object o = new Object();

    boolean fail = false;
    try {
      manager.unregister(o);
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertTrue(fail);

    // it wont be registered
    fail = false;
    try {
      manager.register(o);
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertTrue(fail);

    fail = false;
    try {
      manager.unregister(o);
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertTrue(fail);
  }

  public void unregisterRegistered() {
    final OtherFooModel model = new OtherFooModel();
    final BarModel model2 = new BarModel();
    manager.register(model);
    manager.register(model2);
    manager.configure();

    final Foo foo = new Foo();
    final Bar bar = new Bar();

    assertTrue(manager.register(foo));
    assertTrue(manager.register(bar));

    assertTrue(manager.unregister(foo));

    assertEquals(1, model.calledRegister);
    assertEquals(1, model2.calledRegister);
    assertEquals(1, model.callUnregister);
  }

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

    manager.register(mA);
    manager.register(mAA);
    manager.register(mB);
    manager.register(mB2);
    manager.register(mBB);
    manager.register(mBBB);
    manager.register(mSB);
    manager.register(mC);

    manager.configure();

    final ObjectA a1 = new ObjectA();
    assertTrue(manager.register(a1));
    assertEquals(asList(a1), mA.getRegisteredElements());
    assertEquals(asList(a1), mAA.getRegisteredElements());

    mA.setRegisterAction(REJECT);
    final ObjectA a2 = new ObjectA();
    assertTrue(manager.register(a2));
    assertEquals(asList(a1, a2), mA.getRegisteredElements());
    assertEquals(asList(a1, a2), mAA.getRegisteredElements());

    mAA.setRegisterAction(REJECT);
    final ObjectA a3 = new ObjectA();
    boolean fail = false;
    try {
      manager.register(a3);
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertTrue(fail);
    assertEquals(asList(a1, a2, a3), mA.getRegisteredElements());
    assertEquals(asList(a1, a2, a3), mAA.getRegisteredElements());

    mA.setRegisterAction(ALLOW);
    mAA.setRegisterAction(ALLOW);
    assertTrue(manager.register(a1));// allow duplicates
    assertEquals(asList(a1, a2, a3, a1), mA.getRegisteredElements());
    assertEquals(asList(a1, a2, a3, a1), mAA.getRegisteredElements());

    final ObjectB b1 = new ObjectB();
    assertTrue(manager.register(b1));
    assertEquals(asList(b1), mB.getRegisteredElements());
    assertEquals(asList(b1), mB2.getRegisteredElements());
    assertEquals(asList(b1), mBB.getRegisteredElements());
    assertEquals(asList(b1), mBBB.getRegisteredElements());
    assertEquals(asList(), mSB.getRegisteredElements());

    // subclass of B is registerd in all general models and its subclass
    // model
    final SpecialB s1 = new SpecialB();
    assertTrue(manager.register(s1));
    assertEquals(asList(b1, s1), mB.getRegisteredElements());
    assertEquals(asList(b1, s1), mB2.getRegisteredElements());
    assertEquals(asList(b1, s1), mBB.getRegisteredElements());
    assertEquals(asList(b1, s1), mBBB.getRegisteredElements());
    assertEquals(asList(s1), mSB.getRegisteredElements());

    assertTrue(mC.getRegisteredElements().isEmpty());

    // unregister not registered object
    final ObjectA a4 = new ObjectA();
    assertTrue(manager.unregister(a4));
    assertEquals(asList(a4), mA.getUnregisteredElements());
    assertEquals(asList(a4), mAA.getUnregisteredElements());

    // try again, this time with models rejecting unregister
    mA.setUnregisterAction(REJECT);
    mAA.setUnregisterAction(REJECT);
    fail = false;
    try {
      manager.unregister(a4);
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertTrue(fail);
    assertEquals(asList(a4, a4), mA.getUnregisteredElements());
    assertEquals(asList(a4, a4), mAA.getUnregisteredElements());

    assertTrue(manager.unregister(b1));
    assertEquals(asList(b1), mB.getUnregisteredElements());
    assertEquals(asList(b1), mB2.getUnregisteredElements());
    assertEquals(asList(b1), mBB.getUnregisteredElements());
    assertEquals(asList(b1), mBBB.getUnregisteredElements());
    assertEquals(asList(), mSB.getUnregisteredElements());

    assertTrue(manager.unregister(s1));
    assertEquals(asList(b1, s1), mB.getUnregisteredElements());
    assertEquals(asList(b1, s1), mB2.getUnregisteredElements());
    assertEquals(asList(b1, s1), mBB.getUnregisteredElements());
    assertEquals(asList(b1, s1), mBBB.getUnregisteredElements());
    assertEquals(asList(s1), mSB.getUnregisteredElements());

  }

  @Test
  public void anonymousModelTest() {
    manager.add(new Model<InnerObject>() {
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
    });

    manager.configure();
    boolean fail = false;
    try {
      manager.register(new InnerObject());
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertTrue(fail);
  }

  class InnerObject {}
}

class BrokenRoadModel extends GraphRoadModel {
  public BrokenRoadModel(Graph<? extends ConnectionData> pGraph) {
    super(pGraph, SI.METER, SI.METERS_PER_SECOND);
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

  public DebugModel(Class<T> type) {
    supportedType = type;
    registeredElements = new ArrayList<T>();
    unregisteredElements = new ArrayList<T>();
    setRegisterAction(ALLOW);
    setUnregisterAction(ALLOW);
  }

  public void setRegisterAction(Action a) {
    registerAction = a;
  }

  public void setUnregisterAction(Action a) {
    unregisterAction = a;
  }

  public void setSupportedType(Class<T> type) {
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

  private boolean actionResponse(Action a) {
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

}

class Foo {}

class Bar {}
