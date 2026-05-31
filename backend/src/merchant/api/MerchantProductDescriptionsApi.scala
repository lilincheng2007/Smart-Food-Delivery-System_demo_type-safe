package delivery.merchant.api

import cats.effect.IO
import cats.syntax.all.*
import delivery.merchant.objects.ProductDescriptionPatch
import delivery.merchant.tables.catalogproduct.CatalogProductTable
import delivery.merchant.utils.MerchantApiSupport
import delivery.shared.api.{APIWithRoleMessage, HttpApiError}
import delivery.shared.objects.{MerchantId, OkResponse}

import java.sql.Connection

final case class MerchantProductDescriptionsAPIMessage(
    merchantId: MerchantId,
    descriptions: List[ProductDescriptionPatch]
) extends APIWithRoleMessage[OkResponse]:
  override def plan(connection: Connection, username: String): IO[OkResponse] =
    if descriptions.isEmpty then IO.raiseError(HttpApiError.BadRequest("请提供需要保存的菜品描述"))
    else
      for
        products <- MerchantApiSupport.listOwnedProducts(connection, username, merchantId)
        _ <- if products.isEmpty then IO.raiseError(HttpApiError.BadRequest("请先创建菜品后再保存菜品描述")) else IO.unit
        productById = products.map(product => product.id -> product).toMap
        normalized <- descriptions.distinctBy(_.productId).traverse { patch =>
          val trimmed = patch.description.trim
          productById.get(patch.productId) match
            case None => IO.raiseError(HttpApiError.BadRequest("存在不属于当前店铺的菜品"))
            case Some(product) if trimmed.isEmpty => IO.raiseError(HttpApiError.BadRequest(s"${product.name} 的描述不能为空"))
            case Some(product) if trimmed.length > 160 => IO.raiseError(HttpApiError.BadRequest(s"${product.name} 的描述不能超过 160 个字符"))
            case Some(product) => IO.pure(product.copy(description = trimmed))
        }
        _ <- normalized.traverse_(product => CatalogProductTable.upsert(connection, product))
      yield OkResponse(ok = true)
