package delivery.order.tables.order

import cats.effect.IO
import cats.syntax.all.*
import delivery.order.objects.Order
import delivery.order.tables.orderitem.OrderItemTable
import delivery.shared.json.ApiJsonCodecs.given
import delivery.shared.objects.{MerchantId, OrderId, OrderStatus, RefundStatus, RiderId, UserId, Voucher}
import io.circe.parser.decode
import io.circe.syntax.*
import org.postgresql.util.PGobject

import java.sql.{Connection, PreparedStatement, ResultSet}

object OrderTable:

  private val insertSql: String =
    """
      |INSERT INTO orders (
      |  id, customer_id, customer_name, customer_phone, merchant_id, rider_id,
      |  total_amount, delivery_address, status, placed_at,
      |  original_amount, discount_amount, payable_amount, used_voucher, points_awarded,
      |  refund_status, refund_reason, refund_image_url, refund_requested_at,
      |  refund_merchant_reason, refund_merchant_reviewed_at, refund_admin_reason, refunded_at,
      |  customer_note_text, customer_note_image_url, updated_at
      |)
      |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())
      |ON CONFLICT (id) DO UPDATE SET
      |  customer_id = EXCLUDED.customer_id,
      |  customer_name = EXCLUDED.customer_name,
      |  customer_phone = EXCLUDED.customer_phone,
      |  merchant_id = EXCLUDED.merchant_id,
      |  rider_id = EXCLUDED.rider_id,
      |  total_amount = EXCLUDED.total_amount,
      |  delivery_address = EXCLUDED.delivery_address,
      |  status = EXCLUDED.status,
      |  placed_at = EXCLUDED.placed_at,
      |  original_amount = EXCLUDED.original_amount,
      |  discount_amount = EXCLUDED.discount_amount,
      |  payable_amount = EXCLUDED.payable_amount,
      |  used_voucher = EXCLUDED.used_voucher,
      |  points_awarded = EXCLUDED.points_awarded,
      |  refund_status = EXCLUDED.refund_status,
      |  refund_reason = EXCLUDED.refund_reason,
      |  refund_image_url = EXCLUDED.refund_image_url,
      |  refund_requested_at = EXCLUDED.refund_requested_at,
      |  refund_merchant_reason = EXCLUDED.refund_merchant_reason,
      |  refund_merchant_reviewed_at = EXCLUDED.refund_merchant_reviewed_at,
      |  refund_admin_reason = EXCLUDED.refund_admin_reason,
      |  refunded_at = EXCLUDED.refunded_at,
      |  customer_note_text = EXCLUDED.customer_note_text,
      |  customer_note_image_url = EXCLUDED.customer_note_image_url,
      |  updated_at = now()
      |""".stripMargin

  def upsert(connection: Connection, order: Order): IO[Order] =
    IO.blocking {
      val statement = connection.prepareStatement(insertSql)
      try
        bindOrder(statement, order)
        val _ = statement.executeUpdate()
        order
      finally statement.close()
    }.flatTap(saved => OrderItemTable.replaceForOrder(connection, saved.id, saved.items))

  private val selectColumns: String =
    """
      |SELECT id, customer_id, customer_name, customer_phone, merchant_id, rider_id,
      |       total_amount, delivery_address, status, placed_at,
      |       original_amount, discount_amount, payable_amount, used_voucher, points_awarded,
      |       refund_status, refund_reason, refund_image_url, refund_requested_at,
      |       refund_merchant_reason, refund_merchant_reviewed_at, refund_admin_reason, refunded_at,
      |       customer_note_text, customer_note_image_url
      |FROM orders
      |""".stripMargin

  private val listSql: String =
    s"""
       |$selectColumns
       |ORDER BY created_at DESC
       |""".stripMargin

  def list(connection: Connection): IO[List[Order]] =
    queryMany(connection, listSql)(_ => ())

  private val findByIdSql: String =
    s"""
       |$selectColumns
       |WHERE id = ?
       |""".stripMargin

  def findById(connection: Connection, id: OrderId): IO[Option[Order]] =
    queryOne(connection, findByIdSql)(_.setString(1, id))

  private val listByCustomerIdSql: String =
    s"""
       |$selectColumns
       |WHERE customer_id = ?
       |ORDER BY created_at DESC
       |""".stripMargin

  def listByCustomerId(connection: Connection, customerId: UserId): IO[List[Order]] =
    queryMany(connection, listByCustomerIdSql)(_.setString(1, customerId))

  def listByMerchantIds(connection: Connection, merchantIds: List[MerchantId]): IO[List[Order]] =
    val distinctMerchantIds = merchantIds.distinct
    if distinctMerchantIds.isEmpty then IO.pure(Nil)
    else
      val placeholders = List.fill(distinctMerchantIds.size)("?").mkString(", ")
      val sql =
        s"""
           |$selectColumns
           |WHERE merchant_id IN ($placeholders)
           |ORDER BY created_at DESC
           |""".stripMargin
      queryMany(connection, sql)(statement => bindStrings(statement, distinctMerchantIds))

  private val listByRiderIdSql: String =
    s"""
       |$selectColumns
       |WHERE rider_id = ?
       |ORDER BY created_at DESC
       |""".stripMargin

  def listByRiderId(connection: Connection, riderId: RiderId): IO[List[Order]] =
    queryMany(connection, listByRiderIdSql)(_.setString(1, riderId))

  private val listByRefundStatusSql: String =
    s"""
       |$selectColumns
       |WHERE refund_status = ?
       |ORDER BY created_at DESC
       |""".stripMargin

  def listByRefundStatus(connection: Connection, status: RefundStatus): IO[List[Order]] =
    queryMany(connection, listByRefundStatusSql)(_.setString(1, status.toString))

  private val listRefundRequestsSql: String =
    s"""
       |$selectColumns
       |WHERE refund_status IS NOT NULL
       |ORDER BY created_at DESC
       |""".stripMargin

  def listRefundRequests(connection: Connection): IO[List[Order]] =
    queryMany(connection, listRefundRequestsSql)(_ => ())

  private val listAvailableUnassignedSql: String =
    s"""
       |$selectColumns
       |WHERE status = ? AND rider_id IS NULL
       |ORDER BY created_at DESC
       |""".stripMargin

  def listAvailableUnassigned(connection: Connection): IO[List[Order]] =
    queryMany(connection, listAvailableUnassignedSql)(_.setString(1, OrderStatus.待骑手接单.toString))

  def countActiveByRider(connection: Connection, riderId: RiderId, excludingOrderId: Option[OrderId] = None): IO[Int] =
    IO.blocking {
      val historyStatuses = OrderStatus.history.toList
      val statusPlaceholders = List.fill(historyStatuses.size)("?").mkString(", ")
      val excludingClause = excludingOrderId.map(_ => " AND id <> ?").getOrElse("")
      val statement = connection.prepareStatement(
        s"""
           |SELECT COUNT(*) AS active_count
           |FROM orders
           |WHERE rider_id = ? AND status NOT IN ($statusPlaceholders)$excludingClause
           |""".stripMargin
      )
      try
        statement.setString(1, riderId)
        historyStatuses.zipWithIndex.foreach { case (status, index) => statement.setString(index + 2, status.toString) }
        excludingOrderId.foreach(orderId => statement.setString(historyStatuses.size + 2, orderId))
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then resultSet.getInt("active_count")
          else 0
        finally resultSet.close()
      finally statement.close()
    }

  private val updateStatusSql: String =
    """
      |UPDATE orders
      |SET status = ?, updated_at = now()
      |WHERE id = ?
      |""".stripMargin

  private[order] def updateStatus(connection: Connection, id: OrderId, status: OrderStatus): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(updateStatusSql)
      try
        statement.setString(1, status.toString)
        statement.setString(2, id)
        statement.executeUpdate() == 1
      finally statement.close()
    }

  private val deleteSql: String =
    "DELETE FROM orders WHERE id = ?"

  private[order] def delete(connection: Connection, id: OrderId): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteSql)
      try
        statement.setString(1, id)
        statement.executeUpdate() == 1
      finally statement.close()
    }

  private def bindOrder(statement: PreparedStatement, order: Order): Unit =
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
    statement.setString(9, order.status.toString)
    statement.setString(10, order.placedAt)
    statement.setDouble(11, order.originalAmount)
    statement.setDouble(12, order.discountAmount)
    statement.setDouble(13, order.payableAmount)
    order.usedVoucher match
      case Some(value) => statement.setObject(14, jsonb(value.asJson.noSpaces))
      case None        => statement.setNull(14, java.sql.Types.OTHER)
    statement.setInt(15, order.pointsAwarded)
    order.refundStatus match
      case Some(value) => statement.setString(16, value.toString)
      case None        => statement.setNull(16, java.sql.Types.VARCHAR)
    order.refundReason match
      case Some(value) => statement.setString(17, value)
      case None        => statement.setNull(17, java.sql.Types.VARCHAR)
    order.refundImageUrl match
      case Some(value) => statement.setString(18, value)
      case None        => statement.setNull(18, java.sql.Types.VARCHAR)
    order.refundRequestedAt match
      case Some(value) => statement.setString(19, value)
      case None        => statement.setNull(19, java.sql.Types.VARCHAR)
    order.refundMerchantReason match
      case Some(value) => statement.setString(20, value)
      case None        => statement.setNull(20, java.sql.Types.VARCHAR)
    order.refundMerchantReviewedAt match
      case Some(value) => statement.setString(21, value)
      case None        => statement.setNull(21, java.sql.Types.VARCHAR)
    order.refundAdminReason match
      case Some(value) => statement.setString(22, value)
      case None        => statement.setNull(22, java.sql.Types.VARCHAR)
    order.refundedAt match
      case Some(value) => statement.setString(23, value)
      case None        => statement.setNull(23, java.sql.Types.VARCHAR)
    order.customerNoteText match
      case Some(value) => statement.setString(24, value)
      case None        => statement.setNull(24, java.sql.Types.VARCHAR)
    order.customerNoteImageUrl match
      case Some(value) => statement.setString(25, value)
      case None        => statement.setNull(25, java.sql.Types.VARCHAR)

  private def bindStrings(statement: PreparedStatement, values: List[String]): Unit =
    values.zipWithIndex.foreach { case (value, index) => statement.setString(index + 1, value) }

  private def queryOne(connection: Connection, sql: String)(bind: PreparedStatement => Unit): IO[Option[Order]] =
    queryMany(connection, sql)(bind).map(_.headOption)

  private def queryMany(connection: Connection, sql: String)(bind: PreparedStatement => Unit): IO[List[Order]] =
    IO.blocking {
      val statement = connection.prepareStatement(sql)
      try
        bind(statement)
        val resultSet = statement.executeQuery()
        try
          val builder = List.newBuilder[Order]
          while resultSet.next() do builder += readOrderRow(resultSet)
          builder.result()
        finally resultSet.close()
      finally statement.close()
    }.flatMap(attachItems(connection, _))

  private def attachItems(connection: Connection, orders: List[Order]): IO[List[Order]] =
    if orders.isEmpty then IO.pure(Nil)
    else
      OrderItemTable.listByOrderIds(connection, orders.map(_.id)).map { itemsByOrderId =>
        orders.map(order => order.copy(items = itemsByOrderId.getOrElse(order.id, Nil)))
      }

  private def readOrderRow(resultSet: ResultSet): Order =
    val totalAmount = resultSet.getBigDecimal("total_amount").doubleValue()
    val usedVoucher = Option(resultSet.getString("used_voucher")).flatMap(raw => decode[Voucher](raw).toOption)
    Order(
      id = resultSet.getString("id"),
      customerId = resultSet.getString("customer_id"),
      customerName = resultSet.getString("customer_name"),
      customerPhone = resultSet.getString("customer_phone"),
      merchantId = resultSet.getString("merchant_id"),
      riderId = Option(resultSet.getString("rider_id")),
      items = Nil,
      totalAmount = totalAmount,
      deliveryAddress = resultSet.getString("delivery_address"),
      status = OrderStatus.fromString(resultSet.getString("status")).getOrElse(OrderStatus.待商家接单),
      placedAt = resultSet.getString("placed_at"),
      originalAmount = Option(resultSet.getBigDecimal("original_amount")).map(_.doubleValue()).getOrElse(totalAmount),
      discountAmount = Option(resultSet.getBigDecimal("discount_amount")).map(_.doubleValue()).getOrElse(0),
      payableAmount = Option(resultSet.getBigDecimal("payable_amount")).map(_.doubleValue()).getOrElse(totalAmount),
      usedVoucher = usedVoucher,
      pointsAwarded = resultSet.getInt("points_awarded"),
      refundStatus = Option(resultSet.getString("refund_status")).flatMap(RefundStatus.fromString),
      refundReason = Option(resultSet.getString("refund_reason")),
      refundImageUrl = Option(resultSet.getString("refund_image_url")),
      refundRequestedAt = Option(resultSet.getString("refund_requested_at")),
      refundMerchantReason = Option(resultSet.getString("refund_merchant_reason")),
      refundMerchantReviewedAt = Option(resultSet.getString("refund_merchant_reviewed_at")),
      refundAdminReason = Option(resultSet.getString("refund_admin_reason")),
      refundedAt = Option(resultSet.getString("refunded_at")),
      customerNoteText = Option(resultSet.getString("customer_note_text")),
      customerNoteImageUrl = Option(resultSet.getString("customer_note_image_url"))
    )

  private def jsonb(value: String): PGobject =
    val pg = PGobject()
    pg.setType("jsonb")
    pg.setValue(value)
    pg

end OrderTable
