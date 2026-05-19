package delivery.rider.tables

import cats.effect.IO
import delivery.order.objects.Order
import delivery.rider.objects.*

object RiderDomainOps:

  private def isHistoryStatus(status: String): Boolean =
    status == "已送达" || status == "已完成" || status == "已取消"

  private def replaceRiderSnapshot(state: RiderServiceState, updatedRider: Rider): RiderServiceState =
    state.copy(
      riders = state.riders.map(r => if r.id == updatedRider.id then updatedRider else r),
      riderAccounts = state.riderAccounts.map { account =>
        if account.profile.rider.id != updatedRider.id then account
        else account.copy(profile = account.profile.copy(rider = updatedRider))
      }
    )

  def bootstrapRider(state: RiderServiceState, username: String): IO[Either[String, RiderServiceState]] =
    if state.riderAccounts.exists(_.username == username) then IO.pure(Left("骑手已存在"))
    else
      IO.realTime.map(_.toMillis).map { nowMillis =>
        val newRider = Rider(s"r-$nowMillis", username, "", "请更新当前位置", "空闲", 0, 5, "未分配站点", 0)
        val acc = RiderAccount("rider", username, "", RiderProfile(newRider, 0, Nil, Nil))
        Right(state.copy(riders = state.riders :+ newRider, riderAccounts = state.riderAccounts :+ acc))
      }

  def replaceOrderSnapshot(state: RiderServiceState, updatedOrder: Order): RiderServiceState =
    updatedOrder.riderId match
      case None => state
      case Some(riderId) =>
        state.copy(
          riderAccounts = state.riderAccounts.map { account =>
            if account.profile.rider.id != riderId then account
            else
              val nextPending = account.profile.pendingOrders.filterNot(_.id == updatedOrder.id)
              val nextHistory = account.profile.historyOrders.filterNot(_.id == updatedOrder.id)
              val nextProfile =
                if isHistoryStatus(updatedOrder.status) then
                  account.profile.copy(historyOrders = updatedOrder :: nextHistory, pendingOrders = nextPending)
                else account.profile.copy(pendingOrders = updatedOrder :: nextPending, historyOrders = nextHistory)
              account.copy(profile = nextProfile)
          }
        )

  def grabOrder(state: RiderServiceState, username: String, updatedOrder: Order): Either[String, RiderServiceState] =
    state.riderAccounts.find(_.username == username).toRight("未找到骑手账号").flatMap { account =>
      if account.profile.pendingOrders.length >= 5 then Left("当前骑手最多同时配送 5 单")
      else
        val updatedRider = account.profile.rider.copy(status = "配送中", totalOrders = account.profile.rider.totalOrders + 1)
        val nextState = replaceOrderSnapshot(replaceRiderSnapshot(state, updatedRider), updatedOrder)
        Right(nextState)
    }

  def completeOrder(state: RiderServiceState, username: String, updatedOrder: Order): Either[String, RiderServiceState] =
    state.riderAccounts.find(_.username == username).toRight("未找到骑手账号").flatMap { account =>
      val remainingPendingCount = account.profile.pendingOrders.count(_.id != updatedOrder.id)
      val nextStatus = if remainingPendingCount > 0 then "配送中" else "空闲"
      val updatedRider = account.profile.rider.copy(status = nextStatus, salary = account.profile.rider.salary + 5)
      val nextState = replaceOrderSnapshot(replaceRiderSnapshot(state, updatedRider), updatedOrder)
      Right(nextState)
    }

end RiderDomainOps
