package delivery.review.tables

import cats.effect.IO
import cats.syntax.all.*
import delivery.review.objects.{MerchantReview, ReviewSummary}
import delivery.domain.{MerchantId, OrderId}
import delivery.order.tables.orderitem.OrderItemTable

import java.sql.{Connection, ResultSet}

object MerchantReviewTable:
  private val insertSql: String =
    """
      |INSERT INTO merchant_reviews (id, order_id, merchant_id, customer_id, customer_name, rating, description, image_url)
      |VALUES (?, ?, ?, ?, ?, ?, ?, ?)
      |""".stripMargin

  def create(connection: Connection, review: MerchantReview): IO[MerchantReview] =
    IO.blocking {
      val statement = connection.prepareStatement(insertSql)
      try
        statement.setString(1, review.id)
        statement.setString(2, review.orderId)
        statement.setString(3, review.merchantId)
        statement.setString(4, review.customerId)
        statement.setString(5, review.customerName)
        statement.setInt(6, review.rating)
        statement.setString(7, review.description)
        review.imageUrl match
          case Some(value) => statement.setString(8, value)
          case None        => statement.setNull(8, java.sql.Types.VARCHAR)
        val _ = statement.executeUpdate()
        review
      finally statement.close()
    }

  private val findByOrderSql: String =
    "SELECT id, order_id, merchant_id, customer_id, customer_name, rating, description, image_url, merchant_reply, merchant_reply_at, created_at FROM merchant_reviews WHERE order_id = ?"

  def findByOrder(connection: Connection, orderId: OrderId): IO[Option[MerchantReview]] =
    IO.blocking {
      val statement = connection.prepareStatement(findByOrderSql)
      try
        statement.setString(1, orderId)
        val rs = statement.executeQuery()
        try if rs.next() then Some(read(rs, 0, 0)) else None
        finally rs.close()
      finally statement.close()
    }

  private val listByMerchantSql: String =
    """
      |SELECT r.id, r.order_id, r.merchant_id, r.customer_id, r.customer_name, r.rating, r.description, r.image_url,
      |       r.merchant_reply, r.merchant_reply_at, r.created_at,
      |       COALESCE(SUM(CASE WHEN v.vote = 'up' THEN 1 ELSE 0 END), 0) AS upvotes,
      |       COALESCE(SUM(CASE WHEN v.vote = 'down' THEN 1 ELSE 0 END), 0) AS downvotes
      |FROM merchant_reviews r
      |LEFT JOIN merchant_review_votes v ON v.review_id = r.id
      |WHERE r.merchant_id = ?
      |GROUP BY r.id
      |ORDER BY upvotes DESC, r.created_at DESC
      |""".stripMargin

  def listByMerchant(connection: Connection, merchantId: MerchantId): IO[List[MerchantReview]] =
    IO.blocking {
      val statement = connection.prepareStatement(listByMerchantSql)
      try
        statement.setString(1, merchantId)
        val rs = statement.executeQuery()
        try
          val b = List.newBuilder[MerchantReview]
          while rs.next() do b += read(rs, rs.getInt("upvotes"), rs.getInt("downvotes"))
          b.result()
        finally rs.close()
      finally statement.close()
    }.flatMap(attachOrderItemNames(connection, _))

  private val replySql: String =
    """
      |UPDATE merchant_reviews
      |SET merchant_reply = ?, merchant_reply_at = now()
      |WHERE id = ? AND merchant_id = ?
      |""".stripMargin

  def reply(connection: Connection, reviewId: String, merchantId: MerchantId, reply: String): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(replySql)
      try
        statement.setString(1, reply)
        statement.setString(2, reviewId)
        statement.setString(3, merchantId)
        statement.executeUpdate() > 0
      finally statement.close()
    }

  private val summarySql: String =
    "SELECT COALESCE(AVG(rating), 0) AS average_rating, COUNT(*) AS review_count FROM merchant_reviews WHERE merchant_id = ?"

  def summaryByMerchant(connection: Connection, merchantId: MerchantId): IO[ReviewSummary] =
    IO.blocking {
      val statement = connection.prepareStatement(summarySql)
      try
        statement.setString(1, merchantId)
        val rs = statement.executeQuery()
        try
          if rs.next() then
            val count = rs.getInt("review_count")
            ReviewSummary(if count == 0 then 5.0 else round1(rs.getDouble("average_rating")), count)
          else ReviewSummary(5.0, 0)
        finally rs.close()
      finally statement.close()
    }

  def summariesByMerchant(connection: Connection, merchantIds: List[MerchantId]): IO[Map[MerchantId, ReviewSummary]] =
    val ids = merchantIds.distinct
    if ids.isEmpty then IO.pure(Map.empty)
    else
      IO.blocking {
        val placeholders = List.fill(ids.size)("?").mkString(",")
        val statement = connection.prepareStatement(
          s"SELECT merchant_id, COALESCE(AVG(rating),0) AS average_rating, COUNT(*) AS review_count FROM merchant_reviews WHERE merchant_id IN ($placeholders) GROUP BY merchant_id"
        )
        try
          ids.zipWithIndex.foreach { case (id, index) => statement.setString(index + 1, id) }
          val rs = statement.executeQuery()
          try
            val b = Map.newBuilder[MerchantId, ReviewSummary]
            while rs.next() do
              val count = rs.getInt("review_count")
              b += rs.getString("merchant_id") -> ReviewSummary(if count == 0 then 5.0 else round1(rs.getDouble("average_rating")), count)
            b.result()
          finally rs.close()
        finally statement.close()
      }

  private def read(rs: ResultSet, upvotes: Int, downvotes: Int): MerchantReview =
    MerchantReview(
      id = rs.getString("id"),
      orderId = rs.getString("order_id"),
      merchantId = rs.getString("merchant_id"),
      customerId = rs.getString("customer_id"),
      customerName = rs.getString("customer_name"),
      rating = rs.getInt("rating"),
      description = rs.getString("description"),
      imageUrl = Option(rs.getString("image_url")),
      upvotes = upvotes,
      downvotes = downvotes,
      createdAt = rs.getTimestamp("created_at").toInstant.toString,
      merchantReply = Option(rs.getString("merchant_reply")),
      merchantReplyAt = Option(rs.getTimestamp("merchant_reply_at")).map(_.toInstant.toString)
    )

  private def attachOrderItemNames(connection: Connection, reviews: List[MerchantReview]): IO[List[MerchantReview]] =
    if reviews.isEmpty then IO.pure(Nil)
    else
      OrderItemTable.listByOrderIds(connection, reviews.map(_.orderId)).map { itemsByOrderId =>
        reviews.map { review =>
          val itemNames = itemsByOrderId.getOrElse(review.orderId, Nil).map(item => s"${item.name}×${item.quantity}")
          review.copy(orderItemNames = itemNames)
        }
      }

  private def round1(value: Double): Double =
    BigDecimal(value).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble
