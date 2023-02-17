package robots.controller

import robots.controller.GlobalBotSettings._
import robots.model.enumeration.RobotCommand._
import robots.model.enumeration.RobotCommandType.{HeadRotation, Interact, Movement, Scan}
import robots.model.enumeration.StunCause.MovementCollision
import robots.model.enumeration.{RobotCommand, StunCause}
import robots.model.{BotColors, GridPosition, MapMemory}
import utopia.flow.collection.mutable.VolatileList
import utopia.flow.view.mutable.async.VolatileOption
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.genesis.handling.{Actor, Drawable}
import utopia.genesis.util.Drawer
import utopia.inception.handling.immutable.Handleable
import utopia.paradigm.angular.{Angle, Rotation}
import utopia.paradigm.animation.Animation
import utopia.paradigm.enumeration.Direction2D.{Down, Up}
import utopia.paradigm.enumeration.{Direction2D, RotationDirection}
import utopia.paradigm.shape.shape2d._

import java.awt.Shape
import java.time.Instant
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future, Promise}

object Bot
{
	private val stunRotationAnimation = Animation { p => Rotation.ofCircles(p * 2) }.projectileCurved.reversed
}

/**
 * Bots are the controlled units in this game
 * @author Mikko Hilpinen
 * @since 20.1.2021, v1
 */
