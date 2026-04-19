import type { ReactNode } from 'react'

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'

interface AuthPageShellProps {
  title: string
  description: string
  children: ReactNode
}

export function AuthPageShell({ title, description, children }: AuthPageShellProps) {
  return (
    <main className="relative min-h-screen overflow-hidden bg-[radial-gradient(circle_at_top,rgba(255,212,174,0.35),transparent_36%),linear-gradient(180deg,#fffaf4_0%,#fff7ee_48%,#fff4ea_100%)]">
      <div className="absolute inset-0 bg-[linear-gradient(135deg,rgba(255,255,255,0.6),transparent_42%,rgba(15,23,42,0.04)_100%)]" />
      <section className="relative mx-auto flex min-h-screen w-full max-w-5xl items-center justify-center px-6 py-16 sm:px-8">
        <Card className="w-full max-w-lg border-orange-100 bg-white/92 py-0 shadow-[0_28px_80px_rgba(15,23,42,0.1)]">
          <CardHeader className="gap-3 border-b border-orange-100 px-7 py-7">
            <CardTitle className="text-2xl text-slate-900">{title}</CardTitle>
            <CardDescription>{description}</CardDescription>
          </CardHeader>
          <CardContent className="px-7 py-7">{children}</CardContent>
        </Card>
      </section>
    </main>
  )
}
