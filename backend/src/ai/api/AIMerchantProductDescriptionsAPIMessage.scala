package delivery.ai.api

import cats.effect.IO
import delivery.ai.objects.{AIGeneratedProductDescription}
import delivery.ai.objects.apiTypes.{AIMerchantProductDescriptionsResponse}
import delivery.ai.utils.OpenAIClient
import delivery.merchant.objects.{Merchant, Product}
import delivery.merchant.utils.MerchantApiSupport
import delivery.platform.api.{APIWithRoleMessage, HttpApiError}
import delivery.domain.{MerchantId, ProductId}
import io.circe.Json

import java.sql.Connection
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

final case class AIMerchantProductDescriptionsAPIMessage(merchantId: MerchantId, keywords: String) extends APIWithRoleMessage[AIMerchantProductDescriptionsResponse]:

  private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

  override def plan(connection: Connection, username: String): IO[AIMerchantProductDescriptionsResponse] =
    val trimmedKeywords = keywords.trim
    if trimmedKeywords.isEmpty then IO.raiseError(HttpApiError.BadRequest("请输入菜品关键词"))
    else if trimmedKeywords.length > 200 then IO.raiseError(HttpApiError.BadRequest("菜品关键词不能超过 200 个字符"))
    else
      for
        _ <- OpenAIClient.configured.flatMap { ok =>
          if !ok then IO.raiseError(HttpApiError.BadRequest("AI 服务未配置，请联系管理员")) else IO.unit
        }
        merchant <- MerchantApiSupport.requireOwnedStore(connection, username, merchantId)
        products <- MerchantApiSupport.listOwnedProducts(connection, username, merchantId)
        _ <- if products.isEmpty then IO.raiseError(HttpApiError.BadRequest("请先创建菜品后再生成菜品描述")) else IO.unit
        resultJson <- OpenAIClient.chatCompletion(buildPrompt(merchant, products), trimmedKeywords)
        descriptions = parseDescriptions(resultJson, merchant, products, trimmedKeywords)
      yield AIMerchantProductDescriptionsResponse(merchantId, descriptions, LocalDateTime.now().format(dateFormatter))

  private def buildPrompt(merchant: Merchant, products: List[Product]): String =
    val productText = products.map { product =>
      s"""{"productId":"${product.id}","name":"${escapeText(product.name)}","price":${product.price},"currentDescription":"${escapeText(product.description).take(80)}"}"""
    }.mkString("[", ",", "]")

    s"""你是外卖平台的菜品文案优化助手。请根据店铺信息、商户关键词和菜品清单，为每个已存在菜品生成更有食欲、真实且简洁的描述。

店铺名称：${escapeText(merchant.storeName)}
店铺描述：${escapeText(merchant.description)}
菜品数据：
$productText

请按以下 JSON 格式返回，不要返回其他内容：
{
  "products": [
    {
      "productId": "菜品ID",
      "description": "菜品描述"
    }
  ]
}

要求：
1. 必须为菜品数据中的每个 productId 生成一条描述
2. productId 必须严格来自菜品数据，不要新增或改写 ID
3. 不需要考虑库存、上下架或剩余库存，菜品存在即可生成
4. 每条描述 22-60 个中文字符，突出食材、口味、口感或适用场景
5. 不要承诺不存在的赠品、折扣、配送时效或库存状态"""

  private def parseDescriptions(
      json: Json,
      merchant: Merchant,
      products: List[Product],
      keywords: String
  ): List[AIGeneratedProductDescription] =
    val productIds = products.map(_.id).toSet
    val aiDescriptions: Map[ProductId, String] = json.hcursor.downField("products").as[List[Json]].getOrElse(List.empty).flatMap { item =>
      val cursor = item.hcursor
      val productId = cursor.downField("productId").as[String].getOrElse("")
      val description = cursor.downField("description").as[String].getOrElse("").trim
      if productIds.contains(productId) && description.nonEmpty then Some(productId -> description.take(140)) else None
    }.toMap

    products.map { product =>
      AIGeneratedProductDescription(
        productId = product.id,
        productName = product.name,
        description = aiDescriptions.getOrElse(product.id, fallbackDescription(merchant, product, keywords))
      )
    }

  private def fallbackDescription(merchant: Merchant, product: Product, keywords: String): String =
    s"${product.name}融合${keywords.take(18)}灵感，延续${merchant.storeName}的招牌风味，入口热乎又满足。".take(90)

  private def escapeText(value: String): String =
    value.replace("\n", " ").replace("\"", "'").trim
