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
package org.eclipse.swt.snippets;

/*
 * Display snippet: use the application system menu when available.
 * For a list of all SWT example snippets see
 * http://www.eclipse.org/swt/snippets/
 * @since 3.7
 */
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ArmEvent;
import org.eclipse.swt.events.ArmListener;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

public class Snippet354 {

	static MenuItem getItem(Menu menu, int id) {
		MenuItem[] items = menu.getItems();
		for (int i = 0; i < items.length; i++) {
			if (items[i].getID() == id) {
				return items[i];
			}
		}
		return null;
	}

	public static void main(String[] args) {
		final Display display = new Display();
		Shell shell = new Shell(display);
		shell.setLayout(new GridLayout(1, false));

		Menu appMenuBar = display.getMenuBar();
		if (appMenuBar == null) {
			appMenuBar = new Menu(shell, SWT.BAR);
			shell.setMenuBar(appMenuBar);
		}
		MenuItem file = new MenuItem(appMenuBar, SWT.CASCADE);
		file.setText("File");
		Menu dropdown = new Menu(appMenuBar);
		file.setMenu(dropdown);
		MenuItem exit = new MenuItem(dropdown, SWT.PUSH);
		exit.setText("Exit");
		exit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				display.dispose();
			};
		});

		Listener keyDownFilter = new Listener() {
			@Override
			public void handleEvent(Event event) {
				System.out.println("Key event!");
			}

		};
		display.addFilter(SWT.KeyDown, keyDownFilter);
		display.addFilter(SWT.KeyUp, keyDownFilter);

		ArmListener armListener = new ArmListener() {
			@Override
			public void widgetArmed(ArmEvent e) {
				System.out.println(e);
			}
		};

		Menu systemMenu = display.getSystemMenu();
		if (systemMenu != null) {
			systemMenu.addMenuListener(new MenuListener() {
				@Override
				public void menuHidden(MenuEvent e) {
					System.out.println("App menu closed");
				}

				@Override
				public void menuShown(MenuEvent e) {
					System.out.println("App menu opened");
				}
			});

			MenuItem sysItem = getItem(systemMenu, SWT.ID_QUIT);
			sysItem.addArmListener(armListener);
			sysItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					System.out.println("Quit selected");
				};
			});
			sysItem = getItem(systemMenu, SWT.ID_HIDE_OTHERS);
			sysItem.addArmListener(armListener);
			sysItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					System.out.println("Hide others selected -- and blocked!");
					e.doit = false;
				};
			});
			sysItem = getItem(systemMenu, SWT.ID_HIDE);
			sysItem.addArmListener(armListener);
			sysItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					System.out.println("Hide selected -- and blocked!");
					e.doit = false;
				};
			});
			sysItem = getItem(systemMenu, SWT.ID_PREFERENCES);
			sysItem.addArmListener(armListener);
			sysItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					System.out.println("Preferences selected");
				};
			});
			sysItem = getItem(systemMenu, SWT.ID_ABOUT);
			sysItem.addArmListener(armListener);
			sysItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					System.out.println("About selected");
				};
			});

			int prefsIndex = systemMenu.indexOf(getItem(systemMenu, SWT.ID_PREFERENCES));
			MenuItem newAppMenuItem = new MenuItem(systemMenu, SWT.CASCADE, prefsIndex + 1);
			newAppMenuItem.setText("SWT-added item");
			newAppMenuItem.setAccelerator(SWT.MOD1 | 'i');
			newAppMenuItem.addArmListener(armListener);
			newAppMenuItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					System.out.println("SWT-added item selected");
				};
			});
			Menu subMenu = new Menu(systemMenu);

			for (int i = 0; i < 4; i++) {
				MenuItem subItem = new MenuItem(subMenu, SWT.PUSH);
				subItem.setText("Item " + i);
			}
			newAppMenuItem.setMenu(subMenu);
		}
		Button b = new Button(shell, SWT.PUSH);
		b.setText("Test");
		shell.pack();
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		display.dispose();
	}
}