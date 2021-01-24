package controller

import robots.model.enumeration.RobotCommand
import robots.model.enumeration.RobotCommand.MoveTowards
import utopia.genesis.shape.shape2D.Direction2D

import scala.concurrent.ExecutionContext

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
	 * @return This bot's approximate coordinate in the world. Contains the full current movement vector.
	 */
	def position = bot.worldGridPosition
	/**
	 * A pointer to this bot's position on the grid
	 */
	def positionPointer = bot.worldGridPositionPointer
	
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
	 * Cancels all queued commands
	 * @return The number of commands that were cancelled
	 */
	def abortAllQueuedCommands() = bot.abortAllQueuedCommands()
	
	/**
	 * @param direction Movement direction
	 * @return Future when that movement is complete. Contains false if movement failed or was cancelled.
	 */
	def moveTowards(direction: Direction2D) = accept(MoveTowards(direction))
	
	/**
	 * @param route A movement route
	 * @return Future when the whole movement is complete. Contains false if movement was interrupted.
	 */
	def takeRoute(route: Seq[Direction2D]) = accept(route.map(MoveTowards))
	
	// TODO: Add more utility command methods
}
