package controller

/**
 * A listener that will be called whenever the targeted bot gets stunned
 * @author Mikko Hilpinen
 * @since 24.1.2021, v1
 */
trait BotStunListener
{
	/**
	 * A function that is called when the targeted bot gets stunned
	 */
	def onBotStunned(): Unit
}
