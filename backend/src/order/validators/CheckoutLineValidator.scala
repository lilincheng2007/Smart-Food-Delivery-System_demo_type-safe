package delivery.order.validators

import delivery.merchant.objects.{Product, ProductBundleSelectionType, ProductInventoryMode}
import delivery.order.objects.CheckoutLine
import delivery.domain.{ListingStatus, ProductId}

object CheckoutLineValidator:

  def validateLines(productsById: Map[ProductId, Product], lines: List[CheckoutLine]): Option[String] =
    lines.flatMap { line =>
      if line.quantity <= 0 then Some("商品数量必须大于 0")
      else
        productsById.get(line.productId) match
          case None => Some("购物车包含不存在的商品")
          case Some(product) if product.merchantId != line.merchantId => Some(s"${product.name}不属于当前商家")
          case Some(product) if line.bundleSelections.exists(_.quantity <= 0) => Some(s"${product.name}的套餐选择数量无效")
          case Some(_) => None
    }.headOption

  def validateInventory(productsById: Map[ProductId, Product], lines: List[CheckoutLine]): Option[String] =
    consumedQuantities(productsById, lines).toList.flatMap { case (productId, quantity) =>
      productsById.get(productId).flatMap { product =>
        val mode = product.inventoryMode
        if product.listingStatus != ListingStatus.上架 then Some(s"${product.name}暂未上架")
        else if mode == ProductInventoryMode.soldOut then Some(s"${product.name}已售罄")
        else if product.maxPerOrder.exists(limit => quantity > limit) then Some(s"${product.name}每单限购${product.maxPerOrder.get}份")
        else if mode == ProductInventoryMode.finite && product.remainingStock <= 0 then Some(s"${product.name}已售罄")
        else if mode == ProductInventoryMode.finite && quantity > product.remainingStock then Some(s"${product.name}库存不足，当前仅剩${product.remainingStock}份")
        else None
      }
    }.headOption

  def validateBundleLines(productsById: Map[ProductId, Product], lines: List[CheckoutLine]): Option[String] =
    lines.flatMap(line => productsById.get(line.productId).flatMap(product => validateBundleLine(product, line, productsById))).headOption

  def consumedQuantities(productsById: Map[ProductId, Product], lines: List[CheckoutLine]): Map[ProductId, Int] =
    lines.foldLeft(Map.empty[ProductId, Int]) { (current, line) =>
      productsById.get(line.productId) match
        case None => current
        case Some(product) =>
          val withBase = addQuantity(current, product.id, line.quantity)
          if product.bundleGroups.isEmpty then withBase
          else
            line.bundleSelections.foldLeft(withBase) { (next, selection) =>
              addQuantity(next, selection.productId, selection.quantity * line.quantity)
            }
    }

  private def validateBundleLine(product: Product, line: CheckoutLine, productsById: Map[ProductId, Product]): Option[String] =
    if product.bundleGroups.isEmpty then
      val invalid = line.bundleSelections.exists(selection => productsById.get(selection.productId).forall(_.merchantId != product.merchantId))
      if invalid then Some(s"${product.name}包含不可选菜品") else None
    else
      product.bundleGroups.flatMap { group =>
        val selected = line.bundleSelections.filter(_.groupId == group.id)
        val selectedCount = selected.map(selection => math.max(0, selection.quantity)).sum
        val allowedIds = group.options.map(_.productId).toSet
        val invalid = selected.exists(selection => !allowedIds.contains(selection.productId) || productsById.get(selection.productId).forall(_.merchantId != product.merchantId))
        val duplicated = selected.exists(selection => selection.quantity > 1)
        if selectedCount != group.quantity then Some(s"${product.name}的${group.name}需要选择${group.quantity}件")
        else if invalid then Some(s"${product.name}的${group.name}包含不可选菜品")
        else if group.selectionType == ProductBundleSelectionType.nonRepeatable && duplicated then Some(s"${product.name}的${group.name}不可重复选择同一菜品")
        else None
      }.headOption

  private def addQuantity(values: Map[ProductId, Int], productId: ProductId, quantity: Int): Map[ProductId, Int] =
    values.updated(productId, values.getOrElse(productId, 0) + math.max(0, quantity))

end CheckoutLineValidator
