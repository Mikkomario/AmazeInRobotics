package robots.model.enumeration

import utopia.genesis.shape.shape2D.Direction2D

/**
 * A common trait for causes for robot stun
 * @author Mikko Hilpinen
 * @since 24.1.2021, v1
 */
sealed trait StunCause

object StunCause
{
	/**
	 * When a robot collides with a wall or another robot and is unable to move forward as planned
	 * @param attemptedMovementDirection The attempted movement direction where the movement failed
	 */
	case class MovementCollision(attemptedMovementDirection: Direction2D) extends StunCause
}
