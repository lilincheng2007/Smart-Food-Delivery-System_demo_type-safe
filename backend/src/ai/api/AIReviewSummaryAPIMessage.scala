package delivery.ai.api

import cats.effect.IO
import delivery.ai.objects.apiTypes.AIReviewSummaryResponse
import delivery.ai.utils.OpenAIClient
import delivery.merchant.api.CatalogAPIMessage
import delivery.review.tables.MerchantReviewTable
import delivery.platform.api.{APIWithRoleMessage, HttpApiError}
import delivery.domain.MerchantId
import io.circe.Json
import io.circe.syntax.*

import java.sql.Connection

final case class AIReviewSummaryAPIMessage(merchantId: MerchantId) extends APIWithRoleMessage[AIReviewSummaryResponse]:

  override def plan(connection: Connection, username: String): IO[AIReviewSummaryResponse] =
    for
      _ <- OpenAIClient.configured.flatMap { ok =>
        if !ok then IO.raiseError(HttpApiError.BadRequest("AI 服务未配置，请联系管理员")) else IO.unit
      }
      catalog <- CatalogAPIMessage().plan(connection)
      merchant <- IO.fromOption(catalog.merchants.find(_.id == merchantId))(HttpApiError.NotFound("未找到商家"))
      reviews <- MerchantReviewTable.listByMerchant(connection, merchantId)
      _ <- if reviews.isEmpty then IO.raiseError(HttpApiError.BadRequest("暂无评价，无法生成总结")) else IO.unit
      prompt = buildPrompt(merchant.storeName, reviews.length)
      userMessage = reviewPayload(merchantId, merchant.storeName, reviews).noSpaces
      resultJson <- OpenAIClient.chatCompletion(prompt, userMessage)
      response <- parseResponse(merchantId, merchant.storeName, reviews.length, resultJson)
    yield response

  private def reviewPayload(merchantId: MerchantId, storeName: String, reviews: List[delivery.review.objects.MerchantReview]): Json =
    Json.obj(
      "merchantId" -> merchantId.asJson,
      "storeName" -> storeName.asJson,
      "reviewCount" -> reviews.length.asJson,
      "reviews" -> reviews.take(120).map { review =>
        Json.obj(
          "rating" -> review.rating.asJson,
          "description" -> review.description.take(180).asJson,
          "orderItemNames" -> review.orderItemNames.take(6).asJson,
          "merchantReply" -> review.merchantReply.map(_.take(120)).asJson
        )
      }.asJson
    )

  private def buildPrompt(storeName: String, reviewCount: Int): String =
    s"""你是外卖平台的评价总结助手。请根据用户评价，为商家「$storeName」生成一段展示在评价列表顶部的“评价总结”。

请只返回 JSON，不要返回 Markdown 或其他内容：
{
  "summary": "一段中文评价总结",
  "highlights": ["需要强调的短语1", "短语2", "短语3"]
}

风格要求：
1. summary 模仿外卖 App 评价总结卡：自然、口语、有判断力，像在帮顾客快速读懂这家店。
2. 长度控制在 80-150 个中文字符；评价少时可以更短，但不要空泛。
3. 优先总结高频菜品、口味、分量、配送、服务、性价比、商家回复等真实评价要点。
4. 可使用“如果你是...爱好者，这家店值得一试！”这类句式，但要贴合评价事实。
5. 不要编造评价中没有出现的菜品或体验。差评明显时要客观提到问题。
6. highlights 给 2-4 个 summary 中原样出现的短语，用于橙色高亮；可以是菜品名、口味、配送速度快、分量足等。
7. 如果评价数量为 $reviewCount，也不要在 summary 正文里机械重复数量。"""

  private def parseResponse(merchantId: MerchantId, storeName: String, reviewCount: Int, json: Json): IO[AIReviewSummaryResponse] =
    val cursor = json.hcursor
    val summary = cursor.downField("summary").as[String].getOrElse("评价整体不错，顾客反馈集中在口味、分量和服务体验上，适合想快速了解店铺表现的用户参考。")
    val highlights = cursor.downField("highlights").as[List[String]].getOrElse(Nil)
      .map(_.trim)
      .filter(value => value.nonEmpty && summary.contains(value))
      .distinct
      .take(4)
    IO.pure(AIReviewSummaryResponse(merchantId, storeName, summary.trim, highlights, reviewCount))

end AIReviewSummaryAPIMessage
