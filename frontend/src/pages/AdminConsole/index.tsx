import { useEffect, useMemo, useState } from 'react'
import { CheckCircle2, ChevronDown, ChevronUp, RefreshCw, XCircle } from 'lucide-react'

import { acceptRefundRequestIO } from '@/apis/admin/AdminRefundAcceptAPI'
import { fetchAdminRefundRequestsIO } from '@/apis/admin/AdminRefundRequestsAPI'
import { rejectRefundRequestIO } from '@/apis/admin/AdminRefundRejectAPI'
import { acceptStoreOnboardingRequestIO } from '@/apis/admin/AdminStoreOnboardingAcceptAPI'
import { fetchAdminStoreOnboardingRequestsIO } from '@/apis/admin/AdminStoreOnboardingRequestsAPI'
import { rejectStoreOnboardingRequestIO } from '@/apis/admin/AdminStoreOnboardingRejectAPI'
import { runTask } from '@/apis/shared/client'
import { DeliveryLogoutBar } from '@/components/DeliveryLogoutBar'
import { DeliveryPageShell } from '@/components/DeliveryPageShell'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { useAppChrome } from '@/hooks/useAppChrome'
import { resolveApiMediaUrl } from '@/lib/api-media-url'
import type { StoreOnboardingRequest, StoreOnboardingStatus } from '@/objects/admin/StoreOnboardingRequest'
import type { Order } from '@/objects/order/Order'
import { RefundStatuses, type RefundStatus } from '@/objects/shared/ids'

const statusLabels: Record<StoreOnboardingStatus, string> = {
  pending: '待审核',
  accepted: '已通过',
  rejected: '已驳回',
}

const refundStatusLabels: Record<RefundStatus, string> = {
  [RefundStatuses.pending]: '待商家处理',
  [RefundStatuses.legacyPending]: '待商家处理',
  [RefundStatuses.merchantRejected]: '商家已驳回',
  [RefundStatuses.adminPending]: '待管理员仲裁',
  [RefundStatuses.accepted]: '已通过',
  [RefundStatuses.rejected]: '已驳回',
}

function statusBadgeVariant(status: StoreOnboardingStatus) {
  if (status === 'accepted') return 'default'
  if (status === 'rejected') return 'destructive'
  return 'outline'
}

function refundStatusBadgeVariant(status: RefundStatus | null | undefined) {
  if (status === RefundStatuses.accepted) return 'default'
  if (status === RefundStatuses.rejected || status === RefundStatuses.merchantRejected) return 'destructive'
  return 'outline'
}

function formatDate(value: string | null | undefined) {
  if (!value) return '暂无'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString()
}

const CollapsedListLimit = 3