class Bot(world: World, initialPosition: GridPosition, initialHeading: Direction2D, colors: BotColors)
	extends Handleable with Actor with Drawable
{
	// ATTRIBUTES   ------------------------
	
	private var stunListeners = Vector[BotStunListener]()
	
	private val _currentCommandPointer = VolatileOption[(RobotCommand, Option[Promise[Boolean]])]()
	// Queued command -> promise that will be fulfilled when the command is finished (optional)
	private val commandQueue = VolatileList[(RobotCommand, Option[Promise[Boolean]])]()
	// 0 initially, 1 when command is completed
	private var currentCommandProgress = 0.0
	private var remainingStun = 0.0
	
	private val _memoryPointer = new PointerWithEvents(MapMemory(GridPosition.origin, world.base(initialPosition)))
	
	private val _currentMovementDirectionPointer = new PointerWithEvents[Option[Direction2D]](None)
	
	private val _headingPointer = new PointerWithEvents(initialHeading)
	private var currentRotationDirection: Option[RotationDirection] = None
	
	private val baseHeadTriangle = Triangle(Point.origin, Vector2D(-pixelsPerGridUnit / 2.0, -pixelsPerGridUnit / 2.0),
		Vector2D(pixelsPerGridUnit / 2.0, -pixelsPerGridUnit / 2.0))
	
	private var currentScanShape: Option[Shape] = None
	
	// How many treasures this bot has collected
	private var collectedTreasureCount = 0
	
	/**
	 * A pointer that contains whether this bot is currently idle (without anything to do)
	 */
	val isIdlePointer = _currentCommandPointer.mergeWith(commandQueue) { (current, queue) =>
		current.isEmpty && queue.isEmpty
	}
	
	/**
	 * A pointer to this bot's current position (in it's own relative coordinate system)
	 */
	val relativeGridPositionPointer = _memoryPointer.map { _.botLocation }
	
	/**
	 * A pointer to this bot's position on the grid
	 */
	val worldGridPositionPointer = _memoryPointer
		.mergeWith(_currentMovementDirectionPointer) { (memory, dir) =>
			dir match
			{
				case Some(dir) => memory.botLocation + initialPosition + dir
				case None => memory.botLocation + initialPosition
			}
		}
	
	
	// COMPUTED ----------------------------
	
	/**
	 * @return The current memory state of this bot
	 */
	def memory = _memoryPointer.value
	/**
	 * @return A pointer to this bot's memory / data state
	 */
	def memoryPointer = _memoryPointer.view
	
	/**
	 * @return This bot's current heading (head direction)
	 */
	def heading = _headingPointer.value
	/**
	 * @return A pointer to this bot's current heading (head direction)
	 */
	def headingPointer = _headingPointer.view
	
	/**
	 * @return This bot's approximate coordinate in the world. Contains the full current movement vector.
	 */
	def worldGridPosition = worldGridPositionPointer.value
	
	private def gridPosition = _memoryPointer.value.botLocation + initialPosition
	
	/**
	 * @return A pointer to this bot's current movement direction. Contains None while this bot is not moving.
	 */
	def currentMovementDirectionPointer = _currentMovementDirectionPointer.view
	/**
	 * @return The direction towards which this bot is currently moving. None if not currently moving.
	 */
	def currentMovementDirection = _currentMovementDirectionPointer.value
	private def currentMovementDirection_=(newDirection: Option[Direction2D]) =
		_currentMovementDirectionPointer.value = newDirection
	
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
	def headAngle =
	{
		val standard = currentRotationDirection match
		{
			case Some(direction) => heading.toAngle + Rotation.ofDegrees(currentCommandProgress * 90, direction)
			case None => heading.toAngle
		}
		standard + Bot.stunRotationAnimation(remainingStun)
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
	
	private def centerDrawPosition = drawPosition + gridSquarePixelSize / 2
	
	private def headTriangle = baseHeadTriangle.rotated(headAngle - Angle.up).translated(centerDrawPosition)
	
	private def topLeftDrawPosition = drawPosition
	
	private def topRightDrawPosition = drawPosition + Vector2D(pixelsPerGridUnit)
	
	private def bottomLeftDrawPosition = drawPosition + Vector2D(0, pixelsPerGridUnit)
	
	private def bottomRightDrawPosition = drawPosition + gridSquarePixelSize
	
	// First left side corner, then right side corner
	private def headCorners = heading match
	{
		case Up => topLeftDrawPosition -> topRightDrawPosition
		case Direction2D.Right => topRightDrawPosition -> bottomRightDrawPosition
		case Down => bottomRightDrawPosition -> bottomLeftDrawPosition
		case Direction2D.Left => bottomLeftDrawPosition -> topLeftDrawPosition
	}
	
	
	// IMPLEMENTED  ------------------------
	
	override def act(duration: FiniteDuration) =
	{
		val scaledDuration = duration * world.speedModifier
		
		// If currently stunned, decreases the stun before continuing next action
		if (remainingStun > 0)
			remainingStun = (remainingStun - scaledDuration / defaultStunDuration) max 0.0
		else
			// Chooses the command to advance
			currentCommand.foreach { case (command, completionPromise) =>
				// Advances the command completion
				currentCommandProgress += scaledDuration / command.duration
				// Checks whether command should be completed
				if (currentCommandProgress >= 1)
				{
					finish(command)
					_currentCommandPointer.clear()
					currentCommandProgress = 0.0
					// Completes the promise once the command has finished
					completionPromise.foreach { _.success(true) }
				}
			}
	}
	
	override def draw(drawer: Drawer) =
	{
		// Draws the hull as a rounded rectangle
		val origin = drawPosition
		drawer.onlyFill(colors.body).draw(Bounds(origin.toPoint, gridSquarePixelSize).toRoundedRectangle())
		// Then draws the robot head as a triangle
		drawer.onlyFill(colors.head).draw(headTriangle)
		// May also draw the scan shape
		currentScanShape.foreach { scanShape => drawer.onlyFill(colors.scanBeam).draw(scanShape) }
	}
	
	
	// OTHER    ----------------------------
	
	/**
	 * Registers a new listener to be informed whenever this bot gets stunned
	 * @param listener A bot stun listener
	 */
	def registerStunListener(listener: BotStunListener) = stunListeners :+= listener
	
	/**
	 * Adds a new command for this bot, tracks the completion of that command
	 * @param command Command to run
	 * @return Future of the completion of this command
	 */
	def accept(command: RobotCommand) =
	{
		val completionPromise = Promise[Boolean]()
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
			Future.successful(true)
		else
		{
			val completionPromise = Promise[Boolean]()
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
	def accept(firstCommand: RobotCommand, secondCommand: RobotCommand, moreCommands: RobotCommand*): Future[Boolean] =
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
	
	/**
	 * Cancels all queued commands
	 * @return The number of commands that were cancelled
	 */
	def abortAllQueuedCommands() =
	{
		val aborted = commandQueue.popAll()
		aborted.foreach { _._2.foreach { _.success(false) } }
		aborted.size
	}
	
	private def stun(cause: StunCause) =
	{
		remainingStun = 1.0
		stunListeners.foreach { _.onBotStunned(cause) }
	}
	
	private def start(command: RobotCommand) = command match
	{
		case Move(direction) => startMovingTowards(direction.toDirection(heading))
		case MoveTowards(direction) => startMovingTowards(direction)
		case RotateHead(direction) => currentRotationDirection = Some(direction)
		case LinearScan => startLinearScan()
		case MiniScan => startMiniScan()
		case WideScan => startWideScan()
		case _ => ()
	}
	
	private def finish(command: RobotCommand) = command.commandType match
	{
		case Movement =>
			// May update memorized map data also
			currentMovementDirection.foreach { direction =>
				val newPosition = gridPosition + direction
				_memoryPointer.update { _.withBotLocation(newPosition - initialPosition, world.base.get(newPosition)) }
			}
			currentMovementDirection = None
		case HeadRotation =>
			currentRotationDirection.foreach { direction => _headingPointer.update { _.rotatedQuarterTowards(direction) } }
			currentRotationDirection = None
		case Scan =>
			currentScanShape = None
			val dataTime = Instant.now()
			command match
			{
				case LinearScan =>
					// Updates memory data
					_memoryPointer.update { _.withSquareData(world.state.scan(gridPosition, heading)
						.map { case (pos, square) => (pos - initialPosition) -> square }) }
				case MiniScan =>
					// Updates memory data
					val targetPosition = gridPosition + heading
					world.state.get(targetPosition).foreach { squareType =>
						_memoryPointer.update { _.withUpdatedSquare(targetPosition - initialPosition, squareType, dataTime) }
					}
				case WideScan =>
					// Updates memory data
					_memoryPointer.update { _.withSquareData(world.state.wideScan(gridPosition, heading)
						.values.flatten.map { case (position, square) => (position - initialPosition) -> square }) }
				case _ => ()
			}
		case Interact =>
			// Checks whether there is treasure directly ahead of this bot, if so, collects it
			val targetPosition = gridPosition + heading
			if (world.tryCollectTreasureFrom(targetPosition))
			{
				collectedTreasureCount += 1
				println(s"Treasure collected, now $collectedTreasureCount")
			}
			// Updates memory afterwards
			_memoryPointer.update { _.withoutTreasureAt(targetPosition - initialPosition) }
	}
	
	private def startMovingTowards(direction: Direction2D) =
	{
		// If the specified direction is free, starts moving
		if (world.state(gridPosition + direction).isPassable)
			currentMovementDirection = Some(direction)
		// Otherwise aborts the action and stuns
		else
		{
			_currentCommandPointer.pop().foreach { _._2.foreach { _.success(false) } }
			stun(MovementCollision(direction))
		}
	}
	
	// Activates the linear scanner feature
	private def startLinearScan() =
	{
		val scanBeamLength = world.state.scan(gridPosition, heading).size
		val beamTip = drawPosition + gridSquarePixelSize / 2 + heading((scanBeamLength + 0.5) * pixelsPerGridUnit)
		val (leftOrigin, rightOrigin) = headCorners
		val scanShape =
		{
			// 1 / 0 square scan creates a triangle
			if (scanBeamLength <= 1)
				Triangle.withCorners(leftOrigin.toPoint, beamTip.toPoint, rightOrigin.toPoint)
			// 2+ square scan creates a 5 corner shape (where the tip is a triangle and the rest is a rectangle)
			else
			{
				val widePartVector = heading((scanBeamLength - 1) * pixelsPerGridUnit)
				Polygon(Vector(leftOrigin, leftOrigin + widePartVector, beamTip,
					rightOrigin + widePartVector, rightOrigin).map { _.toPoint })
			}
		}
		currentScanShape = Some(scanShape.toShape)
	}
	
	private def startMiniScan() = currentScanShape = Some(
		Bounds((gridPosition + heading) * pixelsPerGridUnit, gridSquarePixelSize).toShape)
	
	private def startWideScan() =
	{
		val scanBeamLength = world.state.scan(gridPosition, heading).size
		val beamTip = drawPosition + gridSquarePixelSize / 2 + heading((scanBeamLength + 0.5) * pixelsPerGridUnit)
		val (leftOrigin, rightOrigin) = headCorners
		val scanShape =
		{
			if (scanBeamLength > 1)
			{
				val wideEndVector = heading((scanBeamLength - 1.5) * pixelsPerGridUnit)
				val leftShiftVector = heading.rotatedQuarterCounterClockwise(pixelsPerGridUnit / 2.0)
				val rightShiftVector = heading.rotatedQuarterClockwise(pixelsPerGridUnit / 2.0)
				val leftSideEnd = leftOrigin + wideEndVector + leftShiftVector
				val rightSideEnd = rightOrigin + wideEndVector + rightShiftVector
				// Longer than 2 square scan => creates a 7 corner shape
				if (scanBeamLength > 2)
				{
					val wideStartVector = heading(pixelsPerGridUnit / 2.0)
					val leftSideStart = leftOrigin + wideStartVector + leftShiftVector
					val rightSideStart = rightOrigin + wideStartVector + rightShiftVector
					Polygon(Vector(leftOrigin, leftSideStart, leftSideEnd, beamTip,
						rightSideEnd, rightSideStart, rightOrigin).map { _.toPoint })
				}
				// 2 square scan creates a 5 corner shape
				else
					Polygon(Vector(leftOrigin, leftSideEnd, beamTip, rightSideEnd, rightOrigin).map { _.toPoint })
			}
			// 1 / 0 square scan creates a triangle
			else
				Triangle.withCorners(leftOrigin.toPoint, beamTip.toPoint, rightOrigin.toPoint)
		}
		currentScanShape = Some(scanShape.toShape)
	}
	
	
	// NESTED   ------------------------------
	
	object BotWorldDrawer extends Drawable with Handleable
	{
		// Delegates drawing
		override def draw(drawer: Drawer) = _memoryPointer.value.draw(drawer, initialPosition, world.speedModifier)
	}
}
