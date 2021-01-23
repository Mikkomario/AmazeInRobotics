package controller

import robots.model.{BaseGrid, GridPosition}
import robots.model.enumeration.Square.{BotLocation, TreasureLocation}
import utopia.flow.datastructure.mutable.{PointerWithEvents, ResettableLazy}
import utopia.flow.event.ChangeListener

/**
 * A world contains a base maze and also keeps track of and shares data with the bots
 * @author Mikko Hilpinen
 * @since 22.1.2021, v1
 */
class World(val base: BaseGrid, treasures: Vector[GridPosition])
{
	// ATTRIBUTES   -----------------------------
	
	private var bots = Vector[Bot]()
	
	private val remainingTreasuresPointer = new PointerWithEvents(treasures)
	private val worldStatePointer = ResettableLazy { base ++
		(treasures.map { _ -> TreasureLocation } ++ bots.map { _.gridPosition -> BotLocation }) }
	
	private val resetWorldStateListener = ChangeListener.onAnyChange { worldStatePointer.reset() }
	
	
	// INITIAL CODE -----------------------------
	
	// Updates world state when treasures are taken
	remainingTreasuresPointer.addListener(resetWorldStateListener)
	
	
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
		bot.worldGridPositionPointer.addListener(resetWorldStateListener)
	}
}
