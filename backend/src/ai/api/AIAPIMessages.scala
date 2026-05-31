package delivery.ai.api

import cats.effect.IO
import cats.syntax.all.*
import delivery.ai.objects.*
import delivery.ai.utils.OpenAIClient
import delivery.merchant.api.CatalogAPIMessage
import delivery.merchant.objects.{CatalogResponse, Merchant, Product}
import delivery.order.objects.Order
import delivery.order.tables.order.OrderTable
import delivery.shared.api.{APIWithRoleMessage, HttpApiError, sendAPI}
import delivery.shared.db.DatabaseSession
import delivery.shared.objects.OrderStatus
import delivery.user.tables.customerprofile.CustomerProfileTable
import io.circe.Json

import java.sql.Connection
import java.time.{LocalDate, LocalDateTime}
import java.time.format.DateTimeFormatter
import javax.sql.DataSource

final case class AISearchAPIMessage(query: String) extends APIWithRoleMessage[AISearchResponse]:

  override def plan(connection: Connection, username: String): IO[AISearchResponse] =
    if query.trim.isEmpty then IO.raiseError(HttpApiError.BadRequest("搜索内容不能为空"))
    else
      for
        _ <- OpenAIClient.configured.flatMap { ok =>
          if !ok then IO.raiseError(HttpApiError.BadRequest("AI 服务未配置，请联系管理员")) else IO.unit
        }
        catalog <- CatalogAPIMessage().plan(connection)
        prompt = buildPrompt(catalog.merchants, catalog.products)
        resultJson <- OpenAIClient.chatCompletion(prompt, query.trim)
        response <- parseResponse(query.trim, resultJson, catalog.merchants, catalog.products)
      yield response

  private def buildPrompt(merchants: List[Merchant], products: List[Product]): String =
    val merchantSummaries = merchants.map { m =>
      val merchantProducts = products.filter(_.merchantId == m.id).map(p =>
        s"""{"id":"${p.id}","name":"${p.name}","price":${p.price},"desc":"${p.description.take(50)}","sales":${p.monthlySales}}"""
      ).mkString("[", ",", "]")
      s"""{"id":"${m.id}","name":"${m.storeName}","category":"${m.category}","rating":${m.rating},"tags":[${m.tags.mkString("\"","\",\"","\"")}],"products":$merchantProducts}"""
    }.mkString("[", ",", "]")

    s"""你是一个外卖平台的智能推荐助手。根据用户的饮食需求，从以下商家和菜品数据中推荐最匹配的商家和菜品组合。

商家和菜品数据：
$merchantSummaries

请按以下 JSON 格式返回推荐结果，不要返回其他内容：
{
  "merchants": [
    {
      "merchantId": "商家ID",
      "storeName": "商家名称",
      "category": "商家分类",
      "reason": "推荐理由（简短一句话）",
      "products": [
        {
          "productId": "菜品ID",
          "productName": "菜品名称",
          "price": 菜品价格,
          "reason": "推荐理由（简短一句话）"
        }
      ]
    }
  ],
  "summary": "一句话总结推荐"
}

要求：
1. 最多推荐 5 家商家
2. 每家商家推荐 2-5 个菜品
3. merchantId 和 productId 必须来自上面提供的数据
4. 优先推荐评分高、销量好的商家和菜品
5. reason 要针对用户需求给出有说服力的推荐理由"""

  private def parseResponse(
      query: String,
      json: Json,
      merchants: List[Merchant],
      products: List[Product]
  ): IO[AISearchResponse] =
    val merchantIds = merchants.map(_.id).toSet
    val productIds = products.map(_.id).toSet

    val cursor = json.hcursor
    for
      summary <- cursor.downField("summary").as[String] match
        case Right(s) => IO.pure(s)
        case Left(_) => IO.pure("为您推荐以下商家")
      merchantResults <- cursor.downField("merchants").as[List[Json]] match
        case Right(list) => IO.pure(list)
        case Left(_) => IO.pure(List.empty[Json])
      recommendations = merchantResults.flatMap { mJson =>
        val mc = mJson.hcursor
        val mId = mc.downField("merchantId").as[String].getOrElse("")
        if !merchantIds.contains(mId) then None
        else
          val storeName = mc.downField("storeName").as[String].getOrElse(
            merchants.find(_.id == mId).map(_.storeName).getOrElse("")
          )
          val category = mc.downField("category").as[String].getOrElse(
            merchants.find(_.id == mId).map(_.category.toString).getOrElse("")
          )
          val mReason = mc.downField("reason").as[String].getOrElse("")
          val productResults = mc.downField("products").as[List[Json]].getOrElse(List.empty).flatMap { pJson =>
            val pc = pJson.hcursor
            val pId = pc.downField("productId").as[String].getOrElse("")
            if !productIds.contains(pId) then None
            else
              val pName = pc.downField("productName").as[String].getOrElse(
                products.find(_.id == pId).map(_.name).getOrElse("")
              )
              val price = pc.downField("price").as[Double].getOrElse(
                products.find(_.id == pId).map(_.price).getOrElse(0.0)
              )
              val pReason = pc.downField("reason").as[String].getOrElse("")
              Some(AIRecommendedProduct(pId, pName, price, pReason))
          }
          if productResults.isEmpty then None
          else Some(AIRecommendedMerchant(mId, storeName, category, mReason, productResults))
      }
    yield AISearchResponse(query, recommendations, summary)

