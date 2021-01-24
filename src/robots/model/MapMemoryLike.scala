package robots.model

import robots.model.enumeration.Square

/**
 * A common trait for map memory instances
 * @author Mikko Hilpinen
 * @since 24.1.2021, v1
 */
trait MapMemoryLike[+T <: Square]
{
	/**
	 * @param position A grid position
	 * @return Square data in that position
	 */
	def apply(position: GridPosition): MapSquare[Option[T], T]
	
	/**
	 * @param position A grid position
	 * @return Square type data in that position
	 */
	def squareTypeAt(position: GridPosition): Option[T]
	
	/**
	 * @param position A grid position
	 * @return Whether the square type in that position is defined
	 */
	def isDefined(position: GridPosition): Boolean
}
