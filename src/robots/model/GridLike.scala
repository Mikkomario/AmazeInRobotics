package robots.model

import robots.model.enumeration.RelativeDirection.Forward
import robots.model.enumeration.{RelativeDirection, Square}
import utopia.flow.collection.CollectionExtensions._
import utopia.paradigm.enumeration.Direction2D
import utopia.paradigm.shape.template.HasDimensions

/**
 * A common trait for grids
 * @author Mikko Hilpinen
 * @since 22.1.2021, v1
 */
trait GridLike[+A <: Square]
{
	// ABSTRACT --------------------------
	
	/**
	 * @param x Row index
	 * @param y Column index
	 * @return Type of square at that position
	 * @throws IndexOutOfBoundsException If position was outside of this grid
	 */
	@throws[IndexOutOfBoundsException]("If targeting square outside of this grid")
	def apply(x: Int, y: Int): A
	
	/**
	 * @param position A position
	 * @return Whether this grid contains that position
	 */
	def contains(position: HasDimensions[Int]): Boolean
	
	
	// OTHER    --------------------------
	
	/**
	 * @param position Grid position
	 * @return Type of square at that position
	 * @throws IndexOutOfBoundsException If position was outside of this grid
	 */
	@throws[IndexOutOfBoundsException]("If targeting square outside of this grid")
	def apply(position: HasDimensions[Int]): A = apply(position.x, position.y)
	
	/**
	 * @param position Target position
	 * @return Contents of this grid in that position. None if that position is outside of this grid
	 */
	def get(position: HasDimensions[Int]) = if (contains(position)) Some(apply(position)) else None
	
	/**
	 * @param start The first included position
	 * @param direction Direction towards which squares are returned
	 * @param length How many squares should be returned
	 * @return Squares starting from 'start', moving to 'direction' and including 'length' squares
	 *         (less if line goes out of bounds). Includes the positions of the squares as first items.
	 */
	def line(start: GridPosition, direction: Direction2D, length: Int) =
		Iterator.iterate(start) { _ + direction }.take(length).takeWhile(contains).map { p => p -> apply(p) }
	
	/**
	 * Scans this grid in a linear sequence, starting from a certain spot and continuing until line of sight is broken
	 * @param scanPosition Scanner position in this grid (not being scanned)
	 * @param scanDirection Direction which is scanned
	 * @return Squares that can be seen from the scanned position (excluding the position itself).
	 *         Up to one sight-blocking square may be included. Includes grid position of each such square
	 *         (as first item)
	 */
	def scan(scanPosition: GridPosition, scanDirection: Direction2D) =
	{
		val squares = Iterator.iterate(scanPosition) { _ + scanDirection }.drop(1).takeWhile(contains)
			.map { p => p -> apply(p) }.toVector
		squares.indexWhereOption { _._2.blocksSight } match
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
		Map[RelativeDirection, Vector[(GridPosition, A)]](Forward -> directScanResults,
			RelativeDirection.Left -> leftSide, RelativeDirection.Right -> rightSide)
	}
}
