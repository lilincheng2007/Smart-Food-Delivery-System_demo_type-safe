package delivery.order.api

import cats.effect.IO
import delivery.merchant.tables.MerchantDomainOps
import delivery.shared.api.ApiPlan
import delivery.order.objects.{CheckoutRequest, CheckoutResponse}
import delivery.order.tables.OrderDomainOps
import delivery.order.utils.OrderApiSupport
import delivery.shared.objects.DeliveryState
import delivery.shared.db.DeliveryStateOps
import delivery.user.objects.CheckoutCompleteRequest
import delivery.user.tables.UserDomainOps
import org.typelevel.log4cats.slf4j.Slf4jLogger

object CheckoutApi extends ApiPlan[CheckoutApi.CheckoutCommand, Either[CheckoutApi.CheckoutFailure, CheckoutApi.CheckoutSuccess]]:

  final case class CheckoutCommand(
      state: DeliveryState,
      username: String,
      body: CheckoutRequest
  )

  enum CheckoutFailure:
    case CustomerMissing
    case Invalid(msg: String)

  private val logger = Slf4jLogger.getLogger[IO]

  override val name: String = "CheckoutApi"

  override def plan(input: CheckoutApi.CheckoutCommand): IO[Either[CheckoutApi.CheckoutFailure, CheckoutApi.CheckoutSuccess]] =
    for
      _ <- logger.info(s"$name started, username=${input.username}")
      response <- input.state.user.customerAccounts.find(_.username == input.username) match
        case None => IO.pure(Left(CheckoutFailure.CustomerMissing))
        case Some(account) =>
          val profileForOrders =
            (input.body.customerName, input.body.customerPhone, input.body.deliveryAddress) match
              case (Some(n), Some(ph), Some(ad))
                  if n.trim.nonEmpty && ph.trim.nonEmpty && ad.trim.nonEmpty =>
                account.profile.copy(name = n.trim, phone = ph.trim, defaultAddress = ad.trim)
              case _ => account.profile
          OrderDomainOps.buildOrdersForCheckout(
            input.state.merchant.catalogProducts,
            profileForOrders,
            input.body.lines.map(OrderApiSupport.normalizeLine)
          ).flatMap {
            case Left(msg) => IO.pure(Left(CheckoutFailure.Invalid(msg)))
            case Right((orders, totalDebit)) =>
              UserDomainOps.checkoutComplete(input.state.user, CheckoutCompleteRequest(input.username, orders, totalDebit)) match
                case Left(msg) => IO.pure(Left(CheckoutFailure.Invalid(msg)))
                case Right(nextUser) =>
                  val nextOrder = OrderDomainOps.appendOrders(input.state.order, orders)
                  val nextMerchant = MerchantDomainOps.attachOrders(input.state.merchant, orders)
                  val next = DeliveryStateOps.withOrderAndMerchantAndUserState(input.state, nextUser, nextOrder, nextMerchant)
                  val wallet = nextUser.customerAccounts.find(_.username == input.username).map(_.profile.walletBalance).getOrElse(0d)
                  IO.pure(Right(CheckoutSuccess(next, CheckoutResponse(orders = orders, walletBalance = wallet))))
          }
      _ <- logger.info(s"$name finished, success=${response.isRight}")
    yield response

  final case class CheckoutSuccess(nextState: DeliveryState, response: CheckoutResponse)

end CheckoutApi
