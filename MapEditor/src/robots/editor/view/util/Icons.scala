package robots.editor.view.util

import robots.editor.model.enumeration.CustomCursors.Draw
import utopia.flow.util.FileExtensions._
import utopia.genesis.shape.shape2D.Point
import utopia.reach.cursor.CursorType.{Default, Interactive, Text}
import utopia.reach.cursor.{Cursor, CursorSet, CursorType}
import utopia.reflection.image.SingleColorIconCache

/**
 * Used for accessing icons of this project
 * @author Mikko Hilpinen
 * @since 31.1.2021, v1
 */
object Icons
{
	// ATTRIBUTES   --------------------------------
	
	private val cache = new SingleColorIconCache("data/images")
	
	/**
	 * Cursors in this project
	 */
	lazy val cursors =
	{
		val arrow = arrowCursor.map { _.withSourceResolutionOrigin(Point(7, 4)) }
		val hand = handCursor.map { _.withSourceResolutionOrigin(Point(9, 1)) }
		val text = textCursor.map { _.withCenterOrigin }
		val draw = edit.map { _.withSourceResolutionOrigin(Point(6, 41)) }

		val arrowC = Cursor(arrow)
		
		CursorSet(Vector(Interactive -> hand, Text -> text, Draw -> draw)
			.map { case (cType, icon) => cType -> Cursor(icon) }
			.toMap[CursorType, Cursor] + (Default -> arrowC), arrowC)
	}
	
	
	// COMPUTED ----------------------------------
	
	def edit = cache("edit.png")
	
	def arrowCursor = cache("cursor-arrow.png")
	
	def handCursor = cache("cursor-hand.png")
	
	def textCursor = cache("cursor-text.png")
}
