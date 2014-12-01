/*
 * Copyright (C) 2011-2014 Rinde van Lon, iMinds DistriNet, KU Leuven
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

/**
 * Exception that indicates that a path could not be found.
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 */
public class PathNotFoundException extends RuntimeException {

  private static final long serialVersionUID = -1605717570711159457L;

  /**
   * Create new exeception with specified error message.
   * @param string the error message.
   */
  public PathNotFoundException(String string) {
    super(string);
  }

}
