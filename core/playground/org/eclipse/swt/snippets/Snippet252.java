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