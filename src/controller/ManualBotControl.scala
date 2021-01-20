package controller

import robots.model.Bot
import robots.model.enumeration.RobotCommand.MoveTowards
import utopia.genesis.event.KeyStateEvent
import utopia.genesis.handling.KeyStateListener
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
	
	override def onKeyState(event: KeyStateEvent) = event.arrow.foreach { arrow => bot.push(MoveTowards(arrow)) }
	
	override def allowsHandlingFrom(handlerType: HandlerType) = true
}
