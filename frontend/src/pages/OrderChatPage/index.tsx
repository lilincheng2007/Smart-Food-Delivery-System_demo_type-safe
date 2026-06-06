import { type ChangeEvent, type FormEvent, useEffect, useMemo, useRef, useState } from 'react'
import { ArrowLeft, ImagePlus, Loader2, Send, X } from 'lucide-react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'

import { fetchCatalogIO } from '@/apis/merchant/CatalogAPI'
import { fetchMerchantMeIO } from '@/apis/merchant/MerchantMeAPI'
import { fetchCustomerOrdersIO } from '@/apis/order/CustomerOrdersAPI'
import { fetchOrderChatMessagesIO, sendOrderChatMessageIO, uploadOrderChatImageIO } from '@/apis/order/OrderChatAPI'
import { fetchRiderMeIO } from '@/apis/rider/RiderMeAPI'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { useAuthSession } from '@/hooks/useAuthSession'
import type { Order } from '@/objects/order/Order'
import type { OrderChatMessage, OrderChatRole } from '@/objects/order/OrderChatMessage'
import { UserRoles } from '@/objects/shared/ids'

const roleLabels: Record<OrderChatRole, string> = {
  customer: '顾客',
  merchant: '商家',
  rider: '骑手',
}

const chatPeers: Record<OrderChatRole, OrderChatRole[]> = {
  customer: ['merchant', 'rider'],
  merchant: ['customer', 'rider'],
  rider: ['customer', 'merchant'],
}

function isOrderChatRole(value: string | null): value is OrderChatRole {
  return value === UserRoles.customer || value === UserRoles.merchant || value === UserRoles.rider
}

function formatMessageTime(value: string): string {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return date.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false })
}

function customerDisplay(order: Order | undefined) {
  return order?.customerName || order?.customerId || '顾客'
}

