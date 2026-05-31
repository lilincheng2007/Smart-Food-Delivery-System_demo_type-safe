import { Loader2, Search, Sparkles, X } from 'lucide-react'
import { useCallback, useEffect, useRef, useState } from 'react'

type AISearchBarProps = {
  onSearch: (query: string) => void
  loading: boolean
}

export function AISearchBar({ onSearch, loading }: AISearchBarProps) {
  const [query, setQuery] = useState('')
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  const triggerSearch = useCallback(
    (value: string) => {
      if (value.trim().length >= 2) {
        onSearch(value.trim())
      }
    },
    [onSearch],
  )

  const handleInputChange = useCallback(
    (value: string) => {
      setQuery(value)
      if (debounceRef.current) clearTimeout(debounceRef.current)
      debounceRef.current = setTimeout(() => triggerSearch(value), 500)
    },
    [triggerSearch],
  )

  const handleClear = useCallback(() => {
    setQuery('')
    if (debounceRef.current) clearTimeout(debounceRef.current)
  }, [])

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent<HTMLInputElement>) => {
      if (e.key === 'Enter' && query.trim().length >= 2) {
        if (debounceRef.current) clearTimeout(debounceRef.current)
        triggerSearch(query)
      }
    },
    [query, triggerSearch],
  )

  useEffect(() => {
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current)
    }
  }, [])

  return (
    <div className="group relative">
      {/* Glowing border effect */}
      <div className="absolute -inset-[1px] rounded-2xl bg-gradient-to-r from-primary/60 via-purple-400/50 to-primary/60 opacity-0 blur-sm transition-opacity duration-500 group-focus-within:opacity-100" />

      <div className="relative flex items-center gap-3 rounded-2xl border border-border/60 bg-card/80 px-4 py-3 shadow-[0_8px_32px_rgba(124,58,237,0.08)] backdrop-blur-xl transition-all duration-300 group-focus-within:border-primary/40 group-focus-within:shadow-[0_8px_40px_rgba(124,58,237,0.15)]">
        <div className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-gradient-to-br from-primary/20 to-purple-400/20 text-primary">
          <Sparkles className="size-4" aria-hidden />
        </div>

        <input
          type="text"
          value={query}
          onChange={(e) => handleInputChange(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="描述你想吃的，AI 帮你找…"
          className="flex-1 border-0 bg-transparent text-sm text-foreground placeholder:text-muted-foreground/70 focus:outline-none focus:ring-0"
        />

        {loading && (
          <Loader2 className="size-4 shrink-0 animate-spin text-primary" aria-label="搜索中" />
        )}

        {!loading && query.length > 0 && (
          <button
            type="button"
            onClick={handleClear}
            className="flex size-6 shrink-0 items-center justify-center rounded-full bg-muted text-muted-foreground transition-colors hover:bg-muted-foreground/20 hover:text-foreground"
            aria-label="清除搜索"
          >
            <X className="size-3.5" />
          </button>
        )}

        {!loading && query.length === 0 && (
          <Search className="size-4 shrink-0 text-muted-foreground/50" aria-hidden />
        )}
      </div>
    </div>
  )
}
