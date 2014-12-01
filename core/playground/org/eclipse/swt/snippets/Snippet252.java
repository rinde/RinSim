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
 * LineAttributes example snippet: draw 2 polylines with different line
 * attributes.
 * For a list of all SWT example snippets see
 * http://www.eclipse.org/swt/snippets/
 */
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.LineAttributes;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

public class Snippet252 {

	public static void main(String[] args) {
		final Display display = new Display();
		final Shell shell = new Shell(display);

		shell.addListener(SWT.Paint, new Listener() {
			@Override
			public void handleEvent(Event event) {
				GC gc = event.gc;

				gc.setLineAttributes(new LineAttributes(10, SWT.CAP_FLAT, SWT.JOIN_MITER, SWT.LINE_SOLID, null, 0, 10));
				gc.drawPolyline(new int[] { 50, 100, 50, 20, 60, 30, 50, 45 });

				gc.setLineAttributes(new LineAttributes(1 / 2f, SWT.CAP_FLAT, SWT.JOIN_MITER, SWT.LINE_DOT, null, 0, 10));
				gc.drawPolyline(new int[] { 100, 100, 100, 20, 110, 30, 100, 45 });
			}
		});

		shell.setSize(150, 150);
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		display.dispose();
	}
}