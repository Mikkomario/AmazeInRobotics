package robots.model

import robots.model.enumeration.{PermanentSquare, TemporarySquare}

import java.time.Instant

/**
 * Represents a bot's view of the world
 * @author Mikko Hilpinen
 * @since 24.1.2021, v1
 */
case class MapMemory(botLocation: GridPosition, baseData: Map[GridPosition, PermanentSquare],
                     temporariesData: Map[GridPosition, (TemporarySquare, Instant)])
{
	// ATTRIBUTES   ------------------------------
	
	
}