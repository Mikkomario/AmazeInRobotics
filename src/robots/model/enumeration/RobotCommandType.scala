package robots.model.enumeration

/**
 * An enumeration of different types of robot command categories. Each category contains one or more commands
 * @author Mikko Hilpinen
 * @since 20.1.2021, v1
 */
sealed trait RobotCommandType

object RobotCommandType
{
	/**
	 * Common type for (grid) movement commands
	 */
	case object Movement extends RobotCommandType
	
	/**
	 * Command type for robot head rotation commands
	 */
	case object HeadRotation extends RobotCommandType
}
