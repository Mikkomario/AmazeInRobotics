package controller

import controller.GlobalBotSettings._
import robots.model.GridPosition
import robots.model.enumeration.RobotCommand
import robots.model.enumeration.RobotCommand.{Move, MoveTowards, RotateHead}
import robots.model.enumeration.RobotCommandType.{HeadRotation, Movement}
import utopia.flow.async.VolatileOption
import utopia.flow.collection.VolatileList
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.genesis.color.Color
import utopia.genesis.handling.{Actor, Drawable}
import utopia.genesis.shape.shape1D.{Angle, Rotation, RotationDirection}
import utopia.genesis.shape.shape2D._
import utopia.genesis.util.Drawer
import utopia.inception.handling.immutable.Handleable

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future, Promise}

/**
 * Bots are the controlled units in this game
 * @author Mikko Hilpinen
 * @since 20.1.2021, v1
 */
class Bot(initialPosition: GridPosition, initialHeading: Direction2D, bodyColor: Color, headColor: Color)
	extends Handleable with Actor with Drawable
{
	// ATTRIBUTES   ------------------------
	
	private val _currentCommandPointer = VolatileOption[(RobotCommand, Option[Promise[Unit]])]()
	// Queued command -> promise that will be fulfilled when the command is finished (optional)
	private val commandQueue = VolatileList[(RobotCommand, Option[Promise[Unit]])]()
	// 0 initially, 1 when command is completed
	private var currentCommandProgress = 0.0
	
	private val _gridPositionPointer = new PointerWithEvents(initialPosition)
	private var currentMovementDirection: Option[Direction2D] = None
	
	private var heading = initialHeading
	private var currentRotationDirection: Option[RotationDirection] = None
	
	private val baseHeadTriangle = Triangle(Point.origin, Vector2D(-pixelsPerGridUnit / 2.0, -pixelsPerGridUnit / 2.0),
		Vector2D(pixelsPerGridUnit / 2.0, -pixelsPerGridUnit / 2.0))
	
	/**
	 * A pointer that contains whether this bot is currently idle (without anything to do)
	 */
	val isIdlePointer = _currentCommandPointer.mergeWith(commandQueue) { (current, queue) =>
		current.isEmpty && queue.isEmpty
	}
	
	
	// COMPUTED ----------------------------
	
	/**
	 * @return A pointer to this bot's position on the grid
	 */
	def gridPositionPointer = _gridPositionPointer.view
	
	def gridPosition = _gridPositionPointer.value
	private def gridPosition_=(newPosition: GridPosition) = _gridPositionPointer.value = newPosition
	
	/**
	 * @return Whether this robot is currently fulfilling a command
	 */
	def isBusy = !isIdle
	
	/**
	 * @return Whether this bot is currently idle (not fulfilling a command)
	 */
	def isIdle = isIdlePointer.value
	
	/**
	 * @return A future when this bot is idle next time. Completed future if this bot is already idle.
	 */
	def nextIdleFuture(implicit exc: ExecutionContext) = isIdlePointer.futureWhere { !_ }
	
	/**
	 * @return Current position of this bot (in its own coordinate system)
	 */
	def position = currentMovementDirection match
	{
		case Some(direction) => gridPosition + direction * currentCommandProgress
		case None => gridPosition.toVector
	}
	
	/**
	 * @return The direction towards which the head of this robot points at this time
	 */
	def headAngle = currentRotationDirection match
	{
		case Some(direction) => heading.toAngle + Rotation.ofDegrees(currentCommandProgress * 90, direction)
		case None => heading.toAngle
	}
	
	private def currentCommand = _currentCommandPointer.value.orElse {
		val next = commandQueue.pop()
		// Updates and starts the current command as well
		next.foreach { c =>
			// Expects current command progress to be reset at this point
			_currentCommandPointer.setOne(c)
			start(c._1)
		}
		next
	}
	
	private def drawPosition = position * pixelsPerGridUnit
	
	private def headTriangle = baseHeadTriangle.rotated(headAngle - Angle.up).translated(drawPosition)
	
	
	// IMPLEMENTED  ------------------------
	
	override def act(duration: FiniteDuration) =
	{
		// Chooses the command to advance
		currentCommand.foreach { case (command, completionPromise) =>
			// Advances the command completion
			currentCommandProgress += duration / command.duration
			// Checks whether command should be completed
			if (currentCommandProgress >= 1)
			{
				finish(command)
				_currentCommandPointer.clear()
				currentCommandProgress = (currentCommandProgress - 1) min 1.0
				// Completes the promise once the command has finished
				completionPromise.foreach { _.success(()) }
			}
		}
	}
	
	override def draw(drawer: Drawer) =
	{
		// Draws the hull as a rounded rectangle
		val origin = drawPosition
		drawer.onlyFill(bodyColor).draw(Bounds(origin.toPoint, gridSquarePixelSize).toRoundedRectangle())
		// Then draws the robot head as a triangle
		drawer.onlyFill(headColor).translated(gridSquarePixelSize / 2).draw(headTriangle)
	}
	
	
	// OTHER    ----------------------------
	
	/**
	 * Adds a new command for this bot, tracks the completion of that command
	 * @param command Command to run
	 * @return Future of the completion of this command
	 */
	def accept(command: RobotCommand) =
	{
		val completionPromise = Promise[Unit]()
		commandQueue :+= (command, Some(completionPromise))
		completionPromise.future
	}
	
	/**
	 * Adds multiple new commands for this bot. Tracks the completion of those commands
	 * @param commands Commands to issue for this bot
	 * @return Future of the completion of the last of these commands
	 */
	def accept(commands: Seq[RobotCommand]) =
	{
		if (commands.isEmpty)
			Future.successful(())
		else
		{
			val completionPromise = Promise[Unit]()
			commandQueue ++= (commands.dropRight(1).map { _ -> None } :+ commands.last -> Some(completionPromise))
			completionPromise.future
		}
	}
	
	/**
	 * Adds multiple new commands for this bot. Tracks the completion of those commands
	 * @param firstCommand The first command to issue
	 * @param secondCommand The second command to issue
	 * @param moreCommands More commands to issue
	 * @return Future of the completion of the last of these commands
	 */
	def accept(firstCommand: RobotCommand, secondCommand: RobotCommand, moreCommands: RobotCommand*): Future[Unit] =
		accept(Vector(firstCommand, secondCommand) ++ moreCommands)
	
	/**
	 * Adds a new command for this bot. Doesn't track the completion of that command.
	 * @param command New command for this bot
	 */
	def push(command: RobotCommand) = commandQueue :+= (command, None)
	
	/**
	 * Pushes multiple commands for this bot
	 * @param commands Commands to issue in sequence
	 */
	def push(commands: IterableOnce[RobotCommand]) = commandQueue ++= commands.iterator.map { _ -> None }
	
	/**
	 * @param firstCommand First command to issue
	 * @param secondCommand Second command to issue
	 * @param moreCommands More commands to issue
	 */
	def push(firstCommand: RobotCommand, secondCommand: RobotCommand, moreCommands: RobotCommand*): Unit =
		push(Vector(firstCommand, secondCommand) ++ moreCommands)
	
	private def start(command: RobotCommand) = command match
	{
		case Move(direction) => startMovingTowards(direction.toDirection(heading))
		case MoveTowards(direction) => startMovingTowards(direction)
		case RotateHead(direction) => currentRotationDirection = Some(direction)
	}
	
	private def finish(command: RobotCommand) = command.commandType match
	{
		case Movement =>
			currentMovementDirection.foreach { gridPosition += _ }
			currentMovementDirection = None
		case HeadRotation =>
			currentRotationDirection.foreach { direction => heading = heading.rotatedQuarterTowards(direction) }
			currentRotationDirection = None
	}
	
	private def startMovingTowards(direction: Direction2D) = currentMovementDirection = Some(direction)
}
