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
package com.github.rinde.rinsim.geom;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.auto.value.AutoValue;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Doubles;

/**
 * {@link ConnectionData} implementation which allows to associate multiple
 * attributes to a connection (through a {@link HashMap}). There are two
 * "default" supported properties defined: connection length, and maximum speed
 * on a connection.
 * @author Bartosz Michalik
 * @author Rinde van Lon
 * @since 2.0
 */
@AutoValue
public abstract class MultiAttributeData implements ConnectionData {

  /**
   * The attribute key to be used when annotating {@link MultiAttributeData}
   * with theoretical speeds.
   */
  public static final String THEORETICAL_SPEED_ATTRIBUTE = "ts";

  MultiAttributeData() {}

  @Override
  public abstract Optional<Double> getLength();

  /**
   * Returns max speed defined for a connection. If the max speed is not
   * specified {@link Optional#absent()} is returned.
   * @return The max speed.
   */
  public abstract Optional<Double> getMaxSpeed();

  /**
   * @return All attributes that are defined in this object.
   */
  public abstract ImmutableMap<String, Object> getAttributes();

  /**
   * @return A new {@link Builder} instance for creating
   *         {@link MultiAttributeData} instances.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * A builder for creating {@link MultiAttributeData} instances.
   * @author Rinde van Lon
   */
  public static class Builder {
    private Optional<Double> length;
    private Optional<Double> maxSpeed;
    private final Map<String, Object> attributes;

    Builder() {
      length = Optional.absent();
      maxSpeed = Optional.absent();
      attributes = new LinkedHashMap<>();
    }

    /**
     * Sets the length property: {@link MultiAttributeData#getLength()}. By
     * default no length is defined.
     * @param len The length to set.
     * @return This, as per the builder pattern.
     */
    public Builder setLength(double len) {
      checkArgument(len >= 0d && Doubles.isFinite(len),
        "Expected positive value for length but found %s.", length);
      length = Optional.of(len);
      return this;
    }

    /**
     * Sets the max speed property: {@link MultiAttributeData#getMaxSpeed()}. By
     * default no max speed is defined.
     * @param speed The speed to set.
     * @return This, as per the builder pattern.
     */
    public Builder setMaxSpeed(double speed) {
      checkArgument(speed > 0d && Doubles.isFinite(speed),
        "Expected positive value for maxSpeed but found %s.", speed);
      maxSpeed = Optional.of(speed);
      return this;
    }

    /**
     * Adds all attributes from the map.
     * @param map The attributes to add.
     * @return This, as per the builder pattern.
     */
    public Builder addAllAttributes(
        Map<? extends String, ? extends Object> map) {
      attributes.putAll(map);
      return this;
    }

    /**
     * Adds the attribute.
     * @param string The name of the attribute.
     * @param obj The attribute.
     * @return This, as per the builder pattern.
     */
    public Builder addAttribute(String string, Object obj) {
      attributes.put(string, obj);
      return this;
    }

    /**
     * @return The length.
     */
    public Optional<Double> getLength() {
      return length;
    }

    /**
     * @return The max speed.
     */
    public Optional<Double> getMaxSpeed() {
      return maxSpeed;
    }

    /**
     * @return The attributes.
     */
    public Map<String, Object> getAttributes() {
      return Collections.unmodifiableMap(attributes);
    }

    /**
     * Builds a new {@link MultiAttributeData} instance using the properties as
     * set by this builder.
     * @return A new instance.
     */
    public MultiAttributeData build() {
      checkArgument(!getAttributes().isEmpty()
        || getLength().isPresent()
        || getMaxSpeed().isPresent(),
        "At least length, maxSpeed or another attribute must to be defined.");
      return new AutoValue_MultiAttributeData(length, maxSpeed,
        ImmutableMap.copyOf(attributes));
    }
  }
}
