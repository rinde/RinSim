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
package com.github.rinde.rinsim.central.rt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import com.github.rinde.rinsim.central.rt.RtSolverModel.RtSolverModelAPI;
import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.Model.AbstractModelVoid;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.ui.renderers.PanelRenderer;
import com.google.auto.value.AutoValue;
import com.google.common.base.Optional;

/**
 *
 * @author Rinde van Lon
 */
public final class RtSolverPanel
    extends AbstractModelVoid
    implements PanelRenderer {
  private static final int PANEL_SIZE = 200;
  final RtSolverModelAPI model;

  Optional<Composite> parent;
  Optional<Label> label;
  Optional<Table> table;

  enum ComputingState {
    IDLE, COMPUTING;
  }

  RtSolverPanel(RtSolverModelAPI m) {
    model = m;
    label = Optional.absent();
    table = Optional.absent();
  }

  @Override
  public void initializePanel(Composite par) {
    parent = Optional.of(par);
    final GridLayout layout = new GridLayout();
    par.setLayout(layout);

    final GridData labelData = new GridData(SWT.FILL, SWT.NONE, true, false);
    label = Optional.of(new Label(par, 0));
    label.get().setText(ComputingState.IDLE.name());
    label.get().setLayoutData(labelData);

    final GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
    table = Optional.of(
      new Table(par, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL));
    table.get().setLayoutData(data);

    parent.get().layout();
  }

  @Override
  public int preferredSize() {
    return PANEL_SIZE;
  }

  @Override
  public int getPreferredPosition() {
    return SWT.RIGHT;
  }

  @Override
  public String getName() {
    return "Realtime Solver Panel";
  }

  @Override
  public void render() {
    if (table.get().isDisposed()) {
      return;
    }
    table.get().getDisplay().asyncExec(new Runnable() {
      @Override
      public void run() {
        if (table.get().isDisposed()) {
          return;
        }
        table.get().removeAll();
        if (model.isComputing()) {
          label.get().setText(ComputingState.COMPUTING.name());
          for (final RealtimeSolver rs : model.getComputingSolvers()) {
            final TableItem item = new TableItem(table.get(), 0);
            item.setText(rs.toString());
          }
        } else {
          label.get().setText(ComputingState.IDLE.name());
        }
      }
    });

  }

  public static Builder builder() {
    return new AutoValue_RtSolverPanel_Builder();
  }

  @AutoValue
  public static class Builder
      extends AbstractModelBuilder<RtSolverPanel, Void> {

    Builder() {
      setDependencies(RtSolverModelAPI.class);
    }

    @Override
    public RtSolverPanel build(DependencyProvider dependencyProvider) {
      return new RtSolverPanel(dependencyProvider.get(RtSolverModelAPI.class));
    }
  }

}
