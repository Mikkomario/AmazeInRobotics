package robots.model.enumeration

import utopia.genesis.shape.shape2D.Direction2D
import controller.GlobalBotSettings._
import robots.model.enumeration.RobotCommandType.{HeadRotation, Movement}
import utopia.genesis.shape.shape1D.RotationDirection

import scala.concurrent.duration.FiniteDuration

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
	def duration: FiniteDuration
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
}
