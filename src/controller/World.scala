package controller

import robots.model.BaseGrid
import robots.model.enumeration.Square.BotLocation
import utopia.flow.datastructure.mutable.ResettableLazy

/**
 * A world contains a base maze and also keeps track of and shares data with the bots
 * @author Mikko Hilpinen
 * @since 22.1.2021, v1
 */
class World(base: BaseGrid)
{
	// ATTRIBUTES   -----------------------------
	
	private var bots = Vector[Bot]()
	
	private val worldStatePointer = ResettableLazy { base ++ bots.map { _.gridPosition -> BotLocation } }
	
	
	// COMPUTED ---------------------------------
	
	/**
	 * @return Current state of this world
	 */
	def state = worldStatePointer.value
	
	
	// OTHER    ---------------------------------
	
	/**
	 * Registers a new bot into this world
	 * @param bot Bot to register into this world
	 */
	def registerBot(bot: Bot) =
	{
		bots :+= bot
		bot.gridPositionPointer.addAnyChangeListener { worldStatePointer.reset() }
	}
}
