package delivery.ai.services

import cats.effect.IO
import delivery.ai.objects.apiTypes.{AIRecommendedMerchant, AIRecommendedProduct, AISearchResponse}
import delivery.ai.utils.OpenAIClient
import delivery.merchant.objects.{Merchant, Product}
import delivery.merchant.services.CatalogQueryService
import io.circe.Json

import java.sql.Connection

object AISearchService:

  def search(connection: Connection, query: String): IO[AISearchResponse] =
    for
      catalog <- CatalogQueryService.listVisibleCatalog(connection)
      prompt = buildPrompt(catalog.merchants, catalog.products)
      resultJson <- OpenAIClient.chatCompletion(prompt, query)
      response <- parseResponse(query, resultJson, catalog.merchants, catalog.products)
    yield response

  private def buildPrompt(merchants: List[Merchant], products: List[Product]): String =
    val merchantSummaries = merchants.map { m =>
      val merchantProducts = products.filter(_.merchantId == m.id).map(p =>
        s"""{"id":"${p.id}","name":"${p.name}","price":${p.price},"desc":"${p.description.take(50)}","sales":${p.monthlySales}}"""
      ).mkString("[", ",", "]")
      s"""{"id":"${m.id}","name":"${m.storeName}","category":"${m.category}","rating":${m.rating},"tags":[${m.tags.mkString("\"", "\",\"", "\"")}],"products":$merchantProducts}"""
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
        case Left(_)  => IO.pure("为您推荐以下商家")
      merchantResults <- cursor.downField("merchants").as[List[Json]] match
        case Right(list) => IO.pure(list)
        case Left(_)     => IO.pure(List.empty[Json])
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

end AISearchService
