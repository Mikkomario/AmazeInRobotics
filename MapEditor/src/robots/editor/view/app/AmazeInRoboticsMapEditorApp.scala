package robots.editor.view.app

import utopia.flow.async.ThreadPool
import utopia.genesis.generic.GenesisDataType

import scala.concurrent.ExecutionContext

/**
 * The main application for the map editor part of the robotic project
 * @author Mikko Hilpinen
 * @since 30.1.2021, v1
 */
object AmazeInRoboticsMapEditorApp extends App
{
	implicit val exc: ExecutionContext = new ThreadPool("AmazeInRobots").executionContext
	
	System.setProperty("sun.java2d.noddraw", true.toString)
	GenesisDataType.setup()
	
	// TODO: Continue by adding cursors
}
