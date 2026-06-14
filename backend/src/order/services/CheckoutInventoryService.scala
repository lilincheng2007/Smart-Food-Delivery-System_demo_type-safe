package delivery.order.services

import delivery.merchant.objects.{Product, ProductInventoryMode}
import delivery.order.objects.CheckoutLine
import delivery.order.validators.CheckoutLineValidator
import delivery.domain.{InventoryStatus, ListingStatus}

object CheckoutInventoryService:

  def inventoryDeductions(products: List[Product], lines: List[CheckoutLine]): List[Product] =
    val productsById = products.map(product => product.id -> product).toMap
    val consumed = CheckoutLineValidator.consumedQuantities(productsById, lines)
    products.flatMap { product =>
      val quantity = consumed.getOrElse(product.id, 0)
      if quantity <= 0 || product.inventoryMode != ProductInventoryMode.finite then None
      else
        val nextStock = math.max(0, product.remainingStock - quantity)
        Some(product.copy(remainingStock = nextStock, inventoryStatus = inventoryStatus(nextStock, product.listingStatus, product.inventoryMode)))
    }

  private def inventoryStatus(remainingStock: Int, listingStatus: ListingStatus, inventoryMode: ProductInventoryMode): InventoryStatus =
    if listingStatus == ListingStatus.下架 || inventoryMode == ProductInventoryMode.soldOut then InventoryStatus.售罄
    else if inventoryMode == ProductInventoryMode.unlimited then InventoryStatus.充足
    else if remainingStock <= 0 then InventoryStatus.售罄
    else if remainingStock <= 20 then InventoryStatus.紧张
    else InventoryStatus.充足

end CheckoutInventoryService
