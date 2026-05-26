package delivery.user.api

import cats.effect.IO
import delivery.merchant.tables.MerchantDomainOps
import delivery.shared.api.ApiPlan
import delivery.shared.objects.{DeliveryState, OkResponse}
import delivery.shared.db.DeliveryStateOps
import delivery.rider.tables.RiderDomainOps
import delivery.user.objects.RegisterRequest
import delivery.user.tables.UserDomainOps
import delivery.user.utils.UserApiSupport
import org.typelevel.log4cats.slf4j.Slf4jLogger

object RegisterApi extends ApiPlan[RegisterApi.RegisterCommand, Either[String, RegisterApi.RegisterSuccess]]:

  final case class RegisterCommand(state: DeliveryState, body: RegisterRequest)

  private val logger = Slf4jLogger.getLogger[IO]

  override val name: String = "RegisterApi"

  override def plan(input: RegisterApi.RegisterCommand): IO[Either[String, RegisterApi.RegisterSuccess]] =
    for
      _ <- logger.info(s"$name started, username=${input.body.username}")
      response <- input.body.role match
        case "customer" =>
          UserDomainOps.registerCustomer(input.state.user, input.body.username, input.body.password).flatMap {
            case Left(msg) => IO.pure(Left(msg))
            case Right(nextUser) =>
              IO.pure(Right(RegisterSuccess(DeliveryStateOps.withUserState(input.state, nextUser), OkResponse(ok = true))))
          }
        case "merchant" =>
          UserDomainOps.registerMerchantCredential(input.state.user, input.body.username, input.body.password) match
            case Left(msg) => IO.pure(Left(msg))
            case Right(nextUser) =>
              MerchantDomainOps.bootstrapMerchant(input.state.merchant, input.body.username).flatMap {
                case Left(msg) => IO.pure(Left(msg))
                case Right(nextMerchant) =>
                  IO.pure(
                    Right(RegisterSuccess(DeliveryStateOps.withUserAndMerchantState(input.state, nextUser, nextMerchant), OkResponse(ok = true)))
                  )
              }
        case "rider" =>
          UserDomainOps.registerRiderCredential(input.state.user, input.body.username, input.body.password) match
            case Left(msg) => IO.pure(Left(msg))
            case Right(nextUser) =>
              RiderDomainOps.bootstrapRider(input.state.rider, input.body.username).flatMap {
                case Left(msg) => IO.pure(Left(msg))
                case Right(nextRider) =>
                  IO.pure(
                    Right(RegisterSuccess(DeliveryStateOps.withUserAndRiderState(input.state, nextUser, nextRider), OkResponse(ok = true)))
                  )
              }
        case _ => IO.pure(Left(UserApiSupport.invalidRole.error))
      _ <- logger.info(s"$name finished, success=${response.isRight}")
    yield response

  final case class RegisterSuccess(nextState: DeliveryState, response: OkResponse)

end RegisterApi
