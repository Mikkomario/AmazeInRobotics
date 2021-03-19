package robots.editor.view.dialog

import robots.editor.view.util.Icons
import robots.editor.view.util.RobotsSetup._
import utopia.flow.event.Fixed
import utopia.genesis.event.KeyStateEvent
import utopia.genesis.util.Distance
import utopia.reach.component.button.{ImageAndTextButton, ViewImageAndTextButton}
import utopia.reach.component.factory.Mixed
import utopia.reach.component.input.TextField
import utopia.reach.container.{Framing, ReachCanvas, Stack}
import utopia.reflection.container.swing.window.WindowResizePolicy.Program
import utopia.reflection.container.swing.window.Dialog
import utopia.reflection.shape.LengthExtensions._

import java.awt.event.KeyEvent
import scala.concurrent.{ExecutionContext, Promise}

/**
 * An object for creating dialogs for selecting file names
 * @author Mikko Hilpinen
 * @since 31.1.2021, v1
 */
object WriteFileNameDialog
{
	private lazy val inputFieldWidth = Distance.ofCm(5).toScreenPixels.any
	
	/**
	 * Displays a dialog for writing a file name
	 * @param parentWindow Window that will host this dialog
	 * @param exc Implicit execution context
	 * @return Future containing the selected file name or None
	 */
	def display(parentWindow: java.awt.Window)(implicit exc: ExecutionContext) =
	{
		val background = colorScheme.primary.light
		val resultPromise = Promise[Option[String]]()
		
		// Creates window contents first
		// The window contains a text field, an ok button and a cancel button
		val canvas = ReachCanvas(Some(Icons.cursors)) { hierarchy =>
			// Main framing
			Framing(hierarchy).buildFilledWithContext(baseContext, background, Stack).apply(margins.medium.any) { stackFactory =>
				// Main components row
				stackFactory.mapContext { _.forTextComponents }.build(Mixed)
					.row() { factories =>
						// Input text field
						val field = factories(TextField).mapContext { _.noLineBreaksAllowed }
							.forString(inputFieldWidth, Fixed("File Name"))
						def saveAction(): Unit = resultPromise.success(Some(field.value))
						// Save button, which is enabled only while there is input
						val saveButton = factories.mapContext { _.forSecondaryColorButtons }(ViewImageAndTextButton)
							.withStaticTextAndIcon("OK", Icons.save, field.valuePointer.map { _.nonEmpty }) { saveAction() }
						// If enter is pressed in the field, triggers the save button
						field.addFilteredKeyListenerWhileFocused(KeyStateEvent.wasReleasedFilter &&
							KeyStateEvent.keyFilter(KeyEvent.VK_ENTER)) { _ => saveAction() }
						Vector(
							field,
							saveButton,
							// Cancel button which closes this window
							factories.mapContext { _.forPrimaryColorButtons }(ImageAndTextButton)
								.withIcon(Icons.close, "Cancel",
									hotKeyCharacters = Vector(KeyEvent.VK_ESCAPE)) { resultPromise.success(None) }
						)
					}
			}
		}
		
		val window = new Dialog(parentWindow, canvas.parent, "Select map file name", Program)
		// Completes the promise at latest when the window closes
		window.closeFuture.foreach { _ =>
			if (!resultPromise.isCompleted)
				resultPromise.success(None)
		}
		
		// Closes window when results arrive
		val resultFuture = resultPromise.future
		resultFuture.foreach { _ => window.close() }
		
		window.startEventGenerators(actorHandler)
		window.display()
		resultFuture
	}
}
