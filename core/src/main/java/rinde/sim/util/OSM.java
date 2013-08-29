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

import javax.measure.Measure;
import javax.measure.unit.NonSI;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import rinde.sim.core.graph.Connection;
import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.MultiAttributeData;
import rinde.sim.core.graph.Point;
import rinde.sim.core.graph.TableGraph;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class OSM {

  static HashSet<String> highwayNames = new HashSet<String>();

  public static Graph<MultiAttributeData> parse(String filename) {
    try {
      final InputSource inputSource = new InputSource(new FileInputStream(
          filename));
      final XMLReader xmlReader = XMLReaderFactory.createXMLReader();
      // Multimap<Point, Point> graph = HashMultimap.create();

      final TableGraph<MultiAttributeData> graph = new TableGraph<MultiAttributeData>(
          MultiAttributeData.EMPTY);

      final OSMParser parser = new OSMParser(graph);
      xmlReader.setContentHandler(parser);
      xmlReader.setErrorHandler(parser);
      xmlReader.parse(inputSource);

      // remove circular connections
      final List<Connection<MultiAttributeData>> removeList = new ArrayList<Connection<MultiAttributeData>>();
      for (final Connection<MultiAttributeData> connection : graph
          .getConnections()) {
        if (connection.from.equals(connection.to)) {
          removeList.add(connection);
        }
      }
      for (final Connection<MultiAttributeData> connection : removeList) {
        graph.removeConnection(connection.from, connection.to);
      }
      // System.out.println(highwayNames.toString());
      return graph;
    } catch (final Exception e) {
      e.printStackTrace();
      throw new RuntimeException("Failed to load xml file properly: "
          + filename);
    }

  }

  static class OSMParser extends DefaultHandler {

    protected Graph<MultiAttributeData> rs;
    protected HashMap<String, Point> nodes;
    protected WayParser current;

    public OSMParser(Graph<MultiAttributeData> rs) {
      super();
      this.rs = rs;
      nodes = new HashMap<String, Point>();
    }

    // the earth radius in meters
    private static final long EARTH_RADIUS = 6378137L;

    // private double gradeToRadian(double grade) {
    // return grade * Math.PI / 180;
    // }

    @Override
    public void startElement(String namespaceURI, String localName,
        String qualifiedName, Attributes attributes) {
      if (current != null) {
        current
            .startElement(namespaceURI, localName, qualifiedName, attributes);
      } else if (localName.equals("node")) {
        final double lat = Double.parseDouble(attributes.getValue("lat"));
        final double lon = Double.parseDouble(attributes.getValue("lon"));
        // LatLng latlong = new LatLng(lat, lon);
        // latlong.toWGS84();
        // UTMRef ref = latlong.toUTMRef();

        // lat = gradeToRadian(lat);
        // lon = gradeToRadian(lon);
        // long y = Math.round(earthRadius * Math.cos(lat) *
        // Math.cos(lon));

        // cos(latitude * pi/180) * earth circumference

        // ground resolution = cos(latitude * pi/180) * earth
        // circumference / map width
        // = (cos(latitude * pi/180) * 2 * pi * 6378137 meters) / (256 *
        // 2^level pixels)

        // MAGIC constant! Don't touch this without consulting either
        // Rinde van Lon or Bartosz Michalik, preferably both :-)
        final double scale = 1000000 / 1.425139046;// Math.toDegrees(Math.cos(Math.toRadians(lat)
        // * Math.PI /
        // Math.toRadians(180)))
        // * 2 * Math.PI *
        // earthRadius / 1000;

        //

        // sinLatitude = sin(latitude * pi/180)
        //
        // pixelX = ((longitude + 180) / 360) * 256 * 2 level
        // pixelY = (0.5 - log((1 + sinLatitude) / (1 - sinLatitude)) /
        // (4 * pi)) * 256 * 2 level

        // double sinLattitude = Math.sin(Math.toRadians(lat * Math.PI /
        // 180));
        // double x = ((lon + 180) / 360.0);
        // double y = (0.5 - Math.log((1 + sinLattitude) / (1 -
        // sinLattitude)) / (4 * Math.PI));

        // MERCATOR:
        final double x = scale * lon; // Math.round(earthRadius *
        // Math.cos(lat) * Math.sin(lon));
        final double y = scale
            * Math.toDegrees(1.0 / Math.sinh(Math.tan(Math.toRadians(lat))));// Math.log(Math.tan(.25
                                                                             // *
                                                                             // Math.PI
                                                                             // +
                                                                             // .5
                                                                             // *
                                                                             // gradeToRadian(lat)));

        // check: http://www.movable-type.co.uk/scripts/latlong.html for
        // a great explanation

        // converting to the Mercator projection:
        // http://mathworld.wolfram.com/MercatorProjection.html
        // Fun fact: Did you know that the Mercator projection was named
        // after Gerardus Mercator (1512-1594), a Flemish cartographer?

        // double dLat = (l)

        // var R = 6371; // km
        // var dLat = (lat2-lat1).toRad();
        // var dLon = (lon2-lon1).toRad();
        // var lat1 = lat1.toRad();
        // var lat2 = lat2.toRad();
        //
        // var a = Math.sin(dLat/2) * Math.sin(dLat/2) +
        // Math.sin(dLon/2) * Math.sin(dLon/2) * Math.cos(lat1) *
        // Math.cos(lat2);
        // var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        // var d = R * c;

        // ref.

        // String[] converted = CoordinateConversion.latLon2MGRUTM(x,
        // y).split(" ");
        // nodes.put(attributes.getValue("id"), new
        // Point(Double.parseDouble(converted[2]),
        // -Double.parseDouble(converted[3])));

        nodes.put(attributes.getValue("id"), new Point(x, y));

      } else if (localName.equals("way")) {
        current = new WayParser(nodes);
      }
    }

    @Override
    public void endElement(String uri, String localName, String qName)
        throws SAXException {
      if (localName.equals("way")) {
        current.addWaysTo(rs);
        current = null;
      }
    }

    @Override
    public void endDocument() {}

  }

  static class WayParser extends DefaultHandler {

    protected final HashSet<String> highwayTypes = new HashSet<String>(
        Arrays
            .asList("motorway", "motorway_link", "trunk", "trunk_link", "primary", "primary_link", "motorway_junction", "secondary", "secondary_link", "tertiary", "road", "living_street", "residental", "residential", "residential;unclassified", "crossing", "ditch", "unclassified", "raceway", "path", "turning_circle", "track", "trunk_link", "trunk", "platform", "minor"));
    // what about 'unclassified'?

    protected final HashSet<String> junctionTypes = new HashSet<String>(
        Arrays.asList("roundabout"));

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
    public void startElement(String namespaceURI, String localName,
        String qualifiedName, Attributes attributes) {
      if (localName.equals("tag")) {

        if (attributes.getValue("k").equals("highway")) {
          highwayNames.add(attributes.getValue("v"));
        }

        if (attributes.getValue("k").equals("oneway")
            && attributes.getValue("v").equals("yes")) {
          oneWay = true;
        } else if (attributes.getValue("k").equals("highway")
            && highwayTypes.contains(attributes.getValue("v"))) {
          isValidRoad = true;
        } else if (attributes.getValue("k").equals("junction")
            && junctionTypes.contains(attributes.getValue("v"))) {
          isValidRoad = true;
        } else if (attributes.getValue("k").equals("maxspeed")) {
          try {
            maxSpeed = 1000.0 * Integer.parseInt(attributes.getValue("v")
                .replaceAll("\\D", ""));
          } catch (final NumberFormatException nfe) {
            // ignore if this happens, it means that no max speed
            // was defined
          }
        }
      } else if (localName.equals("nd")) {
        nodes.add(attributes.getValue("ref"));
      }
    }

    public void addWaysTo(Graph<MultiAttributeData> graph) {
      if (isValidRoad) {
        for (int i = 1; i < nodes.size(); i++) {
          final Point from = nodeMapping.get(nodes.get(i - 1));
          final Point to = nodeMapping.get(nodes.get(i));
          if (from != null && to != null && !from.equals(to)) {

            final double length = Point.distance(from, to);
            if (!graph.hasConnection(from, to)) {
              graph.addConnection(from, to, new MultiAttributeData(length,
                  Measure.valueOf(maxSpeed, NonSI.KILOMETERS_PER_HOUR)));
            }
            if (!oneWay && !graph.hasConnection(to, from)) {
              graph.addConnection(to, from, new MultiAttributeData(length,
                  Measure.valueOf(maxSpeed, NonSI.KILOMETERS_PER_HOUR)));
            }
          }
        }
      }
    }
  }

}
