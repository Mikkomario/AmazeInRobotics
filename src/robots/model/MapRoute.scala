package robots.model

import robots.model.enumeration.Square
import utopia.genesis.shape.shape2D.Direction2D

object MapRoute
{
	/**
	 * Creates an empty map route (start is the end)
	 * @param position Start / end position
	 * @param map Map this route is calculated on
	 * @tparam T Type of squares in the map
	 * @return A new route
	 */
	def empty[T <: Square](position: GridPosition, map: MapMemoryLike[T]) =
		apply(Vector(position), Vector(), map)
}

/**
 * Represents a route in a map
 * @author Mikko Hilpinen
 * @since 24.1.2021, v1
 */
case class MapRoute[+T <: Square](positions: Vector[GridPosition], directions: Vector[Direction2D],
                                  private val map: MapMemoryLike[T])
{
	// ATTRIBUTES   -------------------------
	
	/**
	 * Squares that are part of this route
	 */
	lazy val squares = positions.map { map(_) }
	
	
	// COMPUTED -----------------------------
	
	/**
	 * @return How much space is crossed, if the same route was take using the most direct path
	 *         (over obstacles, possibly)
	 */
	def directTravelDistance =
	{
		if (positions.size < 2)
			0
		else
			(positions.last - positions.head).dimensions.sum
	}
	
	/**
	 * @return How many movements this route contains
	 */
	def actualTravelDistance = directions.size
	
	/**
	 * @return The effectiveness of movement in this route, where 1 is the highest possible value
	 */
	def effectiveness = if (directions.isEmpty) 1 else directTravelDistance / (actualTravelDistance: Double)
	
	
	// IMPLEMENTED  ---------------------------
	
	override def toString = if (directions.isEmpty) "No Movement" else directions.mkString(" -> ")
	
	
	// OTHER    -------------------------------
	
	/**
	 * @param map Another map
	 * @tparam T2 Type of squares in that map
	 * @return This route in that map
	 */
	def in[T2 <: Square](map: MapMemoryLike[T2]) = copy(map = map)
}
