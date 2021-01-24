package robots.model

import robots.model.enumeration.Square.TreasureLocation
import robots.model.enumeration.{PermanentSquare, Square, TemporarySquare}
import controller.GlobalBotSettings._
import utopia.flow.caching.multi.Cache
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.TimeExtensions._
import utopia.genesis.handling.Drawable
import utopia.genesis.shape.shape2D.{Bounds, Direction2D}
import utopia.genesis.util.Drawer
import utopia.inception.handling.immutable.Handleable

import java.time.Instant

object MapMemory
{
	/**
	 * Creates a new initial map memory
	 * @param botLocation Initial bot location
	 * @param landingSquareType Current location square type
	 * @return A new map memory instance
	 */
	def apply(botLocation: GridPosition, landingSquareType: PermanentSquare): MapMemory =
		new MapMemory(BaseMapMemory(botLocation, Map(botLocation -> landingSquareType)), Map())
}

/**
 * Represents a bot's view of the world
 * @author Mikko Hilpinen
 * @since 24.1.2021, v1
 */
case class MapMemory(base: BaseMapMemory, temporariesData: Map[GridPosition, (TemporarySquare, Instant)])
	extends Drawable with Handleable
{
	// ATTRIBUTES   ------------------------------
	
	/**
	 * All possible routes to grab a treasure (1), also contains grab direction (2)
	 * and last treasure registration time (3)
	 */
	// Finds all known treasure locations
	lazy val treasureRoutes = temporariesData.flatMap { case (position, (square, time)) =>
		if (square == TreasureLocation)
		{
			// Calculates all routes to approach and grab that treasure
			Direction2D.values.flatMap { direction =>
				base.routesTo(position + direction).map { route => (route, direction.opposite, time) }
			}
		}
		else
			None
	}.toSet
	
	
	// COMPUTED -----------------------------------
	
	/**
	 * @return Current bot location
	 */
	def botLocation = base.botLocation
	
	
	// IMPLEMENTED  -------------------------------
	
	override def draw(drawer: Drawer) =
	{
		// Draws all memorized locations
		base.draw(drawer)
		// And all temporary memory locations
		val now = Instant.now()
		val drawers = Cache[TemporarySquare, Drawer] { s => drawer.onlyFill(s.color) }
		temporariesData.foreach { case (position, (square, time)) =>
			val passedDuration = now - time
			val visibility = 1.0 - passedDuration / square.visibilityDuration
			if (visibility > 0)
				drawers(square).withAlpha(visibility).draw(Bounds(position * pixelsPerGridUnit, gridSquarePixelSize))
		}
	}
	
	// OTHER    -----------------------------------
	
	/**
	 * @param newLocation New bot location
	 * @param newSquareType New known square type, if known (default = None)
	 * @return A copy of this data with updated bot location
	 */
	def withBotLocation(newLocation: GridPosition, newSquareType: Option[PermanentSquare] = None) =
	{
		withBase(base.withBotLocation(newLocation, newSquareType))
	}
	
	/**
	 * @param data New square data to insert
	 * @param dataTime Time of data acquisition (default = now)
	 * @return An updated copy of this data
	 */
	def withSquareData(data: Iterable[(GridPosition, Square)], dataTime: Instant = Instant.now()) =
	{
		// Splits the data into temporary and permanent updates
		val (temporaryUpdates, permanentUpdates) = data.dividedWith { case (position, square) =>
			square match
			{
				case p: PermanentSquare => Right(position -> p)
				case t: TemporarySquare => Left(position -> (t -> dataTime))
			}
		}
		if (temporaryUpdates.nonEmpty)
			copy(base = base.withData(permanentUpdates), temporariesData = temporariesData ++ temporaryUpdates)
		else
			withBase(base.withData(permanentUpdates))
	}
	
	/**
	 * @param squareLocation Position of the described square
	 * @param squareType Type of the described square
	 * @param dataTime Information capture time (default = now)
	 * @return An updated copy of this data
	 */
	def withUpdatedSquare(squareLocation: GridPosition, squareType: Square, dataTime: => Instant = Instant.now()) =
	{
		squareType match
		{
			case p: PermanentSquare => withBase(base.withSquare(squareLocation, p))
			case t: TemporarySquare => copy(temporariesData = temporariesData + (squareLocation -> (t -> dataTime)))
		}
	}
	
	/**
	 * @param location Targeted location
	 * @return A copy of this data with no treasure registered at specified location
	 */
	def withoutTreasureAt(location: GridPosition) =
	{
		if (temporariesData.get(location).exists { _._1 == TreasureLocation })
			copy(temporariesData = temporariesData - location)
		else
			this
	}
	
	/**
	 * Calculates the best available treasure route
	 * @param costFunction A function for calculating option cost based on route to travel and treasure grab direction
	 * @param rewardFunction A function for calculating the grab reward based on last known treasure time
	 * @return The best route to grab a treasure, if one is available. Contains route (1), grab direction (2)
	 *         and last known treasure time (3)
	 */
	def calculateBestTreasureRoute(costFunction: (Vector[Direction2D], Direction2D) => Double)
	                              (rewardFunction: Instant => Double) =
		treasureRoutes.maxByOption { case (route, direction, time) =>
			rewardFunction(time) - costFunction(route, direction)
		}
	
	private def withBase(newBase: BaseMapMemory) = if (newBase != base) copy(base = newBase) else this
}