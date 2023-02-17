package robots.editor.view.controller

import robots.editor.view.controller.SquareVC.squareStackSize
import robots.editor.view.util.RobotsSetup
import robots.model.enumeration.Square
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.flow.view.immutable.eventful.Fixed
import utopia.reach.component.factory.ComponentFactoryFactory
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.empty.ViewEmptyLabel
import utopia.reach.component.template.ReachComponentWrapper
import utopia.reflection.component.template.display.RefreshableWithPointer
import utopia.reflection.shape.stack.StackLength

object SquareVC extends ComponentFactoryFactory[SquareVCFactory]
{
	// ATTRIBUTES   ------------------------------
	
	/**
	 * The pixel length of the preferred square side
	 */
	private val squareSideLength = 32
	private val squareStackSize = StackLength(4, squareSideLength, squareSideLength * 3).square
	
	
	// IMPLEMENTED  ------------------------------
	
	override def apply(hierarchy: ComponentHierarchy) = new SquareVCFactory(hierarchy)
}

class SquareVCFactory(parentHierarchy: ComponentHierarchy)
{
	/**
	 * Creates a new square VC
	 * @param initialSquareType The initially displayed square type
	 * @return A new square view controller
	 */
	def apply(initialSquareType: Square) = new SquareVC(parentHierarchy, Some(initialSquareType))
	
	/**
	 * Creates a new square VC
	 * @param initialSquareType The initially displayed square type (optional)
	 * @return A new square view controller
	 */
	def apply(initialSquareType: Option[Square] = None) = new SquareVC(parentHierarchy, initialSquareType)
}

/**
 * A view controller which displays a square
 * @author Mikko Hilpinen
 * @since 29.1.2021, v1
 */
class SquareVC(parentHierarchy: ComponentHierarchy, initialSquareType: Option[Square] = None)
	extends ReachComponentWrapper with RefreshableWithPointer[Option[Square]]
{
	// ATTRIBUTES   -----------------------------
	
	override val contentPointer = new PointerWithEvents(initialSquareType)
	
	override protected val wrapped = ViewEmptyLabel(parentHierarchy)
		.withBackground(contentPointer.map {
			case Some(squareType) => squareType.color
			case None => RobotsSetup.colorScheme.gray
		}, Fixed(squareStackSize))
}
