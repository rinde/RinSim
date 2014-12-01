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
 * ProgressBar example snippet: update a progress bar (from another thread)
 * For a list of all SWT example snippets see
 * http://www.eclipse.org/swt/snippets/
 */
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

public class Snippet56 {

	public static void main(String[] args) {
		final Display display = new Display();
		Shell shell = new Shell(display);
		final ProgressBar bar = new ProgressBar(shell, SWT.SMOOTH);
		Rectangle clientArea = shell.getClientArea();
		bar.setBounds(clientArea.x, clientArea.y, 200, 32);
		shell.open();
		final int maximum = bar.getMaximum();
		new Thread() {
			@Override
			public void run() {
				for (final int[] i = new int[1]; i[0] <= maximum; i[0]++) {
					try {
						Thread.sleep(100);
					} catch (Throwable th) {
					}
					if (display.isDisposed()) {
						return;
					}
					display.asyncExec(new Runnable() {
						@Override
						public void run() {
							if (bar.isDisposed()) {
								return;
							}
							bar.setSelection(i[0]);
						}
					});
				}
			}
		}.start();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		display.dispose();
	}
}