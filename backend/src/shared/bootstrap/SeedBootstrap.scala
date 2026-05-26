package delivery.shared.bootstrap

import delivery.merchant.objects.*
import delivery.merchant.tables.*
import delivery.order.objects.*
import delivery.order.tables.*
import delivery.rider.objects.*
import delivery.rider.tables.*
import delivery.user.objects.{Customer, CustomerDeliveryContact, CustomerProfile}
import delivery.user.tables.*

object SeedBootstrap:

  private def splitOrdersByHistory(source: List[Order]): (List[Order], List[Order]) =
    val pending = source.filter(o => o.status != "已送达" && o.status != "已完成" && o.status != "已取消")
    val history = source.filter(o => o.status == "已送达" || o.status == "已完成" || o.status == "已取消")
    (pending, history)

  lazy val userState: UserServiceState =
    val orders = SeedData.seedOrders
    val customerAccounts = SeedData.seedCustomers.zipWithIndex.map { case (customer, index) =>
      val related = orders.filter(_.customerId == customer.id)
      val (pending, history) = splitOrdersByHistory(related)
      CustomerAccount(
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

    val authCredentials =
      customerAccounts.map(a => AuthCredential(a.role, a.username, a.password)) ++
        merchantState.merchantAccounts.map(a => AuthCredential(a.role, a.username, a.password)) ++
        riderState.riderAccounts.map(a => AuthCredential(a.role, a.username, a.password))

    UserServiceState(
      customers = SeedData.seedCustomers,
      customerAccounts = customerAccounts,
      authCredentials = authCredentials
    )

  lazy val merchantState: MerchantServiceState =
    val demoStores = SeedData.seedMerchants.map { merchant =>
      val merchantOrders = SeedData.seedOrders.filter(_.merchantId == merchant.id)
      val (pending, history) = splitOrdersByHistory(merchantOrders)
      MerchantStoreProfile(
        merchant = merchant,
        products = SeedData.seedProducts.filter(_.merchantId == merchant.id),
        pendingOrders = pending,
        historyOrders = history
      )
    }

    val merchantAccounts = List(
      MerchantAccount(
        role = "merchant",
        username = "merchant_demo",
        password = "123456",
        profile = MerchantProfile(
          id = "merchant-profile-demo",
          ownerName = "演示商家",
          phone = SeedData.seedMerchants.headOption.map(_.phone).getOrElse(""),
          stores = demoStores
        )
      )
    )

    MerchantServiceState(
      merchantAccounts = merchantAccounts,
      catalogMerchants = SeedData.seedMerchants,
      catalogProducts = SeedData.seedProducts
    )

  lazy val riderState: RiderServiceState =
    val riderAccounts = SeedData.seedRiders.zipWithIndex.map { case (rider, index) =>
      val riderOrders = SeedData.seedOrders.filter(_.riderId.contains(rider.id))
      val (pending, history) = splitOrdersByHistory(riderOrders)
      RiderAccount(
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

    RiderServiceState(riders = SeedData.seedRiders, riderAccounts = riderAccounts)

  lazy val orderState: OrderServiceState =
    OrderServiceState(orders = SeedData.seedOrders)

end SeedBootstrap
