package delivery.ai.api

import cats.effect.IO
import delivery.ai.objects.*
import delivery.ai.objects.apiTypes.*
import delivery.ai.utils.OpenAIClient
import delivery.order.objects.Order
import delivery.order.tables.order.OrderTable
import delivery.platform.api.{APIWithRoleMessage, HttpApiError}
import delivery.domain.OrderStatus
import delivery.user.tables.customerprofile.CustomerProfileTable
import io.circe.Json

import java.sql.Connection
import java.time.{LocalDate, LocalDateTime}
import java.time.format.DateTimeFormatter

final case class AIDietWeeklyReportAPIMessage() extends APIWithRoleMessage[AIDietWeeklyReportResponse]:

  private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

  override def plan(connection: Connection, username: String): IO[AIDietWeeklyReportResponse] =
    for
      _ <- OpenAIClient.configured.flatMap { ok =>
        if !ok then IO.raiseError(HttpApiError.BadRequest("AI 服务未配置，请联系管理员")) else IO.unit
      }
      account <- CustomerProfileTable.findByUsername(connection, username).flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(HttpApiError.NotFound("未找到顾客账号"))
      }
      allOrders <- OrderTable.list(connection)
      sevenDaysAgo = LocalDate.now().minusDays(7).atStartOfDay()
      recentOrders = allOrders.filter { order =>
        order.customerId == account.profile.id &&
        (order.status == OrderStatus.已送达 || order.status == OrderStatus.已完成) &&
        parsePlacedAt(order.placedAt).isAfter(sevenDaysAgo)
      }
      _ <- if recentOrders.isEmpty then IO.raiseError(HttpApiError.BadRequest("近7天暂无已完成订单，无法生成周报")) else IO.unit
      prompt = buildDietReportPrompt(recentOrders)
      userMessage = "请根据我最近7天的外卖订单，生成饮食周报"
      resultJson <- OpenAIClient.chatCompletion(prompt, userMessage)
      response <- parseDietResponse(resultJson)
    yield response

  private def parsePlacedAt(placedAt: String): LocalDateTime =
    try LocalDateTime.parse(placedAt, dateFormatter)
    catch case _: Exception => LocalDateTime.MIN

  private def buildDietReportPrompt(orders: List[Order]): String =
    val orderSummaries = orders.map { order =>
      val items = order.items.map(i =>
        s"""{"name":"${escapeJson(i.name)}","unitPrice":${i.unitPrice},"quantity":${i.quantity}}"""
      ).mkString("[", ",", "]")
      s"""{"merchantId":"${escapeJson(order.merchantId)}","items":$items,"totalAmount":${order.totalAmount},"payableAmount":${order.payableAmount},"placedAt":"${escapeJson(order.placedAt)}"}"""
    }.mkString("[", ",", "]")

    s"""你是一个外卖平台的营养分析助手。请根据用户最近 7 天的真实外卖订单数据，生成一份以“顾客饮食分析”和“可执行饮食建议”为核心的饮食周报。

订单数据：
$orderSummaries

请按以下 JSON 格式返回结果，不要返回其他内容：
{
  "summary": {
    "calorieTotal": "估算总热量（如 约8500千卡）",
    "orderCount": 订单数量,
    "topCategory": "根据菜品名称推断的最常点品类（如 中餐/西餐/饮品甜点/快餐）",
    "topMerchant": "最常点商家名称；如果订单数据没有商家名称，请写最常点商家ID"
  },
  "dietAnalysis": "顾客饮食分析。请用 3-5 句话总结近 7 天外卖饮食结构、口味偏好、用餐规律、热量/油盐/蔬菜蛋白摄入倾向，并明确指出 1-2 个做得好的地方和 1-2 个主要风险。",
  "nutritionAnalysis": [
    {
      "name": "营养素名称（蛋白质/碳水化合物/脂肪/膳食纤维/钠/糖 至少覆盖其中 4 项）",
      "amount": "估算摄入量或水平（如 约180g / 偏高）",
      "assessment": "良好/偏高/偏低/需关注"
    }
  ],
  "suggestions": [
    "饮食建议1：必须结合订单中的具体菜品或饮食模式，说明下一周如何调整",
    "饮食建议2：必须具体、可执行，如替换菜品、增加蔬菜、减少含糖饮料或控制夜宵频次",
    "饮食建议3：必须说明预期改善目标"
  ],
  "weeklyTrend": "本周饮食趋势文字描述（2-3句话），概括变化、优点和需要改进的地方",
  "generatedAt": "报告生成时间（yyyy-MM-dd HH:mm格式）"
}

要求：
1. 必须输出 dietAnalysis，且内容要像给该顾客的个性化分析，不能泛泛而谈
2. suggestions 给出 4-6 条实用建议，每条建议都要能直接执行
3. nutritionAnalysis 包含 4-6 个核心营养素，评估要客观，注意外卖饮食常见的高油、高盐、精制碳水、蔬菜不足、含糖饮料等问题
4. 如果订单样本较少，要明确说明不确定性，并基于已知订单给出温和建议
5. 不要进行医疗诊断，不要承诺治疗效果"""

  private def escapeJson(value: String): String =
    value
      .replace("\\", "\\\\")
      .replace("\"", "\\\"")
      .replace("\n", "\\n")
      .replace("\r", "\\r")

  private def parseDietResponse(json: Json): IO[AIDietWeeklyReportResponse] =
    val cursor = json.hcursor
    for
      summaryJson <- cursor.downField("summary").as[Json] match
        case Right(j) => IO.pure(j)
        case Left(_)  => IO.pure(Json.obj())
      sCursor = summaryJson.hcursor
      calorieTotal = sCursor.downField("calorieTotal").as[String].getOrElse("未知")
      orderCount = sCursor.downField("orderCount").as[Int].getOrElse(0)
      topCategory = sCursor.downField("topCategory").as[String].getOrElse("未知")
      topMerchant = sCursor.downField("topMerchant").as[String].getOrElse("未知")
      dietAnalysis = cursor.downField("dietAnalysis").as[String].getOrElse("本周订单样本有限，暂无法形成完整饮食结构分析。建议结合近 7 天已完成订单，关注主食、蛋白质、蔬菜和饮品的搭配是否均衡。")
      nutritionItems <- cursor.downField("nutritionAnalysis").as[List[Json]] match
        case Right(list) => IO.pure(list)
        case Left(_)     => IO.pure(List.empty[Json])
      nutritionAnalysis = nutritionItems.flatMap { nJson =>
        val nc = nJson.hcursor
        val name = nc.downField("name").as[String].getOrElse("")
        val amount = nc.downField("amount").as[String].getOrElse("")
        val assessment = nc.downField("assessment").as[String].getOrElse("")
        if name.nonEmpty then Some(DietNutritionItem(name, amount, assessment)) else None
      }
      suggestions <- cursor.downField("suggestions").as[List[String]] match
        case Right(list) => IO.pure(list)
        case Left(_)     => IO.pure(List.empty[String])
      weeklyTrend = cursor.downField("weeklyTrend").as[String].getOrElse("暂无趋势数据")
      generatedAt = cursor.downField("generatedAt").as[String].getOrElse(
        LocalDateTime.now().format(dateFormatter)
      )
    yield AIDietWeeklyReportResponse(
      DietWeeklySummary(calorieTotal, orderCount, topCategory, topMerchant),
      dietAnalysis,
      nutritionAnalysis,
      suggestions,
      weeklyTrend,
      generatedAt
    )

end AIDietWeeklyReportAPIMessage
