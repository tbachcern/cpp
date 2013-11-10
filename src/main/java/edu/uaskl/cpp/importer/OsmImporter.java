package edu.uaskl.cpp.importer;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import edu.uaskl.cpp.model.edge.EdgeOSM;
import edu.uaskl.cpp.model.graph.GraphUndirected;
import edu.uaskl.cpp.model.node.NodeOSM;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilder;

import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class OsmImporter {

    protected static int getDistance(final OsmNode a, final OsmNode b) {
        // long diffLat = a.lat - b.lat;
        // long diffLon = a.lon - b.lon;
        // return (int) Math.sqrt(diffLat*diffLat+diffLon*diffLon);
        // Spherical Law of Cosines
        return (int) (Math.acos((Math.sin(a.lat) * Math.sin(b.lat)) + (Math.cos(a.lat) * Math.cos(b.lat) * Math.cos(b.lon - b.lon))) * 6367500);
    }

    protected static Document getDomFromFile(final String filename) {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document document = null;
        try {
            final DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(filename);
            document.getDocumentElement().normalize();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            System.out.println("could not open the file");
            e.printStackTrace();
            System.exit(0);
        }
        return document;
    }

    protected static long get100NanoDegrees(final String parsed) {
        long value = 1;
        // different amount of decimal places (up to 7)
        final String decimalPlaces = parsed.split("\\.")[1];
        for (int i = 0; i < (7 - decimalPlaces.length()); ++i)
            value *= 10;
        value *= Long.parseLong(parsed.replace(".", ""), 10);

        return value;
    }

    protected static HashMap<String, OsmNode> getOsmNodes(final Document dom) {
        final HashMap<String, OsmNode> osmNodes = new HashMap<>();
        final Element documentElement = dom.getDocumentElement();
        final NodeList nodes = documentElement.getElementsByTagName("node");
        for (int i = 0; i < nodes.getLength(); ++i) {
            final Element node = (Element) nodes.item(i);
            final String id = node.getAttribute("id");
            final long lat = get100NanoDegrees(node.getAttribute("lat"));
            final long lon = get100NanoDegrees(node.getAttribute("lon"));
            final OsmNode osmNode = new OsmNode(id, lat, lon);
            osmNodes.put(id, osmNode);
        }
        return osmNodes;
    }

    protected GraphUndirected<NodeOSM, EdgeOSM> createNaive(final Document osmFile, final HashMap<String, OsmNode> osmNodes) {
        // add all waypoints to the graph
        final GraphUndirected<NodeOSM, EdgeOSM> osmGraph = new GraphUndirected<NodeOSM, EdgeOSM>();
        final Collection<OsmNode> wayPoints = osmNodes.values();
        for (final OsmNode wayPoint : wayPoints) {
            final NodeOSM newNode = new NodeOSM(wayPoint.id);
            osmGraph.addNode(newNode);
        }

        // connect them
        final Element documentElement = osmFile.getDocumentElement();
        final NodeList ways = documentElement.getElementsByTagName("way");
        for (int i = 0; i < ways.getLength(); ++i) {
            final NodeList childNodes = ways.item(i).getChildNodes();
            String lastWaypoint = null;
            for (int j = 0; j < childNodes.getLength(); ++j) {
                final Node cNode = childNodes.item(j);
                if (cNode.getNodeType() == Node.ELEMENT_NODE) {
                    final Element childNode = (Element) cNode;
                    if ((lastWaypoint == null) && (childNode.getNodeName() == "nd")) {
                        lastWaypoint = childNode.getAttribute("ref");
                        if (!osmNodes.containsKey(lastWaypoint))
                            lastWaypoint = null;
                    } else if (childNode.getNodeName() == "nd") {
                        final String nodeId = childNode.getAttribute("ref");
                        final NodeOSM node = osmGraph.getNode(nodeId);
                        if (!(node == null)) {
                            final int distance = getDistance(osmNodes.get(lastWaypoint), osmNodes.get(childNode.getAttribute("ref")));
                            osmGraph.getNode(lastWaypoint).connectWithNodeAndWeigth(node, distance);
                            lastWaypoint = childNode.getAttribute("ref");
                        }
                    }
                    // for non naive: check for roundabout
                }
            }
        }

        return osmGraph;
    }

    protected static GraphUndirected<NodeOSM, EdgeOSM> createFiltered(final Document osmFile, final HashMap<String, OsmNode> osmNodes) {
        final GraphUndirected<NodeOSM, EdgeOSM> osmGraph = new GraphUndirected<NodeOSM, EdgeOSM>();

        final Element documentElement = osmFile.getDocumentElement();
        final NodeList ways = documentElement.getElementsByTagName("way");
        for (int i = 0; i < ways.getLength(); ++i) { // for each way
            final NodeList childNodes = ways.item(i).getChildNodes();
            String lastWaypoint = null;
            final List<String> metaIds = new LinkedList<>();
            int distance = 0;
            String currentWaypoint = null;
            boolean roundabout = false;
            for (int j = 0; j < childNodes.getLength(); ++j) { // go through the nodes
                final Node cNode = childNodes.item(j);
                if (cNode.getNodeType() == Node.ELEMENT_NODE) {
                    final Element childNode = (Element) cNode;
                    if (childNode.getNodeName() == "nd") {
                        currentWaypoint = childNode.getAttribute("ref");
                        if (osmNodes.containsKey(currentWaypoint)) {
                            if (!(lastWaypoint == null))
                                distance += getDistance(osmNodes.get(lastWaypoint), osmNodes.get(currentWaypoint));
                            lastWaypoint = currentWaypoint;
                            metaIds.add(currentWaypoint);

                        }
                    } else if (childNode.getNodeName() == "tag")
                        if (childNode.hasAttribute("k") && childNode.getAttribute("k").equals("junction") && childNode.getAttribute("v").equals("roundabout")) {
                            roundabout = true;
                            break;
                        }

                }
            }
            if (roundabout)
                distance = 0;
            for (int j = 1; j < metaIds.size(); ++j) {
                final String startNodeId = metaIds.get(j - 1);
                final String lastNodeId = metaIds.get(j);
                if (osmGraph.getNode(startNodeId) == null) {
                    final NodeOSM newNode = new NodeOSM(startNodeId);
                    osmGraph.addNode(newNode);
                }
                if (osmGraph.getNode(lastNodeId) == null) {
                    final NodeOSM newNode = new NodeOSM(lastNodeId);
                    osmGraph.addNode(newNode);
                }
                final List<OsmNode> metaNodes = new LinkedList<>();
                metaNodes.add(osmNodes.get(startNodeId));
                metaNodes.add(osmNodes.get(lastNodeId));
                osmGraph.getNode(startNodeId).connectWithNodeWeigthAndMeta(osmGraph.getNode(lastNodeId), distance, metaNodes);
            }
        }
        // TODO simplify
        final Iterator<NodeOSM> iteratorNodes = osmGraph.getNodes().iterator();
        while (iteratorNodes.hasNext()) {
            final NodeOSM node = iteratorNodes.next();
            if (node.getDegree() == 2) {
                final String currentNodeId = node.getId();
                final List<EdgeOSM> edges = node.getEdges();
                final EdgeOSM edge1 = edges.get(0);
                final EdgeOSM edge2 = edges.get(1);
                final String node1id = edge1.getNode1().getId().equals(currentNodeId) ? edge1.getNode2().getId() : edge1.getNode1().getId();
                final String node2id = edge2.getNode1().getId().equals(currentNodeId) ? edge2.getNode2().getId() : edge2.getNode1().getId();
                // concat the list in the right way
                final List<OsmNode> newMetaNodes = edge1.getMetaNodes(), metaNodes2 = edge2.getMetaNodes();
                // newMetaNodes = metaNodes1.get(0).id==node1id ? metaNodes1 : Collections.reverse(metaNodes1);
                if (newMetaNodes.get(0).id.equals(currentNodeId))
                    Collections.reverse(newMetaNodes);
                if (!metaNodes2.get(0).id.equals(currentNodeId))
                    Collections.reverse(metaNodes2);
                newMetaNodes.addAll(metaNodes2);
                // add a new edge
                osmGraph.getNode(node1id).connectWithNodeWeigthAndMeta(osmGraph.getNode(node2id), edge1.getWeight() + edge2.getWeight(), newMetaNodes);
                // remove the old node
                node.removeAllEdges();
                iteratorNodes.remove();
            }
        }

        return osmGraph;
    }

    public static GraphUndirected<NodeOSM, EdgeOSM> importOsmUndirected(final String filename) {
        final Document osmFile = getDomFromFile(filename);
        final HashMap<String, OsmNode> osmNodes = getOsmNodes(osmFile);
        final GraphUndirected<NodeOSM, EdgeOSM> osmGraph = createFiltered(osmFile, osmNodes);

        /**
         * TODO create edges
         * for each way-element:
         * create the nodes and connect them but not for roundabout
         * for each way which is a roundabout:
         * find the existing nodes and connect them
         */

        return osmGraph;
    }

}