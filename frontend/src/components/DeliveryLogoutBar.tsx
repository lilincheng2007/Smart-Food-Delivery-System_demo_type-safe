import { useNavigate } from 'react-router-dom'

import { Button } from '@/components/ui/button'
import { runTask } from '@/api/shared/TaskIO'
import { clearAuthSessionIO } from '@/lib/auth-session'
import { useAuthSession } from '@/hooks/useAuthSession'

export function DeliveryLogoutBar() {
  const navigate = useNavigate()
  const session = useAuthSession()

  return (
    <div className="flex flex-col items-center gap-3 border-t border-border/60 pt-6">
      {session ? <p className="text-xs text-muted-foreground">当前账号：{session.account}</p> : null}
      <Button
        type="button"
        variant="outline"
        className="cursor-pointer min-w-[9rem] border-border/80 transition-colors hover:border-primary/40 hover:bg-primary/5"
        onClick={() => {
          void runTask(clearAuthSessionIO()).then(() => {
            navigate('/auth/login')
          })
        }}
      >
        退出登录
      </Button>
    </div>
  )
}
