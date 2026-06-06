import { CalendarDays, ChevronDown } from 'lucide-react'
import { useState } from 'react'
import type { ReactNode } from 'react'

import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'

type PromotionDateInputProps = {
  value: string | null | undefined
  onChange: (value: string | null) => void
}

const parseStoredDate = (value: string | null | undefined) => {
  const [year, month, day] = (value ?? '').split('-').map((part) => Number(part))
  if (!year || !month || !day) return null
  const date = new Date(year, month - 1, day)
  if (date.getFullYear() !== year || date.getMonth() !== month - 1 || date.getDate() !== day) return null
  return { year, month, day }
}

const daysInMonth = (year: number, month: number) => new Date(year, month, 0).getDate()
const pad = (value: number) => value.toString().padStart(2, '0')
const toStoredDate = (year: number, month: number, day: number) => `${year}-${pad(month)}-${pad(day)}`

export function PromotionDateInput({ value, onChange }: PromotionDateInputProps) {
  const parsed = parseStoredDate(value)
  const today = new Date()
  const currentYear = today.getFullYear()
  const [draftYear, setDraftYear] = useState(parsed?.year ?? today.getFullYear())
  const [draftMonth, setDraftMonth] = useState(parsed?.month ?? today.getMonth() + 1)
  const [openPart, setOpenPart] = useState<'year' | 'month' | 'day' | null>(null)
  const maxDay = daysInMonth(draftYear, draftMonth)
  const selectedDay = parsed?.year === draftYear && parsed.month === draftMonth ? parsed.day : Math.min(parsed?.day ?? today.getDate(), maxDay)
  const yearOptions = Array.from({ length: 15 }, (_, index) => currentYear - 2 + index)

  const commit = (year: number, month: number, day: number) => {
    onChange(toStoredDate(year, month, Math.min(day, daysInMonth(year, month))))
  }

  const displayYear = parsed?.year ?? null
  const displayMonth = parsed?.month ?? null
  const displayDay = parsed?.day ?? null

  return (
    <div className="relative space-y-2">
      <div className="grid grid-cols-3 gap-2">
        <DatePartButton
          active={openPart === 'year'}
          label={displayYear ? `${displayYear}年` : '选择年'}
          onClick={() => setOpenPart((part) => part === 'year' ? null : 'year')}
        />
        <DatePartButton
          active={openPart === 'month'}
          label={displayMonth ? `${displayMonth}月` : '选择月'}
          onClick={() => setOpenPart((part) => part === 'month' ? null : 'month')}
        />
        <DatePartButton
          active={openPart === 'day'}
          label={displayDay ? `${displayDay}日` : '选择日'}
          onClick={() => setOpenPart((part) => part === 'day' ? null : 'day')}
        />
      </div>
      {openPart ? (
        <div className="absolute bottom-[calc(100%+0.5rem)] left-0 z-50 w-[19rem] rounded-2xl border border-orange-100 bg-white p-3 shadow-xl">
          <div className="mb-3 flex items-center justify-between gap-2">
            <div className="flex items-center gap-2 text-sm font-semibold text-slate-900">
              <CalendarDays className="size-4 text-orange-500" />
              {openPart === 'year' ? '选择年份' : openPart === 'month' ? '选择月份' : '选择日期'}
            </div>
            <Button type="button" size="sm" variant="ghost" className="h-8 px-2 text-xs" onClick={() => onChange(null)}>
              清空
            </Button>
          </div>

          {openPart === 'year' ? (
            <div className="grid grid-cols-3 gap-2">
              {yearOptions.map((year) => (
                <PickerButton
                  key={year}
                  selected={displayYear === year}
                  onClick={() => {
                    setDraftYear(year)
                    commit(year, draftMonth, selectedDay)
                    setOpenPart('month')
                  }}
                >
                  {year}年
                </PickerButton>
              ))}
            </div>
          ) : null}

          {openPart === 'month' ? (
            <div className="grid grid-cols-4 gap-2">
              {Array.from({ length: 12 }, (_, index) => index + 1).map((month) => (
                <PickerButton
                  key={month}
                  selected={displayYear === draftYear && displayMonth === month}
                  onClick={() => {
                    setDraftMonth(month)
                    commit(draftYear, month, selectedDay)
                    setOpenPart('day')
                  }}
                >
                  {month}月
                </PickerButton>
              ))}
            </div>
          ) : null}

          {openPart === 'day' ? (
            <div className="grid grid-cols-7 gap-1.5">
              {Array.from({ length: maxDay }, (_, index) => index + 1).map((day) => (
                <PickerButton
                  key={day}
                  selected={displayYear === draftYear && displayMonth === draftMonth && displayDay === day}
                  className="h-9 px-0"
                  onClick={() => {
                    commit(draftYear, draftMonth, day)
                    setOpenPart(null)
                  }}
                >
                  {day}
                </PickerButton>
              ))}
            </div>
          ) : null}
        </div>
      ) : null}
    </div>
  )
}

function DatePartButton({ active, label, onClick }: { active: boolean; label: string; onClick: () => void }) {
  return (
    <Button
      type="button"
      variant="outline"
      className={cn(
        'h-10 justify-between border-orange-100 bg-white px-3 text-slate-700 hover:border-orange-200 hover:bg-orange-50',
        active && 'border-orange-300 bg-orange-50 text-orange-700',
      )}
      onClick={onClick}
    >
      <span className="truncate">{label}</span>
      <ChevronDown className={cn('size-3.5 transition-transform', active && 'rotate-180')} />
    </Button>
  )
}

function PickerButton({
  selected,
  className,
  children,
  onClick,
}: {
  selected: boolean
  className?: string
  children: ReactNode
  onClick: () => void
}) {
  return (
    <button
      type="button"
      className={cn(
        'h-10 rounded-xl border border-orange-100 bg-orange-50/40 px-2 text-sm font-medium text-slate-700 transition-colors hover:border-orange-300 hover:bg-orange-50 hover:text-orange-700',
        selected && 'border-orange-500 bg-orange-500 text-white hover:bg-orange-500 hover:text-white',
        className,
      )}
      onClick={onClick}
    >
      {children}
    </button>
  )
}

export function PromotionEnableControl({ enabled, onChange }: { enabled: boolean; onChange: (enabled: boolean) => void }) {
  return (
    <label className="flex w-full cursor-pointer flex-col gap-2 rounded-xl border-2 border-rose-300 bg-rose-50 px-4 py-3 text-rose-800 shadow-sm sm:w-52">
      <span className="inline-flex items-center justify-between gap-2">
        <span className="inline-flex items-center gap-2 text-sm font-semibold">
          <input type="checkbox" className="size-4 accent-rose-600" checked={enabled} onChange={(event) => onChange(event.target.checked)} />
          启用该优惠
        </span>
      </span>
      <span className={enabled ? 'rounded-lg bg-rose-600 px-3 py-1 text-center text-sm font-semibold text-white' : 'rounded-lg bg-slate-700 px-3 py-1 text-center text-sm font-semibold text-white'}>
        {enabled ? '优惠已启用' : '优惠已禁用'}
      </span>
    </label>
  )
}
