package robots.model

import utopia.genesis.shape.shape2D.{Direction2D, Point, TwoDimensional, Vector2D, Vector2DLike}
import Direction2D._
import utopia.genesis.util.Scalable

object GridPosition
{
	/**
	 * The (0, 0) grid position
	 */
	val origin = GridPosition(0, 0)
}

/**
 * An integer-based position on a grid-like world
 * @author Mikko Hilpinen
 * @since 20.1.2021, v1
 */
case class GridPosition(override val x: Int, override val y: Int) extends TwoDimensional[Int] with Scalable[Point]
{
	// ATTRIBUTES   -------------------------
	
	/**
	 * @return A 2D vector based on this position
	 */
	lazy val toVector = Vector2D(x, y)
	
	
	// IMPLEMENTED  -------------------------
	
	override def toString = s"($x, $y)"
	
	override val dimensions = Vector(x, y)
	
	override protected def zeroDimension = 0
	
	override def repr = Point(x, y)
	
	override def *(mod: Double) = Point(x * mod, y * mod)
	
	
	// COMPUTED -----------------------------
	
	/**
	 * @return Positions adjacent to this one
	 */
	def adjacent = Direction2D.values.map { this + _ }
	
	
	// OTHER    -----------------------------
	
	/**
	 * @param other Another position
	 * @return A sum of these positions
	 */
	def +(other: GridPosition) = GridPosition(x + other.x, y + other.y)
	
	/**
	 * @param movement X and Y movement
	 * @return A moved copy of this position
	 */
	def +(movement: (Int, Int)) = GridPosition(x + movement._1, y + movement._2)
	
	/**
	 * @param movement Movement direction
	 * @return This position moved 1 unit to the specified direction
	 */
	def +(movement: Direction2D): GridPosition = movement match
	{
		case Up => GridPosition(x, y - 1)
		case Down => GridPosition(x, y + 1)
		case Left => GridPosition(x - 1, y)
		case Right => GridPosition(x + 1, y)
	}
	
	/**
	 * @param movement a movement vector
	 * @tparam V Type of movement vector
	 * @return This position moved by specified amount
	 */
	def +[V <: Vector2DLike[V]](movement: V): V = movement + toVector
	
	/**
	 * @param other Another grid position
	 * @return A subtraction of these grid positions
	 */
	def -(other: GridPosition) = GridPosition(x - other.x, y - other.y)
	
	/**
	 * @param movement X and Y movement
	 * @return A moved copy of this position
	 */
	def -(movement: (Int, Int)) = GridPosition(x - movement._1, y - movement._2)
	
	/**
	 * @param movement Movement direction
	 * @return This position moved -1 unit to the specified direction
	 */
	def -(movement: Direction2D) = this + movement.opposite
	
	/**
	 * @param movement a movement vector
	 * @tparam V Type of movement vector
	 * @return This position moved by specified amount
	 */
	def -[V <: Vector2DLike[V]](movement: V) = -movement + toVector
	
	/**
	 * @param direction Target direction
	 * @param movement Amount of movement
	 * @return A position at that movement away to that direction
	 */
	def towards(direction: Direction2D, movement: Int) = direction match
	{
		case Up => this + (0, -movement)
		case Down => this + (0, movement)
		case Left => this + (-movement, 0)
		case Right => this + (movement, 0)
	}
	
	/**
	 * @param other Another position
	 * @return The top left of these positions - In other words, minimum x and minimum y
	 */
	def topLeft(other: GridPosition) = GridPosition(x min other.x, y min other.y)
	
	/**
	 * @param other Another position
	 * @return The bottom right of these positions - In other words, maximum x and maximum y
	 */
	def bottomRight(other: GridPosition) = GridPosition(x max other.x, y max other.y)
}
