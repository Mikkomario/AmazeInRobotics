package robots.model

import robots.model.enumeration.Square

/**
 * A possible temporary grid state
 * @author Mikko
 * @since 22.1.2021, v
 * @param data Square data. First (outer) vector is for columns and the second (inner) for rows.
 */
case class Grid(data: Vector[Vector[Square]]) extends GridLike[Square]
{
	override def width = data.size
	
	override def height = data.map { _.size }.min
	
	def apply(column: Int, row: Int) = data(column)(row)
}
