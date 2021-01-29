package robots.model

/**
 * Represents a map that can contains the basic world setup
 * @author Mikko Hilpinen
 * @since 29.1.2021, v1
 */
case class WorldMap(baseGrid: BaseGrid, treasureLocations: Set[GridPosition], botStartLocations: Vector[GridPosition])
