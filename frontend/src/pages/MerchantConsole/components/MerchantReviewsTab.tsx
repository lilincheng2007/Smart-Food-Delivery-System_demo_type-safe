import { useEffect, useState } from 'react'
import { Clock, MessageSquareReply, RefreshCw, RotateCcw } from 'lucide-react'

import { acceptMerchantRefundIO } from '@/apis/merchant/MerchantRefundAcceptAPI'
import { fetchMerchantRefundRequestsIO } from '@/apis/merchant/MerchantRefundRequestsAPI'
import { rejectMerchantRefundIO } from '@/apis/merchant/MerchantRefundRejectAPI'
import { replyMerchantReviewIO } from '@/apis/review/MerchantReviewReplyAPI'
import { fetchMerchantReviewsIO } from '@/apis/review/MerchantReviewsAPI'
import { runTask } from '@/apis/shared/client'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { useAppChrome } from '@/hooks/useAppChrome'
import { resolveApiMediaUrl } from '@/lib/api-media-url'
import { cn } from '@/lib/utils'
import { filterReviews, reviewFilterCounts, reviewFilterOptions, type ReviewFilterKey } from '@/lib/review-filters'
import type { MerchantStoreProfile } from '@/objects/merchant/MerchantStoreProfile'
import type { Order } from '@/objects/order/Order'
import type { MerchantReviewsResponse } from '@/objects/review/apiTypes/MerchantReviewsResponse'
import { RefundStatuses, type RefundStatus } from '@/objects/shared/ids'

type MerchantReviewsTabProps = {
  selectedStore: MerchantStoreProfile | null
}

const refundStatusLabels: Record<RefundStatus, string> = {
  [RefundStatuses.pending]: '待商家处理',
  [RefundStatuses.legacyPending]: '待商家处理',
  [RefundStatuses.merchantRejected]: '商家已驳回',
  [RefundStatuses.adminPending]: '管理员仲裁中',
  [RefundStatuses.accepted]: '已通过',
  [RefundStatuses.rejected]: '已驳回',
}

function isMerchantPendingRefund(order: Order) {
  return order.refundStatus === RefundStatuses.pending || order.refundStatus === RefundStatuses.legacyPending
}

function refundBadgeVariant(status: RefundStatus | null | undefined) {
  if (status === RefundStatuses.accepted) return 'default'
  if (status === RefundStatuses.rejected || status === RefundStatuses.merchantRejected) return 'destructive'
  return 'outline'
}

function formatMoney(value: number) {
  return `￥${value.toFixed(2)}`
}

function formatDate(value: string | null | undefined) {
  if (!value) return '暂无'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString()
}

