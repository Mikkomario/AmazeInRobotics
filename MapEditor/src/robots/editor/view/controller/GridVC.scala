package robots.editor.view.controller

import robots.editor.model.enumeration.CustomCursors.Draw
import robots.editor.view.controller.GridVC.{backgroundColor, gridLineColor, gridLineWidth, squareSideStackLength}
import robots.model.GridPosition
import robots.model.enumeration.Square
import utopia.flow.caching.multi.Cache
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.datastructure.template.Viewable
import utopia.genesis.color.Color
import utopia.genesis.event.{MouseButtonStateEvent, MouseEvent, MouseMoveEvent}
import utopia.genesis.handling.{MouseButtonStateListener, MouseMoveListener}
import utopia.genesis.shape.Axis2D
import utopia.genesis.shape.shape2D.{Bounds, Line, Point, Size}
import utopia.genesis.util.{Distance, Drawer}
import utopia.inception.handling.immutable.Handleable
import utopia.reach.component.factory.ComponentFactoryFactory
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{CursorDefining, CustomDrawReachComponent}
import utopia.reach.cursor.Cursor
import utopia.reach.util.Priority.High
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.drawing.template.DrawLevel.Normal
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.stack.{StackLength, StackSize}

object GridVC extends ComponentFactoryFactory[GridVCFactory]
{
	/**
	 * Background color used in this view
	 */
	val backgroundColor = Color.blue.withSaturation(0.2).withLuminosity(0.45)
	
	private val minGridSide = 5
	private val gridLineWidth = 1
	private val gridLineColor = Color.black.withAlpha(0.66)
	
	private val squareSideStackLength =
	{
		val base = Distance.ofCm(1).toScreenPixels
		StackLength(base / 4, base, base * 2)
	}
	
	override def apply(hierarchy: ComponentHierarchy) = new GridVCFactory(hierarchy)
}

class GridVCFactory(parentHierarchy: ComponentHierarchy)
{
	/**
	 * Creates a new grid VC
	 * @param selectedSquareTypePointer A readable pointer to the currently selected square type
	 * @return A new grid vc
	 */
	def apply(selectedSquareTypePointer: Viewable[Option[Square]]) =
		new GridVC(parentHierarchy, selectedSquareTypePointer)
}

/**
 * A view controller for managing an editable grid
 * @author Mikko Hilpinen
 * @since 29.1.2021, v1
 */
