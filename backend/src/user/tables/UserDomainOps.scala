package delivery.user.tables

import cats.effect.IO
import delivery.order.objects.Order
import delivery.user.objects.{
  CheckoutCompleteRequest,
  Customer,
  CustomerDeliveryContact,
  CustomerProfile,
  CustomerProfilePatch
}

object UserDomainOps:

  private def isHistoryStatus(status: String): Boolean =
    status == "已送达" || status == "已完成" || status == "已取消"

  def verifyLogin(state: UserServiceState, role: String, username: String, password: String): Either[String, Unit] =
    state.authCredentials.find(c => c.role == role && c.username == username).map(_.password) match
      case None                     => Left(s"未找到该角色下的账号：$username")
      case Some(p) if p != password => Left("密码错误，请重新输入。")
      case Some(_)                  => Right(())

  def registerCustomer(state: UserServiceState, username: String, password: String): IO[Either[String, UserServiceState]] =
    if state.authCredentials.exists(c => c.role == "customer" && c.username == username) then
      IO.pure(Left("该角色下账号已存在。"))
    else
      IO.realTime.map(_.toMillis).map { nowMillis =>
        val newCustomer = Customer(
          id = s"u-$nowMillis",
          name = username,
          phone = "",
          defaultAddress = "请完善默认收货地址",
          walletBalance = 0,
          orderHistoryIds = Nil,
          vouchers = Nil
        )
        val dc = List(
          CustomerDeliveryContact(
            id = s"${newCustomer.id}-dc-1",
            name = newCustomer.name,
            phone = newCustomer.phone,
            address = newCustomer.defaultAddress,
            isDefault = true
          )
        )
        val acc = CustomerAccount(
          "customer",
          username,
          password,
          CustomerProfile(newCustomer.id, newCustomer.name, newCustomer.phone, newCustomer.defaultAddress, Nil, 0, Nil, Nil, dc)
        )
        val cred = AuthCredential("customer", username, password)
        Right(state.copy(customers = state.customers :+ newCustomer, customerAccounts = state.customerAccounts :+ acc, authCredentials = state.authCredentials :+ cred))
      }

  def registerMerchantCredential(state: UserServiceState, username: String, password: String): Either[String, UserServiceState] =
    if state.authCredentials.exists(c => c.role == "merchant" && c.username == username) then Left("该角色下账号已存在。")
    else Right(state.copy(authCredentials = state.authCredentials :+ AuthCredential("merchant", username, password)))

  def registerRiderCredential(state: UserServiceState, username: String, password: String): Either[String, UserServiceState] =
    if state.authCredentials.exists(c => c.role == "rider" && c.username == username) then Left("该角色下账号已存在。")
    else Right(state.copy(authCredentials = state.authCredentials :+ AuthCredential("rider", username, password)))

  private def trimContact(c: CustomerDeliveryContact): CustomerDeliveryContact =
    c.copy(name = c.name.trim, phone = c.phone.trim, address = c.address.trim)

  def validateDeliveryContacts(list: List[CustomerDeliveryContact]): Either[String, List[CustomerDeliveryContact]] =
    val trimmed = list.map(trimContact)
    if trimmed.exists(c => c.name.isEmpty || c.phone.isEmpty || c.address.isEmpty) then
      Left("收货联系人、电话与地址均不能为空")
    else if trimmed.isEmpty then Left("至少需要一组收货联系人信息")
    else
      val defaults = trimmed.count(_.isDefault)
      if defaults != 1 then Left("收货信息中必须且只能指定一组为默认")
      else Right(trimmed)

  private def defaultContactsFromProfile(p: CustomerProfile): List[CustomerDeliveryContact] =
    List(
      CustomerDeliveryContact(
        id = s"${p.id}-dc-1",
        name = p.name.trim,
        phone = p.phone.trim,
        address = p.defaultAddress.trim,
        isDefault = true
      )
    )

  private def syncDefaultContactWithProfile(p: CustomerProfile): List[CustomerDeliveryContact] =
    if p.deliveryContacts.isEmpty then defaultContactsFromProfile(p)
    else
      p.deliveryContacts.map(c =>
        if c.isDefault then c.copy(name = p.name.trim, phone = p.phone.trim, address = p.defaultAddress.trim)
        else trimContact(c)
      )

  def patchCustomer(state: UserServiceState, username: String, patch: CustomerProfilePatch): Either[String, UserServiceState] =
    state.customerAccounts.find(_.username == username) match
      case None => Left("Not found")
      case Some(acc) =>
        val p = acc.profile
        patch.deliveryContacts match
          case Some(rawList) =>
            validateDeliveryContacts(rawList) match
              case Left(msg) => Left(msg)
              case Right(contacts) =>
                val np = p.copy(
                  walletBalance = patch.walletBalance.getOrElse(p.walletBalance),
                  defaultAddress = patch.defaultAddress.getOrElse(p.defaultAddress),
                  name = patch.name.getOrElse(p.name),
                  phone = patch.phone.getOrElse(p.phone),
                  deliveryContacts = contacts
                )
                Right(state.copy(customerAccounts = state.customerAccounts.map(ca => if ca.username == username then ca.copy(profile = np) else ca)))
          case None =>
            val np0 = p.copy(
              walletBalance = patch.walletBalance.getOrElse(p.walletBalance),
              defaultAddress = patch.defaultAddress.getOrElse(p.defaultAddress),
              name = patch.name.getOrElse(p.name),
              phone = patch.phone.getOrElse(p.phone)
            )
            val contacts =
              if np0.deliveryContacts.isEmpty then defaultContactsFromProfile(np0)
              else syncDefaultContactWithProfile(np0)
            val np = np0.copy(deliveryContacts = contacts)
            Right(state.copy(customerAccounts = state.customerAccounts.map(ca => if ca.username == username then ca.copy(profile = np) else ca)))

  def checkoutComplete(state: UserServiceState, req: CheckoutCompleteRequest): Either[String, UserServiceState] =
    state.customerAccounts.find(_.username == req.username).toRight("未找到顾客账号").flatMap { account =>
      if account.profile.walletBalance < req.totalDebit then Left("余额不足")
      else
        val newWallet = account.profile.walletBalance - req.totalDebit
        val newPending = req.orders.reverse ::: account.profile.pendingOrders
        Right(
          state.copy(
            customerAccounts =
              state.customerAccounts.map(ca =>
                if ca.username == req.username then ca.copy(profile = ca.profile.copy(walletBalance = newWallet, pendingOrders = newPending))
                else ca
              )
          )
        )
    }

  def replaceOrderSnapshot(state: UserServiceState, updatedOrder: Order): UserServiceState =
    state.copy(
      customerAccounts = state.customerAccounts.map { account =>
        if account.profile.id != updatedOrder.customerId then account
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

end UserDomainOps
