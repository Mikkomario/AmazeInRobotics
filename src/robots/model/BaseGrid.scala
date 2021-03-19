package robots.model

import robots.model.enumeration.{PermanentSquare, Square}
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.DataTypeException
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.CollectionExtensions._
import utopia.genesis.shape.shape2D.TwoDimensional

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
	
	/**
	 * Attempts to convert a value to a base grid
	 * @param value Value representing a base grid
	 * @return Parsed grid. Failure if some data was not parseable.
	 */
	def fromValue(value: Value) = value.vector.toTry {
		DataTypeException(s"Can't convert ${value.description} to vector") }
		.flatMap { rows =>
			val squaresForCodes = PermanentSquare.values.map { s => s.characterCode -> s }.toMap
			rows.tryMap { row =>
				row.getString.toCharArray.toVector.tryMap { charCode => squaresForCodes.get(charCode)
					.toTry { new IllegalArgumentException(
						s"'$charCode' is not a recognized base square code. Please use one of ${
							squaresForCodes.keySet.mkString(", ")}") }
				}
			}
		}.map { BaseGrid(_) }
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
	
	
	// COMPUTED -------------------------
	
	/**
	 * @return A value representation of this grid
	 */
	def toValue: Value = data.map { row => row.map { _.characterCode }.mkString("") }
	
	
	// IMPLEMENTED  ---------------------
	
	/**
	 * @param x Row index
	 * @param y Column index
	 * @return Type of square at that position
	 * @throws IndexOutOfBoundsException If position was outside of this grid
	 */
	@throws[IndexOutOfBoundsException]("If targeting square outside of this grid")
	def apply(x: Int, y: Int) = data(x)(y)
	
	override def contains(position: TwoDimensional[Int]) = position.x >= 0 && position.y >= 0 &&
		position.x < width && position.y < height
	
	
	// OTHER    --------------------------
	
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
