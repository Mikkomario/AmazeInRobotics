package robots.editor.view.util

import utopia.genesis.generic.GenesisDataType
import utopia.genesis.handling.mutable.ActorHandler
import utopia.reflection.color.{ColorScheme, ColorSet}
import utopia.reflection.component.context.{AnimationContext, BaseContext, ScrollingContext}
import utopia.reflection.localization.{Localizer, NoLocalization}
import utopia.reflection.shape.Margins
import utopia.reflection.text.Font
import utopia.reflection.text.FontStyle.Plain

/**
 * A set of constants used in this project
 * @author Mikko Hilpinen
 * @since 29.1.2021, v1
 */
object RobotsSetup
{
	GenesisDataType.setup()
	
	val actorHandler = ActorHandler()
	val colorScheme = ColorScheme.twoTone(ColorSet.fromHexes("#212121", "#484848", "#000000").get,
		ColorSet.fromHexes("#ffab00", "#ffdd4b", "#c67c00").get)
	val font = Font("Arial", 12, Plain, 2)
	val margins = Margins(12)
	
	val baseContext: BaseContext = BaseContext(actorHandler, font, colorScheme, margins)
	
	implicit val animationContext: AnimationContext = AnimationContext(actorHandler)
	implicit val scrollingContext: ScrollingContext = ScrollingContext.withDarkRoundedBar(actorHandler)
	
	implicit val defaultLanguageCode: String = "EN"
	implicit val localizer: Localizer = NoLocalization
}
