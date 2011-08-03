package sonumina.math.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Queue;

/**
 * A very basic layout algorithm for directed graphs.
 * 
 * @author Sebastian Bauer
 */
public class DirectedGraphLayout<T>
{
	static class Attr
	{
		int posX;
		int posY;
		int layoutPosX;
		int layoutPosY;
		int distanceToRoot;	/* This defines the vertical rank */
		int horizontalRank;
		int width;
		int height;
	}
	
	static public class Dimension
	{
		public int width;
		public int height;
	}

	static public interface IGetDimension<T>
	{
		void get(T vertex, Dimension d);
	}
	
	static public interface IPosition<T>
	{
		void setSize(int width, int height);
		void set(T vertex, int left, int top);
	}
	
	private DirectedGraph<T> graph;
	private IGetDimension<T> dimensionCallback;
	private IPosition<T> positionCallback;
	private SlimDirectedGraphView<T> slimGraph;
	
	private int maxDistanceToRoot = -1;
	private Attr [] attrs;

	private DirectedGraphLayout(DirectedGraph<T> graph, IGetDimension<T> dimensionCallback, IPosition<T> positionCallback)
	{
		this.graph = graph;
		this.dimensionCallback = dimensionCallback;
		this.positionCallback = positionCallback;
		this.slimGraph = new SlimDirectedGraphView<T>(graph);
		
		attrs = new Attr[graph.getNumberOfVertices()];
		for (int i=0;i<graph.getNumberOfVertices();i++)
			attrs[i] = new Attr();
		
		/* Find the roots */
		List<T> rootList = new ArrayList<T>(4);
		for (T n : graph)
			if (graph.getInDegree(n)==0) rootList.add(n);
		if (rootList.size() == 0)
			rootList.add(graph.getArbitaryNode());
				
		/* Find out the distance to the root of each vertex. Remember the deepest one */
		for (T root : rootList)
		{
			graph.singleSourceLongestPath(root,new DirectedGraph.IDistanceVisitor<T>() {
				public boolean visit(T n, List<T> path, int distance)
				{
					attrs[slimGraph.getVertexIndex(n)].distanceToRoot = distance;
					if (distance > maxDistanceToRoot) maxDistanceToRoot = distance;
					return true;
				}
			});
		}
	}
	
