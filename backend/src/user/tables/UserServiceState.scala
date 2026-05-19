package delivery.user.tables

import delivery.user.objects.Customer

final case class UserServiceState(
    customers: List[Customer],
    customerAccounts: List[CustomerAccount],
    authCredentials: List[AuthCredential]
)
