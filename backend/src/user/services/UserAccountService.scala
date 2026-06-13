package delivery.user.services

import cats.effect.IO
import delivery.merchant.objects.{Merchant, MerchantProfile, MerchantStoreProfile}
import delivery.merchant.tables.MerchantAccountRecord
import delivery.merchant.tables.merchantaccount.MerchantAccountTable
import delivery.merchant.tables.merchantstore.MerchantStoreTable
import delivery.rider.objects.{Rider, RiderProfile}
import delivery.rider.tables.RiderAccountRecord
import delivery.rider.tables.rideraccount.RiderAccountTable
import delivery.domain.{MerchantCategory, RiderStatus, UserRole, Voucher, VoucherId}
import delivery.promotion.services.VoucherSupport
import delivery.user.objects.{Customer, CustomerDeliveryContact, CustomerProfile, CustomerProfilePatch}
import delivery.user.tables.CustomerAccountRecord
import delivery.user.tables.customer.CustomerTable
import delivery.user.tables.customerprofile.CustomerProfileTable

import java.sql.Connection
import java.time.LocalDate
import scala.util.Try

object UserAccountService:

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

  private def welcomeVoucher(customerId: String, nowMillis: Long): Voucher =
    VoucherSupport.standardPlatformVoucher(customerId, 1)

  private def isVoucherExpired(voucher: Voucher): Boolean =
    Try(LocalDate.parse(voucher.expiresAt)).toOption.forall(_.isBefore(LocalDate.now()))

  def discardExpiredVoucher(account: CustomerAccountRecord, voucherId: VoucherId): Either[String, CustomerAccountRecord] =
    val profile = account.profile
    profile.vouchers.find(_.id == voucherId) match
      case None => Left("未找到该优惠券")
      case Some(voucher) if !isVoucherExpired(voucher) => Left("仅可舍弃已过期优惠券")
      case Some(_) => Right(account.copy(profile = profile.copy(vouchers = profile.vouchers.filterNot(_.id == voucherId))))

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

  def patchCustomerAccount(account: CustomerAccountRecord, patch: CustomerProfilePatch): Either[String, CustomerAccountRecord] =
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

  def registerCustomer(connection: Connection, username: String, password: String): IO[Unit] =
    IO.realTime.map(_.toMillis).flatMap { nowMillis =>
      val customerId = s"u-$nowMillis"
      val welcome = welcomeVoucher(customerId, nowMillis)
      val customer = Customer(
        id = customerId,
        name = username,
        phone = "",
        defaultAddress = "请完善默认收货地址",
        walletBalance = 0,
        orderHistoryIds = Nil,
        vouchers = List(welcome),
        foodiePoints = 0,
        foodieLevel = 1
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
        profile = CustomerProfile(customer.id, customer.name, customer.phone, customer.defaultAddress, customer.vouchers, 0, Nil, Nil, deliveryContacts, 0, 1)
      )
      CustomerTable.upsert(connection, customer) *> CustomerProfileTable.upsert(connection, account).void
    }

  def registerMerchant(connection: Connection, username: String, password: String): IO[Unit] =
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

  def registerRider(connection: Connection, username: String, password: String): IO[Unit] =
    IO.realTime.map(_.toMillis).flatMap { nowMillis =>
      val rider = Rider(s"r-$nowMillis", username, "", "请更新当前位置", RiderStatus.空闲, 0, 5, "未分配站点", 0)
      val account = RiderAccountRecord(UserRole.rider.toString, username, password, RiderProfile(rider, 0, Nil, Nil))
      RiderAccountTable.upsert(connection, account).void
    }

end UserAccountService
