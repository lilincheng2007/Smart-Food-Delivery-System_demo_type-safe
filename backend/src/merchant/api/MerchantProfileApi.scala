package delivery.merchant.api

import cats.effect.IO
import delivery.shared.api.ApiPlan
import delivery.merchant.objects.MerchantProfileBody
import delivery.merchant.tables.MerchantDomainOps
import delivery.shared.objects.{DeliveryState, OkResponse}
import delivery.shared.db.DeliveryStateOps
import org.typelevel.log4cats.slf4j.Slf4jLogger

object MerchantProfileApi extends ApiPlan[MerchantProfileApi.MerchantProfileCommand, Either[String, MerchantProfileApi.MerchantProfileSuccess]]:

  final case class MerchantProfileCommand(
      state: DeliveryState,
      username: String,
      body: MerchantProfileBody
  )

  private val logger = Slf4jLogger.getLogger[IO]

  override val name: String = "MerchantProfileApi"

  override def plan(input: MerchantProfileApi.MerchantProfileCommand): IO[Either[String, MerchantProfileApi.MerchantProfileSuccess]] =
    for
      _ <- logger.info(s"$name started, username=${input.username}")
      response <- MerchantDomainOps.replaceMerchantProfile(input.state.merchant, input.username, input.body.profile) match
        case Left(msg) => IO.pure(Left(msg))
        case Right(nextMerchant) =>
          IO.pure(Right(MerchantProfileSuccess(DeliveryStateOps.withMerchantState(input.state, nextMerchant), OkResponse(ok = true))))
      _ <- logger.info(s"$name finished, success=${response.isRight}")
    yield response

  final case class MerchantProfileSuccess(nextState: DeliveryState, response: OkResponse)

end MerchantProfileApi
