package controller

import controller.GlobalBotSettings._
import robots.model.GridPosition
import robots.model.enumeration.{PermanentSquare, RobotCommand, TemporarySquare}
import robots.model.enumeration.RobotCommand.{LinearScan, MiniScan, Move, MoveTowards, RotateHead, WideScan}
import robots.model.enumeration.RobotCommandType.{HeadRotation, Movement, Scan}
import robots.model.enumeration.Square.Empty
import utopia.flow.async.VolatileOption
import utopia.flow.collection.VolatileList
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.util.CollectionExtensions._
import utopia.genesis.animation.Animation
import utopia.genesis.color.Color
import utopia.genesis.handling.{Actor, Drawable}
import utopia.genesis.shape.shape1D.{Angle, Rotation, RotationDirection}
import utopia.genesis.shape.shape2D.Direction2D.{Down, Up}
import utopia.genesis.shape.shape2D._
import utopia.genesis.util.Drawer
import utopia.inception.handling.immutable.Handleable

import java.awt.Shape
import java.time.Instant
import scala.collection.immutable.HashMap
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
class Bot(world: World, initialPosition: GridPosition, initialHeading: Direction2D, bodyColor: Color, headColor: Color,
          scanColor: Color)
	extends Handleable with Actor with Drawable
{
	// ATTRIBUTES   ------------------------
	
	private val _currentCommandPointer = VolatileOption[(RobotCommand, Option[Promise[Unit]])]()
	// Queued command -> promise that will be fulfilled when the command is finished (optional)
	private val commandQueue = VolatileList[(RobotCommand, Option[Promise[Unit]])]()
	// 0 initially, 1 when command is completed
	private var currentCommandProgress = 0.0
	private var remainingStun = 0.0
	
	private val _gridPositionPointer = new PointerWithEvents(initialPosition)
	private val _currentMovementDirectionPointer = new PointerWithEvents[Option[Direction2D]](None)
	
	private var heading = initialHeading
	private var currentRotationDirection: Option[RotationDirection] = None
	
	private val baseHeadTriangle = Triangle(Point.origin, Vector2D(-pixelsPerGridUnit / 2.0, -pixelsPerGridUnit / 2.0),
		Vector2D(pixelsPerGridUnit / 2.0, -pixelsPerGridUnit / 2.0))
	
	private var knownMap: Map[GridPosition, PermanentSquare] = HashMap(initialPosition -> Empty)
	private var temporariesMemory: Map[GridPosition, (TemporarySquare, Instant)] = HashMap()
	
	private var currentScanShape: Option[Shape] = None
	
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
	def worldGridPositionPointer = _gridPositionPointer
		.mergeWith(_currentMovementDirectionPointer) { (p, dir) =>
			dir match
			{
				case Some(dir) => p + dir
				case None => p
			}
		}
	
	def gridPosition = _gridPositionPointer.value
	private def gridPosition_=(newPosition: GridPosition) = _gridPositionPointer.value = newPosition
	
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
		// If currently stunned, decreases the stun before continuing next action
		if (remainingStun > 0)
			remainingStun = (remainingStun - duration / defaultStunDuration) max 0.0
		else
			// Chooses the command to advance
			currentCommand.foreach { case (command, completionPromise) =>
				// Advances the command completion
				currentCommandProgress += duration / command.duration
				// Checks whether command should be completed
				if (currentCommandProgress >= 1)
				{
					finish(command)
					_currentCommandPointer.clear()
					currentCommandProgress = 0.0
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
		drawer.onlyFill(headColor).draw(headTriangle)
		// May also draw the scan shape
		currentScanShape.foreach { scanShape => drawer.onlyFill(scanColor).draw(scanShape) }
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
		case LinearScan => startLinearScan()
		case MiniScan => startMiniScan()
		case WideScan => startWideScan()
	}
	
	private def finish(command: RobotCommand) = command.commandType match
	{
		case Movement =>
			currentMovementDirection.foreach { gridPosition += _ }
			currentMovementDirection = None
		case HeadRotation =>
			currentRotationDirection.foreach { direction => heading = heading.rotatedQuarterTowards(direction) }
			currentRotationDirection = None
		case Scan =>
			currentScanShape = None
			val dataTime = Instant.now()
			command match
			{
				case LinearScan =>
					// Updates memory data
					val originPosition = gridPosition
					val direction = heading
					val squares = world.state.scan(originPosition, direction)
					val (temporaryUpdates, permanentUpdates) = squares.dividedWith { case (position, square) =>
						square match
						{
							case p: PermanentSquare => Right(position -> p)
							case t: TemporarySquare => Left(position -> (t -> dataTime))
						}
					}
					knownMap ++= permanentUpdates
					temporariesMemory ++= temporaryUpdates
				case MiniScan =>
					// Updates memory data
					val targetPosition = gridPosition + heading
					world.state.get(targetPosition).foreach
					{
						case p: PermanentSquare => knownMap += targetPosition -> p
						case t: TemporarySquare => temporariesMemory += targetPosition -> (t -> dataTime)
					}
				case WideScan =>
					// Updates memory data
					val (temporaryUpdates, permanentUpdates) = world.state.wideScan(gridPosition, heading).values.flatten
						.dividedWith { case (position, square) =>
							square match
							{
								case p: PermanentSquare => Right(position -> p)
								case t: TemporarySquare => Left(position -> (t -> dataTime))
							}
						}
					knownMap ++= permanentUpdates
					temporariesMemory ++= temporaryUpdates
				case _ => ()
			}
	}
	
	private def startMovingTowards(direction: Direction2D) =
	{
		// If the specified direction is free, starts moving
		if (world.state(gridPosition + direction).isPassable)
			currentMovementDirection = Some(direction)
		// Otherwise aborts the action and stuns
		else
		{
			_currentCommandPointer.clear()
			remainingStun = 1.0
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
		override def draw(drawer: Drawer) =
		{
			// Draws all memorized locations
			val drawers = PermanentSquare.values.map { s => s -> drawer.onlyFill(s.color) }.toMap
			knownMap.foreach { case (position, square) =>
				drawers(square).draw(Bounds(position * pixelsPerGridUnit, gridSquarePixelSize))
			}
		}
	}
}
