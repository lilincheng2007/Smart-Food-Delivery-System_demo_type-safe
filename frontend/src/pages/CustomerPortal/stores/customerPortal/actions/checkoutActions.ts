import { checkoutIO, type CheckoutDeliverySnapshot } from '@/apis/order/CheckoutAPI'
import { checkoutQuoteIO } from '@/apis/order/CheckoutQuoteAPI'
import { runTask } from '@/apis/shared/client'
import type { OrderMerchantNote } from '@/objects/order/apiTypes/CheckoutRequest'
import type { MerchantId, VoucherId } from '@/objects/shared/ids'

import type { CustomerPortalStore } from '../types'
import type { CustomerPortalGet, CustomerPortalSet } from './types'

export function createCheckoutActions(
  set: CustomerPortalSet,
  get: CustomerPortalGet,
): Pick<CustomerPortalStore, 'checkout'> {
  return {
    checkout: async (options?: {
      merchantId?: MerchantId
      delivery?: CheckoutDeliverySnapshot
      voucherId?: VoucherId
      merchantNotes?: OrderMerchantNote[]
    }) => {
      const merchantId = options?.merchantId
      const { cartLines } = get()
      const lines = merchantId ? cartLines.filter((line) => line.merchantId === merchantId) : cartLines

      if (lines.length === 0) {
        return {
          ok: false,
          message: merchantId ? '本店购物车为空，无法结算。' : '购物车为空，无法结算。',
        }
      }

      const merchantNotes = options?.merchantNotes ?? []

      try {
        const quote = await runTask(checkoutQuoteIO(lines, options?.delivery, options?.voucherId, merchantNotes))
        if (!quote.canCheckout) {
          return { ok: false, message: quote.failureReason ?? '当前订单暂不可结算，请稍后重试。' }
        }

        const data = await runTask(checkoutIO(lines, options?.delivery, options?.voucherId, merchantNotes))
        const nextCartLines = merchantId
          ? cartLines.filter((line) => line.merchantId !== merchantId)
          : []
        set({
          cartLines: nextCartLines,
          activeTab: merchantId ? get().activeTab : 'profile',
        })
        await get().refreshPortal()
        return { ok: true, createdCount: data.orders.length }
      } catch (error) {
        return { ok: false, message: error instanceof Error ? error.message : '结算失败' }
      }
    },
  }
}
