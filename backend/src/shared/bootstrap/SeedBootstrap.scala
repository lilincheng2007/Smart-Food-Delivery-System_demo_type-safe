package delivery.shared.bootstrap

import delivery.merchant.objects.{MerchantProfile, MerchantStoreProfile}
import delivery.merchant.tables.MerchantAccountRecord
import delivery.order.objects.Order
import delivery.rider.objects.RiderProfile
import delivery.rider.tables.RiderAccountRecord
import delivery.user.objects.{CustomerDeliveryContact, CustomerProfile}
import delivery.user.tables.{AuthCredentialRecord, CustomerAccountRecord}

object SeedBootstrap:

  private def splitOrdersByHistory(source: List[Order]): (List[Order], List[Order]) =
    val pending = source.filter(order => !isHistoryStatus(order.status))
    val history = source.filter(order => isHistoryStatus(order.status))
    (pending, history)

  private def isHistoryStatus(status: String): Boolean =
    status == "已送达" || status == "已完成" || status == "已取消"

  lazy val customerAccounts: List[CustomerAccountRecord] =
    SeedData.seedCustomers.zipWithIndex.map { case (customer, index) =>
      val related = SeedData.seedOrders.filter(_.customerId == customer.id)
      val (pending, history) = splitOrdersByHistory(related)
      CustomerAccountRecord(
        role = "customer",
        username = if index == 0 then "customer_demo" else s"customer_${index + 1}",
        password = "123456",
        profile = CustomerProfile(
          id = customer.id,
          name = customer.name,
          phone = customer.phone,
          defaultAddress = customer.defaultAddress,
          vouchers = customer.vouchers,
          walletBalance = customer.walletBalance,
          pendingOrders = pending,
          historyOrders = history,
          deliveryContacts = List(
            CustomerDeliveryContact(
              id = s"${customer.id}-dc-1",
              name = customer.name,
              phone = customer.phone,
              address = customer.defaultAddress,
              isDefault = true
            )
          )
        )
      )
    }

  lazy val merchantAccounts: List[MerchantAccountRecord] =
    val stores = SeedData.seedMerchants.map { merchant =>
      val related = SeedData.seedOrders.filter(_.merchantId == merchant.id)
      val (pending, history) = splitOrdersByHistory(related)
      MerchantStoreProfile(
        merchant = merchant,
        products = SeedData.seedProducts.filter(_.merchantId == merchant.id),
        pendingOrders = pending,
        historyOrders = history
      )
    }

    List(
      MerchantAccountRecord(
        role = "merchant",
        username = "merchant_demo",
        password = "123456",
        profile = MerchantProfile(
          id = "merchant-profile-demo",
          ownerName = "演示商家",
          phone = SeedData.seedMerchants.headOption.map(_.phone).getOrElse(""),
          stores = stores
        )
      )
    )

  lazy val riderAccounts: List[RiderAccountRecord] =
    SeedData.seedRiders.zipWithIndex.map { case (rider, index) =>
      val related = SeedData.seedOrders.filter(_.riderId.contains(rider.id))
      val (pending, history) = splitOrdersByHistory(related)
      RiderAccountRecord(
        role = "rider",
        username = if index == 0 then "rider_demo" else s"rider_${index + 1}",
        password = "123456",
        profile = RiderProfile(
          rider = rider,
          walletBalance = math.round(rider.salary * 0.1).toDouble,
          pendingOrders = pending,
          historyOrders = history
        )
      )
    }

  lazy val authCredentials: List[AuthCredentialRecord] =
    customerAccounts.map(account => AuthCredentialRecord(account.role, account.username, account.password)) ++
      merchantAccounts.map(account => AuthCredentialRecord(account.role, account.username, account.password)) ++
      riderAccounts.map(account => AuthCredentialRecord(account.role, account.username, account.password))

end SeedBootstrap
