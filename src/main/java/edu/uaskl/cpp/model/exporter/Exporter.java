package edu.uaskl.cpp.model.exporter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.uaskl.cpp.map.meta.WayNodeOSM;
import edu.uaskl.cpp.model.edge.EdgeCppOSM;
import edu.uaskl.cpp.model.node.NodeCppOSM;
import edu.uaskl.cpp.model.path.PathExtended;

public class Exporter {
	
	private static String createSegment(List<WayNodeOSM>metaNodes,int id){
		final List<String> colors = new ArrayList<String>();
		colors.add("b1563f");
		colors.add("b4693f");
		colors.add("b67b3f");
		colors.add("b98f3f");
		colors.add("bba33f");
		colors.add("beb83f");
		colors.add("b2c040");
		colors.add("a0c141");
		colors.add("8ec242");
		colors.add("7cc344");
		final StringBuilder output= new StringBuilder();
		output.append("var line");
		output.append(id);
		output.append(" = L.polyline([[");
		output.append(metaNodes.get(0).getLatitude());
		output.append(",");
		output.append(metaNodes.get(0).getLongitude());
		output.append("]");
		for(int i = 1; i < metaNodes.size() ; ++i) {
			output.append(",[");
			output.append(metaNodes.get(i).getLatitude());
			output.append(",");
			output.append(metaNodes.get(i).getLongitude());
			output.append("]");
		}
		output.append("], {weight: 3,opacity: 1,color:'#");
		output.append(colors.get(id%colors.size()));
		output.append("'}).addTo(map);\nline");
		output.append(id);
		output.append(".setText('\u25BA ");
		output.append(id);
		output.append(" ', {repeat: false,offset: 0,attributes: {fill:'black'}});\n");
		return output.toString();
	}
	
	
	public static void exportPathToHTML(PathExtended<NodeCppOSM, EdgeCppOSM> path, File folder){
		final List<NodeCppOSM> nodes = path.getNodes();
		NodeCppOSM previousNode = nodes.get(0);
		NodeCppOSM currentNode;
		try (Writer fw = new FileWriter( new File(folder, "overlay.js" ));) {
			for(int index = 1; index < nodes.size() ; ++index){
				currentNode = nodes.get(index);
				//TODO use unvisited edges
				final EdgeCppOSM edge = previousNode.getEdgeToNode(currentNode);
				final List<WayNodeOSM> metaNodes = edge.getMetadata().getNodes();
				if (((Long)metaNodes.get(0).getID()).equals(currentNode.getId())){
					Collections.reverse(metaNodes);
				}
				fw.write(createSegment(metaNodes,index));
				fw.append(System.lineSeparator());
				previousNode = currentNode;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
