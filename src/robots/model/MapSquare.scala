package robots.model

import robots.model.enumeration.Square
import utopia.paradigm.enumeration.Direction2D

object MapSquare
{
	/**
	 * Map square type where the square content may be known or unknown
	 */
	type OpenMapSquare[+T <: Square] = MapSquare[Option[T], T]
	
	/**
	 * Map square type where the square content is known
	 */
	type KnownMapSquare[+T <: Square] = MapSquare[T, T]
	
	/**
	 * Map square type where the square content is unknown
	 */
	type UnknownMapSquare[+T <: Square] = MapSquare[None.type, T]
}

/**
 * A robots.view into a square location in bot memory
 * @author Mikko Hilpinen
 * @since 24.1.2021, v1
 */
case class MapSquare[+T, +MT <: Square](position: GridPosition, squareType: T, private val map: MapMemoryLike[MT])
{
	// COMPUTED -------------------------
	
	/**
	 * @return Squares adjacent to this one
	 */
	def neighbors = position.adjacent.map { map(_) }
	
	/**
	 * @return Those squares adjacent to this one which type is known
	 */
	def knownNeighbors = position.adjacent.flatMap { newPosition =>
		map.squareTypeAt(newPosition).map { squareType => copy(position = newPosition, squareType = squareType) }
	}
	
	/**
	 * @return Those squares adjacent to this one which type is not known
	 */
	def unknownNeighbors = position.adjacent.flatMap { newPosition =>
		if (map.isDefined(newPosition))
			None
		else
			Some(copy(position = newPosition, squareType = None))
	}
	
	/**
	 * @return Those squares adjacent to this one which can be passed through. Doesn't count squares whose
	 *         type is know known.
	 */
	def passableNeighbors = knownNeighbors.filter { _.squareType.isPassable }
	
	/**
	 * @return Those squares adjacent to this one which can be seen through. Doesn't count squares whose
	 *         type is know known.
	 */
	def seeThroughNeighbors = knownNeighbors.filter { _.squareType.canBeSeenTrough }
	
	/**
	 * @return Number of exits from this square. Contains both minimum and possible maximum value. The difference comes
	 *         from whether unknown neighbors are counted or not.
	 */
	def numberOfPassages =
	{
		val squares = neighbors
		val minPassages = squares.count { _.squareType.exists { _.isPassable } }
		val unknowns = squares.count { _.squareType.isEmpty }
		minPassages to (minPassages + unknowns)
	}
	
	
	// OTHER    ----------------------------
	
	/**
	 * @param map Another map
	 * @tparam MT2 Type of square types in that map
	 * @return This square in that map
	 */
	def in[MT2 <: Square](map: MapMemoryLike[MT2]) = copy(map = map)
	
	/**
	 * @param direction Direction of movement
	 * @return A square next to this one at specified direction
	 */
	def +(direction: Direction2D) = map(position + direction)
}
