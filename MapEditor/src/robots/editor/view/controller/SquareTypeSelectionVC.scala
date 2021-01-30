package robots.editor.view.controller

import robots.editor.view.controller.SquareTypeSelectionVC.exampleSquareStackSize
import robots.model.enumeration.Square
import robots.editor.view.util.RobotsSetup._
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.reach.component.factory.{ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory, Mixed}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.input.RadioButton
import utopia.reach.component.label.{EmptyLabel, TextLabel}
import utopia.reach.component.template.ReachComponentWrapper
import utopia.reach.container.{SegmentGroup, Stack}
import utopia.reflection.component.context.TextContextLike
import utopia.reflection.component.template.input.InteractionWithPointer
import utopia.reflection.container.stack.StackLayout.{Center, Fit, Leading, Trailing}
import utopia.reflection.shape.stack.StackLength

object SquareTypeSelectionVC extends ContextInsertableComponentFactoryFactory[TextContextLike,
	SquareTypeSelectionVCFactory, ContextualSquareTypeSelectionFactory]
{
	// ATTRIBUTES   -------------------------
	
	private val exampleSquareStackSize = StackLength(4, 16, 48).withLowPriority.square
	
	
	// IMPLEMENTED  -------------------------
	
	override def apply(hierarchy: ComponentHierarchy) = new SquareTypeSelectionVCFactory(hierarchy)
}

class SquareTypeSelectionVCFactory(parentHierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[TextContextLike, ContextualSquareTypeSelectionFactory]
{
	override def withContext[N <: TextContextLike](context: N) =
		ContextualSquareTypeSelectionFactory(parentHierarchy, context)
}

case class ContextualSquareTypeSelectionFactory[+N <: TextContextLike](parentHierarchy: ComponentHierarchy, context: N)
	extends ContextualComponentFactory[N, TextContextLike, ContextualSquareTypeSelectionFactory]
{
	// IMPLEMENTED  ----------------------------
	
	override def withContext[N2 <: TextContextLike](newContext: N2) =
		copy(context = newContext)
	
	
	// OTHER    --------------------------------
	
	/**
	 * Creates a new view controller
	 * @param valuePointer A pointer to the currently selected value (default = new pointer)
	 * @return A new square type selection vc
	 */
	def apply(valuePointer: PointerWithEvents[Option[Square]] = new PointerWithEvents[Option[Square]](None)) =
		new SquareTypeSelectionVC(parentHierarchy, valuePointer)(context)
}

/**
 * Used for determining, which block is selected to be drawn
 * @author Mikko Hilpinen
 * @since 30.1.2021, v1
 */
class SquareTypeSelectionVC(parentHierarchy: ComponentHierarchy,
                            override val valuePointer: PointerWithEvents[Option[Square]] = new PointerWithEvents[Option[Square]](None))
                           (implicit context: TextContextLike)
	extends ReachComponentWrapper with InteractionWithPointer[Option[Square]]
{
	// ATTRIBUTES   -------------------------------
	
	private val segmentGroup = SegmentGroup.rowsWithLayouts(Vector(Trailing, Leading, Fit))
	// Uses a vertical stack to hold the components
	override protected val wrapped = Stack(parentHierarchy).withContext(context).build(Stack)
		.column() { rowFactory =>
			// Creates a row for each square type
			(Square.values.map { Some(_) } :+ None).map { squareType =>
				rowFactory.build(Mixed).segmented(segmentGroup, Center, areRelated = true) { factories =>
					// Each row contains a radio button, description label and a coloured block
					Vector(
						factories.next()(RadioButton).apply(valuePointer, squareType),
						factories.next()(TextLabel).apply(squareType match
						{
							case Some(squareType) => squareType.name
							case None => "Clear"
						}),
						factories.next()(EmptyLabel).withoutContext.withBackground(squareType match
						{
							case Some(squareType) => squareType.color
							case None => GridVC.backgroundColor
						}, exampleSquareStackSize)
					)
				}
			}
		}
}