end AISearchAPIMessage

final case class AIOrderProgressNarrativesAPIMessage() extends APIWithRoleMessage[AIOrderProgressNarrativesResponse]:

  private val progressStatuses = List(
    OrderStatus.待接单,
    OrderStatus.制作中,
    OrderStatus.配送中,
    OrderStatus.已送达,
    OrderStatus.已取消
  )
  private val progressStatusDescriptions: Map[OrderStatus, String] = Map(
    OrderStatus.待接单 -> "商家已完成出餐，餐点已打包，正在等待骑手接单/取餐；不要写成等待商家接单、厨房接单或刚下单",
    OrderStatus.制作中 -> "商家已接单，后厨正在制作餐品",
    OrderStatus.配送中 -> "骑手已接单并取餐，正在配送途中",
    OrderStatus.已送达 -> "餐品已送达顾客手中，等待顾客确认完成",
    OrderStatus.已取消 -> "订单已取消，流程结束"
  )
  private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

  override def plan(connection: Connection, username: String): IO[AIOrderProgressNarrativesResponse] =
    val generatedAt = LocalDateTime.now().format(dateFormatter)
    val generatedFor = LocalDate.now().toString
    OpenAIClient.configured.flatMap { ok =>
      if !ok then IO.pure(fallbackResponse(generatedAt, generatedFor))
      else
        OpenAIClient
          .chatCompletion(buildProgressPrompt, "请为顾客订单进度生成今日叙事条文案")
          .flatMap(json => parseProgressResponse(json, generatedAt, generatedFor))
          .handleError(_ => fallbackResponse(generatedAt, generatedFor))
    }

  private def buildProgressPrompt: String =
    val statuses = progressStatuses.map(_.toString).mkString("、")
    val statusDescriptions = progressStatuses.map(status => s"- ${status.toString}：${progressStatusDescriptions(status)}").mkString("\n")
    val exampleStatus = OrderStatus.制作中.toString
    s"""你是一个外卖平台的订单进度文案助手。请为顾客端订单卡片生成轻松、有趣、短句风格的订单进度叙事条。

订单状态：$statuses

订单状态含义：
$statusDescriptions

请按以下 JSON 格式返回结果，不要返回其他内容：
{
  "groups": [
    {
      "status": "$exampleStatus",
      "messages": ["厨师正在颠勺，香气已经开始集合啦"]
    }
  ]
}

要求：
1. 每个状态必须生成 10 条 messages
2. status 必须严格来自订单状态列表
3. 不要为“已完成”生成文案
4. 每条文案 10-24 个中文字符左右，适合显示在订单卡片中
5. 语气有趣但不夸张，不承诺准确时间
6. “待接单”必须表达已出餐、已打包、等待骑手接单/取餐，不能写等待商家确认或厨房准备
7. “制作中”可以包含厨师颠勺、备餐、热锅等画面；“配送中”可以包含骑手、路线、风声等画面"""

  private def parseProgressResponse(
      json: Json,
      generatedAt: String,
      generatedFor: String
  ): IO[AIOrderProgressNarrativesResponse] =
    val cursor = json.hcursor
    val fallbackGroups = fallbackResponse(generatedAt, generatedFor).groups
    for
      groupJsons <- cursor.downField("groups").as[List[Json]] match
        case Right(list) => IO.pure(list)
        case Left(_)     => IO.pure(List.empty[Json])
      aiGroups = groupJsons.flatMap { groupJson =>
        val groupCursor = groupJson.hcursor
        val statusText = groupCursor.downField("status").as[String].getOrElse("")
        val messages = groupCursor.downField("messages").as[List[String]].getOrElse(List.empty)
        OrderStatus.fromString(statusText).filter(progressStatuses.contains).map(status =>
          AIOrderProgressNarrativeGroup(status, messages.map(_.trim).filter(_.nonEmpty).take(10))
        )
      }
      groups = progressStatuses.map { status =>
        val aiMessages = aiGroups.find(_.status == status).map(_.messages).getOrElse(List.empty)
        val fallbackMessages = fallbackGroups.find(_.status == status).map(_.messages).getOrElse(List.empty)
        AIOrderProgressNarrativeGroup(status, (aiMessages ++ fallbackMessages).distinct.take(10))
      }
    yield AIOrderProgressNarrativesResponse(groups, generatedAt, generatedFor)

  private def fallbackResponse(generatedAt: String, generatedFor: String): AIOrderProgressNarrativesResponse =
    AIOrderProgressNarrativesResponse(
      List(
        AIOrderProgressNarrativeGroup(
          OrderStatus.待接单,
          List(
            "餐点已出炉等待骑手接棒",
            "美味已打包静候骑手",
            "商家已备好热乎这一单",
            "取餐台上美味正在待命",
            "骑手接单铃声即将响起",
            "餐盒已就位等待出发",
            "出餐完成美味准备启程",
            "厨房已交棒等待骑手",
            "热乎餐点正在等骑手",
            "美味已整装等待配送"
          )
        ),
        AIOrderProgressNarrativeGroup(
          OrderStatus.制作中,
          List(
            "厨师正在颠勺，香气上线",
            "热锅已就位，美味正在集合",
            "厨房烟火气正在认真营业",
            "食材们正在锅里开派对",
            "主厨正在给美味加点火候",
            "锅铲正在敲出今日节拍",
            "后厨能量条正在稳步上涨",
            "香味正在从厨房偷偷出发",
            "厨师小队正在精准备餐",
            "这份美味正在接受热锅淬炼"
          )
        ),
        AIOrderProgressNarrativeGroup(
          OrderStatus.配送中,
          List(
            "骑手带着美味正在路上",
            "外卖小火箭正在靠近你",
            "餐盒系好安全带出发啦",
            "风里有饭香正在向你移动",
            "骑手正在穿越城市地图",
            "美味快递正在加速抵达",
            "你的餐点正在看沿途风景",
            "配送路线正在认真推进",
            "餐盒正在向餐桌发起冲刺",
            "热乎乎的期待正在靠近"
          )
        ),
        AIOrderProgressNarrativeGroup(
          OrderStatus.已送达,
          List(
            "美味已抵达，请准备开动",
            "餐盒完成最后一段旅程",
            "饭香已经停靠在你身边",
            "这份期待已顺利送达",
            "美味敲门成功，开饭啦",
            "外卖旅程到达幸福终点",
            "餐点已就位，筷子可上场",
            "热乎心情已经送到门口",
            "美味任务完成，准备享用",
            "你的餐桌迎来今日主角"
          )
        ),
        AIOrderProgressNarrativeGroup(
          OrderStatus.已取消,
          List(
            "订单小剧场已暂停演出",
            "这趟美食列车临时停靠",
            "厨房任务已温柔收工",
            "本次美味计划先告一段落",
            "订单已取消，期待下次相遇",
            "餐盒旅程还没出发就返航啦",
            "美食按钮已切回待命状态",
            "这次点单灵感先收藏起来",
            "订单小票已安静退场",
            "美味计划改日继续登场"
          )
        )
      ),
      generatedAt,
      generatedFor
    )

