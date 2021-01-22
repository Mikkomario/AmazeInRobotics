package controller

import utopia.flow.util.TimeExtensions._
import utopia.genesis.shape.shape2D.Size

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
	
	/**
	 * How long rotation actions take by default
	 */
	val defaultRotationDuration = 0.2.seconds
	
	/**
	 * Pixel-wise size of a single grid square
	 */
	val gridSquarePixelSize = Size(pixelsPerGridUnit, pixelsPerGridUnit)
}