export function MerchantReviewsTab({ selectedStore }: MerchantReviewsTabProps) {
  const { showNotice } = useAppChrome()
  const [reviews, setReviews] = useState<MerchantReviewsResponse | null>(null)
  const [refundRequests, setRefundRequests] = useState<Order[]>([])
  const [activeReviewFilter, setActiveReviewFilter] = useState<ReviewFilterKey>('all')
  const [replyDrafts, setReplyDrafts] = useState<Record<string, string>>({})
  const [submittingReplyId, setSubmittingReplyId] = useState<string | null>(null)
  const [reviewingRefund, setReviewingRefund] = useState<{ order: Order; action: 'accept' | 'reject' } | null>(null)
  const [refundReviewReason, setRefundReviewReason] = useState('')
  const [processingRefundId, setProcessingRefundId] = useState<string | null>(null)
  const [refundLoading, setRefundLoading] = useState(false)
  const [refundError, setRefundError] = useState<string | null>(null)
  const selectedMerchantId = selectedStore?.merchant.id ?? null
  const allReviews = reviews?.reviews ?? []
  const reviewCounts = reviewFilterCounts(allReviews)
  const filteredReviews = filterReviews(allReviews, activeReviewFilter)
  const storeRefundRequests = refundRequests.filter((order) => order.merchantId === selectedMerchantId)
  const sortedRefundRequests = [...storeRefundRequests].sort((left, right) => {
    if (isMerchantPendingRefund(left) !== isMerchantPendingRefund(right)) {
      return isMerchantPendingRefund(left) ? -1 : 1
    }
    return (right.refundRequestedAt ?? right.placedAt).localeCompare(left.refundRequestedAt ?? left.placedAt)
  })
  const pendingRefundCount = storeRefundRequests.filter(isMerchantPendingRefund).length

  useEffect(() => {
    if (!selectedMerchantId) {
      setReviews(null)
      setRefundRequests([])
      setRefundError(null)
      setReplyDrafts({})
      setActiveReviewFilter('all')
      return
    }
    void runTask(fetchMerchantReviewsIO(selectedMerchantId))
      .then((nextReviews) => {
        setReviews(nextReviews)
        setReplyDrafts({})
        setActiveReviewFilter('all')
      })
      .catch(() => setReviews(null))

    void refreshRefundRequests()
  }, [selectedMerchantId])

  useEffect(() => {
    if (!selectedMerchantId) return
    const timer = window.setInterval(() => {
      void refreshRefundRequests({ silent: true })
    }, 5000)

    return () => window.clearInterval(timer)
  }, [selectedMerchantId])

  const refreshRefundRequests = async (options?: { silent?: boolean }) => {
    if (!selectedMerchantId) return
    if (!options?.silent) {
      setRefundLoading(true)
    }
    setRefundError(null)
    try {
      const nextRefunds = await runTask(fetchMerchantRefundRequestsIO())
      setRefundRequests(nextRefunds.requests)
    } catch (error) {
      setRefundError(error instanceof Error ? error.message : '退款申请加载失败')
    } finally {
      if (!options?.silent) {
        setRefundLoading(false)
      }
    }
  }

  const refreshFeedback = async () => {
    if (!selectedMerchantId) return
    const nextReviews = await runTask(fetchMerchantReviewsIO(selectedMerchantId))
    setReviews(nextReviews)
    await refreshRefundRequests({ silent: true })
  }

  const handleReplyReview = async (reviewId: string) => {
    if (!selectedMerchantId) {
      showNotice('请先选择店铺。', 'error')
      return
    }

    const review = reviews?.reviews.find((item) => item.id === reviewId)
    const replyContent = (replyDrafts[reviewId] ?? review?.merchantReply ?? '').trim()
    if (!replyContent) {
      showNotice('回复内容不能为空。', 'error')
      return
    }

    setSubmittingReplyId(reviewId)
    try {
      await runTask(replyMerchantReviewIO(reviewId, selectedMerchantId, replyContent))
      const nextReviews = await runTask(fetchMerchantReviewsIO(selectedMerchantId))
      setReviews(nextReviews)
      setReplyDrafts((state) => {
        const { [reviewId]: _discarded, ...rest } = state
        return rest
      })
      showNotice('回复已发布，顾客端也能看见。', 'success')
    } catch (error) {
      showNotice(error instanceof Error ? error.message : '回复失败', 'error')
    } finally {
      setSubmittingReplyId(null)
    }
  }

  const handleRefundReview = async () => {
    if (!reviewingRefund) return
    const reason = refundReviewReason.trim()
    if (reviewingRefund.action === 'reject' && !reason) {
      showNotice('请填写驳回理由，顾客会看到这条反馈。', 'error')
      return
    }

    setProcessingRefundId(reviewingRefund.order.id)
    try {
      if (reviewingRefund.action === 'accept') {
        await runTask(acceptMerchantRefundIO(reviewingRefund.order.id, reason || null))
        showNotice('退款已通过，订单金额将从营业额中扣除并退还顾客。', 'success')
      } else {
        await runTask(rejectMerchantRefundIO(reviewingRefund.order.id, reason))
        showNotice('已驳回退款申请，顾客可继续申请管理员仲裁。', 'success')
      }
      setReviewingRefund(null)
      setRefundReviewReason('')
      await refreshFeedback()
    } catch (error) {
      showNotice(error instanceof Error ? error.message : '退款处理失败', 'error')
    } finally {
      setProcessingRefundId(null)
    }
  }

  return (
    <Card className="border-orange-100 bg-white/95">
      <CardHeader>
        <CardTitle>用户反馈</CardTitle>
        <CardDescription>处理退款申请并回复顾客评价，所有反馈状态都会同步到顾客端。</CardDescription>
      </CardHeader>
      <CardContent className="space-y-5">
        {!selectedStore ? (
          <p className="text-sm text-slate-500">当前未选择店铺。</p>
        ) : (
          <>
            <section className="space-y-3">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div>
                  <h3 className="text-base font-semibold text-slate-900">退款申请</h3>
                  <p className="text-xs text-slate-500">待处理申请和历史申请都会保留在这里。</p>
                </div>
                <Badge variant={pendingRefundCount > 0 ? 'destructive' : 'outline'} className="gap-1">
                  <RotateCcw className="size-3.5" />
                  待处理 {pendingRefundCount}
                </Badge>
              </div>
              <div className="flex flex-wrap items-center justify-between gap-2 rounded-xl border border-orange-100 bg-orange-50/60 px-3 py-2">
                <p className="text-xs text-orange-700">
                  顾客提交退款后会自动进入这里，列表每5秒刷新一次。
                </p>
                <Button
                  type="button"
                  size="sm"
                  variant="outline"
                  disabled={refundLoading}
                  onClick={() => void refreshRefundRequests()}
                >
                  <RefreshCw className={cn('size-4', refundLoading && 'animate-spin')} />
                  刷新退款申请
                </Button>
              </div>
              {refundError ? (
                <p className="rounded-xl border border-rose-100 bg-rose-50 px-3 py-2 text-sm text-rose-600">
                  退款申请加载失败：{refundError}
                </p>
              ) : null}
              {sortedRefundRequests.length === 0 ? (
                <p className="rounded-xl border border-dashed border-orange-100 px-4 py-6 text-sm text-slate-500">
                  暂无退款申请。
                </p>
              ) : (
                <div className="space-y-3">
                  {sortedRefundRequests.map((order) => {
                    const status = order.refundStatus ?? RefundStatuses.pending
                    const isPending = isMerchantPendingRefund(order)
                    return (
                      <article key={order.id} className="rounded-xl border border-orange-100 p-3 text-sm text-slate-700">
                        <div className="flex flex-wrap items-start justify-between gap-3">
                          <div>
                            <div className="flex flex-wrap items-center gap-2">
                              <span className="font-semibold text-slate-900">订单 {order.id}</span>
                              <Badge variant={refundBadgeVariant(status)}>{refundStatusLabels[status]}</Badge>
                            </div>
                            <p className="mt-1 text-xs text-slate-500">
                              {order.customerName} · 实付 {formatMoney(order.payableAmount || order.totalAmount)} · 申请时间 {formatDate(order.refundRequestedAt)}
                            </p>
                          </div>
                          {isPending ? (
                            <div className="flex shrink-0 flex-wrap gap-2">
                              <Button
                                size="sm"
                                disabled={processingRefundId === order.id}
                                onClick={() => {
                                  setReviewingRefund({ order, action: 'accept' })
                                  setRefundReviewReason('')
                                }}
                              >
                                同意退款
                              </Button>
                              <Button
                                size="sm"
                                variant="outline"
                                disabled={processingRefundId === order.id}
                                onClick={() => {
                                  setReviewingRefund({ order, action: 'reject' })
                                  setRefundReviewReason('')
                                }}
                              >
                                驳回
                              </Button>
                            </div>
                          ) : null}
                        </div>
                        <p className="mt-3 leading-6">顾客理由：{order.refundReason ?? '未填写'}</p>
                        {order.refundImageUrl ? (
                          <img src={resolveApiMediaUrl(order.refundImageUrl)} alt="退款凭证" className="mt-2 aspect-video w-full max-w-xs rounded-lg object-cover" />
                        ) : null}
                        {order.refundMerchantReason ? (
                          <p className="mt-2 rounded-lg bg-orange-50 px-3 py-2 text-xs leading-5 text-orange-700">
                            商家反馈：{order.refundMerchantReason}
                          </p>
                        ) : null}
                        {order.refundAdminReason ? (
                          <p className="mt-2 rounded-lg bg-slate-50 px-3 py-2 text-xs leading-5 text-slate-600">
                            管理员仲裁：{order.refundAdminReason}
                          </p>
                        ) : null}
                        {isPending ? (
                          <p className="mt-2 flex items-center gap-1 text-xs text-slate-500">
                            <Clock className="size-3.5" />
                            超过30分钟未处理将自动同意退款。
                          </p>
                        ) : null}
                      </article>
                    )
                  })}
                </div>
              )}
            </section>

            <section className="space-y-3">
              <div>
                <h3 className="text-base font-semibold text-slate-900">顾客评价</h3>
                <p className="text-xs text-slate-500">商家可查看顾客评价并公开回复。</p>
              </div>
              {!reviews || allReviews.length === 0 ? (
                <p className="rounded-xl border border-dashed border-orange-100 px-4 py-6 text-sm text-slate-500">
                  暂无评价。
                </p>
              ) : (
                <>
            <div className="flex flex-wrap gap-2">
              {reviewFilterOptions.map((option) => (
                <button
                  key={option.key}
                  type="button"
                  className={cn(
                    'rounded-full border px-3 py-1.5 text-sm font-medium transition-colors',
                    activeReviewFilter === option.key
                      ? 'border-orange-200 bg-orange-50 text-orange-700'
                      : 'border-slate-200 bg-white text-slate-600 hover:border-orange-200 hover:text-orange-700',
                  )}
                  onClick={() => setActiveReviewFilter(option.key)}
                >
                  {option.label} {reviewCounts[option.key]}
                </button>
              ))}
            </div>
            {filteredReviews.length === 0 ? (
              <p className="rounded-xl border border-dashed border-orange-100 px-4 py-6 text-sm text-slate-500">
                当前标签下暂无评价。
              </p>
            ) : (
              filteredReviews.map((review) => (
                <article key={review.id} className="rounded-xl border border-orange-100 p-3 text-sm text-slate-700">
                  <div className="flex items-center justify-between gap-3">
                    <span className="font-medium text-slate-900">{review.customerName}</span>
                    <span className="text-amber-500">{'★'.repeat(review.rating)}{'☆'.repeat(5 - review.rating)}</span>
                  </div>
                  <p className="mt-2 leading-6">{review.description}</p>
                  {review.orderItemNames && review.orderItemNames.length > 0 ? (
                    <p className="mt-2 rounded-lg bg-orange-50 px-3 py-2 text-xs font-medium text-orange-700">
                      对应菜品：{review.orderItemNames.join('、')}
                    </p>
                  ) : null}
                  {review.imageUrl ? (
                    <img src={resolveApiMediaUrl(review.imageUrl)} alt="评价图片" className="mt-2 aspect-video w-full max-w-xs rounded-lg object-cover" />
                  ) : null}
                  <p className="mt-2 text-xs text-slate-500">赞同 {review.upvotes} · 反对 {review.downvotes}</p>
                  {review.merchantReply ? (
                    <div className="mt-3 rounded-lg border border-orange-100 bg-orange-50 px-3 py-2">
                      <p className="flex items-center gap-1 text-xs font-semibold text-orange-700">
                        <MessageSquareReply className="size-3.5" />
                        商家回复
                      </p>
                      <p className="mt-1 text-sm leading-6 text-slate-700">{review.merchantReply}</p>
                    </div>
                  ) : null}
                  <div className="mt-3 space-y-2">
                    <Label htmlFor={`review-reply-${review.id}`}>
                      {review.merchantReply ? '修改回复' : '回复评价'}
                    </Label>
                    <Textarea
                      id={`review-reply-${review.id}`}
                      value={replyDrafts[review.id] ?? review.merchantReply ?? ''}
                      maxLength={500}
                      placeholder="感谢您的评价，我们会继续改进。"
                      onChange={(event) =>
                        setReplyDrafts((state) => ({
                          ...state,
                          [review.id]: event.target.value,
                        }))
                      }
                    />
                    <div className="flex items-center justify-between gap-3">
                      <span className="text-xs text-slate-500">
                        {(replyDrafts[review.id] ?? review.merchantReply ?? '').trim().length}/500
                      </span>
                      <Button
                        type="button"
                        size="sm"
                        disabled={submittingReplyId === review.id || !(replyDrafts[review.id] ?? review.merchantReply ?? '').trim()}
                        onClick={() => void handleReplyReview(review.id)}
                      >
                        {submittingReplyId === review.id ? '发布中...' : review.merchantReply ? '保存回复' : '发布回复'}
                      </Button>
                    </div>
                  </div>
                </article>
              ))
            )}
                </>
              )}
            </section>
          </>
        )}
      </CardContent>
      <Dialog open={reviewingRefund !== null} onOpenChange={(open) => {
        if (!open) {
          setReviewingRefund(null)
          setRefundReviewReason('')
        }
      }}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{reviewingRefund?.action === 'accept' ? '同意退款' : '驳回退款'}</DialogTitle>
          </DialogHeader>
          <div className="space-y-2">
            <Label htmlFor="refund-review-reason">
              {reviewingRefund?.action === 'accept' ? '处理说明（选填）' : '驳回理由'}
            </Label>
            <Textarea
              id="refund-review-reason"
              value={refundReviewReason}
              maxLength={300}
              placeholder={reviewingRefund?.action === 'accept' ? '例如：已核实情况，同意退款。' : '请说明驳回原因，顾客会看到这条反馈。'}
              onChange={(event) => setRefundReviewReason(event.target.value)}
            />
            <p className="text-xs text-slate-500">{refundReviewReason.trim().length}/300</p>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setReviewingRefund(null)}>取消</Button>
            <Button
              variant={reviewingRefund?.action === 'reject' ? 'destructive' : 'default'}
              disabled={processingRefundId === reviewingRefund?.order.id}
              onClick={() => void handleRefundReview()}
            >
              {processingRefundId === reviewingRefund?.order.id ? '处理中...' : '确认'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </Card>
  )
}
