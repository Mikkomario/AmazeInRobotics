package controller

import utopia.flow.util.TimeExtensions._

/**
 * Global configurations used in bot functions
 * @author Mikko Hilpinen
 * @since 20.1.2021, v1
 */
object GlobalBotSettings
{
	/**
	 * Number of pixels a single grid space takes
	 */
	val pixelsPerGridUnit = 48
	
	/**
	 * How long it takes to move a single grid unit
	 */
	val singleMovementDuration = 0.5.seconds
}
