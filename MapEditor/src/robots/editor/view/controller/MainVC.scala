package robots.editor.view.controller

import robots.model.enumeration.Square
import robots.model.enumeration.Square.Empty
import robots.editor.view.util.RobotsSetup._
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.genesis.shape.Axis.X
import utopia.reach.component.factory.Mixed
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.ReachComponentWrapper
import utopia.reach.container.{AlignFrame, Framing, Stack}
import utopia.reflection.component.context.{ColorContext, TextContext}
import utopia.reflection.component.drawing.immutable.BackgroundDrawer
import utopia.reflection.shape.Alignment.Center
import utopia.reflection.shape.stack.StackLength
import utopia.reflection.shape.LengthExtensions._

/**
 * The main view controller for this app
 * @author Mikko Hilpinen
 * @since 30.1.2021, v1
 */
class MainVC(parentHierarchy: ComponentHierarchy) extends ReachComponentWrapper
{
	override protected val wrapped = Stack(parentHierarchy).build(Mixed).apply(X, margin = StackLength.fixedZero) { factories =>
		// Places the main grid view on the left side (aligned) and the square type selection view on the right side (framed)
		val selectedSquareTypePointer = new PointerWithEvents[Option[Square]](Some(Empty))
		Vector(
			factories(AlignFrame).build(GridVC)
				.apply(Center, Vector(BackgroundDrawer(colorScheme.gray))) { _.apply(selectedSquareTypePointer) },
			factories(Framing).buildFilledWithMappedContext[ColorContext, TextContext,
				ContextualSquareTypeSelectionFactory](baseContext, colorScheme.gray.dark,
				SquareTypeSelectionVC) { _.forTextComponents }
				.apply(margins.medium.any.toInsets.expandingToBottom) { _.apply(selectedSquareTypePointer) }
		)
	}
}
