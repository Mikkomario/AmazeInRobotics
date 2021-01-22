package robots.model

import robots.model.enumeration.{PermanentSquare, Square}
import utopia.flow.util.CollectionExtensions._

object BaseGrid
{
	/**
	 * @param rows Square rows
	 * @return A grid based on those rows
	 */
	def fromRows(rows: Vector[Vector[PermanentSquare]]) =
	{
		val width = rows.map { _.size }.min
		apply((0 until width).map { x => rows.map { _(x) } }.toVector)
	}
}

/**
 * A permanent grid that forms a maze map
 * @author Mikko Hilpinen
 * @since 22.1.2021, v1
 * @param data Square data. First (outer) vector is for columns and the second (inner) for rows.
 */
case class BaseGrid(data: Vector[Vector[PermanentSquare]]) extends GridLike[PermanentSquare]
{
	// ATTRIBUTES   ----------------------
	
	/**
	 * The width of this grid in squares
	 */
	val width = data.size
	
	/**
	 * The height of this grid in squares
	 */
	val height = data.map { _.size }.min
	
	
	// OTHER    --------------------------
	
	/**
	 * @param row Row index
	 * @param column Column index
	 * @return Type of square at that position
	 * @throws IndexOutOfBoundsException If position was outside of this grid
	 */
	@throws[IndexOutOfBoundsException]("If targeting square outside of this grid")
	def apply(row: Int, column: Int) = data(row)(column)
	
	/**
	 * Updates a number of squares in this grid
	 * @param squares Squares to update
	 * @return Updated version of this grid
	 */
	def ++(squares: Iterable[(GridPosition, Square)]) =
	{
		val updates = squares.toMultiMap { case (position, square) => position.x -> (position.y -> square) }
		val newData = (0 until width).map { columnIndex =>
			val original = data(columnIndex)
			updates.get(columnIndex) match
			{
				case Some(changes) =>
					val changeMap = changes.toMap
					(0 until height).map { rowIndex => changeMap.getOrElse(rowIndex, original(rowIndex)) }.toVector
				case None => original
			}
		}.toVector
		Grid(newData)
	}
}
