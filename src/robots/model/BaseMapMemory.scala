package robots.model

import robots.model.enumeration.PermanentSquare
import utopia.flow.datastructure.immutable.Graph
import utopia.flow.util.CollectionExtensions._
import utopia.genesis.shape.shape2D.Direction2D

import scala.annotation.tailrec
import scala.collection.immutable.VectorBuilder

/**
 * Represents a bot's view of the stable world
 * @author Mikko Hilpinen
 * @since 24.1.2021, v1
 */
case class BaseMapMemory(botLocation: GridPosition, data: Map[GridPosition, PermanentSquare])
{
	// ATTRIBUTES   ---------------------------
	
	// Route graph & scannable unknown locations
	lazy val (graph, unknownLocations) =
	{
		val (connections, unknowns) = collectGraph(Set(botLocation), Set())
		Graph(connections) -> unknowns
	}
	
	/**
	 * Current bot location node
	 */
	lazy val originNode = graph(botLocation)
	
	/**
	 * Route and direction data where mini scans are possible and will yield results
	 */
	lazy val miniScanRoutes = scanRoutesUsing(miniScanDirectionsFrom)
	/**
	 * Shortest routes to mini scan opportunities
	 */
	lazy val bestMiniScanRoutes = bestRoutesFrom(miniScanRoutes)
	
	/**
	 * Route and direction data where linear scans are possible and will yield results
	 */
	lazy val linearScanRoutes = scanRoutesUsing(linearScanDirectionsFrom)
	/**
	 * Shortest routes to linear scan opportunities
	 */
	lazy val bestLinearScanRoutes = bestRoutesFrom(linearScanRoutes)
	
	
	// OTHERS   -------------------------------
	
	/**
	 * Finds the best mini scan opportunity available
	 * @param currentHeading Current robot heading
	 * @param preferredMovementDirection Preferred direction of initial movement
	 * @return The best current mini scan opportunity
	 */
	def bestMiniScan(currentHeading: Direction2D, preferredMovementDirection: Direction2D) =
		bestRouteFrom(bestMiniScanRoutes, currentHeading, preferredMovementDirection)
	
	/**
	 * Finds the best linear scan opportunity available
	 * @param currentHeading Current robot heading
	 * @param preferredMovementDirection Preferred direction of initial movement
	 * @return The best current linear scan opportunity
	 */
	def bestLinearScan(currentHeading: Direction2D, preferredMovementDirection: Direction2D) =
		bestRouteFrom(bestLinearScanRoutes, currentHeading, preferredMovementDirection)
	
	private def bestRouteFrom(routes: Iterable[(Vector[Direction2D], Direction2D)], currentHeading: Direction2D,
	                          preferredMovementDirection: Direction2D) =
	{
		routes.bestMatch(Vector(
			_._2 == currentHeading,
			_._1.head == preferredMovementDirection
		)).headOption
	}
	
	private def bestRoutesFrom(routes: Set[(Vector[Direction2D], Direction2D)]) =
	{
		val routesWithLengths = routes.toMultiMap { route => route._1.size -> route }
		routesWithLengths.keys.minOption match
		{
			case Some(shortestLength) => routesWithLengths(shortestLength)
			case None => Set[(Vector[Direction2D], Direction2D)]()
		}
	}
	
	private def scanRoutesUsing(scan: GridPosition => IterableOnce[Direction2D]) =
		graph.nodes.flatMap { node =>
			lazy val routes = originNode.routesTo(node).map { _.map { _.content } }
			scan(node.content).iterator.flatMap { direction => routes.map { _ -> direction } }
		}
	
	private def miniScanDirectionsFrom(position: GridPosition) =
		Direction2D.values.filter { direction => unknownLocations.contains(position + direction) }
	
	private def linearScanDirectionsFrom(position: GridPosition) =
		Direction2D.values.filter { canLinearScan(position, _) }
	
	@tailrec
	private def canLinearScan(from: GridPosition, to: Direction2D): Boolean =
	{
		val targetPosition = from + to
		unknownLocations.contains(targetPosition) ||
			(data.get(targetPosition).exists { _.canBeSeenTrough } && canLinearScan(targetPosition, to))
	}
	
	private def collectGraph(origins: Set[GridPosition], previousOrigins: Set[GridPosition]): (Set[(GridPosition, Direction2D, GridPosition)], Set[GridPosition]) =
	{
		// Checks the traversable nodes accessible from those origins. Collects unknown neighbor squares as well
		val newUnknownsBuilder = new VectorBuilder[GridPosition]()
		val nextOriginsBuilder = new VectorBuilder[GridPosition]()
		val routesToNextOriginsBuilder = new VectorBuilder[(GridPosition, Direction2D, GridPosition)]()
		
		// Checks available options from each origin
		origins.iterator.foreach { origin =>
			// Checks the adjacent squares for
			// a) unknown squares
			// b) squares that can be traversed to, which haven't yet been origins
			Direction2D.values.foreach { direction =>
				val targetPosition = origin + direction
				data.get(targetPosition) match
				{
					case Some(known) =>
						// If the route is passable (and not yet recorded), includes that in the next iteration and
						// remembers this connection
						if (known.isPassable)
						{
							if (!origins.contains(targetPosition) && !previousOrigins.contains(targetPosition))
							{
								nextOriginsBuilder += targetPosition
								routesToNextOriginsBuilder += Tuple3(origin, direction, targetPosition)
							}
						}
						// If the target square can't be traversed but can be seen through,
						// records a possible unknown square behind it
						else if (known.canBeSeenTrough)
						{
							val behindPosition = targetPosition + direction
							if (!data.contains(behindPosition))
								newUnknownsBuilder += behindPosition
						}
					case None => newUnknownsBuilder += targetPosition
				}
			}
		}
		
		// Remembers the results and moves to the next iteration, based on the found passages
		if (nextOriginsBuilder.nonEmpty)
		{
			val newPreviousOrigins = origins ++ previousOrigins
			val (recursiveRoutes, recursiveUnknowns) = collectGraph(nextOriginsBuilder.result().toSet,
				newPreviousOrigins)
			(routesToNextOriginsBuilder.result().toSet ++ recursiveRoutes,
				newUnknownsBuilder.result().toSet ++ recursiveUnknowns)
		}
		else
			routesToNextOriginsBuilder.result().toSet -> newUnknownsBuilder.result().toSet
	}
}
