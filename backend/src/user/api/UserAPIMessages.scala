package delivery.user.api

import cats.effect.IO
import delivery.merchant.objects.{Merchant, MerchantProfile, MerchantStoreProfile}
import delivery.merchant.tables.MerchantAccountRecord
import delivery.merchant.tables.merchantaccount.MerchantAccountTable
import delivery.merchant.tables.merchantstore.MerchantStoreTable
import delivery.rider.objects.{Rider, RiderProfile}
import delivery.rider.tables.RiderAccountRecord
import delivery.rider.tables.rideraccount.RiderAccountTable
import delivery.shared.api.{APIMessage, APIWithRoleMessage, HttpApiError}
import delivery.shared.auth.JwtSupport
import delivery.shared.objects.{MerchantCategory, OkResponse, RiderStatus, UserRole}
import delivery.user.objects.{Customer, CustomerDeliveryContact, CustomerMeResponse, CustomerProfile, CustomerProfilePatch, CustomerWalletTopUpResponse, LoginResponse}
import delivery.user.tables.{AuthCredentialRecord, CustomerAccountRecord}
import delivery.user.tables.authcredential.AuthCredentialTable
import delivery.user.tables.customer.CustomerTable
import delivery.user.tables.customerprofile.CustomerProfileTable
import delivery.user.utils.UserApiSupport

import java.sql.Connection

private def trimContact(contact: CustomerDeliveryContact): CustomerDeliveryContact =
  contact.copy(name = contact.name.trim, phone = contact.phone.trim, address = contact.address.trim)

private def validateDeliveryContacts(list: List[CustomerDeliveryContact]): Either[String, List[CustomerDeliveryContact]] =
  val trimmed = list.map(trimContact)
  if trimmed.exists(contact => contact.name.isEmpty || contact.phone.isEmpty || contact.address.isEmpty) then
    Left("收货联系人、电话与地址均不能为空")
  else if trimmed.isEmpty then Left("至少需要一组收货联系人信息")
  else
    val defaults = trimmed.count(_.isDefault)
    if defaults != 1 then Left("收货信息中必须且只能指定一组为默认")
    else Right(trimmed)

private def defaultContactsFromProfile(profile: CustomerProfile): List[CustomerDeliveryContact] =
  List(
    CustomerDeliveryContact(
      id = s"${profile.id}-dc-1",
      name = profile.name.trim,
      phone = profile.phone.trim,
      address = profile.defaultAddress.trim,
      isDefault = true
    )
  )

private def syncDefaultContactWithProfile(profile: CustomerProfile): List[CustomerDeliveryContact] =
  if profile.deliveryContacts.isEmpty then defaultContactsFromProfile(profile)
  else
    profile.deliveryContacts.map(contact =>
      if contact.isDefault then contact.copy(name = profile.name.trim, phone = profile.phone.trim, address = profile.defaultAddress.trim)
      else trimContact(contact)
    )

private def patchCustomerAccount(account: CustomerAccountRecord, patch: CustomerProfilePatch): Either[String, CustomerAccountRecord] =
  val profile = account.profile
  patch.deliveryContacts match
    case Some(rawList) =>
      validateDeliveryContacts(rawList).map { contacts =>
        val nextProfile = profile.copy(
          defaultAddress = patch.defaultAddress.getOrElse(profile.defaultAddress),
          name = patch.name.getOrElse(profile.name),
          phone = patch.phone.getOrElse(profile.phone),
          deliveryContacts = contacts
        )
        account.copy(profile = nextProfile)
      }
    case None =>
      val nextProfileBase = profile.copy(
        defaultAddress = patch.defaultAddress.getOrElse(profile.defaultAddress),
        name = patch.name.getOrElse(profile.name),
        phone = patch.phone.getOrElse(profile.phone)
      )
      Right(account.copy(profile = nextProfileBase.copy(deliveryContacts = syncDefaultContactWithProfile(nextProfileBase))))

final case class LoginAPIMessage(role: UserRole, username: String, password: String) extends APIMessage[LoginResponse]:
  override def plan(connection: Connection): IO[LoginResponse] =
    val roleValue = role.toString
    for
      credential <- AuthCredentialTable.find(connection, roleValue, username)
      _ <- credential.map(_.password) match
        case None                     => IO.raiseError(HttpApiError.Unauthorized(s"未找到该角色下的账号：$username"))
        case Some(value) if value != password => IO.raiseError(HttpApiError.Unauthorized("密码错误，请重新输入。"))
        case Some(_)                  => IO.unit
      token <- JwtSupport.signToken(username, roleValue)
    yield LoginResponse(token, username, role)

