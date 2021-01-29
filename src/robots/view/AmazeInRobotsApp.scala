package robots.view

import robots.controller.ai.SimpleAi
import robots.controller.{Bot, BotCommandInterface, GlobalBotSettings, MapReader, World}
import robots.model.BotColors
import utopia.flow.async.ThreadPool
import utopia.flow.util.FileExtensions._
import utopia.flow.util.TimeExtensions._
import utopia.genesis.color.Color
import utopia.genesis.shape.shape2D.Direction2D.Up
import utopia.genesis.shape.shape2D.Size
import utopia.genesis.util.DefaultSetup
import utopia.genesis.view.GlobalKeyboardEventHandler

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

/**
 * The main application for this project
 * @author Mikko Hilpinen
 * @since 21.1.2021, v1
 */
object AmazeInRobotsApp extends App
{
	implicit val exc: ExecutionContext = new ThreadPool("AmazeInRobots").executionContext
	
	val map = MapReader("test-data/test-map-1.txt").get
	val world = new World(map, 2.5)
	val random = new Random()
	val bot = new Bot(world, map.botStartLocations(random.nextInt(map.botStartLocations.size)), Up,
		BotColors(Color.red.timesLuminosity(0.66), Color.red, Color.cyan.withAlpha(0.66)))
	// val robots.controller = new ManualBotControl(bot)
	
	world.registerBot(bot)
	
	val worldSize = Size(10, 10) * GlobalBotSettings.pixelsPerGridUnit
	val setup = new DefaultSetup(worldSize, "AmazeInRobots")
	setup.canvas.setBackground(Color.blue.withLuminosity(0.25).toAwt)
	
	setup.registerObjects(/*new BaseGridDrawer(map)*/bot.BotWorldDrawer, bot/*, robots.controller*/)
	
	GlobalKeyboardEventHandler.specifyExecutionContext(exc)
	setup.start()
	
	val startTime = Instant.now()
	world.gameOverPointer.addListener { ended =>
		if (ended.newValue)
		{
			val duration = (Instant.now() - startTime) * world.speedModifier
			println(s"Bot completed game in ${duration.description}")
		}
	}
	val aiFuture = Future { SimpleAi(new BotCommandInterface(bot), world.gameOverPointer) }
	aiFuture.foreach { _ =>
		println("Ai finished")
		System.exit(0)
	}
}
