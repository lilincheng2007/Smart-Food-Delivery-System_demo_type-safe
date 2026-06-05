import type { MerchantReview } from '@/objects/review/MerchantReview'

export type ReviewFilterKey = 'all' | 'positive' | 'negative' | 'withImage' | 'merchantReply'

export const reviewFilterOptions: Array<{ key: ReviewFilterKey; label: string }> = [
  { key: 'all', label: '全部' },
  { key: 'positive', label: '好评' },
  { key: 'negative', label: '差评' },
  { key: 'withImage', label: '有图' },
  { key: 'merchantReply', label: '商家回复' },
]

export function filterReviews(reviews: MerchantReview[], filter: ReviewFilterKey): MerchantReview[] {
  switch (filter) {
    case 'positive':
      return reviews.filter((review) => review.rating >= 4)
    case 'negative':
      return reviews.filter((review) => review.rating <= 2)
    case 'withImage':
      return reviews.filter((review) => Boolean(review.imageUrl?.trim()))
    case 'merchantReply':
      return reviews.filter((review) => Boolean(review.merchantReply?.trim()))
    case 'all':
      return reviews
  }
}

export function reviewFilterCounts(reviews: MerchantReview[]): Record<ReviewFilterKey, number> {
  return {
    all: reviews.length,
    positive: reviews.filter((review) => review.rating >= 4).length,
    negative: reviews.filter((review) => review.rating <= 2).length,
    withImage: reviews.filter((review) => Boolean(review.imageUrl?.trim())).length,
    merchantReply: reviews.filter((review) => Boolean(review.merchantReply?.trim())).length,
  }
}
