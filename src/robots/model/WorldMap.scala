package robots.model

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.VectorType
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.generic.model.template.ModelLike.AnyModel

object WorldMap extends FromModelFactory[WorldMap]
{
	private val schema = ModelDeclaration(PropertyDeclaration("base", VectorType))
	
	override def apply(model: AnyModel) = schema.validate(model).toTry.flatMap { valid =>
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
