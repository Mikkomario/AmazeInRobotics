package robots.model

import robots.model.enumeration.RobotCommand
import robots.model.enumeration.RobotCommand.{Move, MoveTowards}
import robots.model.enumeration.RobotCommandType.Movement
import utopia.flow.collection.VolatileList
import utopia.genesis.handling.Actor
import utopia.genesis.shape.shape2D.Direction2D
import utopia.inception.handling.immutable.Handleable

import scala.concurrent.duration.FiniteDuration

/**
 * Bots are the controlled units in this game
 * @author Mikko Hilpinen
 * @since 20.1.2021, v1
 */
class Bot(initialPosition: GridPosition, initialHeading: Direction2D) extends Handleable with Actor
{
	// ATTRIBUTES   ------------------------
	
	private var _currentCommand: Option[RobotCommand] = None
	private val commandQueue = VolatileList[RobotCommand]()
	// 0 initially, 1 when command is completed
	private var currentCommandProgress = 0.0
	
	private var gridPosition = initialPosition
	private var currentMovementDirection: Option[Direction2D] = None
	
	// TODO: Add rotation command support
	private val heading = initialHeading
	// private var currentRotationDirection: Option[RotationDirection] = None
	
	
	// COMPUTED ----------------------------
	
	/**
	 * @return Current position of this bot (in its own coordinate system)
	 */
	def position = currentMovementDirection match
	{
		case Some(direction) => gridPosition + direction * currentCommandProgress
		case None => gridPosition.toVector
	}
	
	private def currentCommand = _currentCommand.orElse {
		val next = commandQueue.pop()
		// Updates and starts the current command as well
		next.foreach { c =>
			// Expects current command progress to be reset at this point
			_currentCommand = Some(c)
			start(c)
		}
		next
	}
	
	
	// IMPLEMENTED  ------------------------
	
	override def act(duration: FiniteDuration) =
	{
		// Chooses the command to advance
		currentCommand.foreach { command =>
			// Advances the command completion
			currentCommandProgress += duration / command.duration
			// Checks whether command should be completed
			if (currentCommandProgress >= 1)
			{
				finish(command)
				currentCommandProgress = (currentCommandProgress - 1) min 1.0
			}
		}
	}
	
	
	// OTHER    ----------------------------
	
	private def start(command: RobotCommand) = command match
	{
		case Move(direction) => startMovingTowards(direction.toDirection(heading))
		case MoveTowards(direction) => startMovingTowards(direction)
	}
	
	private def finish(command: RobotCommand) = command.commandType match
	{
		case Movement =>
			currentMovementDirection.foreach { gridPosition += _ }
			currentMovementDirection = None
	}
	
	private def startMovingTowards(direction: Direction2D) = currentMovementDirection = Some(direction)
}
