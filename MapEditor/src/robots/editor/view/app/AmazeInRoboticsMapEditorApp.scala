package robots.editor.view.app

import robots.editor.view.controller.MainVC
import robots.editor.view.util.Icons
import utopia.flow.async.ThreadPool
import utopia.genesis.generic.GenesisDataType
import utopia.genesis.view.GlobalKeyboardEventHandler
import utopia.reach.container.ReachCanvas
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.Program
import utopia.reflection.util.SingleFrameSetup

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
	import robots.editor.view.util.RobotsSetup._
	
	GlobalKeyboardEventHandler.specifyExecutionContext(exc)
	
	val canvas = ReachCanvas(Some(Icons.cursors)) { hierarchy =>
		// The canvas only contains the main VC
		new MainVC(hierarchy)
	}
	
	val frame = Frame.windowed(canvas.parent, "AmazeInRobots Map Editor", Program)
	frame.setToCloseOnEsc()
	new SingleFrameSetup(actorHandler, frame).start()
}
