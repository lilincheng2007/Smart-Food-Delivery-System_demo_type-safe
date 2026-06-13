package delivery.review.tables

import cats.effect.IO
import delivery.review.objects.{ReviewSummary, RiderReview}
import delivery.domain.{OrderId, RiderId}

import java.sql.{Connection, ResultSet}

object RiderReviewTable:
  private val insertSql: String =
    "INSERT INTO rider_reviews (id, order_id, rider_id, customer_id, customer_name, rating) VALUES (?, ?, ?, ?, ?, ?)"

  def create(connection: Connection, review: RiderReview): IO[RiderReview] =
    IO.blocking {
      val statement = connection.prepareStatement(insertSql)
      try
        statement.setString(1, review.id)
        statement.setString(2, review.orderId)
        statement.setString(3, review.riderId)
        statement.setString(4, review.customerId)
        statement.setString(5, review.customerName)
        statement.setInt(6, review.rating)
        val _ = statement.executeUpdate()
        review
      finally statement.close()
    }

  def findByOrder(connection: Connection, orderId: OrderId): IO[Option[RiderReview]] =
    IO.blocking {
      val statement = connection.prepareStatement("SELECT id, order_id, rider_id, customer_id, customer_name, rating, created_at FROM rider_reviews WHERE order_id = ?")
      try
        statement.setString(1, orderId)
        val rs = statement.executeQuery()
        try if rs.next() then Some(read(rs)) else None
        finally rs.close()
      finally statement.close()
    }

  def listByRider(connection: Connection, riderId: RiderId): IO[List[RiderReview]] =
    IO.blocking {
      val statement = connection.prepareStatement("SELECT id, order_id, rider_id, customer_id, customer_name, rating, created_at FROM rider_reviews WHERE rider_id = ? ORDER BY created_at DESC")
      try
        statement.setString(1, riderId)
        val rs = statement.executeQuery()
        try
          val b = List.newBuilder[RiderReview]
          while rs.next() do b += read(rs)
          b.result()
        finally rs.close()
      finally statement.close()
    }

  def summaryByRider(connection: Connection, riderId: RiderId): IO[ReviewSummary] =
    IO.blocking {
      val statement = connection.prepareStatement("SELECT COALESCE(AVG(rating), 0) AS average_rating, COUNT(*) AS review_count FROM rider_reviews WHERE rider_id = ?")
      try
        statement.setString(1, riderId)
        val rs = statement.executeQuery()
        try
          if rs.next() then
            val count = rs.getInt("review_count")
            ReviewSummary(if count == 0 then 5.0 else round1(rs.getDouble("average_rating")), count)
          else ReviewSummary(5.0, 0)
        finally rs.close()
      finally statement.close()
    }

  private def read(rs: ResultSet): RiderReview =
    RiderReview(
      id = rs.getString("id"),
      orderId = rs.getString("order_id"),
      riderId = rs.getString("rider_id"),
      customerId = rs.getString("customer_id"),
      customerName = rs.getString("customer_name"),
      rating = rs.getInt("rating"),
      createdAt = rs.getTimestamp("created_at").toInstant.toString
    )

  private def round1(value: Double): Double =
    BigDecimal(value).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble
