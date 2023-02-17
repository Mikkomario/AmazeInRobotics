package robots.controller

import utopia.flow.view.template.eventful.Changing

import scala.concurrent.ExecutionContext

/**
 * A factory trait common to all bot AI implementations
 * @author Mikko Hilpinen
 * @since 25.1.2021, v1
 */
trait AiFactory
{
	/**
	 * Runs the AI. Blocks until the AI has completed running.
	 * @param controller Bot control interface to use
	 * @param gameOverPointer A pointer that will contain true when the game is over / when this AI should stop. The
	 *                        function should complete within a reasonable time after this pointer has changed to true.
	 */
	def apply(controller: BotCommandInterface, gameOverPointer: Changing[Boolean])(implicit exc: ExecutionContext): Unit
}
