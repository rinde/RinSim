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

import rinde.sim.core.graph.Connection;
import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.MultiAttributeEdgeData;
import rinde.sim.core.graph.Point;
import rinde.sim.core.graph.TableGraph;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class OSM {

	static HashSet<String> highwayNames = new HashSet<String>();

	public static Graph<MultiAttributeEdgeData> parse(String filename) {
		try {
			InputSource inputSource = new InputSource(new FileInputStream(filename));
			XMLReader xmlReader = XMLReaderFactory.createXMLReader();
			//Multimap<Point, Point> graph = HashMultimap.create();

			TableGraph<MultiAttributeEdgeData> graph = new TableGraph<MultiAttributeEdgeData>(MultiAttributeEdgeData.EMPTY);

			OSMParser parser = new OSMParser(graph);
			xmlReader.setContentHandler(parser);
			xmlReader.setErrorHandler(parser);
			xmlReader.parse(inputSource);

			// remove circular connections
			List<Connection<MultiAttributeEdgeData>> removeList = new ArrayList<Connection<MultiAttributeEdgeData>>();
			for (Connection<MultiAttributeEdgeData> connection : graph.getConnections()) {
				if (connection.from.equals(connection.to)) {
					removeList.add(connection);
				}
			}
			for (Connection<MultiAttributeEdgeData> connection : removeList) {
				graph.removeConnection(connection.from, connection.to);
			}
			System.out.println(highwayNames.toString());
			return graph;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to load xml file properly: " + filename);
		}

	}

	static class OSMParser extends DefaultHandler {

		protected Graph<MultiAttributeEdgeData> rs;
		protected HashMap<String, Point> nodes;
		protected WayParser current;

		public OSMParser(Graph<MultiAttributeEdgeData> rs) {
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
				Arrays.asList("motorway", "motorway_link", "trunk", "trunk_link", "primary", "primary_link", "motorway_junction", "secondary", "secondary_link", "tertiary", "road", "living_street", "residental", "residential", "residential;unclassified", "crossing", "ditch", "unclassified", "raceway", "path", "turning_circle", "track", "trunk_link", "trunk", "platform", "minor"));
		// what about 'unclassified'?

		protected final HashSet<String> junctionTypes = new HashSet<String>(Arrays.asList("roundabout"));

		protected List<String> nodes;
		protected double maxSpeed;
		protected boolean oneWay;
		protected boolean isValidRoad;
		protected HashMap<String, Point> nodeMapping;

		public WayParser(HashMap<String, Point> nodeMapping) {
			nodes = new ArrayList<String>();
			oneWay = false;
			maxSpeed = Double.NaN;
			isValidRoad = false;
			this.nodeMapping = nodeMapping;
		}

		@Override
		public void startElement(String namespaceURI, String localName, String qualifiedName, Attributes attributes) {
			if (localName.equals("tag")) {

				if (attributes.getValue("k").equals("highway")) {
					highwayNames.add(attributes.getValue("v"));
				}

				if (attributes.getValue("k").equals("oneway") && attributes.getValue("v").equals("yes")) {
					oneWay = true;
				} else if (attributes.getValue("k").equals("highway") && highwayTypes.contains(attributes.getValue("v"))) {
					isValidRoad = true;
				} else if (attributes.getValue("k").equals("junction") && junctionTypes.contains(attributes.getValue("v"))) {
					isValidRoad = true;
				} else if (attributes.getValue("k").equals("maxspeed")) {
					try {
						maxSpeed = Integer.parseInt(attributes.getValue("v").replaceAll("\\D", ""));
					} catch (NumberFormatException nfe) {
						// ignore if this happens, it means that no max speed was defined
					}
				}
			} else if (localName.equals("nd")) {
				nodes.add(attributes.getValue("ref"));
			}
		}

		public void addWaysTo(Graph<MultiAttributeEdgeData> graph) {
			if (isValidRoad) {
				for (int i = 1; i < nodes.size(); i++) {
					Point from = nodeMapping.get(nodes.get(i - 1));
					Point to = nodeMapping.get(nodes.get(i));
					if (from != null && to != null && !from.equals(to)) {
						double length = Point.distance(from, to);
						if (!graph.hasConnection(from, to)) {
							graph.addConnection(from, to, new MultiAttributeEdgeData(length, maxSpeed));
						}
						if (!oneWay && !graph.hasConnection(to, from)) {
							graph.addConnection(to, from, new MultiAttributeEdgeData(length, maxSpeed));
						}
					}
				}
			}
		}
	}

}
