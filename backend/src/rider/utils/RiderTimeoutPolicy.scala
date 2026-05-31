package delivery.rider.utils

import java.time.Instant
import scala.concurrent.duration.*

object RiderTimeoutPolicy:
  val EnergyPerDeliveredOrder: Int = 10
  val TimeoutCardEnergyCost: Int = 100
  val DeliveryTimeout: FiniteDuration = 45.minutes

  def deadlineAt(assignedAt: Instant): Instant =
    assignedAt.plusSeconds(DeliveryTimeout.toSeconds)

  def overtimeSeconds(assignedAt: Instant, completedAt: Instant): Int =
    val seconds = completedAt.getEpochSecond - deadlineAt(assignedAt).getEpochSecond
    Math.max(seconds, 0).toInt

  def isTimeout(assignedAt: Instant, completedAt: Instant): Boolean =
    overtimeSeconds(assignedAt, completedAt) > 0
