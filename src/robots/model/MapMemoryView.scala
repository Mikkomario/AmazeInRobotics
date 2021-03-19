package robots.model

import robots.model.enumeration.Square
import utopia.flow.time.TimeExtensions._

import java.time.Instant
import scala.concurrent.duration.Duration

/**
 * A robots.view point into a map memory which contains both permanent and temporary information
 * @author Mikko Hilpinen
 * @since 27.1.2021, v1
 */
case class MapMemoryView(map: MapMemory, deadlines: Map[Square, Duration]) extends MapMemoryLike[Square]
{
	// IMPLEMENTED  -----------------------------
	
	override def apply(position: GridPosition) =
		MapSquare(position, squareTypeAt(position), this)
	
	// Uses temporary data if it's available and not too old
	// Otherwise uses (permanent) base map data
	override def squareTypeAt(position: GridPosition) = map.temporariesData.get(position)
		.filter { case (squareType, captureTime) => checkDeadlineFor(squareType, captureTime) }
		.map { _._1 }.orElse { map.base.squareTypeAt(position) }
	
	override def isDefined(position: GridPosition) = map.base.isDefined(position) || isTemporaryDefinedFor(position)
	
	
	// OTHER    ---------------------------------
	
	private def isTemporaryDefinedFor(position: GridPosition) = map.temporariesData.get(position)
		.exists { case (squareType, captureTime) => checkDeadlineFor(squareType, captureTime) }
	
	private def checkDeadlineFor(squareType: Square, captureTime: Instant) =
		deadlines.get(squareType).forall { Instant.now() - captureTime < _ }
}
