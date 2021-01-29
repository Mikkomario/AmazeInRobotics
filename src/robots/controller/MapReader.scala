package robots.controller

import robots.model.{BaseGrid, GridPosition, WorldMap}
import robots.model.enumeration.Square.{Empty, Wall}
import utopia.flow.util.LinesFrom

import java.nio.file.Path
import scala.collection.immutable.VectorBuilder

/**
 * Used for reading maps (grids) from text files
 * @author Mikko Hilpinen
 * @since 22.1.2021, v1
 */
object MapReader
{
	/**
	 * @param path Path from which map data is read from
	 * @return A grid read from that file, read treasure locations and read bot starting locations. May contain a failure
	 */
	def apply(path: Path) = LinesFrom.path(path).map { lines =>
		val baseGrid = BaseGrid.fromRows(lines.map { _.map(charToSquare).toVector })
		val treasureLocationsBuilder = new VectorBuilder[GridPosition]()
		val botLocationsBuilder = new VectorBuilder[GridPosition]()
		lines.indices.foreach { y =>
			val line = lines(y)
			line.indices.foreach { x =>
				line(x) match
				{
					case 'O' => treasureLocationsBuilder += GridPosition(x, y)
					case 'S' => botLocationsBuilder += GridPosition(x, y)
					case _ => ()
				}
			}
		}
		WorldMap(baseGrid, treasureLocationsBuilder.result().toSet, botLocationsBuilder.result())
	}
	
	private def charToSquare(char: Char) = char match
	{
		case 'X' => Wall
		case _ => Empty
	}
}
