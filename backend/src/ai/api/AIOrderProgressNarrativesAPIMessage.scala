package delivery.ai.api

import cats.effect.IO
import delivery.ai.objects.*
import delivery.ai.objects.apiTypes.*
import delivery.ai.utils.OpenAIClient
import delivery.shared.api.APIWithRoleMessage
import delivery.shared.objects.OrderStatus
import io.circe.Json

import java.sql.Connection
import java.time.{LocalDate, LocalDateTime}
import java.time.format.DateTimeFormatter

final case class AIOrderProgressNarrativesAPIMessage() extends APIWithRoleMessage[AIOrderProgressNarrativesResponse]:

  private val progressStatuses = List(
    OrderStatus.待商家接单,
    OrderStatus.制作中,
    OrderStatus.待骑手接单,
    OrderStatus.配送中,
    OrderStatus.已送达,
    OrderStatus.已取消
  )
  private val progressStatusDescriptions: Map[OrderStatus, String] = Map(
    OrderStatus.待商家接单 -> "顾客已付款并提交订单，正在等待商家确认接单；不能写成等待骑手或已经出餐",
    OrderStatus.制作中 -> "商家已接单，后厨正在制作餐品",
    OrderStatus.待骑手接单 -> "商家已完成出餐，餐点已打包，正在等待骑手接单/取餐；不能写成等待商家确认或厨房刚接单",
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
6. “待商家接单”必须表达等待商家确认，不能写等待骑手、已出餐或正在配送
7. “待骑手接单”必须表达已出餐、已打包、等待骑手接单/取餐，不能写等待商家确认或厨房准备
8. “制作中”可以包含厨师颠勺、备餐、热锅等画面；“配送中”可以包含骑手、路线、风声等画面"""

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
          OrderStatus.待商家接单,
          List(
            "订单已到店等待确认",
            "商家正在查看你的点单",
            "美味申请等待店家接收",
            "小票已送达商家前台",
            "店家确认按钮即将亮起",
            "订单正在排队等店家回应",
            "你的点单正在等待接待",
            "商家确认中请稍候片刻",
            "美食邀约正在等待回复",
            "订单已提交静候商家接单"
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
          OrderStatus.待骑手接单,
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
