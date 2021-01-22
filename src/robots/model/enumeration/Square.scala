package robots.model.enumeration

import robots.model.enumeration.Square.{Empty, Wall}
import utopia.genesis.color.Color

/**
 * A common trait for different types of grid squares
 * @author Mikko Hilpinen
 * @since 22.1.2021, v1
 */
sealed trait Square
{
	/**
	 * @return Whether this square can be moved through
	 */
	def isPassable: Boolean
	
	/**
	 * @return Whether this square blocks the line of sight
	 */
	def blocksSight: Boolean
}

/**
 * These squares don't appear or disappear
 */
sealed trait PermanentSquare extends Square
{
	/**
	 * @return Color that represents this type of square
	 */
	def color: Color
}

/**
 * These squares may appear, disappear or move
 */
sealed trait TemporarySquare extends Square

object Square
{
	/**
	 * An empty square that can be freely be moved through
	 */
	object Empty extends PermanentSquare
	{
		override val color = Color.gray(0.9)
		
		override def isPassable = true
		
		override def blocksSight = false
	}
	
	/**
	 * A wall square that blocks movement and sight
	 */
	object Wall extends PermanentSquare
	{
		override val color = Color.black
		
		override def isPassable = false
		
		override def blocksSight = true
	}
	
	/**
	 * A square that represents a robot's temporary location
	 */
	object BotLocation extends TemporarySquare
	{
		override def isPassable = false
		
		override def blocksSight = false
	}
}

object PermanentSquare
{
	/**
	 * All permanent square options
	 */
	val values = Vector(Empty, Wall)
}