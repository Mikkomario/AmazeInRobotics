package robots.model.enumeration

import utopia.genesis.shape.shape2D.Direction2D
import Direction2D._

/**
 * An enumeration for main directions that are relative to the current heading
 * @author Mikko Hilpinen
 * @since 20.1.2021, v1
 */
sealed trait RelativeDirection
{
	/**
	 * Converts this relative direction to an absolute direction when the object's heading is known
	 * @param heading The direction the moving object is facing
	 * @return An absolute direction based on this direction an specified heading
	 */
	def toDirection(heading: Direction2D): Direction2D
}

object RelativeDirection
{
	/**
	 * Direction that moves objects straight where they're facing
	 */
	case object Forward extends RelativeDirection
	{
		override def toDirection(heading: Direction2D) = heading
	}
	
	/**
	 * Direction that moves objects to opposite direction to what they're facing
	 */
	case object Backward extends RelativeDirection
	{
		override def toDirection(heading: Direction2D) = heading.opposite
	}
	
	/**
	 * Direction that moves objects to the left (relative)
	 */
	case object Left extends RelativeDirection
	{
		override def toDirection(heading: Direction2D) = heading match
		{
			case Up => Direction2D.Left
			case Direction2D.Left => Down
			case Down => Direction2D.Right
			case Direction2D.Right => Up
		}
	}
	
	/**
	 * Direction that moves objects to the right (positive)
	 */
	case object Right extends RelativeDirection
	{
		override def toDirection(heading: Direction2D) = heading match
		{
			case Up => Direction2D.Right
			case Direction2D.Right => Direction2D.Down
			case Down => Direction2D.Left
			case Direction2D.Left => Up
		}
	}
}
