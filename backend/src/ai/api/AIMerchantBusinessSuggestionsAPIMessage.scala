package delivery.ai.api

import cats.effect.IO
import delivery.ai.objects.apiTypes.AIMerchantBusinessSuggestionsResponse
import delivery.ai.utils.OpenAIClient
import delivery.merchant.objects.{Merchant, Product}
import delivery.merchant.utils.MerchantApiSupport
import delivery.order.objects.Order
import delivery.order.tables.order.OrderTable
import delivery.review.tables.MerchantReviewTable
import delivery.platform.api.{APIWithRoleMessage, HttpApiError}
import delivery.domain.{MerchantId, OrderStatus, RefundStatus}
import io.circe.Json

import java.sql.Connection
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

final case class AIMerchantBusinessSuggestionsAPIMessage(merchantId: MerchantId) extends APIWithRoleMessage[AIMerchantBusinessSuggestionsResponse]:

  private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

  override def plan(connection: Connection, username: String): IO[AIMerchantBusinessSuggestionsResponse] =
    for
      _ <- OpenAIClient.configured.flatMap { ok =>
        if !ok then IO.raiseError(HttpApiError.BadRequest("AI 服务未配置，请联系管理员")) else IO.unit
      }
      merchant <- MerchantApiSupport.requireOwnedStore(connection, username, merchantId)
      products <- MerchantApiSupport.listOwnedProducts(connection, username, merchantId)
      orders <- OrderTable.listByMerchantIds(connection, List(merchantId))
      reviews <- MerchantReviewTable.listByMerchant(connection, merchantId)
      prompt = buildPrompt(merchant, products, orders, reviews)
      resultJson <- OpenAIClient.chatCompletion(prompt, "请根据店铺近 7 天经营数据生成商家经营建议")
      response <- parseResponse(resultJson, merchantId)
    yield response

  private def buildPrompt(
      merchant: Merchant,
      products: List[Product],
      orders: List[Order],
      reviews: List[delivery.review.objects.MerchantReview]
  ): String =
    val now = LocalDateTime.now()
    val todayStart = now.toLocalDate.atStartOfDay()
    val thisWeekStart = todayStart.minusDays(6)
    val tomorrowStart = todayStart.plusDays(1)
    val lastWeekStart = thisWeekStart.minusDays(7)
    val thisWeekOrders = orders.filter(order => inRange(order, thisWeekStart, tomorrowStart))
    val lastWeekOrders = orders.filter(order => inRange(order, lastWeekStart, thisWeekStart))
    val validRevenueOrder = (order: Order) => order.status != OrderStatus.已取消 && order.status != OrderStatus.已退款
    val thisWeekRevenue = thisWeekOrders.filter(validRevenueOrder).map(order => receivableAmount(order)).sum
    val lastWeekRevenue = lastWeekOrders.filter(validRevenueOrder).map(order => receivableAmount(order)).sum
    val refundCount = thisWeekOrders.count(order => order.status == OrderStatus.已退款 || order.refundStatus.contains(RefundStatus.已通过))
    val lowStockProducts = products.filter(_.remainingStock < 10).sortBy(_.remainingStock).take(5)
    val topProducts = productSales(orders).take(5)
    val badReviews = reviews.filter(_.rating <= 2).take(8)

    val topProductsText =
      if topProducts.isEmpty then "暂无销量数据"
      else topProducts.map { case (name, quantity) => s"""{"name":"${escapeText(name)}","quantity":$quantity}""" }.mkString("[", ",", "]")
    val lowStockText =
      if lowStockProducts.isEmpty then "暂无低库存菜品"
      else lowStockProducts.map(product => s"""{"name":"${escapeText(product.name)}","remainingStock":${product.remainingStock}}""").mkString("[", ",", "]")
    val badReviewText =
      if badReviews.isEmpty then "暂无低评分评价"
      else badReviews.map(review => s"""{"rating":${review.rating},"description":"${escapeText(review.description).take(120)}","items":"${escapeText(review.orderItemNames.mkString("、"))}"}""").mkString("[", ",", "]")

    s"""你是外卖平台的商家经营分析顾问。请基于真实店铺数据生成经营建议，不要编造数据。

店铺信息：
{"merchantId":"${merchant.id}","storeName":"${escapeText(merchant.storeName)}","category":"${merchant.category}","description":"${escapeText(merchant.description).take(160)}"}

近 7 天经营数据：
{"orderCount":${thisWeekOrders.length},"lastPeriodOrderCount":${lastWeekOrders.length},"revenue":${roundMoney(thisWeekRevenue)},"lastPeriodRevenue":${roundMoney(lastWeekRevenue)},"refundCount":$refundCount}

热销菜品：
$topProductsText

低库存菜品：
$lowStockText

近期低评分评价：
$badReviewText

请按以下 JSON 格式返回，不要返回其他内容：
{
  "summary": "1-2 句话总结经营状态",
  "suggestions": [
    "具体经营建议1",
    "具体经营建议2",
    "具体经营建议3",
    "具体经营建议4"
  ]
}

要求：
1. suggestions 返回 3-5 条，每条 18-45 个中文字符
2. 建议必须能从订单、营业额、热销、库存、退款或评价数据中得到依据
3. 不要声称已经执行了促销、补货或人员安排
4. 如果数据不足，要明确建议继续观察，不要编造趋势
5. 语气像给商家看的经营建议，具体、克制、可行动"""

  private def parseResponse(json: Json, merchantId: MerchantId): IO[AIMerchantBusinessSuggestionsResponse] =
    val cursor = json.hcursor
    val summary = cursor.downField("summary").as[String].getOrElse("AI 已分析当前店铺数据，请结合订单、库存与评价变化安排经营动作。").trim.take(160)
    val suggestions = cursor.downField("suggestions").as[List[String]].getOrElse(Nil).map(_.trim).filter(_.nonEmpty).distinct.take(5)
    val normalizedSuggestions =
      if suggestions.nonEmpty then suggestions
      else List("当前数据不足以判断明确趋势，建议持续观察近 7 天订单与评价变化。")
    IO.pure(
      AIMerchantBusinessSuggestionsResponse(
        merchantId = merchantId,
        summary = summary,
        suggestions = normalizedSuggestions,
        generatedAt = LocalDateTime.now().format(dateFormatter)
      )
    )

  private def inRange(order: Order, start: LocalDateTime, end: LocalDateTime): Boolean =
    val placedAt = parsePlacedAt(order.placedAt)
    !placedAt.isBefore(start) && placedAt.isBefore(end)

  private def parsePlacedAt(value: String): LocalDateTime =
    val normalized = value.trim.replace("T", " ").take(16)
    try LocalDateTime.parse(normalized, dateFormatter)
    catch case _: Exception =>
      try LocalDateTime.parse(value)
      catch case _: Exception => LocalDateTime.of(1970, 1, 1, 0, 0)

  private def receivableAmount(order: Order): Double =
    if order.merchantReceivableAmount > 0 then order.merchantReceivableAmount
    else if order.payableAmount > 0 then order.payableAmount
    else order.totalAmount

  private def productSales(orders: List[Order]): List[(String, Int)] =
    orders
      .filter(order => order.status != OrderStatus.已取消 && order.status != OrderStatus.已退款)
      .flatMap(_.items)
      .groupMapReduce(_.name)(_.quantity)(_ + _)
      .toList
      .sortBy { case (_, quantity) => -quantity }

  private def roundMoney(value: Double): Double =
    BigDecimal(value).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble

  private def escapeText(value: String): String =
    value.replace("\n", " ").replace("\"", "'").trim
