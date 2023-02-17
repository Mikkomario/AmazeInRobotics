package robots.editor.view.controller

import robots.editor.view.controller.MainVC.mapsDirectory
import robots.editor.view.dialog.WriteFileNameDialog
import robots.editor.view.util.Icons
import robots.model.enumeration.Square
import robots.model.enumeration.Square.Empty
import robots.editor.view.util.RobotsSetup._
import robots.model.WorldMap
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.util.StringExtensions._
import utopia.paradigm.enumeration.Axis.X
import utopia.reach.component.factory.Mixed
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{ReachComponentLike, ReachComponentWrapper}
import utopia.reach.component.wrapper.ComponentCreationResult
import utopia.reach.container.multi.stack.Stack
import utopia.reach.container.wrapper.{AlignFrame, Framing}
import utopia.reflection.component.context.{ButtonContext, ColorContext, TextContext}
import utopia.reflection.component.drawing.immutable.BackgroundDrawer
import utopia.paradigm.enumeration.Alignment.Center
import utopia.reach.component.button.image.{ContextualImageAndTextButtonFactory, ImageAndTextButton}
import utopia.reflection.shape.stack.StackLength
import utopia.reflection.shape.LengthExtensions._

import java.nio.file.Path
import scala.concurrent.ExecutionContext

object MainVC
{
	private lazy val mapsDirectory = ("maps": Path).createDirectories().get
}

/**
 * The main view controller for this app
 * @author Mikko Hilpinen
 * @since 30.1.2021, v1
 */
class MainVC(parentHierarchy: ComponentHierarchy)(implicit exc: ExecutionContext) extends ReachComponentWrapper
{
	// ATTRIBUTES   -----------------------------------
	
	private var currentTargetFile: Option[Path] = None
	
	// Contains a stack with main content at the bottom and a header row at the top
	private val (_wrapped, gridVC) = Stack(parentHierarchy).build(Mixed).apply(margin = StackLength.fixedZero) { factories =>
		// Creates the main content first
		val (mainContent, gridVC) = factories(Stack).build(Mixed).apply(X, margin = StackLength.fixedZero) { factories =>
			// Places the main grid view on the left side (aligned) and the square type selection view on the right side (framed)
			val selectedSquareTypePointer = new PointerWithEvents[Option[Square]](Some(Empty))
			val (gridPanel, gridVC) = factories(AlignFrame).build(GridVC)
				.apply(Center, Vector(BackgroundDrawer(colorScheme.gray))) { _.apply(selectedSquareTypePointer) }.toTuple
			ComponentCreationResult[Vector[ReachComponentLike]](Vector(
				gridPanel,
				// Square selection panel
				factories(Framing).buildFilledWithMappedContext[ColorContext, TextContext,
					ContextualSquareTypeSelectionFactory](baseContext, colorScheme.primary,
					SquareTypeSelectionVC) { _.forTextComponents }
					.apply(margins.medium.any.toInsets.expandingToBottom) { _.apply(selectedSquareTypePointer) }
			)).withResult(gridVC)
		}.parentAndResult
		ComponentCreationResult.many(Vector(
			// Header panel with a save button
			factories(Framing).buildFilledWithMappedContext[ColorContext, ButtonContext, ContextualImageAndTextButtonFactory](
				baseContext, colorScheme.primary.dark, ImageAndTextButton) { _.forTextComponents.forSecondaryColorButtons }
				.apply(margins.medium.any.toInsets.expandingToRight) { buttonFactory =>
					buttonFactory.withIcon(Icons.save, "Save") { currentTargetFile match
					{
						// If file path is already determined, updates the file contents
						case Some(path) => path.writeJson(gridVC.toWorldMap).failure.foreach { _.printStackTrace() }
						case None =>
							// Otherwise queries for the file path
							parentHierarchy.parentWindow.foreach { window =>
								WriteFileNameDialog.display(window).foreach { _.foreach { input =>
									// Processes the file name
									val fileNameWithExtension =
										if (input.endsWithIgnoreCase(".json")) input else input + ".json"
									val path = mapsDirectory/fileNameWithExtension
									currentTargetFile = Some(path)
									// Saves the map file
									path.writeJson(gridVC.toWorldMap).failure.foreach { _.printStackTrace() }
								} }
							}
					} }
				}.parent,
			mainContent
		), gridVC)
	}.parentAndResult
	
	
	// IMPLEMENTED  ---------------------------------
	
	override protected def wrapped = _wrapped
	
	
	// OTHER    -------------------------------------
	
	/**
	 * Imports a map to edit
	 * @param worldMap A world map to edit
	 * @param path The path from which this map was imported from
	 */
	def importMap(worldMap: WorldMap, path: Path) =
	{
		gridVC.importMap(worldMap)
		currentTargetFile = Some(path)
	}
}
