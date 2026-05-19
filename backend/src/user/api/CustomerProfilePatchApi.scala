package delivery.user.api

import cats.effect.IO
import delivery.shared.api.ApiPlan
import delivery.shared.objects.{DeliveryState, OkResponse}
import delivery.shared.db.DeliveryStateOps
import delivery.user.objects.CustomerProfilePatch
import delivery.user.tables.UserDomainOps
import org.typelevel.log4cats.slf4j.Slf4jLogger

object CustomerProfilePatchApi
    extends ApiPlan[CustomerProfilePatchApi.CustomerProfilePatchCommand, Either[String, CustomerProfilePatchApi.CustomerProfilePatchSuccess]]:

  final case class CustomerProfilePatchCommand(
      state: DeliveryState,
      username: String,
      body: CustomerProfilePatch
  )

  private val logger = Slf4jLogger.getLogger[IO]

  override val name: String = "CustomerProfilePatchApi"

  override def plan(input: CustomerProfilePatchApi.CustomerProfilePatchCommand): IO[Either[String, CustomerProfilePatchApi.CustomerProfilePatchSuccess]] =
    for
      _ <- logger.info(s"$name started, username=${input.username}")
      response <- UserDomainOps.patchCustomer(input.state.user, input.username, input.body) match
        case Left(msg) => IO.pure(Left(msg))
        case Right(nextUser) =>
          IO.pure(Right(CustomerProfilePatchSuccess(DeliveryStateOps.withUserState(input.state, nextUser), OkResponse(ok = true))))
      _ <- logger.info(s"$name finished, success=${response.isRight}")
    yield response

  final case class CustomerProfilePatchSuccess(nextState: DeliveryState, response: OkResponse)

end CustomerProfilePatchApi
