import { useState } from 'react'
import { MapPin, Phone, Star, Trash2, UserRound } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Separator } from '@/components/ui/separator'
import { useAppChrome } from '@/hooks/useAppChrome'
import { useCustomerPortalStore } from '@/pages/CustomerPortal/stores/use-customer-portal-store'

import { DeliveryContactAddDialog } from './DeliveryContactAddDialog'
import {
  appendContact,
  normalizedDeliveryContacts,
  removeContactById,
  setDefaultById,
} from '@/lib/deliveryContacts'

export function DeliveryContactsSection() {
  const { showNotice } = useAppChrome()
  const customerAccount = useCustomerPortalStore((s) => s.customerAccount)
  const saveDeliveryContacts = useCustomerPortalStore((s) => s.saveDeliveryContacts)
  const [addOpen, setAddOpen] = useState(false)

  if (!customerAccount) {
    return null
  }

  const profile = customerAccount.profile
  const contacts = normalizedDeliveryContacts(profile)

  return (
    <>
      <Card className="border-border/70 bg-card/90 shadow-sm backdrop-blur-sm">
        <CardHeader className="flex flex-row flex-wrap items-start justify-between gap-3 pb-3">
          <div className="space-y-1">
            <CardTitle className="text-lg">收货信息组</CardTitle>
            <CardDescription>可保存多组联系人、电话、地址；下单时可在结算页选用。</CardDescription>
          </div>
          <Button type="button" size="sm" className="cursor-pointer shrink-0" onClick={() => setAddOpen(true)}>
            新增一组
          </Button>
        </CardHeader>
        <CardContent className="space-y-3">
          {contacts.map((c) => (
            <div
              key={c.id}
              className="rounded-xl border border-border/60 bg-muted/15 px-4 py-3 text-sm shadow-sm"
            >
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div className="flex flex-wrap items-center gap-2">
                  <span className="flex items-center gap-1.5 font-medium text-foreground">
                    <UserRound className="size-4 text-primary" aria-hidden />
                    {c.name}
                  </span>
                  {c.isDefault ? (
                    <Badge variant="secondary" className="text-xs">
                      默认
                    </Badge>
                  ) : null}
                </div>
                <div className="flex flex-wrap gap-2">
                  {!c.isDefault ? (
                    <Button
                      type="button"
                      size="sm"
                      variant="outline"
                      className="h-8 cursor-pointer gap-1 px-2 text-xs"
                      onClick={async () => {
                        const result = await saveDeliveryContacts(setDefaultById(contacts, c.id))
                        if (result.ok) {
                          showNotice('已更新默认收货信息。', 'success')
                        } else {
                          showNotice(result.message, 'error')
                        }
                      }}
                    >
                      <Star className="size-3.5" aria-hidden />
                      设为默认
                    </Button>
                  ) : null}
                  <Button
                    type="button"
                    size="sm"
                    variant="outline"
                    className="h-8 cursor-pointer gap-1 px-2 text-xs text-destructive hover:text-destructive"
                    onClick={async () => {
                      const next = removeContactById(contacts, c.id)
                      if ('error' in next) {
                        showNotice(next.error, 'error')
                        return
                      }
                      const result = await saveDeliveryContacts(next)
                      if (result.ok) {
                        showNotice('已删除该组收货信息。', 'success')
                      } else {
                        showNotice(result.message, 'error')
                      }
                    }}
                  >
                    <Trash2 className="size-3.5" aria-hidden />
                    删除
                  </Button>
                </div>
              </div>
              <Separator className="my-2" />
              <p className="flex items-center gap-1.5 text-muted-foreground">
                <Phone className="size-3.5 shrink-0" aria-hidden />
                {c.phone}
              </p>
              <p className="mt-1 flex items-start gap-1.5 leading-relaxed text-muted-foreground">
                <MapPin className="mt-0.5 size-3.5 shrink-0" aria-hidden />
                {c.address}
              </p>
            </div>
          ))}
        </CardContent>
      </Card>

      <DeliveryContactAddDialog
        open={addOpen}
        onOpenChange={setAddOpen}
        onSubmit={async (payload) => {
          const next = appendContact(contacts, payload, crypto.randomUUID())
          const result = await saveDeliveryContacts(next)
          if (result.ok) {
            showNotice('已保存新的收货信息组。', 'success')
            return { ok: true as const }
          }
          return { ok: false as const, message: result.message }
        }}
      />
    </>
  )
}
