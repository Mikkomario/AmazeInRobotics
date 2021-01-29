package robots.controller

import robots.model.{GridPosition, MapRoute}
import robots.model.enumeration.{PermanentSquare, RobotCommand, ScanType}
import robots.model.enumeration.RobotCommand.{Collect, MoveTowards, RotateHead}
import utopia.flow.async.AsyncExtensions._
import utopia.genesis.shape.shape1D.RotationDirection
import utopia.genesis.shape.shape1D.RotationDirection.Clockwise
import utopia.genesis.shape.shape2D.Direction2D

import scala.concurrent.{ExecutionContext, Future}

/**
 * An interface that is provided for use in bot controls
 * @author Mikko Hilpinen
 * @since 24.1.2021, v1
 */
class BotCommandInterface(bot: Bot)
{
	// COMPUTED ------------------------
	
	/**
	 * @return Whether this robot is currently fulfilling a command
	 */
	def isBusy = bot.isBusy
	/**
	 * @return Whether this bot is currently idle (not fulfilling a command)
	 */
	def isIdle = bot.isIdle
	/**
	 * A pointer that contains whether this bot is currently idle (without anything to do)
	 */
	def isIdlePointer = bot.isIdlePointer
	/**
	 * @return A future when this bot is idle next time. Completed future if this bot is already idle.
	 */
	def nextIdleFuture(implicit exc: ExecutionContext) = bot.nextIdleFuture
	
	/**
	 * @return This bot's position, relative to its starting location
	 */
	def position = bot.relativeGridPositionPointer.value
	/**
	 * A pointer to this bot's position, relative to its starting position
	 */
	def positionPointer = bot.relativeGridPositionPointer
	
	/**
	 * @return The direction towards which this bot is currently moving. None if not currently moving.
	 */
	def currentMovementDirection = bot.currentMovementDirection
	/**
	 * @return A pointer to this bot's current movement direction. Contains None while this bot is not moving.
	 */
	def currentMovementDirectionPointer = bot.currentMovementDirectionPointer
	
	/**
	 * @return This bot's current heading (head direction)
	 */
	def heading = bot.heading
	/**
	 * @return A pointer to this bot's current heading (head direction)
	 */
	def headingPointer = bot.headingPointer
	
	/**
	 * @return This bot's current memory / data
	 */
	def memory = bot.memory
	/**
	 * @return A pointer to this bot's memory / data
	 */
	def memoryPointer = bot.memoryPointer
	
	
	// OTHER    --------------------------------
	
	/**
	 * Adds a new listener to be called whenever this bot gets stunned
	 * @param listener A stun listener to register
	 */
	def registerStunListener(listener: BotStunListener) = bot.registerStunListener(listener)
	
	/**
	 * Adds a new command for this bot, tracks the completion of that command
	 * @param command Command to run
	 * @return Future of the completion of this command
	 */
	def accept(command: RobotCommand) = bot.accept(command)
	
	/**
	 * Adds multiple new commands for this bot. Tracks the completion of those commands
	 * @param commands Commands to issue for this bot
	 * @return Future of the completion of the last of these commands
	 */
	def accept(commands: Seq[RobotCommand]) = bot.accept(commands)
	
	/**
	 * Adds multiple new commands for this bot. Tracks the completion of those commands
	 * @param firstCommand The first command to issue
	 * @param secondCommand The second command to issue
	 * @param moreCommands More commands to issue
	 * @return Future of the completion of the last of these commands
	 */
	def accept(firstCommand: RobotCommand, secondCommand: RobotCommand, moreCommands: RobotCommand*) =
		bot.accept(firstCommand, secondCommand, moreCommands: _*)
	
	/**
	 * Adds a new command for this bot. Doesn't track the completion of that command.
	 * @param command New command for this bot
	 */
	def push(command: RobotCommand) = bot.push(command)
	
	/**
	 * Pushes multiple commands for this bot
	 * @param commands Commands to issue in sequence
	 */
	def push(commands: IterableOnce[RobotCommand]) = bot.push(commands)
	
	/**
	 * @param firstCommand First command to issue
	 * @param secondCommand Second command to issue
	 * @param moreCommands More commands to issue
	 */
	def push(firstCommand: RobotCommand, secondCommand: RobotCommand, moreCommands: RobotCommand*) =
		bot.push(firstCommand, secondCommand, moreCommands: _*)
	
	/**
	 * Executes the specified command. Blocks.
	 * @param command Command to execute
	 * @return Whether command succeeded.
	 */
	def executeBlocking(command: RobotCommand) = accept(command).waitFor().getOrElse(false)
	
	/**
	 * Executes the specified commands. Blocks.
	 * @param commands Commands to execute
	 * @return Whether all the commands succeeded.
	 */
	def executeBlocking(commands: Seq[RobotCommand]) = accept(commands).waitFor().getOrElse(false)
	
	/**
	 * Cancels all queued commands
	 * @return The number of commands that were cancelled
	 */
	def abortAllQueuedCommands() = bot.abortAllQueuedCommands()
	
	/**
	 * Moves this bot towards specified direction
	 * @param direction Movement direction
	 * @return Future when that movement is complete. Contains false if movement failed or was cancelled.
	 */
	def moveTowards(direction: Direction2D) = accept(MoveTowards(direction))
	
