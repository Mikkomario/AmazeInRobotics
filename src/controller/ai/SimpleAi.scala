package controller.ai

import controller.GlobalBotSettings._
import controller.{AiFactory, BotCommandInterface}
import controller.ai.SimpleAi.openScanBlocks
import robots.model.enumeration.RobotCommand.{LinearScan, MiniScan, WideScan}
import utopia.flow.event.ChangingLike
import utopia.flow.util.WaitUtils
import utopia.flow.util.TimeExtensions._

import scala.concurrent.ExecutionContext

object SimpleAi extends AiFactory
{
	// ATTRIBUTES   -------------------------
	
	private val openScanBlocks = 1.75
	
	
	// IMPLEMENTED  -------------------------
	
	override def apply(controller: BotCommandInterface, gameOverPointer: ChangingLike[Boolean])
	                  (implicit exc: ExecutionContext) =
	{
		gameOverPointer.futureWhere { !_ }.foreach { _ => controller.abortAllQueuedCommands() }
		
		val ai = new SimpleAi(controller)
		while (!gameOverPointer.value && ai.cycle()) { }
	}
}

/**
 * A simple AI implementation
 * @author Mikko Hilpinen
 * @since 25.1.2021, v1
 */
class SimpleAi(controller: BotCommandInterface)
{
	// ATTRIBUTES   ---------------------------
	
	private var failures = 0
	
	
	// OTHER    -------------------------------
	
	/**
	 * Performs a single ai action cycle. Blocks.
	 */
	def cycle() =
	{
		// Prioritizes treasure
		if (controller.collectNearestTreasureBlocking())
		{
			failures = 0
			println("Found treasure")
		}
		// Finds the best scan target
		controller.memory.base.calculateBestScan(
			1.0 / MiniScan.duration.toMillis * 100,
			(1.0 + openScanBlocks) / LinearScan.duration.toMillis * 100) { (blocks, isOpen) =>
			if (isOpen)
				(blocks.size + openScanBlocks) / WideScan.duration.toMillis * 100
			else
				blocks.size / 2.0 / WideScan.duration.toMillis * 100
		} { (route, direction) =>
			100 + route.actualTravelDistance * singleMovementDuration.toMillis * 0.7 +
				controller.headRotationCommands(direction).size * defaultRotationDuration.toMillis
		} match
		{
			case Some((route, direction, scan)) =>
				failures = 0
				println(s"Moves ${route.actualTravelDistance} times and scans ($scan) towards $direction")
				// Executes the best scan target
				if (controller.takeRouteBlocking(route))
				{
					if (controller.pointHeadTowardsBlocking(direction))
						controller.executeBlocking(scan.toCommand)
				}
			case None =>
				println("No scan option found")
				WaitUtils.wait(0.5.seconds, this)
				failures += 1
		}
		failures < 3
	}
}
