import type { ReactNode } from 'react'
import { NavLink } from 'react-router-dom'

import type { UserRole } from '@/objects/shared/ids'
import { useAuthSession } from '@/hooks/useAuthSession'
import { cn } from '@/lib/utils'

interface NavItem {
  to: string
  label: string
  roles: readonly UserRole[]
}

const navItems: NavItem[] = [
  { to: '/delivery/customer', label: '顾客端', roles: ['customer'] },
  { to: '/delivery/merchant', label: '商家端', roles: ['merchant'] },
  { to: '/delivery/rider', label: '骑手端', roles: ['rider'] },
]

interface DeliveryPageShellProps {
  children: ReactNode
}

export function DeliveryPageShell({ children }: DeliveryPageShellProps) {
  const session = useAuthSession()
  const currentRole: UserRole | null = session?.role ?? null
  const visibleNavItems = navItems.filter((item) => currentRole && item.roles.includes(currentRole))

  return (
    <main className="relative min-h-screen overflow-hidden bg-[linear-gradient(165deg,oklch(0.985_0.02_12)_0%,oklch(0.96_0.03_95)_42%,oklch(0.94_0.04_280/0.18)_100%)] px-5 py-8 sm:px-10 sm:py-10">
      <div
        aria-hidden
        className="delivery-ambient-blob pointer-events-none absolute -left-28 -top-32 h-[22rem] w-[22rem] rounded-full bg-[radial-gradient(circle,oklch(0.88_0.12_12/0.55),transparent_65%)] blur-2xl"
      />
      <div
        aria-hidden
        className="delivery-ambient-blob pointer-events-none absolute -right-20 top-1/3 h-[18rem] w-[18rem] rounded-full bg-[radial-gradient(circle,oklch(0.82_0.14_264/0.35),transparent_68%)] blur-2xl [animation-delay:-4s]"
      />
      <div
        aria-hidden
        className="pointer-events-none absolute inset-x-0 bottom-0 h-48 bg-[linear-gradient(180deg,transparent,oklch(0.97_0.02_12/0.9))]"
      />
      <section className="relative mx-auto max-w-6xl space-y-6">
        {visibleNavItems.length > 0 ? (
          <nav className="flex flex-wrap gap-2 rounded-2xl border border-white/70 bg-white/80 p-4 shadow-[0_18px_50px_rgba(15,23,42,0.06)] backdrop-blur-xl dark:border-white/10 dark:bg-slate-950/55 sm:p-5">
            {visibleNavItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) =>
                  cn(
                    'cursor-pointer rounded-full border px-3.5 py-1.5 text-sm font-medium transition-[color,background-color,border-color,box-shadow] duration-200',
                    isActive
                      ? 'border-transparent bg-gradient-to-r from-primary to-[oklch(0.62_0.18_45)] text-primary-foreground shadow-[0_10px_30px_rgba(225,29,72,0.35)] dark:to-[oklch(0.68_0.14_45)]'
                      : 'border-border/80 bg-background/90 text-foreground/85 hover:border-primary/35 hover:bg-primary/5 hover:text-foreground',
                  )
                }
              >
                {item.label}
              </NavLink>
            ))}
          </nav>
        ) : null}
        {children}
      </section>
    </main>
  )
}
