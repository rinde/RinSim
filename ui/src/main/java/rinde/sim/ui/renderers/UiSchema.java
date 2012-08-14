package rinde.sim.ui.renderers;

import static com.google.common.collect.Maps.newHashMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;

/**
 * Used to configure a graphical representation of the model elements.
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * 
 */
public class UiSchema {
	private Map<String, Color> colorRegistry;
	private Map<String, Image> imageRegistry;

	private final HashMap<String, RGB> colorCache;
	private final HashMap<String, String> imgCache;
	private final boolean useDefault;

	public final static String DEFAULT = "default_color";

	/**
	 * Create the schema.
	 * @param pUseDefault when use default is <code>true</code> the default
	 *            color is used
	 */
	public UiSchema(boolean pUseDefault) {
		colorCache = newHashMap();
		imgCache = newHashMap();
		useDefault = pUseDefault;
	}

	public UiSchema() {
		this(true);
	}

	// public void add(Class<?> type, ImageDescriptor descriptor) {
	// imgCache.put(type.getName(), descriptor);
	// }

	public void add(Class<?> type, RGB rgb) {
		colorCache.put(type.getName(), rgb);
	}

	public void add(String key, RGB rgb) {
		colorCache.put(key, rgb);
	}

	public void add(Class<?> type, String fileName) {
		// final ImageDescriptor descriptor =
		// ImageDescriptor.createFromFile(type, fileName);
		imgCache.put(type.getName(), fileName);
	}

	public Image getImage(Class<?> type) {
		return imageRegistry.get(type.getName());
	}

	public Color getColor(Class<?> type) {
		final Color color = colorRegistry.get(type.getName());
		if (color == null && useDefault) {
			return colorRegistry.get(DEFAULT);
		}
		return color;
	}

	public Color getDefaultColor() {
		return colorRegistry.get(DEFAULT);
	}

	public Color getColor(String key) {
		final Color color = colorRegistry.get(key);
		if (color == null && useDefault) {
			return colorRegistry.get(DEFAULT);
		}
		return color;
	}

	public void initialize(Device d) {
		flushCache(d);
	}

	private void flushCache(Device d) {
		if (colorRegistry != null) {
			return;
		}
		colorRegistry = newHashMap();
		imageRegistry = newHashMap();
		colorRegistry.put(DEFAULT, new Color(d, new RGB(0xff, 0, 0)));
		for (final Entry<String, RGB> e : colorCache.entrySet()) {
			colorRegistry.put(e.getKey(), new Color(d, e.getValue()));
		}

		for (final Entry<String, String> e : imgCache.entrySet()) {
			imageRegistry.put(e.getKey(), new Image(d, getClass().getResourceAsStream(e.getValue())));
		}
	}
}