class GridVC(override val parentHierarchy: ComponentHierarchy, selectedSquareTypePointer: Viewable[Option[Square]])
	extends CustomDrawReachComponent with CursorDefining
{
	// ATTRIBUTES   -------------------------------
	
	private val dataPointer = new PointerWithEvents[Map[GridPosition, Square]](Map())
	
	private val gridPositionsPointer = dataPointer.map { data =>
		// Calculates the smallest and largest x and y values, having at least 'minGridSide' difference
		var minX = 0
		var minY = 0
		var maxX = 0
		var maxY = 0
		data.keys.foreach { p =>
			if (p.x < minX)
				minX = p.x
			else if (p.x > maxX)
				maxX = p.x
			
			if (p.y < minY)
				minY = p.y
			else if (p.y > maxY)
				maxY = p.y
		}
		GridPosition(minX - 1, minY - 1) -> GridPosition(maxX + 1, maxY + 1)
	}
	
	private val gridSizePointer = gridPositionsPointer.map { case (min, max) => max + (1, 1) - min }
	
	override val customDrawers = Vector[CustomDrawer](GridDrawer)
	
	
	// INITIAL CODE -------------------------------
	
	// Revalidates / repaints on certain updates
	gridSizePointer.addAnyChangeListener { revalidateAndRepaint(High) }
	dataPointer.addListener { change =>
		// Checks which squares changed and repaints those
		change.mergeBy { _.keySet } { _ ++ _ }.foreach { position =>
			if (change.differentBy { _.get(position) })
				repaintArea(GridDrawer.boundsForSquare(position), High)
		}
	}
	
	// Starts listening to mouse events
	addMouseButtonListener(GridMouseListener)
	addMouseMoveListener(GridMouseListener)
	
	
	// COMPUTED -----------------------------------
	
	private def data = dataPointer.value
	
	private def minPosition = gridPositionsPointer.value._1
	
	/**
	 * @return Current size of this grid in squares
	 */
	private def gridSize = gridSizePointer.value
	
	private def gridWidthSquares = gridSize.x
	
	private def gridHeightSquares = gridSize.y
	
	
	// IMPLEMENTED  -------------------------------
	
	override def calculatedStackSize = StackSize(
		squareSideStackLength * gridWidthSquares, squareSideStackLength * gridHeightSquares)
	
	override def updateLayout() = ()
	
	override def cursorType = Draw
	
	override def cursorBounds = GridDrawer.gridBounds + positionInTop
	
	override def cursorToImage(cursor: Cursor, position: Point) =
	{
		val targetPosition = calculateGridPosition(position)
		data.get(targetPosition) match
		{
			case Some(square) => cursor.over(square.color)
			case None => cursor.over(backgroundColor)
		}
	}
	
	
	// OTHER    -----------------------------------
	
	// NB: Parameter is relative to the grid top left corner, not the position of this component
	private def calculateGridPosition(gridPixelPosition: Point) =
	{
		val rawTargetPosition = gridPixelPosition / GridDrawer.squareSideLength
		GridPosition(rawTargetPosition.x.toInt, rawTargetPosition.y.toInt) + minPosition
	}
	
	
	// NESTED   -----------------------------------
	
	private object GridDrawer extends CustomDrawer
	{
		// ATTRIBUTES   ---------------------------
		
		private var _gridBounds = Bounds.zero
		private var _squareSideLength = 1.0
		
		
		// COMPUTED -------------------------------
		
		def gridBounds = _gridBounds
		
		def squareSideLength = _squareSideLength
		
		
		// OTHER    -------------------------------
		
		def boundsForSquare(position: GridPosition) = Bounds(_gridBounds.position +
			(position - minPosition) * _squareSideLength, Size.square(_squareSideLength))
		
		
		// IMPLEMENTED  ---------------------------
		
		override def drawLevel = Normal
		
		override def draw(drawer: Drawer, bounds: Bounds) =
		{
			val size = gridSize
			val squareSideLength = Axis2D.values.map { axis => bounds.size.along(axis) / size.along(axis) }.min
			val squarePixelSize = Size.square(squareSideLength)
			val gridPixelSize = (size * squareSideLength).toSize
			val gridTopLeftPosition = Alignment.Center.position(gridPixelSize, bounds, fitWithinBounds = false).position
			val topLeftSquarePosition = minPosition
			val newGridBounds = Bounds(gridTopLeftPosition, gridPixelSize)
			
			// Draws the background
			drawer.onlyFill(backgroundColor).draw(newGridBounds)
			
			// Draws the square data
			val squareDrawers = Cache[Square, Drawer] { s => drawer.onlyFill(s.color) }
			data.foreach { case (position, squareType) =>
				squareDrawers(squareType).draw(Bounds(gridTopLeftPosition +
					(position - topLeftSquarePosition) * squareSideLength, squarePixelSize))
			}
			
			// Draws the lines
			val lineDrawer = drawer.onlyEdges(gridLineColor).withStroke(gridLineWidth)
			Axis2D.values.foreach { axis =>
				val lineVector = axis.perpendicular(gridPixelSize.along(axis.perpendicular))
				(0 to size.along(axis)).foreach { c =>
					val start = gridTopLeftPosition + axis(c * squareSideLength)
					lineDrawer.draw(Line.ofVector(start, lineVector))
				}
			}
			
			// Remembers last draw area information
			_gridBounds = newGridBounds
			this._squareSideLength = squareSideLength
		}
	}
	
	private object GridMouseListener extends MouseButtonStateListener with MouseMoveListener with Handleable
	{
		// ATTRIBUTES   ----------------------------------
		
		private val areaFilter = MouseEvent.isOverAreaFilter { GridDrawer.gridBounds + position }
		
		// Reacts to mouse presses inside grid bounds
		override val mouseButtonStateEventFilter = MouseButtonStateEvent.leftPressedFilter && areaFilter
		
		// Also reacts to mouse drags inside grid bounds
		override val mouseMoveEventFilter = MouseEvent.isLeftDownFilter && areaFilter
		
		
		// IMPLEMENTED  ---------------------------------
		
		override def onMouseButtonState(event: MouseButtonStateEvent) =
		{
			handleMouseEvent(event)
			None
		}
		
		override def onMouseMove(event: MouseMoveEvent) = handleMouseEvent(event)
		
		
		// OTHER    -------------------------------------
		
		def handleMouseEvent(event: MouseEvent[_]) =
		{
			// Calculates the currently targeted grid position
			val targetPosition = calculateGridPosition(event.mousePosition - GridDrawer.gridBounds.position)
			// Updates data if necessary
			selectedSquareTypePointer.value match
			{
				case Some(fillType) =>
					if (!data.get(targetPosition).contains(fillType))
						dataPointer.update { _ + (targetPosition -> fillType) }
				case None =>
					if (data.contains(targetPosition))
						dataPointer.update { _ - targetPosition }
			}
		}
	}
}