end AIOrderProgressNarrativesAPIMessage

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
        s"""{"name":"${i.name}","unitPrice":${i.unitPrice},"quantity":${i.quantity}}"""
      ).mkString("[", ",", "]")
      s"""{"merchantId":"${order.merchantId}","items":$items,"totalAmount":${order.totalAmount},"placedAt":"${order.placedAt}"}"""
    }.mkString("[", ",", "]")

    s"""你是一个外卖平台的营养分析助手。根据用户最近7天的外卖订单数据，生成一份饮食周报。

订单数据：
$orderSummaries

请按以下 JSON 格式返回结果，不要返回其他内容：
{
  "summary": {
    "calorieTotal": "估算总热量（如 约8500千卡）",
    "orderCount": 订单数量,
    "topCategory": "最常点品类（如 中餐）",
    "topMerchant": "最常点商家名称"
  },
  "nutritionAnalysis": [
    {
      "name": "营养素名称（如 蛋白质/碳水化合物/脂肪/膳食纤维/钠）",
      "amount": "估算摄入量（如 约180g）",
      "assessment": "评估（良好/偏高/偏低）"
    }
  ],
  "suggestions": [
    "针对性饮食建议1",
    "针对性饮食建议2",
    "针对性饮食建议3"
  ],
  "weeklyTrend": "本周饮食趋势文字描述（2-3句话）",
  "generatedAt": "报告生成时间（yyyy-MM-dd HH:mm格式）"
}

要求：
1. 基于订单菜品名称和分量合理估算营养数据
2. nutritionAnalysis 包含 4-6 个核心营养素
3. suggestions 给出 3-5 条实用的饮食改善建议
4. 评估要客观，注意外卖饮食常见的营养不均衡问题
5. weeklyTrend 要指出饮食中的优点和需要改进的地方"""

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
      nutritionAnalysis,
      suggestions,
      weeklyTrend,
      generatedAt
    )

end AIDietWeeklyReportAPIMessage