export default function OrderChatPage() {
  const { orderId = '' } = useParams()
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const session = useAuthSession()
  const [messages, setMessages] = useState<OrderChatMessage[]>([])
  const [draft, setDraft] = useState('')
  const [pendingImage, setPendingImage] = useState<{ file: File; previewUrl: string } | null>(null)
  const [loading, setLoading] = useState(true)
  const [sending, setSending] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [peerTitle, setPeerTitle] = useState('')
  const [contextLine, setContextLine] = useState('')
  const fileInputRef = useRef<HTMLInputElement | null>(null)
  const messageEndRef = useRef<HTMLDivElement | null>(null)

  const sessionRole = session?.role ?? null
  const currentRole = isOrderChatRole(sessionRole) ? sessionRole : null
  const peerRole = useMemo(() => {
    const rawPeer = searchParams.get('peer')
    if (!isOrderChatRole(rawPeer) || !currentRole) return null
    return chatPeers[currentRole].includes(rawPeer) ? rawPeer : null
  }, [currentRole, searchParams])

  const loadMessages = async (showLoading: boolean) => {
    if (!orderId || !peerRole) return
    if (showLoading) setLoading(true)
    try {
      const response = await fetchOrderChatMessagesIO(orderId, peerRole)()
      setMessages(response.messages)
      setError(null)
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : '聊天记录加载失败')
    } finally {
      if (showLoading) setLoading(false)
    }
  }

  const loadPeerContext = async () => {
    if (!orderId || !peerRole || !currentRole) return
    try {
      if (currentRole === UserRoles.customer) {
        const [ordersResponse, catalog] = await Promise.all([fetchCustomerOrdersIO()(), fetchCatalogIO()()])
        const order = [...ordersResponse.pendingOrders, ...ordersResponse.historyOrders].find((item) => item.id === orderId)
        const merchant = order ? catalog.merchants.find((item) => item.id === order.merchantId) : null
        setPeerTitle(peerRole === UserRoles.merchant ? (merchant?.storeName ?? '商家') : (order?.riderId ? `骑手 ${order.riderId}` : '骑手'))
        setContextLine(merchant ? `订单 ${orderId} · ${merchant.storeName}` : `订单 ${orderId}`)
        return
      }

      if (currentRole === UserRoles.merchant) {
        const me = await fetchMerchantMeIO()()
        const stores = me.merchantAccount.profile.stores ?? []
        const orders = stores.flatMap((store) => [...store.pendingOrders, ...store.historyOrders])
        const order = orders.find((item) => item.id === orderId)
        const store = stores.find((item) => item.merchant.id === order?.merchantId)
        setPeerTitle(peerRole === UserRoles.customer ? customerDisplay(order) : (order?.riderId ? `骑手 ${order.riderId}` : '骑手'))
        setContextLine(`订单 ${orderId}${store ? ` · ${store.merchant.storeName}` : ''}`)
        return
      }

      if (currentRole === UserRoles.rider) {
        const [me, catalog] = await Promise.all([fetchRiderMeIO()(), fetchCatalogIO()().catch(() => ({ merchants: [], products: [], platformPromotions: [] }))])
        const orders = [
          ...(me.riderAccount.profile.pendingOrders ?? []),
          ...(me.riderAccount.profile.historyOrders ?? []),
          ...(me.availableOrders ?? []),
        ]
        const order = orders.find((item) => item.id === orderId)
        const merchant = order ? catalog.merchants.find((item) => item.id === order.merchantId) : null
        setPeerTitle(peerRole === UserRoles.customer ? customerDisplay(order) : (merchant?.storeName ?? '商家'))
        setContextLine(`订单 ${orderId}${merchant ? ` · ${merchant.storeName}` : ''}`)
      }
    } catch {
      setPeerTitle(roleLabels[peerRole])
      setContextLine(`订单 ${orderId}`)
    }
  }

  useEffect(() => {
    void loadMessages(true)
    void loadPeerContext()
    const timer = window.setInterval(() => {
      void loadMessages(false)
    }, 5000)
    return () => window.clearInterval(timer)
  }, [currentRole, orderId, peerRole])

  useEffect(() => {
    messageEndRef.current?.scrollIntoView({ block: 'end' })
  }, [messages.length])

  useEffect(() => {
    return () => {
      if (pendingImage) URL.revokeObjectURL(pendingImage.previewUrl)
    }
  }, [pendingImage])

  const sendMessage = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const content = draft.trim()
    if ((!content && !pendingImage) || !orderId || !peerRole || sending) return
    setSending(true)
    try {
      let latestMessages = messages
      if (pendingImage) {
        const imageUrl = await uploadOrderChatImageIO(pendingImage.file)()
        const response = await sendOrderChatMessageIO(orderId, peerRole, 'image', imageUrl)()
        latestMessages = response.messages
        URL.revokeObjectURL(pendingImage.previewUrl)
        setPendingImage(null)
      }
      if (content) {
        const response = await sendOrderChatMessageIO(orderId, peerRole, 'text', content)()
        latestMessages = response.messages
      }
      setMessages(latestMessages)
      setDraft('')
      setError(null)
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : '消息发送失败')
    } finally {
      setSending(false)
    }
  }

  const sendImage = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file || sending) return
    setPendingImage((current) => {
      if (current) URL.revokeObjectURL(current.previewUrl)
      return { file, previewUrl: URL.createObjectURL(file) }
    })
  }

  const clearPendingImage = () => {
    setPendingImage((current) => {
      if (current) URL.revokeObjectURL(current.previewUrl)
      return null
    })
  }

  const canSend = draft.trim().length > 0 || pendingImage !== null

  if (session === undefined) return null

  if (!orderId || !currentRole || !peerRole) {
    return (
      <div className="flex min-h-screen flex-col bg-[#ededed]">
        <header className="flex h-16 items-center border-b bg-[#f7f7f7] px-4">
          <Button variant="ghost" size="icon" onClick={() => navigate(-1)} aria-label="返回">
            <ArrowLeft className="size-6" />
          </Button>
          <h1 className="flex-1 text-center text-lg font-semibold">订单聊天</h1>
          <span className="size-10" />
        </header>
        <main className="flex flex-1 items-center justify-center px-6 text-center text-sm text-slate-500">聊天对象无效。</main>
      </div>
    )
  }

  return (
    <div className="flex h-screen min-h-screen flex-col bg-[#ededed]">
      <header className="flex h-16 shrink-0 items-center border-b border-slate-200 bg-[#f7f7f7] px-4">
        <Button variant="ghost" size="icon" className="cursor-pointer" onClick={() => navigate(-1)} aria-label="返回">
          <ArrowLeft className="size-6" />
        </Button>
        <div className="min-w-0 flex-1 text-center">
          <h1 className="truncate text-lg font-semibold text-slate-950">{peerTitle || roleLabels[peerRole]}</h1>
          <p className="truncate text-xs text-slate-500">{contextLine || `订单 ${orderId}`}</p>
        </div>
        <span className="size-10" />
      </header>

      <main className="flex-1 overflow-y-auto px-4 py-5">
        {loading ? (
          <div className="flex h-full items-center justify-center text-sm text-slate-500">
            <Loader2 className="mr-2 size-4 animate-spin" />
            正在加载聊天记录
          </div>
        ) : messages.length === 0 ? (
          <div className="flex h-full items-center justify-center text-sm text-slate-400">暂无聊天记录</div>
        ) : (
          <div className="space-y-4 pb-2">
            {messages.map((message) => {
              const mine = message.senderRole === currentRole
              const isImageMessage = message.messageType === 'image'
              return (
                <div key={message.id} className={`flex ${mine ? 'justify-end' : 'justify-start'}`}>
                  <div className={`max-w-[78%] ${mine ? 'items-end' : 'items-start'} flex flex-col gap-1`}>
                    <span className="px-1 text-xs text-slate-400">{formatMessageTime(message.createdAt)}</span>
                    <div className={isImageMessage ? '' : `rounded-2xl px-3 py-2 shadow-sm ${mine ? 'bg-emerald-500 text-white' : 'bg-white text-slate-950'}`}>
                      {isImageMessage ? (
                        <img src={message.content} alt="聊天图片" className="max-h-64 max-w-full rounded-xl object-contain shadow-sm" />
                      ) : (
                        <p className="whitespace-pre-wrap break-words text-sm leading-6">{message.content}</p>
                      )}
                    </div>
                  </div>
                </div>
              )
            })}
            <div ref={messageEndRef} />
          </div>
        )}
      </main>

      {error ? <div className="shrink-0 border-t border-rose-100 bg-rose-50 px-4 py-2 text-sm text-rose-600">{error}</div> : null}

      <form onSubmit={sendMessage} className="shrink-0 border-t border-slate-200 bg-[#f7f7f7] px-3 py-3">
        {pendingImage ? (
          <div className="mb-2 flex items-center gap-2 rounded-xl bg-white p-2">
            <div className="relative h-16 w-16 overflow-hidden rounded-lg border border-slate-200 bg-slate-50">
              <img src={pendingImage.previewUrl} alt="待发送图片" className="h-full w-full object-cover" />
              <button
                type="button"
                className="absolute right-1 top-1 flex size-5 items-center justify-center rounded-full bg-black/55 text-white"
                onClick={clearPendingImage}
                aria-label="移除图片"
              >
                <X className="size-3" />
              </button>
            </div>
            <span className="text-sm text-slate-500">图片已添加，点击发送后发出</span>
          </div>
        ) : null}
        <div className="flex items-center gap-2">
          <input ref={fileInputRef} type="file" accept="image/*" className="hidden" onChange={sendImage} />
          <Button type="button" size="icon" variant="ghost" className="shrink-0 cursor-pointer rounded-full" onClick={() => fileInputRef.current?.click()} disabled={sending} aria-label="添加图片">
            <ImagePlus className="size-6" />
          </Button>
          <Input
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            placeholder="输入消息"
            className="h-11 flex-1 rounded-md border-white bg-white"
            disabled={sending}
          />
          <Button type="submit" size="icon" className="shrink-0 cursor-pointer rounded-full" disabled={sending || !canSend} aria-label="发送">
            {sending ? <Loader2 className="size-4 animate-spin" /> : <Send className="size-4" />}
          </Button>
        </div>
      </form>
    </div>
  )
}
