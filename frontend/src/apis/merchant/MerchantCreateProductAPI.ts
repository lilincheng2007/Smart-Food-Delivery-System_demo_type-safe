import { APIMessage } from '@/apis/shared/APIMessage'
import type { TaskIO } from '@/apis/shared/TaskIO'
import { sendAPI } from '@/apis/shared/sendAPI'
import type { CreateProductRequest } from '@/objects/merchant/apiTypes/CreateProductRequest'
import type { Product } from '@/objects/merchant/Product'
import type { ListingStatus, MerchantId } from '@/objects/shared/ids'

class MerchantCreateProductAPI extends APIMessage<Product> {
  readonly apiName = 'merchantcreateproductapi'
  readonly merchantId: MerchantId
  readonly name: string
  readonly description: string
  readonly imageUrl: string
  readonly categoryName: string
  readonly price: number
  readonly remainingStock: number
  readonly listingStatus: ListingStatus

  constructor(
    merchantId: MerchantId,
    name: string,
    description: string,
    imageUrl: string,
    categoryName: string,
    price: number,
    remainingStock: number,
    listingStatus: ListingStatus,
  ) {
    super()
    this.merchantId = merchantId
    this.name = name
    this.description = description
    this.imageUrl = imageUrl
    this.categoryName = categoryName
    this.price = price
    this.remainingStock = remainingStock
    this.listingStatus = listingStatus
  }
}

export function createMerchantProductIO(input: CreateProductRequest): TaskIO<Product> {
  return sendAPI(
    new MerchantCreateProductAPI(
      input.merchantId,
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
