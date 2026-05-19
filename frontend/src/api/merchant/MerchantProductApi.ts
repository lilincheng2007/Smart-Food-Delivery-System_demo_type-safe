import type { TaskIO } from '@/api/shared/TaskIO'
import type { CreateProductRequest } from '@/objects/merchant/CreateProductRequest'
import type { CreateProductResponse } from '@/objects/merchant/CreateProductResponse'
import type { UpdateProductRequest } from '@/objects/merchant/UpdateProductRequest'
import type { UpdateProductResponse } from '@/objects/merchant/UpdateProductResponse'
import { apiPostIO, apiPutIO } from '@/api/shared/client'

export function createMerchantProductIO(input: CreateProductRequest): TaskIO<CreateProductResponse> {
  return apiPostIO('/merchant/me/products', input)
}

export function updateMerchantProductIO(productId: string, input: UpdateProductRequest): TaskIO<UpdateProductResponse> {
  return apiPutIO(`/merchant/me/products/${productId}`, input)
}
