package robots.model

import robots.model.enumeration.Square
import utopia.paradigm.shape.template.HasDimensions

/**
 * A possible temporary grid state
 * @author Mikko
 * @since 22.1.2021, v
 * @param data Square data. First (outer) vector is for columns and the second (inner) for rows.
 */
case class Grid(data: Vector[Vector[Square]]) extends GridLike[Square]
{
	// ATTRIBUTES   --------------------------
	
	/**
	 * Width of this grid in squares
	 */
	lazy val width = data.size
	
	/**
	 * Height of this grid in squares
	 */
	lazy val height = data.map { _.size }.min
	
	
	// IMPLEMENTED  --------------------------
	
	def apply(x: Int, y: Int) = data(x)(y)
	
	override def contains(position: HasDimensions[Int]) = position.x >= 0 && position.y >= 0 &&
		position.x < width && position.y < height
}
