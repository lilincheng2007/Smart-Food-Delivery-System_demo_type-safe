package delivery.admin.utils

import delivery.admin.objects.{AdminAccountPublic, AdminMeResponse, OrdersPanelResponse, OverviewResponse, PlatformMetaResponse}
import delivery.admin.tables.AdminAccount
import delivery.shared.objects.ErrorBody

object AdminApiSupport:

  def adminNotFound: ErrorBody = ErrorBody("未找到账号")

  def adminMeResponse(username: String, account: AdminAccount): AdminMeResponse =
    AdminMeResponse(
      username = username,
      role = "admin",
      adminAccount = AdminAccountPublic(account.role, account.username, account.displayName)
    )

  def overview(state: delivery.shared.objects.DeliveryState): OverviewResponse =
    OverviewResponse(
      merchants = state.merchant.catalogMerchants,
      orders = state.order.orders,
      riders = state.rider.riders,
      campaigns = state.admin.campaigns,
      complaintTickets = state.admin.complaintTickets
    )

  def ordersPanel(state: delivery.shared.objects.DeliveryState): OrdersPanelResponse =
    OrdersPanelResponse(orders = state.order.orders, riders = state.rider.riders)

  def platformMeta(state: delivery.shared.objects.DeliveryState): PlatformMetaResponse =
    PlatformMetaResponse(
      campaigns = state.admin.campaigns,
      complaintTickets = state.admin.complaintTickets,
      merchantApplications = state.admin.merchantApplications,
      serviceAgents = state.admin.serviceAgents,
      operationsManagers = state.admin.operationsManagers
    )

end AdminApiSupport
