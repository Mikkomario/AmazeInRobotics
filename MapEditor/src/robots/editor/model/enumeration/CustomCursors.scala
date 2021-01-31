package robots.editor.model.enumeration

import utopia.reach.cursor.CursorType
import utopia.reach.cursor.CursorType.Interactive

/**
 * Contains cursor type specific to this project
 * @author Mikko Hilpinen
 * @since 31.1.2021, v1
 */
object CustomCursors
{
	/**
	 * A cursor used when drawing / editing
	 */
	case object Draw extends CursorType
	{
		override def backup = Some(Interactive)
	}
}
