package robots.model

import robots.controller.GlobalBotSettings._
import robots.model.enumeration.PermanentSquare
import robots.model.enumeration.ScanType.{Linear, Mini, Wide}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Graph
import utopia.flow.collection.immutable.Graph.GraphViewNode
import utopia.flow.collection.immutable.caching.cache.Cache
import utopia.genesis.util.Drawer
import utopia.paradigm.enumeration.Direction2D
import utopia.paradigm.shape.shape2d.Bounds

import scala.annotation.tailrec
import scala.collection.immutable.VectorBuilder

/**
 * Represents a bot's robots.view of the stable world
 * @author Mikko Hilpinen
 * @since 24.1.2021, v1
 */
case class BaseMapMemory(botLocation: GridPosition, data: Map[GridPosition, PermanentSquare])
	extends MapMemoryLike[PermanentSquare]
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
	// TODO: Use another algorithm
	lazy val bestMiniScanRoutes = bestRoutesFrom(miniScanRoutes)
	
	/**
	 * Route and direction data where linear scans are possible and will yield results
	 */
	lazy val linearScanRoutes = scanRoutesUsing(linearScanDirectionsFrom)
	/**
	 * Shortest routes to linear scan opportunities
	 */
	// TODO: Use another algorithm
	lazy val bestLinearScanRoutes = bestRoutesFrom(linearScanRoutes)
	
	/**
	 * All possible wide scan options that are available. Each result contains 4 items:<br>
	 * 1) Route information<br>
	 * 2) Scan direction<br>
	 * 3) List of positions to uncover (minimum)<br>
	 * 4) Whether there is a possibility to uncover more squares with the scan
	 */
	// Goes through each node, checking for possible scan results in any direction
	lazy val wideScanOptions = graphNodes.flatMap { node =>
		lazy val routes = routesTo(node)
		val origin = node.value
		Direction2D.values.flatMap { direction =>
			val (knownResults, possiblyMore) = wideScanResultFrom(origin + direction, direction)
			// If known or unknown results were found, returns that route + direction data
			if (knownResults.nonEmpty)
				routes.map { route => (route, direction, knownResults, possiblyMore) }
			else
				None
		}
	}
	
	
	// COMPUTED ------------------------------
	
	/**
	 * @return Known graph nodes
	 */
	def graphNodes =
		if (graph.isEmpty) Set(graph(botLocation)) else graph.nodes
	
	
	// IMPLEMENTED  --------------------------
	
	override def apply(position: GridPosition) =
		MapSquare(position, data.get(position), this)
	
	override def squareTypeAt(position: GridPosition) = data.get(position)
	
	override def isDefined(position: GridPosition) = data.contains(position)
	
	
	// OTHER   -------------------------------
	
	/**
	 * Draws this map data
	 * @param drawer Drawer to perform the drawing
	 * @param zeroCoordinateLocation Grid coordinates of the (0,0) instance in this data
	 */
	def draw(drawer: Drawer, zeroCoordinateLocation: GridPosition) =
	{
		val drawers = Cache[PermanentSquare, Drawer] { s => drawer.onlyFill(s.color) }
		data.foreach { case (position, square) =>
			drawers(square).draw(Bounds((position + zeroCoordinateLocation) * pixelsPerGridUnit, gridSquarePixelSize))
		}
	}
	
	/**
	 * @param newLocation New known bot location
	 * @param newSquareType The type of the new location square, if known (default = None)
	 * @return A copy of this data with update bot location information
	 */
	def withBotLocation(newLocation: GridPosition, newSquareType: Option[PermanentSquare] = None) =
	{
		if (botLocation == newLocation && newSquareType.forall { data.get(newLocation).contains(_) })
			this
		else
			newSquareType match
			{
				case Some(newType) => copy(botLocation = newLocation, data = data + (newLocation -> newType))
				case None => copy(botLocation = newLocation)
			}
	}
	
	/**
	 * @param squares Square data
	 * @return A copy of this memory data with updated square information
	 */
	def withData(squares: Iterable[(GridPosition, PermanentSquare)]) =
	{
		// Only updates this instance if changes are to be made
		if (squares.exists { case (position, square) => !data.get(position).contains(square) })
			copy(data = data ++ squares)
		else
			this
	}
	
	/**
	 * @param squarePosition position of the described square
	 * @param squareType Described square's type
	 * @return An updated copy of this data
	 */
	def withSquare(squarePosition: GridPosition, squareType: PermanentSquare) =
	{
		if (data.get(squarePosition).contains(squareType))
			this
		else
			copy(data = data + (squarePosition -> squareType))
	}
	
	/**
	 * @param position A grid position
	 * @return All known routes to from the current bot location to that position
	 */
	def routesTo(position: GridPosition): Iterable[MapRoute[PermanentSquare]] = routesTo(graph(position))
	
	/**
	 * @param node A graph node
	 * @return All routes from the current bot location to that node
	 */
	def routesTo(node: GraphViewNode[GridPosition, Direction2D]) =
	{
		if (node.value == originNode.value)
			Set(MapRoute.empty(botLocation, this))
		else
			originNode.routesTo(node).map { route =>
				MapRoute(botLocation +: route.map { _.end.value }, route.map { _.value }, this)
			}
	}
	
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
	
	/**
	 * Calculates the best mini-scan to perform
	 * @param costFunction A function for calculating the cost of a route + scan direction combination
	 * @return The best mini scan option cost-wise (lowest cost)
	 */
	// TODO: Use another algorithm
	def calculateBestMiniScan(costFunction: (MapRoute[PermanentSquare], Direction2D) => Double) =
		miniScanRoutes.minByOption { case (route, dir) => costFunction(route, dir) }
	
	/**
	 * Calculates the best linear scan to perform
	 * @param costFunction A function for calculating the cost of a route + scan direction combination
	 * @return The best linear scan option cost-wise (lowest cost)
	 */
	// TODO: Use another algorithm
	def calculateBestLinearScan(costFunction: (MapRoute[PermanentSquare], Direction2D) => Double) =
		linearScanRoutes.minByOption { case (route, direction) => costFunction(route, direction) }
	
	/**
	 * Calculates the best wide scan to perform
	 * @param costFunction A function for calculating the cost of a route + scan direction combination
	 * @param rewardFunction A function for calculating scan reward. Accepts known squares to uncover and a
	 *                       boolean indicating whether there may be more squares uncovered.
	 * @return The best wide scan option rewards and cost-wise
	 */
	// TODO: Use another algorithm
	def calculateBestWideScan(costFunction: (MapRoute[PermanentSquare], Direction2D) => Double)
	                         (rewardFunction: (Set[GridPosition], Boolean) => Double) =
		wideScanOptions.maxByOption { case (route, dir, known, more) =>
			rewardFunction(known, more) - costFunction(route, dir)
		}
	
	/**
	 * Calculates the best scan to perform
	 * @param miniScanReward Base reward for all mini-scans
	 * @param linearScanReward Base reward for all linear scans
	 * @param wideRewardFunction A function for calculating wide scan rewards. Accepts known squares to uncover and a
	 *                       boolean indicating whether there may be more squares uncovered.
	 * @param costFunction A function for calculating the cost of a route + scan direction combination
	 * @return The scan option that provides the greatest reward / cost ratio
	 */
	// TODO: Use another algorithm
	def calculateBestScan(miniScanReward: Double, linearScanReward: Double)
	                     (wideRewardFunction: (Set[GridPosition], Boolean) => Double)
	                     (costFunction: (MapRoute[PermanentSquare], Direction2D) => Double) =
	{
		// Calculates costs / rewards for each option
		val miniScansWithRewards = miniScanRoutes.map { case (route, dir) => (route -> dir) -> (
			miniScanReward / costFunction(route, dir)) }
		val linearScansWithRewards = linearScanRoutes.map { case (route, dir) => (route -> dir) -> (
			linearScanReward / costFunction(route, dir)) }
		val wideScansWithRewards = wideScanOptions.map { case (route, dir, minResults, open) => (route -> dir) -> (
			wideRewardFunction(minResults, open) / costFunction(route, dir))
		}
		// Selects the best option
		(miniScansWithRewards.view.map { _ -> Mini } ++ linearScansWithRewards.view.map { _ -> Linear } ++
			wideScansWithRewards.view.map { _ -> Wide }).maxByOption { _._1._2 }
			.map { case (((route, dir), _), scanType) => (route, dir, scanType) }
	}
	
	private def bestRouteFrom(routes: Iterable[(MapRoute[PermanentSquare], Direction2D)], currentHeading: Direction2D,
	                          preferredMovementDirection: Direction2D) =
	{
		routes.bestMatch(
			_._2 == currentHeading,
			_._1.directions.headOption.forall { _ == preferredMovementDirection }
		).maxByOption { _._1.directions.count { _ == preferredMovementDirection } }
	}
	
	private def bestRoutesFrom(routes: Set[(MapRoute[PermanentSquare], Direction2D)]) =
	{
		val routesWithLengths = routes.toMultiMap { route => route._1.actualTravelDistance -> route }
		routesWithLengths.keys.minOption match
		{
			case Some(shortestLength) => routesWithLengths(shortestLength)
			case None => Set[(MapRoute[PermanentSquare], Direction2D)]()
		}
	}
	
	private def scanRoutesUsing(scan: GridPosition => IterableOnce[Direction2D]) =
	{
		graphNodes.flatMap { node =>
			lazy val routes = routesTo(node)
			scan(node.value).iterator.flatMap { direction => routes.map { _ -> direction } }
		}
	}
	
	private def miniScanDirectionsFrom(position: GridPosition) =
	{
		Direction2D.values.filter { direction =>
			unknownLocations.contains(position + direction)
		}
	}
	
	private def linearScanDirectionsFrom(position: GridPosition) =
		Direction2D.values.filter { canLinearScan(position, _) }
	
	@tailrec
	private def canLinearScan(from: GridPosition, to: Direction2D): Boolean =
	{
		val targetPosition = from + to
		unknownLocations.contains(targetPosition) ||
			(data.get(targetPosition).exists { _.canBeSeenTrough } && canLinearScan(targetPosition, to))
	}
	
	private def wideScanResultFrom(position: GridPosition, direction: Direction2D): (Set[GridPosition], Boolean) =
	{
		if (unknownLocations.contains(position))
			Set(position) -> true
		else if (data.get(position).exists { _.canBeSeenTrough })
		{
			// Checks whether there are non-scanned sides on the path, then moves forward recursively
			val nonScannedSides = direction.perpendicular.map { position + _ }.filter { !data.contains(_) }
			val (recursiveResults, isOpen) = wideScanResultFrom(position + direction, direction)
			(recursiveResults ++ nonScannedSides) -> isOpen
		}
		else
			Set[GridPosition]() -> false
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