	private void layout()
	{
		final int horizSpace = 0;
		final int vertSpace = 0;

		if (graph.getNumberOfVertices() == 0)
			return;

		/* Determine the dimension of each node */
		final Dimension dim = new Dimension();
		for (int i=0;i<slimGraph.getNumberOfVertices();i++)
		{
			Attr a = attrs[i];
			dimensionCallback.get(slimGraph.getVertex(i), dim);
			a.width = dim.width;
			a.height = dim.height;
		}
		
		/* Determine the heights of each level and the width of each level as well as the number of objects per level */
		int [] levelHeight = new int[maxDistanceToRoot+1];
		int [] levelWidth = new int[maxDistanceToRoot+1];
		int [] levelCounts = new int[maxDistanceToRoot+1];
		ArrayList [] levelNodes = new ArrayList[maxDistanceToRoot+1];
		int maxLevelWidth = -1;
		for (int i=0;i<slimGraph.getNumberOfVertices();i++)
		{
			T n = slimGraph.getVertex(i);
			Attr a = attrs[i];
			if (a.height > levelHeight[a.distanceToRoot])
				levelHeight[a.distanceToRoot] = a.height;
			levelCounts[a.distanceToRoot]++;
			levelWidth[a.distanceToRoot] += a.width;
			if (levelNodes[a.distanceToRoot] == null)
				levelNodes[a.distanceToRoot] = new ArrayList();
			levelNodes[a.distanceToRoot].add(n);
		}
		for (int i=0;i<=maxDistanceToRoot;i++)
		{
			levelWidth[i] += horizSpace * levelCounts[i];
			if (levelWidth[i] > maxLevelWidth)
				maxLevelWidth = levelWidth[i];
		}
		
		/* Determine the vertical position of each level */
		int [] levelYPos = new int[maxDistanceToRoot+1];
		for (int i=1;i<levelYPos.length;i++)
			levelYPos[i] = levelYPos[i-1] + levelHeight[i-1] + vertSpace;

		/* Assign ypos */
		for (int i=0;i<slimGraph.getNumberOfVertices();i++)
		{
			Attr a = attrs[i];
			a.layoutPosY = levelYPos[a.distanceToRoot];
		}

		/* Distribute x rank of nodes for each level */
		int [] levelCurXRank = new int[maxDistanceToRoot+1];
		for (int i=0;i<slimGraph.getNumberOfVertices();i++)
		{
			Attr a = attrs[i];
			a.horizontalRank = levelCurXRank[a.distanceToRoot]++;
		}

		/* Assign initial xpos */
		int [] levelCurXPos = new int[maxDistanceToRoot+1];
		for (int i=0;i<slimGraph.getNumberOfVertices();i++)
		{
			Attr a = attrs[i];
			a.layoutPosX = levelCurXPos[a.distanceToRoot];
			levelCurXPos[a.distanceToRoot] += a.width + horizSpace;
		}

		int currentScore = scoreLayout(maxDistanceToRoot, levelNodes);

		/* Build node queue */
		LinkedList<T> nodeQueue = new LinkedList<T>();
		for (int l = 0; l <= maxDistanceToRoot; l++)
		{
			for (int j=0;j<levelNodes[l].size();j++)
			{
				T n = (T) levelNodes[l].get(j);
				nodeQueue.add(n);
			}
		}

		boolean onlyAcceptImprovements = true;

		/* In each run, we select a node which decreases the score best */
		for (int run = 0; run < 100; run++)
		{
			int bestScore = currentScore;
			int bestLayoutPosX = -1;
			T bestNode = null;

			ListIterator<T> queueIter = nodeQueue.listIterator();
			
			LinkedList<T> savedNodes = new LinkedList<T>(); 

			boolean improved = false;
			
			/* First pass, we try to improve the configuration */

			while (queueIter.hasNext())
			{
				T n = queueIter.next();
				Attr na = attrs[slimGraph.getVertexIndex(n)];
				
				int horizRank = na.horizontalRank;
				int vertRank = na.distanceToRoot;
				
				/* Determine the minimal x position of this node. This is aligned to the left border of the node */
				int minX;
				if (horizRank==0) minX = 0;
				else minX = attrs[slimGraph.getVertexIndex((T)levelNodes[vertRank].get(horizRank-1))].layoutPosX + attrs[slimGraph.getVertexIndex((T)levelNodes[vertRank].get(horizRank-1))].width + horizSpace; 

				/* Determine the maximal x position of this node. This is aligned to the left border of the node */
				int maxX;
				if (horizRank==levelNodes[vertRank].size()-1) maxX = maxLevelWidth - na.width;
				else maxX = attrs[slimGraph.getVertexIndex((T)levelNodes[vertRank].get(horizRank+1))].layoutPosX - horizSpace - na.width;

				/* Determine all neighbors */
				ArrayList<T> neighbors = new ArrayList<T>();
				Iterator<T> iter = graph.getParentNodes(n);
				while (iter.hasNext())
					neighbors.add(iter.next());
				iter = graph.getChildNodes(n);
				while (iter.hasNext())
					neighbors.add(iter.next());

				/* Remember the current pos */
				int savedLayoutPosX = na.layoutPosX; 

				int sumX = 0;
				int cnt = 0;
				for (T neighbor : neighbors)
				{
					Attr neighbora = attrs[slimGraph.getVertexIndex(neighbor)];
					sumX += getEdgeX(neighbora);
					cnt++;
				}
				
				na.layoutPosX = Math.min(maxX,Math.max(minX,sumX / cnt - na.width / 2));

				int newScore = scoreLayout(maxDistanceToRoot, levelNodes);
				if (newScore <= bestScore && savedLayoutPosX != na.layoutPosX)
				{
					if (newScore < bestScore || !onlyAcceptImprovements)
					{
						bestScore = newScore;
						bestLayoutPosX = na.layoutPosX;
						bestNode = n;
						
						if (newScore == bestScore)
						{
//							System.out.println("Node \"" + n.toString() + "\" attains same score at pos " + na.layoutPosX);
							queueIter.remove();
							savedNodes.addLast(n);
						} else
						{
							improved = true;
						}
					}
				}

				na.layoutPosX = savedLayoutPosX; /* Restore */
			}
			
//			System.out.println(run + "  " + improved + " " + bestNode);
			
			if (bestNode != null)
			{
				attrs[slimGraph.getVertexIndex(bestNode)].layoutPosX =  bestLayoutPosX;
				currentScore = bestScore;
				onlyAcceptImprovements = true;
			} else
			{
				if (!onlyAcceptImprovements)
					break;

				onlyAcceptImprovements = false;
			}
			
			for (T n : savedNodes)
				nodeQueue.addLast(n);
		}
		
		/* Calculate area */
		int width = 0;
		int height = 0;
		for (int i=0;i<slimGraph.getNumberOfVertices();i++)
		{
			Attr a = attrs[i];
			if (a.layoutPosX + a.width > width) width = a.layoutPosX + a.width - 1;
			if (a.layoutPosY + a.height > height) height = a.layoutPosY + a.height - 1;
		}		
		positionCallback.setSize(width, height);

		/* Emit positions */
		for (int i=0;i<slimGraph.getNumberOfVertices();i++)
		{
			Attr a = attrs[i];
			positionCallback.set(slimGraph.getVertex(i), a.layoutPosX, a.layoutPosY);
		}
	}

	/**
	 * Scores the current layout.
	 * 
	 * @param nodes2Attrs
	 * @param maxDistanceToRoot
	 * @param levelNodes
	 * @return
	 */
	private int scoreLayout(int maxDistanceToRoot, ArrayList[] levelNodes)
	{
		int length = 0;
		for (int i=1;i<=maxDistanceToRoot;i++)
		{
			for (int j=0;j<levelNodes[i].size();j++)
			{
				T n = (T) levelNodes[i].get(j);
				Attr na = attrs[slimGraph.getVertexIndex(n)];
				int e1x = getEdgeX(na);
				
				Iterator<T> parents = graph.getParentNodes(n);
				while (parents.hasNext())
				{
					T p = parents.next();
					Attr ap = attrs[slimGraph.getVertexIndex(p)];
					int e2x = getEdgeX(ap);
					length += Math.abs(e1x - e2x);
				}
			}
		}
		return length;
	}

	/**
	 * Returns the x coordiate of the given attr.
	 * 
	 * @param na
	 * @return
	 */
	private final int getEdgeX(Attr na)
	{
		return na.layoutPosX + na.width / 2;
	}

	public static <T> void layout(DirectedGraph<T> graph, IGetDimension<T> dimensionCallback, IPosition<T> positionCallback)
	{
		new DirectedGraphLayout<T>(graph,dimensionCallback,positionCallback).layout();
	}
}
