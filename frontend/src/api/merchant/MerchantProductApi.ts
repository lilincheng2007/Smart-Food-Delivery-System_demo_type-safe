import { APIMessage } from '@/api/shared/APIMessage'
import type { TaskIO } from '@/api/shared/TaskIO'
import { sendAPI } from '@/api/shared/sendAPI'
import type { CreateProductRequest } from '@/objects/merchant/CreateProductRequest'
import type { Product } from '@/objects/merchant/Product'
import type { UpdateProductRequest } from '@/objects/merchant/UpdateProductRequest'

class MerchantCreateProductAPI extends APIMessage<Product> {
  readonly merchantId: string
  readonly name: string
  readonly description: string
  readonly price: number
  readonly remainingStock: number
  readonly listingStatus: string

  constructor(
    merchantId: string,
    name: string,
    description: string,
    price: number,
    remainingStock: number,
    listingStatus: string,
  ) {
    super()
    this.merchantId = merchantId
    this.name = name
    this.description = description
    this.price = price
    this.remainingStock = remainingStock
    this.listingStatus = listingStatus
  }
}

class MerchantProductAPI extends APIMessage<Product> {
  readonly productId: string
  readonly name: string
  readonly description: string
  readonly price: number
  readonly remainingStock: number
  readonly listingStatus: string

  constructor(
    productId: string,
    name: string,
    description: string,
    price: number,
    remainingStock: number,
    listingStatus: string,
  ) {
    super()
    this.productId = productId
    this.name = name
    this.description = description
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
      input.price,
      input.remainingStock,
      input.listingStatus,
    ),
  )
}

export function updateMerchantProductIO(productId: string, input: UpdateProductRequest): TaskIO<Product> {
  return sendAPI(
    new MerchantProductAPI(
      productId,
      input.name,
      input.description,
      input.price,
      input.remainingStock,
      input.listingStatus,
    ),
  )
}
