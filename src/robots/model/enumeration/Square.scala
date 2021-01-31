package robots.model.enumeration

import robots.model.enumeration.Square.{BotLocation, Empty, TreasureLocation, Wall}
import utopia.flow.util.TimeExtensions._
import utopia.genesis.color.Color

import scala.concurrent.duration.Duration

/**
 * A common trait for different types of grid squares
 * @author Mikko Hilpinen
 * @since 22.1.2021, v1
 */
sealed trait Square
{
	// ABSTRACT ----------------------------
	
	/**
	 * @return Whether this square can be moved through
	 */
	def isPassable: Boolean
	
	/**
	 * @return Whether this square blocks the line of sight
	 */
	def blocksSight: Boolean
	
	/**
	 * @return Color that represents this type of square
	 */
	def color: Color
	
	/**
	 * @return Name of this square type
	 */
	def name: String
	
	/**
	 * @return A character that represents this square type
	 */
	def characterCode: Char
	
	
	// COMPUTED ----------------------------
	
	/**
	 * @return Whether this square can be seen through
	 */
	def canBeSeenTrough = !blocksSight
}

/**
 * These squares don't appear or disappear
 */
sealed trait PermanentSquare extends Square

/**
 * These squares may appear, disappear or move
 */
sealed trait TemporarySquare extends Square
{
	/**
	 * @return How long this temporary square should remain visible
	 */
	def visibilityDuration: Duration
}

object Square
{
	// ATTRIBUTES   -------------------------
	
	/**
	 * All known square types
	 */
	lazy val values = PermanentSquare.values ++ TemporarySquare.values
	
	
	// NESTED   -----------------------------
	
	/**
	 * An empty square that can be freely be moved through
	 */
	object Empty extends PermanentSquare
	{
		override val color = Color.gray(0.9)
		
		override def isPassable = true
		
		override def blocksSight = false
		
		override def name = "Empty"
		
		override def characterCode = ' '
	}
	
	/**
	 * A wall square that blocks movement and sight
	 */
	object Wall extends PermanentSquare
	{
		override val color = Color.black
		
		override def isPassable = false
		
		override def blocksSight = true
		
		override def name = "Wall"
		
		override def characterCode = 'X'
	}
	
	/**
	 * A square that represents a robot's temporary location
	 */
	object BotLocation extends TemporarySquare
	{
		override val color = Color.red
		
		override val visibilityDuration = 4.seconds
		
		override def isPassable = false
		
		override def blocksSight = false
		
		override def name = "Bot Location"
		
		override def characterCode = 'B'
	}
	
	/**
	 * A square that represents last known location of a treasure
	 */
	object TreasureLocation extends TemporarySquare
	{
		override val visibilityDuration = 35.seconds
		
		override def color = Color.red.average(Color.yellow).withLuminosity(0.7)
		
		override def isPassable = true
		
		override def blocksSight = false
		
		override def name = "Treasure"
		
		override def characterCode = 'T'
	}
}

object PermanentSquare
{
	/**
	 * All permanent square options
	 */
	lazy val values = Vector(Empty, Wall)
}

object TemporarySquare
{
	/**
	 * All temporary square options
	 */
	lazy val values = Vector(TreasureLocation, BotLocation)
}