export default function AdminConsole() {
  const { showNotice } = useAppChrome()
  const [requests, setRequests] = useState<StoreOnboardingRequest[]>([])
  const [refundRequests, setRefundRequests] = useState<Order[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [rejectingRequest, setRejectingRequest] = useState<StoreOnboardingRequest | null>(null)
  const [reviewingRefund, setReviewingRefund] = useState<{ order: Order; action: 'accept' | 'reject' } | null>(null)
  const [rejectionReason, setRejectionReason] = useState('')
  const [refundReviewReason, setRefundReviewReason] = useState('')
  const [processingId, setProcessingId] = useState<string | null>(null)
  const [showAllOnboardingRequests, setShowAllOnboardingRequests] = useState(false)
  const [showAllRefundRequests, setShowAllRefundRequests] = useState(false)

  const pendingCount = useMemo(() => requests.filter((request) => request.status === 'pending').length, [requests])
  const refundPendingCount = useMemo(
    () => refundRequests.filter((order) => order.refundStatus === RefundStatuses.adminPending).length,
    [refundRequests],
  )
  const displayedRequests = showAllOnboardingRequests ? requests : requests.slice(0, CollapsedListLimit)
  const displayedRefundRequests = showAllRefundRequests ? refundRequests : refundRequests.slice(0, CollapsedListLimit)

  const loadRequests = async () => {
    setIsLoading(true)
    setErrorMessage(null)
    try {
      const [response, refundResponse] = await Promise.all([
        runTask(fetchAdminStoreOnboardingRequestsIO()),
        runTask(fetchAdminRefundRequestsIO()),
      ])
      setRequests(response.requests)
      setRefundRequests(refundResponse.requests)
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : '加载审核申请失败')
    } finally {
      setIsLoading(false)
    }
  }

  const handleRefundReview = async () => {
    if (!reviewingRefund) return
    const reason = refundReviewReason.trim()
    if (reviewingRefund.action === 'reject' && !reason) {
      showNotice('请填写退款驳回原因。', 'error')
      return
    }

    setProcessingId(reviewingRefund.order.id)
    try {
      if (reviewingRefund.action === 'accept') {
        await runTask(acceptRefundRequestIO(reviewingRefund.order.id, reason || null))
        showNotice('退款已通过，款项已退回顾客钱包。', 'success')
      } else {
        await runTask(rejectRefundRequestIO(reviewingRefund.order.id, reason))
        showNotice('退款申请已驳回。', 'success')
      }
      setReviewingRefund(null)
      setRefundReviewReason('')
      await loadRequests()
    } catch (error) {
      showNotice(error instanceof Error ? error.message : '退款审核失败', 'error')
    } finally {
      setProcessingId(null)
    }
  }

  useEffect(() => {
    void loadRequests()
  }, [])

  const handleAccept = async (requestId: string) => {
    setProcessingId(requestId)
    try {
      await runTask(acceptStoreOnboardingRequestIO(requestId))
      showNotice('店铺申请已通过。', 'success')
      await loadRequests()
    } catch (error) {
      showNotice(error instanceof Error ? error.message : '通过申请失败', 'error')
    } finally {
      setProcessingId(null)
    }
  }

  const handleReject = async () => {
    if (!rejectingRequest) return
    if (!rejectionReason.trim()) {
      showNotice('请填写驳回原因。', 'error')
      return
    }

    setProcessingId(rejectingRequest.id)
    try {
      await runTask(rejectStoreOnboardingRequestIO(rejectingRequest.id, rejectionReason.trim()))
      showNotice('店铺申请已驳回。', 'success')
      setRejectingRequest(null)
      setRejectionReason('')
      await loadRequests()
    } catch (error) {
      showNotice(error instanceof Error ? error.message : '驳回申请失败', 'error')
    } finally {
      setProcessingId(null)
    }
  }

  return (
    <DeliveryPageShell>
      <Card className="border-slate-200 bg-white/95">
        <CardHeader className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <CardTitle>店铺入驻审核</CardTitle>
            <p className="mt-1 text-sm text-slate-500">待审核 {pendingCount} 个，共 {requests.length} 个申请</p>
          </div>
          <Button type="button" variant="outline" onClick={() => void loadRequests()} disabled={isLoading}>
            <RefreshCw className="size-4" />
            刷新
          </Button>
        </CardHeader>
        <CardContent className="space-y-3">
          {isLoading ? <p className="text-sm text-slate-500">加载中…</p> : null}
          {errorMessage ? <p className="text-sm text-rose-600">{errorMessage}</p> : null}
          {!isLoading && !errorMessage && requests.length === 0 ? (
            <p className="text-sm text-slate-500">暂无店铺入驻申请。</p>
          ) : null}

          {displayedRequests.map((request) => (
            <article key={request.id} className="rounded-lg border border-slate-200 bg-white p-4">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                <div className="space-y-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <h2 className="text-base font-semibold text-slate-950">{request.storeName}</h2>
                    <Badge variant={statusBadgeVariant(request.status)}>{statusLabels[request.status]}</Badge>
                  </div>
                  <p className="text-sm text-slate-600">{request.address}</p>
                  <p className="text-sm text-slate-500">申请商家：{request.ownerUsername}</p>
                </div>
                {request.status === 'pending' ? (
                  <div className="flex shrink-0 gap-2">
                    <Button
                      type="button"
                      size="sm"
                      onClick={() => void handleAccept(request.id)}
                      disabled={processingId === request.id}
                    >
                      <CheckCircle2 className="size-4" />
                      通过
                    </Button>
                    <Button
                      type="button"
                      size="sm"
                      variant="outline"
                      onClick={() => {
                        setRejectingRequest(request)
                        setRejectionReason('')
                      }}
                      disabled={processingId === request.id}
                    >
                      <XCircle className="size-4" />
                      驳回
                    </Button>
                  </div>
                ) : null}
              </div>

              <div className="mt-3 grid gap-3 text-sm text-slate-600 md:grid-cols-[1fr_14rem]">
                <p className="leading-6">{request.description}</p>
                <div className="space-y-1 rounded-md bg-slate-50 p-3 text-xs text-slate-500">
                  <p>提交时间：{formatDate(request.createdAt)}</p>
                  <p>审核时间：{formatDate(request.reviewedAt)}</p>
                  {request.reviewedBy ? <p>审核人：{request.reviewedBy}</p> : null}
                  {request.rejectionReason ? <p className="text-rose-600">驳回原因：{request.rejectionReason}</p> : null}
                </div>
              </div>
            </article>
          ))}
          {!showAllOnboardingRequests && requests.length > CollapsedListLimit ? (
            <Button type="button" variant="ghost" className="mx-auto flex cursor-pointer text-slate-500" onClick={() => setShowAllOnboardingRequests(true)}>
              更多
              <ChevronDown className="size-4" />
            </Button>
          ) : null}
          {showAllOnboardingRequests && requests.length > CollapsedListLimit ? (
            <Button type="button" variant="ghost" className="mx-auto flex cursor-pointer text-slate-500" onClick={() => setShowAllOnboardingRequests(false)}>
              收起
              <ChevronUp className="size-4" />
            </Button>
          ) : null}
        </CardContent>
      </Card>

      <Card className="border-slate-200 bg-white/95">
        <CardHeader className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <CardTitle>退款仲裁审核</CardTitle>
            <p className="mt-1 text-sm text-slate-500">待仲裁 {refundPendingCount} 个，共 {refundRequests.length} 个申请</p>
          </div>
          <Button type="button" variant="outline" onClick={() => void loadRequests()} disabled={isLoading}>
            <RefreshCw className="size-4" />
            刷新
          </Button>
        </CardHeader>
        <CardContent className="space-y-3">
          {isLoading ? <p className="text-sm text-slate-500">加载中…</p> : null}
          {!isLoading && refundRequests.length === 0 ? (
            <p className="text-sm text-slate-500">暂无退款申请。</p>
          ) : null}
          {displayedRefundRequests.map((order) => {
            const refundStatus = order.refundStatus ?? RefundStatuses.pending
            const isPendingRefund = refundStatus === RefundStatuses.adminPending
            return (
              <article key={order.id} className="rounded-lg border border-slate-200 bg-white p-4">
                <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
                  <div className="min-w-0 space-y-2">
                    <div className="flex flex-wrap items-center gap-2">
                      <h2 className="text-base font-semibold text-slate-950">订单 {order.id}</h2>
                      <Badge variant={refundStatusBadgeVariant(refundStatus)}>{refundStatusLabels[refundStatus]}</Badge>
                    </div>
                    <p className="text-sm text-slate-600">顾客：{order.customerName} · {order.customerPhone}</p>
                    <p className="text-sm text-slate-600">退款金额：¥{order.payableAmount.toFixed(2)}</p>
                    <p className="text-sm text-slate-600">菜品：{order.items.map((item) => `${item.name}×${item.quantity}`).join('、')}</p>
                    {order.refundReason ? <p className="text-sm leading-6 text-slate-700">顾客理由：{order.refundReason}</p> : null}
                    {order.refundMerchantReason ? (
                      <p className="text-sm leading-6 text-orange-700">商家理由：{order.refundMerchantReason}</p>
                    ) : null}
                    {order.refundAdminReason ? (
                      <p className={order.refundStatus === RefundStatuses.rejected ? 'text-sm leading-6 text-rose-600' : 'text-sm leading-6 text-slate-700'}>
                        管理员反馈：{order.refundAdminReason}
                      </p>
                    ) : null}
                    {order.refundedAt ? <p className="text-sm text-slate-500">退款时间：{formatDate(order.refundedAt)}</p> : null}
                    {order.refundImageUrl ? (
                      <img
                        src={resolveApiMediaUrl(order.refundImageUrl)}
                        alt="退款凭证"
                        className="aspect-video w-full max-w-sm rounded-lg border border-slate-200 object-cover"
                      />
                    ) : null}
                  </div>
                  {isPendingRefund ? (
                    <div className="flex shrink-0 gap-2">
                      <Button
                        type="button"
                        size="sm"
                        onClick={() => {
                          setReviewingRefund({ order, action: 'accept' })
                          setRefundReviewReason('')
                        }}
                        disabled={processingId === order.id}
                      >
                        <CheckCircle2 className="size-4" />
                        通过退款
                      </Button>
                      <Button
                        type="button"
                        size="sm"
                        variant="outline"
                        onClick={() => {
                          setReviewingRefund({ order, action: 'reject' })
                          setRefundReviewReason('')
                        }}
                        disabled={processingId === order.id}
                      >
                        <XCircle className="size-4" />
                        驳回
                      </Button>
                    </div>
                  ) : null}
                </div>
              </article>
            )
          })}
          {!showAllRefundRequests && refundRequests.length > CollapsedListLimit ? (
            <Button type="button" variant="ghost" className="mx-auto flex cursor-pointer text-slate-500" onClick={() => setShowAllRefundRequests(true)}>
              更多
              <ChevronDown className="size-4" />
            </Button>
          ) : null}
          {showAllRefundRequests && refundRequests.length > CollapsedListLimit ? (
            <Button type="button" variant="ghost" className="mx-auto flex cursor-pointer text-slate-500" onClick={() => setShowAllRefundRequests(false)}>
              收起
              <ChevronUp className="size-4" />
            </Button>
          ) : null}
        </CardContent>
      </Card>

      <Dialog open={Boolean(rejectingRequest)} onOpenChange={(open) => !open && setRejectingRequest(null)}>
        <DialogContent className="max-h-[min(88vh,34rem)] w-[min(40rem,calc(100vw-2rem))] overflow-y-auto rounded-2xl bg-white p-5 sm:p-6">
          <DialogHeader className="pr-8">
            <DialogTitle className="leading-6">填写驳回原因</DialogTitle>
          </DialogHeader>
          <div className="space-y-2">
            <Label htmlFor="rejection-reason">驳回原因</Label>
            <Textarea
              id="rejection-reason"
              value={rejectionReason}
              className="min-h-40 resize-y break-words"
              placeholder="请输入需要商家修改或补充的信息"
              onChange={(event) => setRejectionReason(event.target.value)}
            />
          </div>
          <DialogFooter className="pt-2">
            <Button type="button" variant="outline" onClick={() => setRejectingRequest(null)}>
              取消
            </Button>
            <Button type="button" onClick={() => void handleReject()} disabled={Boolean(processingId)}>
              确认驳回
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={Boolean(reviewingRefund)} onOpenChange={(open) => !open && setReviewingRefund(null)}>
        <DialogContent className="max-h-[min(88vh,34rem)] w-[min(40rem,calc(100vw-2rem))] overflow-y-auto rounded-2xl bg-white p-5 sm:p-6">
          <DialogHeader className="pr-8">
            <DialogTitle className="leading-6">
              {reviewingRefund?.action === 'accept' ? '通过退款申请' : '驳回退款申请'}
            </DialogTitle>
          </DialogHeader>
          <div className="space-y-2">
            <Label htmlFor="refund-review-reason">
              {reviewingRefund?.action === 'accept' ? '通过说明（选填）' : '驳回原因'}
            </Label>
            <Textarea
              id="refund-review-reason"
              value={refundReviewReason}
              className="min-h-32 resize-y break-words"
              placeholder={reviewingRefund?.action === 'accept' ? '可填写退款处理说明' : '请输入驳回退款的理由'}
              onChange={(event) => setRefundReviewReason(event.target.value)}
            />
          </div>
          <DialogFooter className="pt-2">
            <Button type="button" variant="outline" onClick={() => setReviewingRefund(null)}>
              取消
            </Button>
            <Button type="button" onClick={() => void handleRefundReview()} disabled={Boolean(processingId)}>
              确认
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <DeliveryLogoutBar />
    </DeliveryPageShell>
  )
}
