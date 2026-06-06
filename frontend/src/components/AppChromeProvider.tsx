import { useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'

import { FeedbackModal } from '@/components/FeedbackModal'
import { GlobalNotificationCenter } from '@/components/GlobalNotificationCenter'
import { runTask } from '@/apis/shared/TaskIO'
import { clearTimeoutIO, setTimeoutIO } from '@/apis/shared/browser'
import { AppChromeContext } from '@/lib/app-chrome-context'
import type { AgentFeedbackInput } from '@/lib/agent-feedback-client'
import { sendAgentFeedbackIO } from '@/lib/agent-feedback-client'
import type { FeedbackDialogRequest, NoticeState, NoticeTone } from '@/lib/mock-types'
import { cn } from '@/lib/utils'

function resolveTargetName(componentName?: string, eventName?: string) {
  return eventName ?? componentName ?? '未命名交互'
}

function buildFeedbackPayload(request: FeedbackDialogRequest, feedbackText: string): AgentFeedbackInput {
  return {
    pageName: request.pageName,
    route: request.route,
    componentOrEventName: resolveTargetName(request.componentName, request.eventName),
    interactionName: request.interactionName,
    selectedMockResult: request.selectedMockResult ?? '用户主动反馈',
    feedbackText,
    prioritySuggestion: request.prioritySuggestion,
  }
}

export function AppChromeProvider({ children }: { children: ReactNode }) {
  const [feedbackRequest, setFeedbackRequest] = useState<FeedbackDialogRequest | null>(null)
  const [isSubmittingFeedback, setIsSubmittingFeedback] = useState(false)
  const [notice, setNotice] = useState<NoticeState | null>(null)

  const showNotice = (message: string, tone: NoticeTone = 'info') => {
    setNotice({ tone, message })
  }

  const contextValue = useMemo(
    () => ({
      openFeedbackDialog: (request: FeedbackDialogRequest) => setFeedbackRequest(request),
      showNotice,
    }),
    [],
  )

  useEffect(() => {
    if (!notice) {
      return
    }

    let timer = -1
    void runTask(setTimeoutIO(() => setNotice(null), 4000)).then((id) => {
      timer = id
    })

    return () => {
      if (timer >= 0) {
        void runTask(clearTimeoutIO(timer))
      }
    }
  }, [notice])

  const handleFeedbackSubmit = async (text: string) => {
    if (!feedbackRequest) {
      return
    }

    const trimmedText = text.trim()
    if (!trimmedText) {
      return
    }

    setIsSubmittingFeedback(true)
    const result = await runTask(sendAgentFeedbackIO(buildFeedbackPayload(feedbackRequest, trimmedText)))
    setIsSubmittingFeedback(false)
    showNotice(result.message, result.ok ? 'success' : 'error')

    if (result.ok) {
      setFeedbackRequest(null)
    }
  }

  return (
    <AppChromeContext.Provider value={contextValue}>
      {children}
      <GlobalNotificationCenter />
      {feedbackRequest ? (
        <FeedbackModal
          request={feedbackRequest}
          isSubmitting={isSubmittingFeedback}
          onClose={() => setFeedbackRequest(null)}
          onSubmit={handleFeedbackSubmit}
        />
      ) : null}
      {notice && notice.tone === 'error' ? (
        <div className="pointer-events-none fixed bottom-8 left-1/2 z-[18] w-full max-w-md -translate-x-1/2 px-4">
          <div className="rounded-full border border-rose-200 bg-white/95 px-4 py-3 text-center text-sm text-rose-700 shadow-[0_18px_40px_rgba(15,23,42,0.12)] backdrop-blur">
            {notice.message}
          </div>
        </div>
      ) : null}
      {notice && notice.tone !== 'error' ? (
        <div
          className={cn(
            'fixed right-4 bottom-4 z-[18] max-w-[min(420px,calc(100vw-2rem))] rounded-2xl border px-4 py-3 shadow-lg backdrop-blur',
            notice.tone === 'success' && 'border-emerald-200 bg-emerald-50 text-emerald-900',
            notice.tone === 'info' && 'border-border bg-white/95 text-foreground',
          )}
        >
          {notice.message}
        </div>
      ) : null}
    </AppChromeContext.Provider>
  )
}
