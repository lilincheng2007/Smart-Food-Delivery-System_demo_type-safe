package delivery.merchant.state

import cats.effect.IO
import delivery.merchant.objects.*
import delivery.order.objects.Order

object MerchantDomainOps:

  private def computeInventoryStatus(remainingStock: Int, listingStatus: String): String =
    if listingStatus == "下架" || remainingStock <= 0 then "售罄"
    else if remainingStock <= 20 then "紧张"
    else "充足"

  private def isHistoryStatus(status: String): Boolean =
    status == "已送达" || status == "已完成" || status == "已取消"

  private def pushOrderToMerchantStores(accounts: List[MerchantAccount], order: Order): List[MerchantAccount] =
    accounts.map { acc =>
      val newStores = acc.profile.stores.map { sp =>
        if sp.merchant.id == order.merchantId then
          val isHistory = isHistoryStatus(order.status)
          if isHistory then sp.copy(historyOrders = order :: sp.historyOrders)
          else sp.copy(pendingOrders = order :: sp.pendingOrders)
        else sp
      }
      acc.copy(profile = acc.profile.copy(stores = newStores))
    }

  def attachOrders(state: MerchantServiceState, orders: List[Order]): MerchantServiceState =
    state.copy(merchantAccounts = orders.foldLeft(state.merchantAccounts)((accs, o) => pushOrderToMerchantStores(accs, o)))

  def hasStoreAccess(state: MerchantServiceState, username: String, merchantId: String): Boolean =
    state.merchantAccounts
      .find(_.username == username)
      .exists(_.profile.stores.exists(_.merchant.id == merchantId))

  def replaceOrderSnapshot(state: MerchantServiceState, updatedOrder: Order): MerchantServiceState =
    state.copy(
      merchantAccounts = state.merchantAccounts.map { account =>
        val nextStores = account.profile.stores.map { store =>
          if store.merchant.id != updatedOrder.merchantId then store
          else
            val nextPending = store.pendingOrders.filterNot(_.id == updatedOrder.id)
            val nextHistory = store.historyOrders.filterNot(_.id == updatedOrder.id)
            if isHistoryStatus(updatedOrder.status) then store.copy(historyOrders = updatedOrder :: nextHistory, pendingOrders = nextPending)
            else store.copy(pendingOrders = updatedOrder :: nextPending, historyOrders = nextHistory)
        }
        account.copy(profile = account.profile.copy(stores = nextStores))
      }
    )

  private def patchMerchantImage(merchantId: String, imageUrl: Option[String])(m: Merchant): Merchant =
    if m.id == merchantId then m.copy(imageUrl = imageUrl) else m

  def updateStoreImage(state: MerchantServiceState, username: String, merchantId: String, rawUrl: String): Either[String, MerchantServiceState] =
    if !hasStoreAccess(state, username, merchantId) then Left("无权修改该店铺图片")
    else
      val trimmed = rawUrl.trim
      val urlOk =
        trimmed.isEmpty ||
          trimmed.startsWith("http://") ||
          trimmed.startsWith("https://") ||
          trimmed.startsWith("/api/merchant/store-images/")
      if trimmed.nonEmpty && !urlOk then
        Left("图片链接须为 http(s) 地址、本地上传生成的 /api/merchant/store-images/... 路径，或留空以清除")
      else
        val imageOpt = if trimmed.isEmpty then None else Some(trimmed)
        Right(
          state.copy(
            catalogMerchants = state.catalogMerchants.map(patchMerchantImage(merchantId, imageOpt)),
            merchantAccounts = state.merchantAccounts.map { acc =>
              acc.copy(profile =
                acc.profile.copy(stores =
                  acc.profile.stores.map { sp =>
                    if sp.merchant.id == merchantId then sp.copy(merchant = patchMerchantImage(merchantId, imageOpt)(sp.merchant))
                    else sp
                  }
                )
              )
            }
          )
        )

  def replaceMerchantProfile(state: MerchantServiceState, username: String, profile: MerchantProfile): Either[String, MerchantServiceState] =
    state.merchantAccounts.find(_.username == username) match
      case None => Left("Not found")
      case Some(_) =>
        Right(state.copy(merchantAccounts = state.merchantAccounts.map(ma => if ma.username == username then ma.copy(profile = profile) else ma)))

  def createMerchantStore(state: MerchantServiceState, username: String, storeName: String, address: String): IO[Either[String, (MerchantServiceState, String)]] =
    state.merchantAccounts.find(_.username == username) match
      case None => IO.pure(Left("未找到商家账号"))
      case Some(acc) =>
        IO.realTime.map(_.toMillis).map { nowMillis =>
          val m = Merchant(s"m-local-$nowMillis", storeName, "中餐", address, acc.profile.phone, 5, List("新店"), Nil, None)
          val sp = MerchantStoreProfile(m, Nil, Nil, Nil)
          val newAcc = acc.copy(profile = acc.profile.copy(stores = acc.profile.stores :+ sp))
          Right((state.copy(merchantAccounts = state.merchantAccounts.map(ma => if ma.username == username then newAcc else ma), catalogMerchants = state.catalogMerchants :+ m), m.id))
        }

  def createProduct(
      state: MerchantServiceState,
      username: String,
      merchantId: String,
      name: String,
      description: String,
      price: Double,
      remainingStock: Int,
      listingStatus: String
  ): IO[Either[String, (MerchantServiceState, Product)]] =
    if listingStatus != "上架" && listingStatus != "下架" then IO.pure(Left("上/下架状态不合法"))
    else if name.trim.isEmpty || description.trim.isEmpty then IO.pure(Left("菜品名称和描述不能为空"))
    else if price < 0 || remainingStock < 0 then IO.pure(Left("价格和库存不能为负数"))
    else if !hasStoreAccess(state, username, merchantId) then IO.pure(Left("无权在该店铺下创建菜品"))
    else
      IO.realTime.map(_.toMillis).map { nowMillis =>
        val product = Product(
          id = s"p-local-$nowMillis",
          merchantId = merchantId,
          name = name.trim,
          price = price,
          description = description.trim,
          imageUrl = s"https://picsum.photos/200/120?product-$nowMillis",
          monthlySales = 0,
          remainingStock = remainingStock,
          listingStatus = listingStatus,
          inventoryStatus = computeInventoryStatus(remainingStock, listingStatus),
          discountText = None
        )

        val nextMerchantAccounts = state.merchantAccounts.map { account =>
          if account.username != username then account
            else
              val nextStores = account.profile.stores.map { store =>
                if store.merchant.id != merchantId then store
                else
                  val nextMerchant = store.merchant.copy(featuredProductIds = store.merchant.featuredProductIds :+ product.id)
                  store.copy(merchant = nextMerchant, products = store.products :+ product)
              }
              account.copy(profile = account.profile.copy(stores = nextStores))
        }

        val nextCatalogMerchants = state.catalogMerchants.map { merchant =>
          if merchant.id != merchantId then merchant
          else merchant.copy(featuredProductIds = merchant.featuredProductIds :+ product.id)
        }

        Right((
          state.copy(
            merchantAccounts = nextMerchantAccounts,
            catalogMerchants = nextCatalogMerchants,
            catalogProducts = state.catalogProducts :+ product
          ),
          product
        ))
      }

  def updateProduct(
      state: MerchantServiceState,
      username: String,
      productId: String,
      name: String,
      description: String,
      price: Double,
      remainingStock: Int,
      listingStatus: String
  ): Either[String, (MerchantServiceState, Product)] =
    if listingStatus != "上架" && listingStatus != "下架" then Left("上/下架状态不合法")
    else if name.trim.isEmpty || description.trim.isEmpty then Left("菜品名称和描述不能为空")
    else if price < 0 || remainingStock < 0 then Left("价格和库存不能为负数")
    else
      state.catalogProducts.find(_.id == productId).toRight("未找到菜品").flatMap { existing =>
        if !hasStoreAccess(state, username, existing.merchantId) then Left("无权编辑该菜品")
        else
          val updatedProduct = existing.copy(
            name = name.trim,
            description = description.trim,
            price = price,
            remainingStock = remainingStock,
            listingStatus = listingStatus,
            inventoryStatus = computeInventoryStatus(remainingStock, listingStatus)
          )

          val nextMerchantAccounts = state.merchantAccounts.map { account =>
            if account.username != username then account
            else
              val nextStores = account.profile.stores.map { store =>
                if store.merchant.id != existing.merchantId then store
                else store.copy(products = store.products.map(product => if product.id == productId then updatedProduct else product))
              }
              account.copy(profile = account.profile.copy(stores = nextStores))
          }

          Right((
            state.copy(
              merchantAccounts = nextMerchantAccounts,
              catalogProducts = state.catalogProducts.map(product => if product.id == productId then updatedProduct else product)
            ),
            updatedProduct
          ))
      }

  def bootstrapMerchant(state: MerchantServiceState, username: String): IO[Either[String, MerchantServiceState]] =
    if state.merchantAccounts.exists(_.username == username) then IO.pure(Left("商户已存在"))
    else
      IO.realTime.map(_.toMillis).map { nowMillis =>
        val newMerchant = Merchant(s"m-$nowMillis", s"${username}的店铺", "中餐", "请完善店铺地址", "", 5, Nil, Nil, None)
        val acc = MerchantAccount("merchant", username, "", MerchantProfile(s"merchant-profile-${newMerchant.id}", username, "", List(MerchantStoreProfile(newMerchant, Nil, Nil, Nil))))
        Right(state.copy(merchantAccounts = state.merchantAccounts :+ acc, catalogMerchants = state.catalogMerchants :+ newMerchant))
      }

end MerchantDomainOps
