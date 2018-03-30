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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.Model.AbstractModel;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.ui.renderers.PanelRenderer;
import com.google.auto.value.AutoValue;

class TestPanelRenderer extends AbstractModel<Void> implements PanelRenderer {

  protected final String name;
  protected final int position;
  protected final int size;

  TestPanelRenderer(String n, int pos, int s) {
    name = n;
    position = pos;
    size = s;
  }

  @Override
  public void initializePanel(Composite c) {
    c.setLayout(new FillLayout());
    final Button b = new Button(c, SWT.PUSH);
    b.setText("push me " + name);
  }

  @Override
  public int getPreferredPosition() {
    return position;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public int preferredSize() {
    return size;
  }

  @Override
  public void render() {}

  @Override
  public boolean register(Void element) {
    return false;
  }

  @Override
  public boolean unregister(Void element) {
    return false;
  }

  static Builder builder(String name) {
    return new AutoValue_TestPanelRenderer_Builder(name, SWT.LEFT, 100);
  }

  @AutoValue
  abstract static class Builder extends
      AbstractModelBuilder<TestPanelRenderer, Void> {

    Builder() {}

    abstract String name();

    abstract int preferredPosition();

    abstract int preferredSize();

    Builder withPosition(int position) {
      return new AutoValue_TestPanelRenderer_Builder(name(), position,
        preferredSize());
    }

    Builder withSize(int size) {
      return new AutoValue_TestPanelRenderer_Builder(name(),
        preferredPosition(), size);
    }

    @Override
    public TestPanelRenderer build(DependencyProvider dependencyProvider) {
      return new TestPanelRenderer(name(), preferredPosition(),
        preferredSize());
    }
  }
}
