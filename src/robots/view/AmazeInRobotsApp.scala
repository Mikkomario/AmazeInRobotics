package robots.view

import robots.controller.ai.SimpleAi
import robots.controller.{Bot, BotCommandInterface, GlobalBotSettings, World}
import robots.model.{BotColors, WorldMap}
import utopia.bunnymunch.jawn.JsonBunny
import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.ThreadPool
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.FileExtensions._
import utopia.flow.util.TimeExtensions._
import utopia.flow.util.StringExtensions._
import utopia.genesis.color.Color
import utopia.genesis.shape.shape2D.Direction2D.Up
import utopia.genesis.shape.shape2D.Size
import utopia.genesis.util.DefaultSetup
import utopia.genesis.view.GlobalKeyboardEventHandler

import java.nio.file.Path
import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
 * The main application for this project
 * @author Mikko Hilpinen
 * @since 21.1.2021, v1
 */
object AmazeInRobotsApp extends App
{
	implicit val exc: ExecutionContext = new ThreadPool("AmazeInRobots").executionContext
	
	val botColors = BotColors(Color.red.timesLuminosity(0.66), Color.red, Color.cyan.withAlpha(0.66))
	
	/*
	val map = MapReader("test-data/test-map-1.txt").get
	val world = new World(map, 2.5)
	val random = new Random()
	val bot = new Bot(world, map.botStartLocations(random.nextInt(map.botStartLocations.size)), Up, botColors)*/
	// val robots.controller = new ManualBotControl(bot)
	
	// world.registerBot(bot)
	
	val setup = new DefaultSetup(Size(10, 10) * GlobalBotSettings.pixelsPerGridUnit, "AmazeInRobots")
	//setup.canvas.setBackground(Color.blue.withSaturation(0.2).withLuminosity(0.45).toAwt)
	
	//setup.registerObjects(/*new BaseGridDrawer(map)*/bot.BotWorldDrawer, bot/*, robots.controller*/)
	
	GlobalKeyboardEventHandler.specifyExecutionContext(exc)
	//setup.start()
	
	setup.start()
	
	// Plays each map & start location once
	("maps": Path).children.get.filter { _.fileType ~== "json" }.foreach { file =>
		println(s"Reading map data from ${file.fileName}")
		JsonBunny.munchPath(file).flatMap { v => WorldMap(v.getModel) } match
		{
			case Success(map) =>
				map.botStartLocations.zipWithIndex.foreach { case (startLocation, index) =>
					println(s"Starting round ${index + 1}")
					val world = new World(map, 2 + index * 3)
					val bot = new Bot(world, startLocation, Up, botColors)
					
					world.registerBot(bot)
					setup.canvas.prefferedGameWorldSize = Size(world.base.width * GlobalBotSettings.pixelsPerGridUnit,
						world.base.height * GlobalBotSettings.pixelsPerGridUnit)
					
					val startTime = Instant.now()
					setup.registerObjects(bot.BotWorldDrawer, bot)
					
					world.gameOverPointer.futureWhere { b => b }.foreach { _ =>
						val duration = (Instant.now() - startTime) * world.speedModifier
						println(s"Bot completed ${file.fileName} ($index) in ${duration.description}")
					}
					Future { SimpleAi(new BotCommandInterface(bot), world.gameOverPointer) }.waitFor(5.minutes)
						.failure.foreach { _.printStackTrace() }
					
					setup.removeObjects(bot.BotWorldDrawer, bot)
				}
			case Failure(error) =>
				println(s"Couldn't read map from $file. Skips it.")
				error.printStackTrace()
		}
	}
	
	println("All maps completed!")
	System.exit(0)
}
