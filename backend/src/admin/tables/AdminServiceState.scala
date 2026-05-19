package delivery.admin.tables

import delivery.admin.objects.*

final case class AdminServiceState(
    adminAccounts: List[AdminAccount],
    serviceAgents: List[CustomerServiceAgent],
    operationsManagers: List[OperationsManager],
    merchantApplications: List[MerchantApplication],
    complaintTickets: List[ComplaintTicket],
    campaigns: List[PromotionCampaign]
)
