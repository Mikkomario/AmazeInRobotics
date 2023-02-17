package robots.view

import robots.model.BaseGrid
import robots.model.enumeration.PermanentSquare
import robots.controller.GlobalBotSettings._
import utopia.flow.collection.CollectionExtensions._
import utopia.genesis.handling.Drawable
import utopia.paradigm.shape.shape2d.{Bounds, Point}
import utopia.genesis.util.Drawer
import utopia.inception.handling.HandlerType

/**
 * Used for drawing the base grid
 * @author Mikko Hilpinen
 * @since 22.1.2021, v1
 */
class BaseGridDrawer(grid: BaseGrid) extends Drawable
{
	override def draw(drawer: Drawer) =
	{
		val drawers = PermanentSquare.values.map { v => v -> drawer.onlyFill(v.color) }.toMap
		// Draws each grid block
		grid.data.foreachWithIndex { (column, columnIndex) =>
			column.foreachWithIndex { (square, rowIndex) =>
				drawers(square).draw(Bounds(Point(columnIndex, rowIndex) * pixelsPerGridUnit, gridSquarePixelSize))
			}
		}
	}
	
	override def allowsHandlingFrom(handlerType: HandlerType) = true
}
