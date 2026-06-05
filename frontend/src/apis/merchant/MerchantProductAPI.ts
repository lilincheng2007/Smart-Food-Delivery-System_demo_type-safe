import { APIMessage } from '@/apis/shared/APIMessage'
import type { TaskIO } from '@/apis/shared/TaskIO'
import { sendAPI } from '@/apis/shared/sendAPI'
import type { Product } from '@/objects/merchant/Product'
import type { UpdateProductRequest } from '@/objects/merchant/apiTypes/UpdateProductRequest'
import type { ListingStatus, ProductId } from '@/objects/shared/ids'

class MerchantProductAPI extends APIMessage<Product> {
  readonly apiName = 'merchantproductapi'
  readonly productId: ProductId
  readonly name: string
  readonly description: string
  readonly imageUrl: string
  readonly categoryName: string
  readonly price: number
  readonly remainingStock: number
  readonly listingStatus: ListingStatus

  constructor(
    productId: ProductId,
    name: string,
    description: string,
    imageUrl: string,
    categoryName: string,
    price: number,
    remainingStock: number,
    listingStatus: ListingStatus,
  ) {
    super()
    this.productId = productId
    this.name = name
    this.description = description
    this.imageUrl = imageUrl
    this.categoryName = categoryName
    this.price = price
    this.remainingStock = remainingStock
    this.listingStatus = listingStatus
  }
}

export function updateMerchantProductIO(productId: ProductId, input: UpdateProductRequest): TaskIO<Product> {
  return sendAPI(
    new MerchantProductAPI(
      productId,
      input.name,
      input.description,
      input.imageUrl,
      input.categoryName,
      input.price,
      input.remainingStock,
      input.listingStatus,
    ),
  )
}
