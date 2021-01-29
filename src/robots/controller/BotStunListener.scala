package robots.controller

import robots.model.enumeration.StunCause

/**
 * A listener that will be called whenever the targeted bot gets stunned
 * @author Mikko Hilpinen
 * @since 24.1.2021, v1
 */
trait BotStunListener
{
	/**
	 * A function that is called when the targeted bot gets stunned
	 * @param cause The cause for the bot stun
	 */
	def onBotStunned(cause: StunCause): Unit
}
