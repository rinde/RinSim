/**
 * 
 */
package rinde.sim.util;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import rinde.sim.core.Point;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class OSM {

	public static Multimap<Point, Point> parse(String filename) {
		try {
			InputSource inputSource = new InputSource(new FileInputStream(filename));
			XMLReader xmlReader = XMLReaderFactory.createXMLReader();
			Multimap<Point, Point> graph = HashMultimap.create();
			OSMParser parser = new OSMParser(graph);
			xmlReader.setContentHandler(parser);
			xmlReader.setErrorHandler(parser);
			xmlReader.parse(inputSource);
			return graph;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to load xml file properly: " + filename);
		}
	}

	static class OSMParser extends DefaultHandler {

		protected Multimap<Point, Point> rs;
		protected HashMap<String, Point> nodes;
		protected WayParser current;

		public OSMParser(Multimap<Point, Point> rs) {
			super();
			this.rs = rs;
			nodes = new HashMap<String, Point>();
		}

		private static final long earthRadius = 63674490L;

		private double gradeToRadian(double grade) {
			return grade * Math.PI / 180;
		}

		@Override
		public void startElement(String namespaceURI, String localName, String qualifiedName, Attributes attributes) {
			if (current != null) {
				current.startElement(namespaceURI, localName, qualifiedName, attributes);
			} else if (localName.equals("node")) {
				double lat = Double.parseDouble(attributes.getValue("lat"));
				double lon = Double.parseDouble(attributes.getValue("lon"));
				//				LatLng latlong = new LatLng(lat, lon);
				//latlong.toWGS84();
				//				UTMRef ref = latlong.toUTMRef();

				lat = gradeToRadian(lat);
				lon = gradeToRadian(lon);
				long y = Math.round(earthRadius * Math.cos(lat) * Math.cos(lon));
				long x = Math.round(earthRadius * Math.cos(lat) * Math.sin(lon));

				//ref.

				//String[] converted = CoordinateConversion.latLon2MGRUTM(x, y).split(" ");
				//				nodes.put(attributes.getValue("id"), new Point(Double.parseDouble(converted[2]), -Double.parseDouble(converted[3])));

				nodes.put(attributes.getValue("id"), new Point(x, y));

			} else if (localName.equals("way")) {
				current = new WayParser(nodes);
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			if (localName.equals("way")) {
				current.addWaysTo(rs);
				current = null;
			}
		}

		@Override
		public void endDocument() {
		}

	}

	static class WayParser extends DefaultHandler {

		protected final HashSet<String> highwayTypes = new HashSet<String>(
				Arrays.asList("motorway", "motorway_link", "trunk", "trunk_link", "primary", "primary_link", "motorway_junction", "secondary", "secondary_link", "tertiary", "road", "living_street", "residential"));
		// what about 'unclassified'?

		protected final HashSet<String> junctionTypes = new HashSet<String>(Arrays.asList("roundabout"));

		protected List<String> nodes;
		protected boolean oneWay;
		protected boolean isValidRoad;
		protected HashMap<String, Point> nodeMapping;

		public WayParser(HashMap<String, Point> nodeMapping) {
			nodes = new ArrayList<String>();
			oneWay = false;
			isValidRoad = false;
			this.nodeMapping = nodeMapping;
		}

		@Override
		public void startElement(String namespaceURI, String localName, String qualifiedName, Attributes attributes) {
			if (localName.equals("tag")) {
				if (attributes.getValue("k").equals("oneway") && attributes.getValue("v").equals("yes")) {
					oneWay = true;
				} else if (attributes.getValue("k").equals("highway") && highwayTypes.contains(attributes.getValue("v"))) {
					isValidRoad = true;
				} else if (attributes.getValue("k").equals("junction") && junctionTypes.contains(attributes.getValue("v"))) {
					isValidRoad = true;
				}
			} else if (localName.equals("nd")) {
				nodes.add(attributes.getValue("ref"));
			}
		}

		public void addWaysTo(Multimap<Point, Point> graph) {
			if (isValidRoad) {
				for (int i = 1; i < nodes.size(); i++) {
					Point from = nodeMapping.get(nodes.get(i - 1));
					Point to = nodeMapping.get(nodes.get(i));
					if (!graph.containsEntry(from, to)) {
						graph.put(from, to);
					}
					if (!oneWay && !graph.containsEntry(to, from)) {
						graph.put(to, from);
					}
				}
			}
		}
	}

}
