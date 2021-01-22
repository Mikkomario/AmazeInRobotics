package view

import controller.{Bot, GlobalBotSettings, ManualBotControl, MapReader, World}
import robots.model.GridPosition
import utopia.flow.async.ThreadPool
import utopia.flow.util.FileExtensions._
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
	
	val map = MapReader("test-data/test-map-1.txt").get
	val world = new World(map)
	val bot = new Bot(world, GridPosition(5, 5), Up, Color.red.timesLuminosity(0.66), Color.red,
		Color.cyan.withAlpha(0.66))
	val controller = new ManualBotControl(bot)
	
	world.registerBot(bot)
	
	val worldSize = Size(10, 10) * GlobalBotSettings.pixelsPerGridUnit
	val setup = new DefaultSetup(worldSize, "AmazeInRobots")
	
	setup.registerObjects(/*new BaseGridDrawer(map)*/bot.BotWorldDrawer, bot, controller)
	
	GlobalKeyboardEventHandler.specifyExecutionContext(exc)
	setup.start()
}
