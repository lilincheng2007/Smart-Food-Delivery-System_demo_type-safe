package delivery.shared.db

import cats.effect.IO
import cats.syntax.all.*
import delivery.merchant.objects.{Merchant, MerchantProfile, MerchantStoreProfile, Product}
import delivery.merchant.tables.{MerchantAccount, MerchantServiceState, MerchantTables}
import delivery.order.objects.{Order, OrderItem}
import delivery.order.tables.{OrderServiceState, OrderTables}
import delivery.rider.objects.{Rider, RiderProfile}
import delivery.rider.tables.{RiderAccount, RiderServiceState, RiderTables}
import delivery.shared.json.ApiJsonCodecs.given
import delivery.shared.objects.{DeliveryState, Voucher}
import delivery.user.objects.{Customer, CustomerDeliveryContact, CustomerProfile}
import delivery.user.tables.{AuthCredential, CustomerAccount, UserServiceState, UserTables}
import io.circe.parser.decode
import io.circe.syntax.*
import io.circe.{Decoder, Encoder, Json}
import org.postgresql.util.PGobject
import org.typelevel.log4cats.Logger

import javax.sql.DataSource
import java.sql.{Connection, PreparedStatement, ResultSet}

object DeliveryStateStore:

  private final case class ServiceTable[S](
      name: String,
      seed: DeliveryState => S,
      read: DeliveryState => S
  )

  private val userTable: ServiceTable[UserServiceState] =
    ServiceTable("user_service_state", _.user, _.user)

  private val orderTable: ServiceTable[OrderServiceState] =
    ServiceTable("order_service_state", _.order, _.order)

  private val merchantTable: ServiceTable[MerchantServiceState] =
    ServiceTable("merchant_service_state", _.merchant, _.merchant)

  private val riderTable: ServiceTable[RiderServiceState] =
    ServiceTable("rider_service_state", _.rider, _.rider)

  def migrate(ds: DataSource)(using log: Logger[IO]): IO[Unit] =
    withConnection(ds) { connection =>
      withTransaction(connection) {
        for
          _ <- List(
            UserTables.initialize(connection),
            MerchantTables.initialize(connection),
            OrderTables.initialize(connection),
            RiderTables.initialize(connection)
          ).sequence_
          _ <- List(
            createLegacyStateTable(connection, userTable.name),
            createLegacyStateTable(connection, orderTable.name),
            createLegacyStateTable(connection, merchantTable.name),
            createLegacyStateTable(connection, riderTable.name)
          ).sequence_
          hasData <- normalizedHasData(connection)
          _ <-
            if hasData then IO.unit
            else
              for
                migrated <- readCurrentServiceSnapshot(connection).flatMap {
                  case Some(value) => IO.pure(Some(value))
                  case None        => readLegacySnapshot(connection)
                }
                _ <- saveNormalized(connection, migrated.getOrElse(DeliveryState.seed))
              yield ()
        yield ()
      }
    }.flatTap(_ => log.info("DB schema ready (normalized delivery tables)"))

  def load(ds: DataSource): IO[DeliveryState] =
    withConnection(ds) { connection =>
      normalizedHasData(connection).flatMap {
        case true => loadNormalized(connection)
        case false =>
          val seed = DeliveryState.seed
          withTransaction(connection)(saveNormalized(connection, seed)).as(seed)
      }
    }

  def save(ds: DataSource)(state: DeliveryState): IO[Unit] =
    withConnection(ds)(connection => withTransaction(connection)(saveNormalized(connection, state)))

  private def normalizedHasData(connection: Connection): IO[Boolean] =
    IO.blocking {
      val statement = connection.createStatement()
      try
        val resultSet = statement.executeQuery("SELECT 1 FROM auth_credentials LIMIT 1")
        try resultSet.next()
        finally resultSet.close()
      finally statement.close()
    }

  private def loadNormalized(connection: Connection): IO[DeliveryState] =
    IO.blocking {
      val authCredentials = readAuthCredentials(connection)
      val orders = readOrders(connection)
      val customers = readCustomers(connection)
      val customerAccounts = readCustomerAccounts(connection, authCredentials)
      val catalogProducts = readCatalogProducts(connection)
      val catalogMerchants = readCatalogMerchants(connection)
      val merchantAccounts = readMerchantAccounts(connection, catalogProducts, orders)
      val riders = readRiders(connection)
      val riderAccounts = readRiderAccounts(connection, orders)

      DeliveryState(
        user = UserServiceState(customers, customerAccounts, authCredentials),
        order = OrderServiceState(orders),
        merchant = MerchantServiceState(merchantAccounts, catalogMerchants, catalogProducts),
        rider = RiderServiceState(riders, riderAccounts)
      )
    }

  private def saveNormalized(connection: Connection, state: DeliveryState): IO[Unit] =
    IO.blocking {
      clearNormalized(connection)

      state.user.authCredentials.filter(credential => isSupportedRole(credential.role)).foreach(insertAuthCredential(connection, _))
      state.user.customers.foreach(insertCustomer(connection, _))
      state.user.customerAccounts.foreach(insertCustomerAccount(connection, _))

      state.merchant.merchantAccounts.foreach(insertMerchantAccount(connection, _))
      state.merchant.merchantAccounts.foreach { account =>
        account.profile.stores.foreach(store => insertMerchantStore(connection, account.username, store.merchant))
      }
      state.merchant.catalogProducts.foreach(insertCatalogProduct(connection, _))

      state.rider.riderAccounts.foreach(account => insertRiderProfile(connection, account.profile.rider, account.profile.walletBalance))
      state.rider.riderAccounts.foreach(insertRiderAccount(connection, _))

      state.order.orders.foreach(insertOrder(connection, _))
      state.rider.riderAccounts.foreach { account =>
        val riderId = account.profile.rider.id
        val assigned = (account.profile.pendingOrders ++ account.profile.historyOrders).distinctBy(_.id)
        assigned.foreach(order => insertRiderAssignment(connection, riderId, order.id, order.status))
      }
    }

  private def clearNormalized(connection: Connection): Unit =
    val tables = List(
      "checkout_requests",
      "rider_assignments",
      "order_items",
      "orders",
      "catalog_products",
      "catalog_merchants",
      "merchant_stores",
      "merchant_accounts",
      "customer_sessions",
      "customer_profiles",
      "customers",
      "rider_accounts",
      "rider_profiles",
      "auth_credentials"
    )
    tables.foreach { table =>
      val statement = connection.createStatement()
      try
        val _ = statement.executeUpdate(s"DELETE FROM $table")
        ()
      finally statement.close()
    }

  private def readAuthCredentials(connection: Connection): List[AuthCredential] =
    queryList(connection.prepareStatement("SELECT role, username, password FROM auth_credentials ORDER BY created_at ASC")) { rs =>
      AuthCredential(rs.getString("role"), rs.getString("username"), rs.getString("password"))
    }

  private def insertAuthCredential(connection: Connection, credential: AuthCredential): Unit =
    val statement = connection.prepareStatement(
      """
        |INSERT INTO auth_credentials (role, username, password)
        |VALUES (?, ?, ?)
        |""".stripMargin
    )
    try
      statement.setString(1, credential.role)
      statement.setString(2, credential.username)
      statement.setString(3, credential.password)
      val _ = statement.executeUpdate()
      ()
    finally statement.close()

  private def readCustomers(connection: Connection): List[Customer] =
    queryList(
      connection.prepareStatement(
        """
          |SELECT id, name, phone, default_address, wallet_balance, order_history_ids, vouchers
          |FROM customers
          |ORDER BY created_at ASC
          |""".stripMargin
      )
    ) { rs =>
      Customer(
        id = rs.getString("id"),
        name = rs.getString("name"),
        phone = rs.getString("phone"),
        defaultAddress = rs.getString("default_address"),
        walletBalance = rs.getBigDecimal("wallet_balance").doubleValue(),
        orderHistoryIds = decodeJson[List[String]](rs.getString("order_history_ids")),
        vouchers = decodeJson[List[Voucher]](rs.getString("vouchers"))
      )
    }

  private def insertCustomer(connection: Connection, customer: Customer): Unit =
    val statement = connection.prepareStatement(
      """
        |INSERT INTO customers (
        |  id, name, phone, default_address, wallet_balance, order_history_ids, vouchers
        |)
        |VALUES (?, ?, ?, ?, ?, ?, ?)
        |""".stripMargin
    )
    try
      statement.setString(1, customer.id)
      statement.setString(2, customer.name)
      statement.setString(3, customer.phone)
      statement.setString(4, customer.defaultAddress)
      statement.setDouble(5, customer.walletBalance)
      statement.setObject(6, jsonb(customer.orderHistoryIds.asJson.noSpaces))
      statement.setObject(7, jsonb(customer.vouchers.asJson.noSpaces))
      val _ = statement.executeUpdate()
      ()
    finally statement.close()

  private def readCustomerAccounts(connection: Connection, credentials: List[AuthCredential]): List[CustomerAccount] =
    queryList(
      connection.prepareStatement(
        """
          |SELECT id, username, role, name, phone, default_address, wallet_balance,
          |       vouchers, pending_orders, history_orders, delivery_contacts
          |FROM customer_profiles
          |ORDER BY username ASC
          |""".stripMargin
      )
    ) { rs =>
      val username = rs.getString("username")
      CustomerAccount(
        role = rs.getString("role"),
        username = username,
        password = credentials.find(c => c.role == "customer" && c.username == username).map(_.password).getOrElse(""),
        profile = CustomerProfile(
          id = rs.getString("id"),
          name = rs.getString("name"),
          phone = rs.getString("phone"),
          defaultAddress = rs.getString("default_address"),
          vouchers = decodeJson[List[Voucher]](rs.getString("vouchers")),
          walletBalance = rs.getBigDecimal("wallet_balance").doubleValue(),
          pendingOrders = decodeJson[List[Order]](rs.getString("pending_orders")),
          historyOrders = decodeJson[List[Order]](rs.getString("history_orders")),
          deliveryContacts = decodeJson[List[CustomerDeliveryContact]](rs.getString("delivery_contacts"))
        )
      )
    }

  private def insertCustomerAccount(connection: Connection, account: CustomerAccount): Unit =
    val p = account.profile
    val statement = connection.prepareStatement(
      """
        |INSERT INTO customer_profiles (
        |  id, username, role, name, phone, default_address, wallet_balance,
        |  vouchers, pending_orders, history_orders, delivery_contacts
        |)
        |VALUES (?, ?, 'customer', ?, ?, ?, ?, ?, ?, ?, ?)
        |""".stripMargin
    )
    try
      statement.setString(1, p.id)
      statement.setString(2, account.username)
      statement.setString(3, p.name)
      statement.setString(4, p.phone)
      statement.setString(5, p.defaultAddress)
      statement.setDouble(6, p.walletBalance)
      statement.setObject(7, jsonb(p.vouchers.asJson.noSpaces))
      statement.setObject(8, jsonb(p.pendingOrders.asJson.noSpaces))
      statement.setObject(9, jsonb(p.historyOrders.asJson.noSpaces))
      statement.setObject(10, jsonb(p.deliveryContacts.asJson.noSpaces))
      val _ = statement.executeUpdate()
      ()
    finally statement.close()

  private def readMerchantAccounts(connection: Connection, products: List[Product], orders: List[Order]): List[MerchantAccount] =
    queryList(
      connection.prepareStatement(
        """
          |SELECT username, role, password, profile_id, owner_name, phone
          |FROM merchant_accounts
          |ORDER BY created_at ASC
          |""".stripMargin
      )
    ) { rs =>
      val username = rs.getString("username")
      val stores = readMerchantStores(connection, username, products, orders)
      MerchantAccount(
        role = rs.getString("role"),
        username = username,
        password = rs.getString("password"),
        profile = MerchantProfile(
          id = rs.getString("profile_id"),
          ownerName = rs.getString("owner_name"),
          phone = rs.getString("phone"),
          stores = stores
        )
      )
    }

  private def insertMerchantAccount(connection: Connection, account: MerchantAccount): Unit =
    val statement = connection.prepareStatement(
      """
        |INSERT INTO merchant_accounts (username, role, password, profile_id, owner_name, phone)
        |VALUES (?, 'merchant', ?, ?, ?, ?)
        |""".stripMargin
    )
    try
      statement.setString(1, account.username)
      statement.setString(2, account.password)
      statement.setString(3, account.profile.id)
      statement.setString(4, account.profile.ownerName)
      statement.setString(5, account.profile.phone)
      val _ = statement.executeUpdate()
      ()
    finally statement.close()

  private def readCatalogMerchants(connection: Connection): List[Merchant] =
    queryList(
      connection.prepareStatement(
        """
          |SELECT id, store_name, category, address, phone, rating, tags, featured_product_ids, image_url
          |FROM catalog_merchants
          |ORDER BY updated_at DESC
          |""".stripMargin
      )
    )(readMerchant)

  private def readMerchantStores(connection: Connection, username: String, products: List[Product], orders: List[Order]): List[MerchantStoreProfile] =
    val statement = connection.prepareStatement(
      """
        |SELECT id, store_name, category, address, phone, rating, tags, featured_product_ids, image_url
        |FROM merchant_stores
        |WHERE owner_username = ?
        |ORDER BY created_at ASC
        |""".stripMargin
    )
    try
      statement.setString(1, username)
      val resultSet = statement.executeQuery()
      try
        val builder = List.newBuilder[MerchantStoreProfile]
        while resultSet.next() do
          val merchant = readMerchant(resultSet)
          val merchantOrders = orders.filter(_.merchantId == merchant.id)
          val (pending, history) = merchantOrders.partition(order => !isHistoryStatus(order.status))
          builder += MerchantStoreProfile(
            merchant = merchant,
            products = products.filter(_.merchantId == merchant.id),
            pendingOrders = pending,
            historyOrders = history
          )
        builder.result()
      finally resultSet.close()
    finally statement.close()

  private def insertMerchantStore(connection: Connection, username: String, merchant: Merchant): Unit =
    val storeStatement = connection.prepareStatement(
      """
        |INSERT INTO merchant_stores (
        |  id, owner_username, store_name, category, address, phone, rating, tags, featured_product_ids, image_url
        |)
        |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        |""".stripMargin
    )
    try
      bindMerchant(storeStatement, merchant, Some(username))
      val _ = storeStatement.executeUpdate()
      ()
    finally storeStatement.close()

    val catalogStatement = connection.prepareStatement(
      """
        |INSERT INTO catalog_merchants (
        |  id, store_name, category, address, phone, rating, tags, featured_product_ids, image_url
        |)
        |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        |ON CONFLICT (id) DO NOTHING
        |""".stripMargin
    )
    try
      bindMerchant(catalogStatement, merchant, None)
      val _ = catalogStatement.executeUpdate()
      ()
    finally catalogStatement.close()

  private def readCatalogProducts(connection: Connection): List[Product] =
    queryList(
      connection.prepareStatement(
        """
          |SELECT id, merchant_id, name, price, description, image_url, monthly_sales,
          |       remaining_stock, listing_status, inventory_status, discount_text
          |FROM catalog_products
          |ORDER BY updated_at DESC
          |""".stripMargin
      )
    ) { rs =>
      Product(
        id = rs.getString("id"),
        merchantId = rs.getString("merchant_id"),
        name = rs.getString("name"),
        price = rs.getBigDecimal("price").doubleValue(),
        description = rs.getString("description"),
        imageUrl = rs.getString("image_url"),
        monthlySales = rs.getInt("monthly_sales"),
        remainingStock = rs.getInt("remaining_stock"),
        listingStatus = rs.getString("listing_status"),
        inventoryStatus = rs.getString("inventory_status"),
        discountText = Option(rs.getString("discount_text"))
      )
    }

  private def insertCatalogProduct(connection: Connection, product: Product): Unit =
    val statement = connection.prepareStatement(
      """
        |INSERT INTO catalog_products (
        |  id, merchant_id, name, price, description, image_url, monthly_sales,
        |  remaining_stock, listing_status, inventory_status, discount_text
        |)
        |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        |""".stripMargin
    )
    try
      statement.setString(1, product.id)
      statement.setString(2, product.merchantId)
      statement.setString(3, product.name)
      statement.setDouble(4, product.price)
      statement.setString(5, product.description)
      statement.setString(6, product.imageUrl)
      statement.setInt(7, product.monthlySales)
      statement.setInt(8, product.remainingStock)
      statement.setString(9, product.listingStatus)
      statement.setString(10, product.inventoryStatus)
      product.discountText match
        case Some(value) => statement.setString(11, value)
        case None        => statement.setNull(11, java.sql.Types.VARCHAR)
      val _ = statement.executeUpdate()
      ()
    finally statement.close()

  private def readRiders(connection: Connection): List[Rider] =
    queryList(
      connection.prepareStatement(
        """
          |SELECT id, name, phone, realtime_location, status, total_orders, rating, station, salary
          |FROM rider_profiles
          |ORDER BY created_at ASC
          |""".stripMargin
      )
    )(readRider)

  private def insertRiderProfile(connection: Connection, rider: Rider, walletBalance: Double): Unit =
    val statement = connection.prepareStatement(
      """
        |INSERT INTO rider_profiles (
        |  id, name, phone, realtime_location, status, total_orders, rating, station, salary, wallet_balance
        |)
        |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        |""".stripMargin
    )
    try
      statement.setString(1, rider.id)
      statement.setString(2, rider.name)
      statement.setString(3, rider.phone)
      statement.setString(4, rider.realtimeLocation)
      statement.setString(5, rider.status)
      statement.setInt(6, rider.totalOrders)
      statement.setDouble(7, rider.rating)
      statement.setString(8, rider.station)
      statement.setDouble(9, rider.salary)
      statement.setDouble(10, walletBalance)
      val _ = statement.executeUpdate()
      ()
    finally statement.close()

  private def readRiderAccounts(connection: Connection, orders: List[Order]): List[RiderAccount] =
    queryList(
      connection.prepareStatement(
        """
          |SELECT a.username, a.role, a.password, p.id, p.name, p.phone, p.realtime_location,
          |       p.status, p.total_orders, p.rating, p.station, p.salary, p.wallet_balance
          |FROM rider_accounts a
          |JOIN rider_profiles p ON p.id = a.rider_id
          |ORDER BY a.created_at ASC
          |""".stripMargin
      )
    ) { rs =>
      val rider = readRider(rs)
      val riderOrders = orders.filter(_.riderId.contains(rider.id))
      val (pending, history) = riderOrders.partition(order => !isHistoryStatus(order.status))
      RiderAccount(
        role = rs.getString("role"),
        username = rs.getString("username"),
        password = rs.getString("password"),
        profile = RiderProfile(
          rider = rider,
          walletBalance = rs.getBigDecimal("wallet_balance").doubleValue(),
          pendingOrders = pending,
          historyOrders = history
        )
      )
    }

  private def insertRiderAccount(connection: Connection, account: RiderAccount): Unit =
    val statement = connection.prepareStatement(
      """
        |INSERT INTO rider_accounts (username, role, password, rider_id)
        |VALUES (?, 'rider', ?, ?)
        |""".stripMargin
    )
    try
      statement.setString(1, account.username)
      statement.setString(2, account.password)
      statement.setString(3, account.profile.rider.id)
      val _ = statement.executeUpdate()
      ()
    finally statement.close()

  private def insertRiderAssignment(connection: Connection, riderId: String, orderId: String, status: String): Unit =
    val statement = connection.prepareStatement(
      """
        |INSERT INTO rider_assignments (rider_id, order_id, status)
        |VALUES (?, ?, ?)
        |ON CONFLICT (rider_id, order_id) DO NOTHING
        |""".stripMargin
    )
    try
      statement.setString(1, riderId)
      statement.setString(2, orderId)
      statement.setString(3, if status == "制作中" || status == "待接单" then "配送中" else status)
      val _ = statement.executeUpdate()
      ()
    finally statement.close()

  private def readOrders(connection: Connection): List[Order] =
    queryList(
      connection.prepareStatement(
        """
          |SELECT id, customer_id, customer_name, customer_phone, merchant_id, rider_id,
          |       total_amount, delivery_address, status, placed_at
          |FROM orders
          |ORDER BY created_at DESC
          |""".stripMargin
      )
    ) { rs =>
      val orderId = rs.getString("id")
      Order(
        id = orderId,
        customerId = rs.getString("customer_id"),
        customerName = rs.getString("customer_name"),
        customerPhone = rs.getString("customer_phone"),
        merchantId = rs.getString("merchant_id"),
        riderId = Option(rs.getString("rider_id")),
        items = readOrderItems(connection, orderId),
        totalAmount = rs.getBigDecimal("total_amount").doubleValue(),
        deliveryAddress = rs.getString("delivery_address"),
        status = rs.getString("status"),
        placedAt = rs.getString("placed_at")
      )
    }

  private def insertOrder(connection: Connection, order: Order): Unit =
    val statement = connection.prepareStatement(
      """
        |INSERT INTO orders (
        |  id, customer_id, customer_name, customer_phone, merchant_id, rider_id,
        |  total_amount, delivery_address, status, placed_at
        |)
        |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        |""".stripMargin
    )
    try
      statement.setString(1, order.id)
      statement.setString(2, order.customerId)
      statement.setString(3, order.customerName)
      statement.setString(4, order.customerPhone)
      statement.setString(5, order.merchantId)
      order.riderId match
        case Some(value) => statement.setString(6, value)
        case None        => statement.setNull(6, java.sql.Types.VARCHAR)
      statement.setDouble(7, order.totalAmount)
      statement.setString(8, order.deliveryAddress)
      statement.setString(9, order.status)
      statement.setString(10, order.placedAt)
      val _ = statement.executeUpdate()
      ()
    finally statement.close()

    order.items.foreach(insertOrderItem(connection, order.id, _))

  private def readOrderItems(connection: Connection, orderId: String): List[OrderItem] =
    val statement = connection.prepareStatement(
      """
        |SELECT product_id, name, unit_price, quantity
        |FROM order_items
        |WHERE order_id = ?
        |ORDER BY id ASC
        |""".stripMargin
    )
    try
      statement.setString(1, orderId)
      val resultSet = statement.executeQuery()
      try
        val builder = List.newBuilder[OrderItem]
        while resultSet.next() do
          builder += OrderItem(
            productId = resultSet.getString("product_id"),
            name = resultSet.getString("name"),
            unitPrice = resultSet.getBigDecimal("unit_price").doubleValue(),
            quantity = resultSet.getInt("quantity")
          )
        builder.result()
      finally resultSet.close()
    finally statement.close()

  private def insertOrderItem(connection: Connection, orderId: String, item: OrderItem): Unit =
    val statement = connection.prepareStatement(
      """
        |INSERT INTO order_items (order_id, product_id, name, unit_price, quantity)
        |VALUES (?, ?, ?, ?, ?)
        |""".stripMargin
    )
    try
      statement.setString(1, orderId)
      statement.setString(2, item.productId)
      statement.setString(3, item.name)
      statement.setDouble(4, item.unitPrice)
      statement.setInt(5, item.quantity)
      val _ = statement.executeUpdate()
      ()
    finally statement.close()

  private def bindMerchant(statement: PreparedStatement, merchant: Merchant, ownerUsername: Option[String]): Unit =
    statement.setString(1, merchant.id)
    ownerUsername match
      case Some(username) =>
        statement.setString(2, username)
        statement.setString(3, merchant.storeName)
        statement.setString(4, merchant.category)
        statement.setString(5, merchant.address)
        statement.setString(6, merchant.phone)
        statement.setDouble(7, merchant.rating)
        statement.setObject(8, jsonb(merchant.tags.asJson.noSpaces))
        statement.setObject(9, jsonb(merchant.featuredProductIds.asJson.noSpaces))
        merchant.imageUrl match
          case Some(value) => statement.setString(10, value)
          case None        => statement.setNull(10, java.sql.Types.VARCHAR)
      case None =>
        statement.setString(2, merchant.storeName)
        statement.setString(3, merchant.category)
        statement.setString(4, merchant.address)
        statement.setString(5, merchant.phone)
        statement.setDouble(6, merchant.rating)
        statement.setObject(7, jsonb(merchant.tags.asJson.noSpaces))
        statement.setObject(8, jsonb(merchant.featuredProductIds.asJson.noSpaces))
        merchant.imageUrl match
          case Some(value) => statement.setString(9, value)
          case None        => statement.setNull(9, java.sql.Types.VARCHAR)

  private def readMerchant(rs: ResultSet): Merchant =
    Merchant(
      id = rs.getString("id"),
      storeName = rs.getString("store_name"),
      category = rs.getString("category"),
      address = rs.getString("address"),
      phone = rs.getString("phone"),
      rating = rs.getBigDecimal("rating").doubleValue(),
      tags = decodeJson[List[String]](rs.getString("tags")),
      featuredProductIds = decodeJson[List[String]](rs.getString("featured_product_ids")),
      imageUrl = Option(rs.getString("image_url"))
    )

  private def readRider(rs: ResultSet): Rider =
    Rider(
      id = rs.getString("id"),
      name = rs.getString("name"),
      phone = rs.getString("phone"),
      realtimeLocation = rs.getString("realtime_location"),
      status = rs.getString("status"),
      totalOrders = rs.getInt("total_orders"),
      rating = rs.getBigDecimal("rating").doubleValue(),
      station = rs.getString("station"),
      salary = rs.getBigDecimal("salary").doubleValue()
    )

  private def isHistoryStatus(status: String): Boolean =
    status == "已送达" || status == "已完成" || status == "已取消"

  private def isSupportedRole(role: String): Boolean =
    role == "customer" || role == "merchant" || role == "rider"

  private def decodeJson[A: Decoder](raw: String): A =
    decode[A](raw).fold(error => throw new IllegalStateException(error.getMessage, error), identity)

  private def queryList[A](statement: PreparedStatement)(read: ResultSet => A): List[A] =
    try
      val resultSet = statement.executeQuery()
      try
        val builder = List.newBuilder[A]
        while resultSet.next() do builder += read(resultSet)
        builder.result()
      finally resultSet.close()
    finally statement.close()

  private def jsonb(value: String): PGobject =
    val pg = PGobject()
    pg.setType("jsonb")
    pg.setValue(value)
    pg

  private def createLegacyStateTable(connection: Connection, tableName: String): IO[Unit] =
    IO.blocking {
      val statement = connection.createStatement()
      try
        val _ = statement.execute(
          s"""CREATE TABLE IF NOT EXISTS $tableName (
             |  singleton SMALLINT PRIMARY KEY CHECK (singleton = 1),
             |  payload JSONB NOT NULL
             |)""".stripMargin
        )
        ()
      finally statement.close()
    }

  private def readCurrentServiceSnapshot(connection: Connection): IO[Option[DeliveryState]] =
    List(
      readLegacyServiceTable(connection, userTable),
      readLegacyServiceTable(connection, orderTable),
      readLegacyServiceTable(connection, merchantTable),
      readLegacyServiceTable(connection, riderTable)
    ).sequence.map {
      case List(Some(user: UserServiceState), Some(order: OrderServiceState), Some(merchant: MerchantServiceState), Some(rider: RiderServiceState)) =>
        Some(DeliveryState(user, order, merchant, rider))
      case _ => None
    }

  private def readLegacyServiceTable[S: Decoder](connection: Connection, table: ServiceTable[S]): IO[Option[S]] =
    IO.blocking {
      val statement = connection.prepareStatement(s"SELECT payload::text FROM ${table.name} WHERE singleton = 1")
      try
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then Some(resultSet.getString(1))
          else None
        finally resultSet.close()
      finally statement.close()
    }.map(_.map(raw => decode[S](raw).fold(error => throw new IllegalStateException(error.getMessage, error), identity)))

  private def readLegacySnapshot(connection: Connection)(using log: Logger[IO]): IO[Option[DeliveryState]] =
    tableExists(connection, "delivery_app_state").flatMap {
      case false => IO.pure(None)
      case true =>
        IO.blocking {
          val statement = connection.prepareStatement("SELECT payload::text FROM delivery_app_state WHERE singleton = 1")
          try
            val resultSet = statement.executeQuery()
            try
              if resultSet.next() then Some(resultSet.getString(1))
              else None
            finally resultSet.close()
          finally statement.close()
        }.flatMap {
          case None => IO.pure(None)
          case Some(raw) =>
            decodeLegacyDeliveryState(raw) match
              case Right(value) => IO.pure(Some(value))
              case Left(err) =>
                log.warn(s"Ignoring incompatible legacy delivery_app_state snapshot: ${err.getMessage}").as(None)
        }
    }

  private def decodeLegacyDeliveryState(raw: String): Either[io.circe.Error, DeliveryState] =
    decode[Json](raw).flatMap { json =>
      val cursor = json.hcursor
      for
        user <- cursor.downField("user").as[UserServiceState]
        order <- cursor.downField("order").as[OrderServiceState]
        merchant <- cursor.downField("merchant").as[MerchantServiceState]
        rider <- cursor.downField("rider").as[RiderServiceState]
      yield DeliveryState(user, order, merchant, rider)
    }

  private def tableExists(connection: Connection, tableName: String): IO[Boolean] =
    IO.blocking {
      val resultSet = connection.getMetaData.getTables(null, null, tableName, Array("TABLE"))
      try resultSet.next()
      finally resultSet.close()
    }

  private def withTransaction[A](connection: Connection)(use: IO[A]): IO[A] =
    for
      originalAutoCommit <- IO.blocking(connection.getAutoCommit)
      _ <- IO.blocking(connection.setAutoCommit(false))
      result <- use.attempt
      _ <- result match
        case Right(_) => IO.blocking(connection.commit())
        case Left(_)  => IO.blocking(connection.rollback()).handleErrorWith(_ => IO.unit)
      _ <- IO.blocking(connection.setAutoCommit(originalAutoCommit)).handleErrorWith(_ => IO.unit)
      value <- IO.fromEither(result)
    yield value

  private def withConnection[A](ds: DataSource)(use: Connection => IO[A]): IO[A] =
    IO.blocking(ds.getConnection()).bracket { connection =>
      use(connection)
    } { connection =>
      IO.blocking(connection.close()).handleErrorWith(_ => IO.unit)
    }

end DeliveryStateStore
