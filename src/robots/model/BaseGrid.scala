package robots.model

import robots.model.enumeration.{PermanentSquare, RelativeDirection}
import robots.model.enumeration.RelativeDirection.Forward
import utopia.flow.util.CollectionExtensions._
import utopia.genesis.shape.shape2D.{Direction2D, TwoDimensional}

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
case class BaseGrid(data: Vector[Vector[PermanentSquare]])
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
	 * @param position A position
	 * @return Whether this grid contains that position
	 */
	def contains(position: TwoDimensional[Int]) = position.x >= 0 && position.x < width &&
		position.y >= 0 && position.y < height
	
	/**
	 * @param row Row index
	 * @param column Column index
	 * @return Type of square at that position
	 * @throws IndexOutOfBoundsException If position was outside of this grid
	 */
	@throws[IndexOutOfBoundsException]("If targeting square outside of this grid")
	def apply(row: Int, column: Int) = data(row)(column)
	
	/**
	 * @param position Grid position
	 * @return Type of square at that position
	 * @throws IndexOutOfBoundsException If position was outside of this grid
	 */
	@throws[IndexOutOfBoundsException]("If targeting square outside of this grid")
	def apply(position: TwoDimensional[Int]): PermanentSquare = apply(position.x, position.y)
	
	/**
	 * @param start The first included position
	 * @param direction Direction towards which squares are returned
	 * @param length How many squares should be returned
	 * @return Squares starting from 'start', moving to 'direction' and including 'length' squares
	 *         (less if line goes out of bounds)
	 */
	def line(start: GridPosition, direction: Direction2D, length: Int) =
		Iterator.iterate(start) { _ + direction }.take(length).takeWhile(contains).map { apply(_) }
	
	/**
	 * Scans this grid in a linear sequence, starting from a certain spot and continuing until line of sight is broken
	 * @param scanPosition Scanner position in this grid (not being scanned)
	 * @param scanDirection Direction which is scanned
	 * @return Squares that can be seen from the scanned position (excluding the position itself).
	 *         Up to one sight-blocking square may be included.
	 */
	def scan(scanPosition: GridPosition, scanDirection: Direction2D) =
	{
		val squares = Iterator.iterate(scanPosition) { _ + scanDirection }.drop(1).takeWhile(contains)
			.map { apply(_) }.toVector
		squares.indexWhereOption { _.blocksSight } match
		{
			case Some(lastVisibleIndex) => squares.take(lastVisibleIndex + 1)
			case None => squares
		}
	}
	
	/**
	 * Scans rows directly in front and also includes the rows / columns on each side of that scan area
	 * @param scanPosition Scanner position in this grid (not being scanned)
	 * @param scanDirection Direction which is scanned
	 * @return Forward: Squares that can be seen from the scanned position (excluding the position itself).
	 *         Up to one sight-blocking square may be included.
	 *         Left / Right: The squares on both sides of the main scan area.
	 */
	def wideScan(scanPosition: GridPosition, scanDirection: Direction2D) =
	{
		// Performs the direct scan first
		val directScanResults = scan(scanPosition, scanDirection)
		// Then scans the rows/columns next to the scan results
		val leftSide = line(scanPosition + scanDirection.rotatedQuarterCounterClockwise + scanDirection,
			scanDirection, directScanResults.size - 1).toVector
		val rightSide = line(scanPosition + scanDirection.rotatedQuarterClockwise + scanDirection, scanDirection,
			directScanResults.size - 1).toVector
		// Returns the results in a map
		Map[RelativeDirection, Vector[PermanentSquare]](Forward -> directScanResults,
			RelativeDirection.Left -> leftSide, RelativeDirection.Right -> rightSide)
	}
}