final case class RegisterAPIMessage(role: UserRole, username: String, password: String) extends APIMessage[OkResponse]:
  override def plan(connection: Connection): IO[OkResponse] =
    val roleValue = role.toString
    for
      existing <- AuthCredentialTable.find(connection, roleValue, username)
      _ <- existing match
        case Some(_) => IO.raiseError(HttpApiError.BadRequest("该角色下账号已存在。"))
        case None    => IO.unit
      _ <- AuthCredentialTable.upsert(connection, AuthCredentialRecord(roleValue, username, password))
      _ <- role match
        case UserRole.customer => registerCustomer(connection)
        case UserRole.merchant => registerMerchant(connection)
        case UserRole.rider    => registerRider(connection)
    yield OkResponse(ok = true)

  private def registerCustomer(connection: Connection): IO[Unit] =
    IO.realTime.map(_.toMillis).flatMap { nowMillis =>
      val customer = Customer(
        id = s"u-$nowMillis",
        name = username,
        phone = "",
        defaultAddress = "请完善默认收货地址",
        walletBalance = 0,
        orderHistoryIds = Nil,
        vouchers = Nil
      )
      val deliveryContacts = List(
        CustomerDeliveryContact(
          id = s"${customer.id}-dc-1",
          name = customer.name,
          phone = customer.phone,
          address = customer.defaultAddress,
          isDefault = true
        )
      )
      val account = CustomerAccountRecord(
        role = UserRole.customer.toString,
        username = username,
        password = password,
        profile = CustomerProfile(customer.id, customer.name, customer.phone, customer.defaultAddress, Nil, 0, Nil, Nil, deliveryContacts)
      )
      CustomerTable.upsert(connection, customer) *> CustomerProfileTable.upsert(connection, account).void
    }

  private def registerMerchant(connection: Connection): IO[Unit] =
    IO.realTime.map(_.toMillis).flatMap { nowMillis =>
      val merchant = Merchant(s"m-$nowMillis", s"${username}的店铺", MerchantCategory.中餐, "请完善店铺地址", "", 5, Nil, Nil, None, "")
      val account = MerchantAccountRecord(
        role = UserRole.merchant.toString,
        username = username,
        password = password,
        profile = MerchantProfile(s"merchant-profile-${merchant.id}", username, "", List(MerchantStoreProfile(merchant, Nil, Nil, Nil)))
      )
      MerchantAccountTable.upsert(connection, account) *> MerchantStoreTable.upsert(connection, username, merchant).void
    }

  private def registerRider(connection: Connection): IO[Unit] =
    IO.realTime.map(_.toMillis).flatMap { nowMillis =>
      val rider = Rider(s"r-$nowMillis", username, "", "请更新当前位置", RiderStatus.空闲, 0, 5, "未分配站点", 0)
      val account = RiderAccountRecord(UserRole.rider.toString, username, password, RiderProfile(rider, 0, Nil, Nil))
      RiderAccountTable.upsert(connection, account).void
    }

final case class CustomerMeAPIMessage() extends APIWithRoleMessage[CustomerMeResponse]:
  override def plan(connection: Connection, username: String): IO[CustomerMeResponse] =
    for
      response <- CustomerProfileTable.findByUsername(connection, username)
      output <- response.map(account => UserApiSupport.customerMeResponse(username, account)) match
        case None => IO.raiseError(HttpApiError.NotFound(UserApiSupport.customerNotFound.error))
        case Some(value) => IO.pure(value)
    yield output

final case class CustomerProfilePatchAPIMessage(patch: CustomerProfilePatch) extends APIWithRoleMessage[OkResponse]:
  override def plan(connection: Connection, username: String): IO[OkResponse] =
    for
      account <- CustomerProfileTable.findByUsername(connection, username).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.NotFound(UserApiSupport.customerNotFound.error))
      }
      nextAccount <- patchCustomerAccount(account, patch) match
        case Left(msg) => IO.raiseError(HttpApiError.BadRequest(msg))
        case Right(value) => IO.pure(value)
      _ <- CustomerProfileTable.upsert(connection, nextAccount)
    yield OkResponse(ok = true)

final case class CustomerRechargeAPIMessage(amount: Double) extends APIWithRoleMessage[CustomerWalletTopUpResponse]:
  override def plan(connection: Connection, username: String): IO[CustomerWalletTopUpResponse] =
    if amount <= 0 || amount.isNaN || amount.isInfinity then IO.raiseError(HttpApiError.BadRequest("充值金额必须为正数"))
    else
      for
        account <- CustomerProfileTable.findByUsername(connection, username).flatMap {
          case Some(value) => IO.pure(value)
          case None        => IO.raiseError(HttpApiError.NotFound(UserApiSupport.customerNotFound.error))
        }
        nextWalletBalance = BigDecimal(account.profile.walletBalance + amount).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
        nextAccount = account.copy(profile = account.profile.copy(walletBalance = nextWalletBalance))
        _ <- CustomerProfileTable.upsert(connection, nextAccount)
      yield CustomerWalletTopUpResponse(nextWalletBalance)
