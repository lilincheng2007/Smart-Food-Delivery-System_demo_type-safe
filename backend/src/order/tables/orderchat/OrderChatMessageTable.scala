package delivery.order.tables.orderchat

import cats.effect.IO
import delivery.order.objects.{OrderChatMessage, OrderChatMessageType, OrderChatRole, OrderChatUnreadCount}
import delivery.domain.OrderId

import java.sql.{Connection, ResultSet}

object OrderChatMessageTable:
  private val insertSql: String =
    """
      |INSERT INTO order_chat_messages (id, order_id, sender_role, peer_role, message_type, content)
      |VALUES (?, ?, ?, ?, ?, ?)
      |""".stripMargin

  def create(connection: Connection, message: OrderChatMessage): IO[OrderChatMessage] =
    IO.blocking {
      val statement = connection.prepareStatement(insertSql)
      try
        statement.setString(1, message.id)
        statement.setString(2, message.orderId)
        statement.setString(3, message.senderRole.toString)
        statement.setString(4, message.peerRole.toString)
        statement.setString(5, message.messageType.toString)
        statement.setString(6, message.content)
        statement.executeUpdate()
        ()
      finally statement.close()
    }.map(_ => message)

  def listForPair(connection: Connection, orderId: OrderId, roleA: OrderChatRole, roleB: OrderChatRole): IO[List[OrderChatMessage]] =
    IO.blocking {
      val statement = connection.prepareStatement(
        """
          |SELECT id, order_id, sender_role, peer_role, message_type, content, created_at, read_at
          |FROM order_chat_messages
          |WHERE order_id = ? AND (
          |  (sender_role = ? AND peer_role = ?) OR
          |  (sender_role = ? AND peer_role = ?)
          |)
          |ORDER BY created_at ASC
          |""".stripMargin
      )
      try
        statement.setString(1, orderId)
        statement.setString(2, roleA.toString)
        statement.setString(3, roleB.toString)
        statement.setString(4, roleB.toString)
        statement.setString(5, roleA.toString)
        val rs = statement.executeQuery()
        try
          val b = List.newBuilder[OrderChatMessage]
          while rs.next() do b += read(rs)
          b.result()
        finally rs.close()
      finally statement.close()
    }

  def markReadForPair(connection: Connection, orderId: OrderId, currentRole: OrderChatRole, peerRole: OrderChatRole): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(
        """
          |UPDATE order_chat_messages
          |SET read_at = now()
          |WHERE order_id = ? AND sender_role = ? AND peer_role = ? AND read_at IS NULL
          |""".stripMargin
      )
      try
        statement.setString(1, orderId)
        statement.setString(2, peerRole.toString)
        statement.setString(3, currentRole.toString)
        val _ = statement.executeUpdate()
        ()
      finally statement.close()
    }

  def unreadCountsForOrders(connection: Connection, currentRole: OrderChatRole, orderIds: List[OrderId]): IO[List[OrderChatUnreadCount]] =
    val distinctOrderIds = orderIds.distinct
    if distinctOrderIds.isEmpty then IO.pure(Nil)
    else
      val placeholders = List.fill(distinctOrderIds.size)("?").mkString(", ")
      IO.blocking {
        val statement = connection.prepareStatement(
          s"""
             |SELECT
             |  order_id,
             |  sender_role,
             |  COUNT(*) AS unread_count,
             |  (array_agg(message_type ORDER BY created_at DESC))[1] AS latest_message_type,
             |  (array_agg(content ORDER BY created_at DESC))[1] AS latest_content
             |FROM order_chat_messages
             |WHERE peer_role = ? AND read_at IS NULL AND order_id IN ($placeholders)
             |GROUP BY order_id, sender_role
             |""".stripMargin
        )
        try
          statement.setString(1, currentRole.toString)
          distinctOrderIds.zipWithIndex.foreach { case (orderId, index) => statement.setString(index + 2, orderId) }
          val rs = statement.executeQuery()
          try
            val b = List.newBuilder[OrderChatUnreadCount]
            while rs.next() do
              b += OrderChatUnreadCount(
                orderId = rs.getString("order_id"),
                peerRole = OrderChatRole.fromString(rs.getString("sender_role")).getOrElse(OrderChatRole.customer),
                unreadCount = rs.getInt("unread_count"),
                latestMessageType = Option(rs.getString("latest_message_type")).flatMap(OrderChatMessageType.fromString),
                latestContent = Option(rs.getString("latest_content")),
                latestCreatedAt = Option(rs.getTimestamp("latest_created_at")).map(_.toInstant.toString)
              )
            b.result()
          finally rs.close()
        finally statement.close()
      }

  private def read(rs: ResultSet): OrderChatMessage =
    OrderChatMessage(
      id = rs.getString("id"),
      orderId = rs.getString("order_id"),
      senderRole = OrderChatRole.fromString(rs.getString("sender_role")).getOrElse(OrderChatRole.customer),
      peerRole = OrderChatRole.fromString(rs.getString("peer_role")).getOrElse(OrderChatRole.customer),
      messageType = OrderChatMessageType.fromString(rs.getString("message_type")).getOrElse(OrderChatMessageType.text),
      content = rs.getString("content"),
      createdAt = rs.getTimestamp("created_at").toInstant.toString,
      readAt = Option(rs.getTimestamp("read_at")).map(_.toInstant.toString)
    )

end OrderChatMessageTable
