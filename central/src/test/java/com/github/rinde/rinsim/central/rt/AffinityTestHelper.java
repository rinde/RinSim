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
package com.github.rinde.rinsim.central.rt;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.auto.value.AutoValue;
import com.google.common.base.Optional;

import net.openhft.affinity.AffinityLock;

/**
 *
 * @author Rinde van Lon
 */
public final class AffinityTestHelper {
  private static final String CPU_NOT_AVAILABLE = "CPU not available";
  private static final String RESERVED_4_APP = "Reserved for this application";
  private static final Pattern THREAD_PATTERN =
    Pattern.compile("(\\d): Thread\\[(.*),(\\d),(.*)\\] alive=(true|false)");
  private static final Pattern NO_THREAD_PATTERN = Pattern.compile(
    "(\\d): (" + CPU_NOT_AVAILABLE + "|" + RESERVED_4_APP + ")");

  private AffinityTestHelper() {}

  public static List<AffinityLockInfo> getLockInfo() {
    final String[] parts = AffinityLock.dumpLocks().split("\n");
    final List<AffinityLockInfo> infos = new ArrayList<>();
    for (int i = 0; i < parts.length; i++) {
      infos.add(parse(parts[i]));
    }
    return infos;
  }

  static AffinityLockInfo parse(String str) {
    final Matcher threadMatcher = THREAD_PATTERN.matcher(str);
    if (threadMatcher.matches()) {
      final int cpuId = Integer.parseInt(threadMatcher.group(1));
      final String name = threadMatcher.group(2);
      final int priority = Integer.parseInt(threadMatcher.group(3));
      final String parentName = threadMatcher.group(4);
      final boolean alive = Boolean.parseBoolean(threadMatcher.group(5));
      final ThreadInfo threadInfo =
        ThreadInfo.create(cpuId, priority, name, parentName, alive);
      return AffinityLockInfo.create(threadInfo);
    }
    final Matcher noThreadMatcher = NO_THREAD_PATTERN.matcher(str);
    checkArgument(noThreadMatcher.matches(), "Invalid input: %s.", str);
    final int cpuId = Integer.parseInt(noThreadMatcher.group(1));
    final boolean availableForApp =
      noThreadMatcher.group(2).equals(RESERVED_4_APP);
    return AffinityLockInfo.create(cpuId, availableForApp);
  }

  @AutoValue
  public abstract static class AffinityLockInfo {
    AffinityLockInfo() {}

    public abstract int getCpuId();

    public abstract boolean isAvailableForApp();

    public abstract Optional<ThreadInfo> getThreadInfo();

    static AffinityLockInfo create(int cpuId, boolean availableForApp) {
      checkArgument(cpuId >= 0);
      return new AutoValue_AffinityTestHelper_AffinityLockInfo(
          cpuId, availableForApp, Optional.<ThreadInfo>absent());
    }

    static AffinityLockInfo create(ThreadInfo threadInfo) {
      checkNotNull(threadInfo);
      return new AutoValue_AffinityTestHelper_AffinityLockInfo(
          threadInfo.getCpuId(), true, Optional.of(threadInfo));
    }
  }

  @AutoValue
  public abstract static class ThreadInfo {
    ThreadInfo() {}

    public abstract int getCpuId();

    public abstract int getPriority();

    public abstract String getName();

    public abstract String getParentName();

    public abstract boolean isAlive();

    static ThreadInfo create(int cpuId, int priority, String name,
        String parentName, boolean alive) {
      return new AutoValue_AffinityTestHelper_ThreadInfo(cpuId,
          priority, name, parentName, alive);
    }
  }
}
