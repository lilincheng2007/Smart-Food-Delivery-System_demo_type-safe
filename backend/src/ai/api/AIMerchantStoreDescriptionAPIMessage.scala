package delivery.ai.api

import cats.effect.IO
import delivery.ai.objects.apiTypes.AIMerchantStoreDescriptionResponse
import delivery.ai.utils.OpenAIClient
import delivery.merchant.objects.{Merchant, Product}
import delivery.merchant.utils.MerchantApiSupport
import delivery.platform.api.{APIWithRoleMessage, HttpApiError}
import delivery.domain.MerchantId
import io.circe.Json

import java.sql.Connection
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

final case class AIMerchantStoreDescriptionAPIMessage(merchantId: MerchantId, keywords: String) extends APIWithRoleMessage[AIMerchantStoreDescriptionResponse]:

  private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

  override def plan(connection: Connection, username: String): IO[AIMerchantStoreDescriptionResponse] =
    val trimmedKeywords = keywords.trim
    if trimmedKeywords.isEmpty then IO.raiseError(HttpApiError.BadRequest("请输入店铺关键词"))
    else if trimmedKeywords.length > 200 then IO.raiseError(HttpApiError.BadRequest("店铺关键词不能超过 200 个字符"))
    else
      for
        _ <- OpenAIClient.configured.flatMap { ok =>
          if !ok then IO.raiseError(HttpApiError.BadRequest("AI 服务未配置，请联系管理员")) else IO.unit
        }
        merchant <- MerchantApiSupport.requireOwnedStore(connection, username, merchantId)
        products <- MerchantApiSupport.listOwnedProducts(connection, username, merchantId)
        resultJson <- OpenAIClient.chatCompletion(buildPrompt(merchant, products), trimmedKeywords)
        description <- parseDescription(resultJson, merchant, trimmedKeywords)
      yield AIMerchantStoreDescriptionResponse(merchantId, description, LocalDateTime.now().format(dateFormatter))

  private def buildPrompt(merchant: Merchant, products: List[Product]): String =
    val productText =
      if products.isEmpty then "暂无菜品"
      else products.map(product => s"- ${escapeText(product.name)}：${escapeText(product.description).take(60)}，价格 ${product.price} 元").mkString("\n")

    s"""你是外卖平台的商家品牌文案助手。请根据店铺信息、菜品与商户关键词，为顾客端生成一段优美但真实的店铺描述。

店铺名称：${escapeText(merchant.storeName)}
店铺类别：${merchant.category}
店铺地址：${escapeText(merchant.address)}
当前店铺描述：${escapeText(merchant.description)}
菜品列表：
$productText

请按以下 JSON 格式返回，不要返回其他内容：
{
  "description": "店铺描述"
}

要求：
1. 只生成 description 字段
2. 文案 40-90 个中文字符，适合顾客端商家卡片和店铺详情展示
3. 结合商户关键词、店铺定位和菜品特色，不要编造不存在的服务承诺
4. 语气温暖、有食欲、有品牌感，不要使用夸张绝对化表达"""

  private def parseDescription(json: Json, merchant: Merchant, keywords: String): IO[String] =
    val raw = json.hcursor.downField("description").as[String].getOrElse("").trim
    val description =
      if raw.nonEmpty then raw.take(240)
      else s"${merchant.storeName}围绕${keywords.take(30)}打造暖心风味，精选店内招牌菜品，把热乎与安心送到每一餐。".take(120)
    IO.pure(description)

  private def escapeText(value: String): String =
    value.replace("\n", " ").replace("\"", "'").trim
