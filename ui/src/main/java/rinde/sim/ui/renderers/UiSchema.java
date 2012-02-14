package rinde.sim.ui.renderers;

import java.util.HashMap;
import java.util.Map.Entry;

import javax.imageio.ImageReader;

import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;

/**
 * Used to configure a graphical representation of the model elements.
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 *
 */
public class UiSchema {
	private ColorRegistry colorRegistry;
	private ImageRegistry imageRegistry;
	
	private HashMap<String, RGB> colorCache;
	private HashMap<String, ImageDescriptor> imgCache;
	
	public final static String DEFAULT = "default_color";
	
	public UiSchema() {
		colorCache = new HashMap<String, RGB>();
		imgCache = new HashMap<String, ImageDescriptor>();
	}
	
	
	public void add(Class<?> type, ImageDescriptor descriptor) {
		imgCache.put(type.getName(), descriptor);
	}
	
	public void add(Class<?> type, RGB rgb) {
		colorCache.put(type.getName(), rgb);	
	}
	
	public void add(String key, RGB rgb) {
		colorCache.put(key, rgb);
	}
	
	
	public void add(Class<?> type, String fileName) {
		ImageDescriptor descriptor = ImageDescriptor.createFromFile(type, fileName);
		imgCache.put(type.getName(), descriptor);
	}
	
	public Image getImage(Class<?> type) {
		return imageRegistry.get(type.getName());
	}
	
	public Color getColor(Class<?> type) {
		Color color = colorRegistry.get(type.getName());
		if(color == null) return colorRegistry.get(DEFAULT);
		return color;
	}
	
	public Color getDefaultColor() {
		return colorRegistry.get(DEFAULT);
	}
	
	
	public Color getColor(String key) {
		Color color = colorRegistry.get(key);
		if(color == null) return colorRegistry.get(DEFAULT);
		return color;
	}
	
	public void initialize() {
		flushCache();
	}
	
	private void flushCache() {
		if(colorRegistry != null) return;
		colorRegistry = new ColorRegistry();
		imageRegistry = new ImageRegistry();
		colorRegistry.put(DEFAULT, new RGB(0xff, 0, 0));
		for (Entry<String, RGB> e : colorCache.entrySet()) {
			colorRegistry.put(e.getKey(), e.getValue());	
		}
		
		for (Entry<String, ImageDescriptor> e : imgCache.entrySet()) {
			imageRegistry.put(e.getKey(), e.getValue());	
		}
	}
}
