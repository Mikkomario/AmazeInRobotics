package robots.model.enumeration

import utopia.paradigm.enumeration.Direction2D
import robots.controller.GlobalBotSettings._
import robots.model.enumeration.RobotCommandType.{HeadRotation, Interact, Movement, Scan}
import utopia.paradigm.enumeration.RotationDirection

import scala.concurrent.duration.Duration

/**
 * A common trait for various commands a control unit an give to a bot
 * @author Mikko Hilpinen
 * @since 20.1.2021, v1
 */
sealed trait RobotCommand
{
	/**
	 * @return General type of this command
	 */
	def commandType: RobotCommandType
	
	/**
	 * @return Name of this command
	 */
	def name: String
	
	/**
	 * @return How long it takes to execute this command
	 */
	def duration: Duration
}

object RobotCommand
{
	/**
	 * Moves the robot towards a relative direction
	 * @param direction Direction to which the robot should move
	 */
	case class Move(direction: RelativeDirection) extends RobotCommand
	{
		override def name = s"Move $direction"
		
		override def duration = singleMovementDuration
		
		override def commandType = Movement
	}
	
	/**
	 * Moves the robot towards an absolute direction
	 * @param direction Direction to which the robot should move
	 */
	case class MoveTowards(direction: Direction2D) extends RobotCommand
	{
		override def name = s"Move towards $direction"
		
		override def duration = singleMovementDuration
		
		override def commandType = Movement
	}
	
	/**
	 * Rotates the head of the robot to the specified direction
	 * @param direction Direction towards which the head is rotated
	 */
	case class RotateHead(direction: RotationDirection) extends RobotCommand
	{
		override def commandType = HeadRotation
		
		override def name = s"Rotate head $direction"
		
		override def duration = defaultRotationDuration
	}
	
	/**
	 * Performs a linear scan to recognize surroundings
	 */
	case object LinearScan extends RobotCommand
	{
		override def commandType = Scan
		
		override def name = "Scan (linear)"
		
		override def duration = defaultScanDuration
	}
	
	/**
	 * Scans a single adjacent block
	 */
	case object MiniScan extends RobotCommand
	{
		override val duration = defaultScanDuration * 0.33
		
		override def commandType = Scan
		
		override def name = "Scan (mini)"
	}
	
	/**
	 * Scans a line of blocks, and also blocks adjacent to those blocks
	 */
	case object WideScan extends RobotCommand
	{
		override val duration = defaultScanDuration * 2.5
		
		override def commandType = Scan
		
		override def name = "Scan (wide)"
	}
	
	/**
	 * Collects items, like treasure
	 */
	case object Collect extends RobotCommand
	{
		override def commandType = Interact
		
		override def name = "Collect"
		
		override def duration = defaultInteractDuration
	}
}
