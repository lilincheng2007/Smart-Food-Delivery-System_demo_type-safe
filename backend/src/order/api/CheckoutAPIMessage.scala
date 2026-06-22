package delivery.order.api

import cats.effect.IO
import delivery.order.objects.{CheckoutLine}
import delivery.order.objects.apiTypes.{CheckoutRequest, CheckoutResponse, OrderMerchantNote}
import delivery.order.services.CheckoutCommandService
import delivery.platform.api.APIWithRoleMessage
import delivery.domain.VoucherId

import java.sql.Connection

final case class CheckoutAPIMessage(
    lines: List[CheckoutLine],
    customerName: Option[String],
    customerPhone: Option[String],
    deliveryAddress: Option[String],
    voucherId: Option[VoucherId],
    merchantNotes: List[OrderMerchantNote] = Nil
) extends APIWithRoleMessage[CheckoutResponse]:
  override def plan(connection: Connection, username: String): IO[CheckoutResponse] =
    val body = CheckoutRequest(lines, customerName, customerPhone, deliveryAddress, voucherId, merchantNotes)
    CheckoutCommandService.submitCheckout(connection, username, body)
