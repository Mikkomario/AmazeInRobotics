package robots.model

import utopia.flow.datastructure.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Property
import utopia.flow.generic.{FromModelFactory, ModelConvertible, VectorType}
import utopia.flow.generic.ValueConversions._

object WorldMap extends FromModelFactory[WorldMap]
{
	private val schema = ModelDeclaration(PropertyDeclaration("base", VectorType))
	
	override def apply(model: template.Model[Property]) = schema.validate(model).toTry.flatMap { valid =>
		BaseGrid.fromValue(valid("base")).map { base =>
			WorldMap(base,
				valid("treasure_locations").getVector.flatMap { _.model }.flatMap { GridPosition(_).toOption }.toSet,
				valid("bot_start_locations").getVector.flatMap { _.model }.flatMap { GridPosition(_).toOption })
		}
	}
}

/**
 * Represents a map that can contains the basic world setup
 * @author Mikko Hilpinen
 * @since 29.1.2021, v1
 */
case class WorldMap(baseGrid: BaseGrid, treasureLocations: Set[GridPosition], botStartLocations: Vector[GridPosition])
	extends ModelConvertible
{
	override def toModel = Model(Vector(
		"base" -> baseGrid.toValue,
		"treasure_locations" -> treasureLocations.toVector.map { _.toModel },
		"bot_start_locations" -> botStartLocations.map { _.toModel }
	))
}
