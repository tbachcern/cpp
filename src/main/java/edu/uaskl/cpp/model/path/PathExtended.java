package edu.uaskl.cpp.model.path;

import java.util.List;

import edu.uaskl.cpp.model.node.NodeExtended;
import edu.uaskl.cpp.model.path.interfaces.Path;
import edu.uaskl.cpp.tools.CollectionTools;

/**
 * @author tbach
 */
public class PathExtended<T extends NodeExtended<T, ?>> implements Path<T> {
    private final List<T> nodes;
    private int distance;

    public PathExtended(final List<T> nodes) {
    	this.nodes = nodes;
        this.distance = 0;
    }

    public PathExtended(final List<T> nodes, final int distance) {
        this.nodes = nodes;
        this.distance = distance;
    }

    @Override
    public int getDistance() {
        return distance;
    }

    @Override
    public int getLength() {
        return nodes.size();
    }

    @Override
    public List<T> getNodes() {
        return nodes;
    }

    @Override
    public String toString() {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Distance: ").append(distance).append(", length: ").append(nodes.size()).append("\n");
        stringBuilder.append(CollectionTools.join("->", nodes));
        return stringBuilder.toString();
    }
}
