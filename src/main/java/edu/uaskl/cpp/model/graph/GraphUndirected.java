package edu.uaskl.cpp.model.graph;

import edu.uaskl.cpp.algorithmen.AlgorithmsUndirected;
import edu.uaskl.cpp.model.meta.MetadataCreatorCpp;
import edu.uaskl.cpp.model.meta.interfaces.Metadata;
import edu.uaskl.cpp.model.node.NodeCpp;

/**
 * @author tbach
 */
public class GraphUndirected<M extends Metadata> extends GraphBasic<M> {
    private final AlgorithmsUndirected algorithms = new AlgorithmsUndirected(this);

    public GraphUndirected() {
        super("Undirected Graph");
    }

    protected GraphUndirected(final String string) {
        super(string);
    }

    protected GraphUndirected(String name, MetadataCreatorCpp<M> metadataCreator) {
    	super(name, metadataCreator);
    }

    @Override
    public AlgorithmsUndirected getAlgorithms() {
        return algorithms;
    }

    @Override
    public int getGetNumberOfEdges() {
        return (super.getGetNumberOfEdges() >> 1); // the same edge is counted 2 times for directed graphs, but only once for undirected
    }

    /** Running time: O(log(|nodes| + |edgesFromGivenNode|*|edgesFromRelatedNode|)) */
    public boolean entferneKnoten(final NodeCpp<M> nodeToRemove) {
        final boolean successful = this.nodes.remove(nodeToRemove);
        if (!successful)
            return false;
        nodeToRemove.removeAllEdges();
        return true;
    }
}
