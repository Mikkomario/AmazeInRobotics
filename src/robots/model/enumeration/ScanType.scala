package robots.model.enumeration

import robots.model.enumeration.RobotCommand.{LinearScan, MiniScan, WideScan}

/**
 * A common trait / enumeration for different types of scans
 * @author Mikko Hilpinen
 * @since 25.1.2021, v1
 */
sealed trait ScanType
{
	/**
	 * @return A robot command associated with this scan type
	 */
	def toCommand: RobotCommand
}

object ScanType
{
	/**
	 * A single square scan
	 */
	case object Mini extends ScanType
	{
		override def toCommand = MiniScan
	}
	
	/**
	 * A linear line scan
	 */
	case object Linear extends ScanType
	{
		override def toCommand = LinearScan
	}
	
	/**
	 * A wide line scan
	 */
	case object Wide extends ScanType
	{
		override def toCommand = WideScan
	}
}
