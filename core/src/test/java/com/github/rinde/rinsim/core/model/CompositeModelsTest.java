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

import static com.google.common.base.Verify.verifyNotNull;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.truth.Truth.assertThat;

import javax.annotation.Nullable;

import org.junit.Test;

import com.github.rinde.rinsim.core.model.Model.AbstractModel;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;

/**
 * @author Rinde van Lon
 *
 */
public class CompositeModelsTest {

  static Function<Object, Class<?>> toClass() {
    return ToClass.INSTANCE;
  }

  enum ToClass implements Function<Object, Class<?>> {
    INSTANCE {
      @Nullable
      @Override
      public Class<?> apply(@Nullable Object input) {
        return verifyNotNull(input).getClass();
      }
    }
  }

  /**
   * Test multi composite.
   */
  @Test
  public void testComposite() {
    final ModelManager mm = ModelManager.builder()
        .add(new ModelA.Builder())
        .build();

    final Iterable<Class<?>> classes = transform(mm.getModels(), toClass());

    assertThat(classes).containsAllOf(ModelA.class, ModelAsub1.class,
        ModelAsub1sub1.class, ModelAsub2.class).inOrder();
  }

  /**
   * Test multi composite.
   */
  @Test
  public void testCompositeDefault() {
    final ModelManager mm = ModelManager.builder()
        .addDefaultProvider(new ModelA.Builder())
        .build();

    final Iterable<Class<?>> classes = transform(mm.getModels(), toClass());

    assertThat(classes).containsAllOf(ModelA.class, ModelAsub1.class,
        ModelAsub1sub1.class, ModelAsub2.class).inOrder();
  }

  static class ModelA extends NopModel {
    static class Builder extends NopBuilder<ModelA>implements
        CompositeModelBuilder<ModelA, Void> {
      Builder() {
        setDependencies(ModelAsub1sub1.class);
      }

      @Override
      public ModelA build(DependencyProvider dependencyProvider) {
        assertThat(dependencyProvider.get(ModelAsub1sub1.class)).isNotNull();
        return new ModelA();
      }

      @Override
      public ImmutableSet<ModelBuilder<?, ?>> getChildren() {
        return ImmutableSet.<ModelBuilder<?, ?>>of(
            new ModelAsub1.Builder(),
            new ModelAsub2.Builder());
      }
    }
  }

  static class ModelAsub1 extends NopModel {
    static class Builder extends NopBuilder<ModelAsub1>implements
        CompositeModelBuilder<ModelAsub1, Void> {
      @Override
      public ModelAsub1 build(DependencyProvider dependencyProvider) {
        return new ModelAsub1();
      }

      @Override
      public ImmutableSet<ModelBuilder<?, ?>> getChildren() {
        return ImmutableSet
            .<ModelBuilder<?, ?>>of(new ModelAsub1sub1.Builder());
      }
    }
  }

  static class ModelAsub2 extends NopModel {
    static class Builder extends NopBuilder<ModelAsub2> {
      @Override
      public ModelAsub2 build(DependencyProvider dependencyProvider) {
        return new ModelAsub2();
      }
    }
  }

  static class ModelAsub1sub1 extends NopModel {
    static class Builder extends NopBuilder<ModelAsub1sub1> {
      Builder() {
        setProvidingTypes(ModelAsub1sub1.class);
      }

      @Override
      public ModelAsub1sub1 build(DependencyProvider dependencyProvider) {
        return new ModelAsub1sub1();
      }
    }
  }

  abstract static class NopBuilder<T extends Model<Void>> extends
      AbstractModelBuilder<T, Void> {
    @Override
    public String toString() {
      return getClass().getDeclaringClass().getSimpleName() + "."
          + getClass().getSimpleName();
    }
  }

  static class NopModel extends AbstractModel<Void> {
    @Override
    public boolean register(Void element) {
      return false;
    }

    @Override
    public boolean unregister(Void element) {
      return false;
    }

    @Override
    public String toString() {
      return getClass().getSimpleName();
    }

    @Override
    public <U> U get(Class<U> clazz) {
      return clazz.cast(this);
    }
  }

}
