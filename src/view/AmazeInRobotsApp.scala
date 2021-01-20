package view

import controller.{GlobalBotSettings, ManualBotControl}
import robots.model.{Bot, GridPosition}
import utopia.flow.async.ThreadPool
import utopia.genesis.color.Color
import utopia.genesis.shape.shape2D.Direction2D.Up
import utopia.genesis.shape.shape2D.Size
import utopia.genesis.util.DefaultSetup
import utopia.genesis.view.GlobalKeyboardEventHandler

import scala.concurrent.ExecutionContext

/**
 * The main application for this project
 * @author Mikko Hilpinen
 * @since 21.1.2021, v1
 */
object AmazeInRobotsApp extends App
{
	implicit val exc: ExecutionContext = new ThreadPool("AmazeInRobots").executionContext
	
	val bot = new Bot(GridPosition(5, 5), Up, Color.red.timesLuminosity(0.66), Color.red)
	val controller = new ManualBotControl(bot)
	
	val worldSize = Size(10, 10) * GlobalBotSettings.pixelsPerGridUnit
	val setup = new DefaultSetup(worldSize, "AmazeInRobots")
	
	setup.registerObjects(bot, controller)
	
	GlobalKeyboardEventHandler.specifyExecutionContext(exc)
	setup.start()
}
