import type { ReactNode } from 'react'

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'

interface AuthPageShellProps {
  title: string
  description: string
  children: ReactNode
}

export function AuthPageShell({ title, description, children }: AuthPageShellProps) {
  return (
    <main className="relative min-h-screen overflow-hidden bg-[linear-gradient(165deg,oklch(0.985_0.02_12)_0%,oklch(0.96_0.03_95)_48%,oklch(0.94_0.04_280/0.2)_100%)]">
      <div
        aria-hidden
        className="delivery-ambient-blob pointer-events-none absolute -left-24 -top-28 h-[20rem] w-[20rem] rounded-full bg-[radial-gradient(circle,oklch(0.88_0.12_12/0.5),transparent_65%)] blur-2xl"
      />
      <div
        aria-hidden
        className="delivery-ambient-blob pointer-events-none absolute -right-16 bottom-0 h-[18rem] w-[18rem] rounded-full bg-[radial-gradient(circle,oklch(0.82_0.14_264/0.32),transparent_68%)] blur-2xl [animation-delay:-5s]"
      />
      <div className="absolute inset-0 bg-[linear-gradient(135deg,rgba(255,255,255,0.72),transparent_46%,rgba(37,99,235,0.05)_100%)]" />
      <section className="relative mx-auto flex min-h-screen w-full max-w-5xl items-center justify-center px-6 py-16 sm:px-8">
        <Card className="w-full max-w-lg border-white/70 bg-white/85 py-0 shadow-[0_32px_90px_rgba(15,23,42,0.12)] backdrop-blur-xl dark:border-white/10 dark:bg-slate-950/60">
          <CardHeader className="gap-3 border-b border-border/70 px-7 py-7">
            <CardTitle className="text-balance text-2xl font-semibold tracking-tight text-foreground">{title}</CardTitle>
            <CardDescription className="text-pretty leading-relaxed">{description}</CardDescription>
          </CardHeader>
          <CardContent className="px-7 py-7">{children}</CardContent>
        </Card>
      </section>
    </main>
  )
}
