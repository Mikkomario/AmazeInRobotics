package robots.editor.view.controller

import robots.model.WorldMap
import utopia.bunnymunch.jawn.JsonBunny
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.FileExtensions._
import utopia.flow.util.StringExtensions._

import scala.jdk.CollectionConverters._
import java.awt.dnd.{DnDConstants, DropTargetDragEvent, DropTargetDropEvent, DropTargetEvent, DropTargetListener}
import java.io.File
import java.nio.file.Path
import scala.util.{Failure, Success, Try}

object GridFileDropListener
{
	/**
	 * @param onMapLoaded A function called when a map is successfully loaded
	 * @return A new file drop listener
	 */
	def apply(onMapLoaded: (WorldMap, Path) => Unit) = new GridFileDropListener(onMapLoaded)
}

/**
 * Used for handling file drop events concerning map files
 * @author Mikko Hilpinen
 * @since 31.1.2021, v1
 */
class GridFileDropListener(onMapLoaded: (WorldMap, Path) => Unit) extends DropTargetListener
{
	override def dragEnter(dtde: DropTargetDragEvent) = println("Drag enter")
	
	override def dragOver(dtde: DropTargetDragEvent) = ()
	
	override def dropActionChanged(dtde: DropTargetDragEvent) = println("Action changed")
	
	override def dragExit(dte: DropTargetEvent) = println("Drag exit")
	
	override def drop(event: DropTargetDropEvent) =
	{
		// Accepts to copy the dropped item
		event.acceptDrop(DnDConstants.ACTION_COPY)
		
		// Checks whether the event target is a map (.json) file
		val item = event.getTransferable
		item.getTransferDataFlavors.toVector.filter { _.isFlavorJavaFileListType }.findMap { flavor =>
			Try {
				item.getTransferData(flavor).asInstanceOf[java.lang.Iterable[File]].asScala
					.map { _.toPath }.find { _.fileType ~== "json" }
			} match
			{
				case Success(result) => result
				case Failure(error) =>
					error.printStackTrace()
					None
			}
		} match
		{
			case Some(file) =>
				// Attempts to parse the dropped item as a map and import it to grid VC
				JsonBunny.munchPath(file).flatMap { json => WorldMap(json.getModel) } match
				{
					case Success(map) =>
						onMapLoaded(map, file)
						event.dropComplete(true)
					case Failure(error) =>
						error.printStackTrace()
						event.dropComplete(false)
				}
			case None => event.dropComplete(false)
		}
	}
}
