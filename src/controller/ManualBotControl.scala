package controller

import robots.model.Bot
import robots.model.enumeration.RelativeDirection.{Backward, Forward}
import robots.model.enumeration.RobotCommand.{Move, MoveTowards, RotateHead}
import utopia.genesis.event.KeyStateEvent
import utopia.genesis.handling.KeyStateListener
import utopia.genesis.shape.shape1D.RotationDirection.{Clockwise, Counterclockwise}
import utopia.inception.handling.HandlerType

/**
 * A robot controller that is commanded manually
 * @author Mikko Hilpinen
 * @since 21.1.2021, v1
 */
class ManualBotControl(bot: Bot) extends KeyStateListener
{
	// ATTRIBUTES   -----------------------------
	
	override def keyStateEventFilter =
		KeyStateEvent.wasPressedFilter && KeyStateEvent.arrowKeysFilter
	
	
	// IMPLEMENTED  -----------------------------
	
	override def onKeyState(event: KeyStateEvent) = event.arrow.foreach { arrow =>
		val command =
		{
			if (event.keyStatus.control)
				arrow.horizontal match
				{
					case Some(horizontalDir) =>
						RotateHead(if (horizontalDir.sign.isPositive) Clockwise else Counterclockwise)
					case None => Move(if (arrow.sign.isPositive) Backward else Forward)
				}
			else
				MoveTowards(arrow)
		}
		bot.push(command)
	}
	
	override def allowsHandlingFrom(handlerType: HandlerType) = true
}