	/**
	 * Moves this bot towards specified direction. Blocks until complete.
	 * @param direction Direction towards which this bot is moved
	 * @return Whether the movement was successfully completed
	 */
	def moveTowardsBlocking(direction: Direction2D) = moveTowards(direction).waitFor().getOrElse(false)
	
	/**
	 * @param route A movement route
	 * @return Future when the whole movement is complete. Contains false if movement was interrupted.
	 */
	def takeRoute(route: Seq[Direction2D]) = accept(route.map(MoveTowards))
	
	/**
	 * @param route A movement route
	 * @return Future when the whole movement is complete. Contains false if movement was interrupted.
	 */
	def takeRoute(route: MapRoute[_]): Future[Boolean] = takeRoute(route.directions)
	
	/**
	 * Moves this bot along the specified route. Blocks.
	 * @param route A movement route
	 * @return Whether the whole movement completed successfully.
	 */
	def takeRouteBlocking(route: MapRoute[_]) = takeRoute(route).waitFor().getOrElse(false)
	
	/**
	 * Moves to the specified position
	 * @param position Target position
	 * @return Future that completes with true when movement completes or completes with false when movement fails.
	 *         Immediately returns false if no path could be calculated to the destination.
	 */
	def moveTo(position: GridPosition) =
		moveToOptimized(position) { _.minByOption { _.actualTravelDistance } }
	
	/**
	 * Moves to the specified position
	 * @param position Target position
	 * @param selectRoute A function for selecting the route to use
	 * @return Future that completes with true when movement completes or completes with false when movement fails.
	 *         Immediately returns false if no path could be calculated to the destination.
	 */
	def moveToOptimized(position: GridPosition)(selectRoute: Set[MapRoute[PermanentSquare]] => Option[MapRoute[_]]) =
		selectRoute(memory.base.routesTo(position)) match
		{
			case Some(route) => takeRoute(route)
			case None => Future.successful(false)
		}
	
	/**
	 * Commands to issue for rotating the robot head towards the specified direction
	 * @param targetDirection Targeted robot head direction
	 * @return Commands to issue to this robot
	 */
	def headRotationCommands(targetDirection: Direction2D) =
	{
		val current = heading
		if (current == targetDirection)
			Vector()
		else if (current.opposite == targetDirection)
			Vector(RotateHead(Clockwise), RotateHead(Clockwise))
		else
			RotationDirection.values.find { current.rotatedQuarterTowards(_) == targetDirection }
				.map(RotateHead).toVector
	}
	
	/**
	 * Makes this bot head the specified direction. May return immediately if already facing the specified direction.
	 * @param direction Direction to face.
	 * @return A future that will complete when this bot's head has rotated. Contains false if rotation failed.
	 */
	def pointHeadTowards(direction: Direction2D) = accept(headRotationCommands(direction))
	
	/**
	 * Makes this bot face the specified direction. Blocks.
	 * @param direction Direction to face
	 * @return Whether this bot successfully changed direction
	 */
	def pointHeadTowardsBlocking(direction: Direction2D) =
		pointHeadTowards(direction).waitFor().getOrElse(false)
	
	/**
	 * Makes this bot head the specified direction. May return immediately if already facing the specified direction.
	 * @param direction Direction to face.
	 * @param additionalCommands Commands to execute after successful head rotation
	 * @return A future that will complete when this bot's head has rotated and additional commands have been
	 *         performed. Contains false if rotation or additional action failed.
	 */
	def pointHeadTowardsAndThen(direction: Direction2D, additionalCommands: Seq[RobotCommand]) =
		accept(headRotationCommands(direction) ++ additionalCommands)
	
	/**
	 * Makes this bot head the specified direction. May return immediately if already facing the specified direction.
	 * @param direction Direction to face.
	 * @param action Action to perform once the head has been aligned
	 * @return A future that will complete when this bot's head has rotated and additional commands have been
	 *         performed. Contains false if rotation or additional action failed.
	 */
	def pointHeadTowardsAndThen(direction: Direction2D, action: RobotCommand): Future[Boolean] =
		accept(headRotationCommands(direction) :+ action)
	
	/**
	 * Scans towards the specified direction using the specified scan method
	 * @param direction Direction of scan
	 * @param scanType Type of scan
	 * @return A future that will complete once the scan has completed.
	 *         Contains failure if the command was cancelled or aborted.
	 */
	def scanTowards(direction: Direction2D, scanType: ScanType) =
		pointHeadTowardsAndThen(direction, scanType.toCommand)
	
	/**
	 * Attempts to collect treasure nearby
	 * @return A future that completes once the collection has finished. None if there was no treasure to collect.
	 */
	def tryCollectNearestTreasure() = memory.bestTreasureRoute(heading)
		.map { case (route, direction, _) =>
			accept(route.directions.map(MoveTowards) ++ headRotationCommands(direction) :+ Collect )
		}
	
	/**
	 * Attempts to collect treasure nearby
	 * @return A future that completes once the collection has finished. Contains failure if collection failed.
	 */
	def collectNearestTreasure() = tryCollectNearestTreasure().getOrElse(Future.successful(false))
	
	/**
	 * Collects nearest treasure, if there is any. Blocks.
	 * @return Whether treasure collection succeeded. False if there was no treasure to collect or if the completion
	 *         failed otherwise.
	 */
	def collectNearestTreasureBlocking() = tryCollectNearestTreasure() match
	{
		case Some(future) => future.waitFor().getOrElse(false)
		case None => false
	}
}
