package controller

import robots.model.BaseGrid
import robots.model.enumeration.Square.{Empty, Wall}
import utopia.flow.util.LinesFrom

import java.nio.file.Path

/**
 * Used for reading maps (grids) from text files
 * @author Mikko Hilpinen
 * @since 22.1.2021, v1
 */
object MapReader
{
	/**
	 * @param path Path from which map data is read from
	 * @return A grid read from that file. May contain a failure
	 */
	def apply(path: Path) = LinesFrom.path(path).map { lines =>
		BaseGrid.fromRows(lines.map { _.map(charToSquare).toVector })
	}
	
	private def charToSquare(char: Char) = char match
	{
		case 'X' => Wall
		case _ => Empty
	}
}
