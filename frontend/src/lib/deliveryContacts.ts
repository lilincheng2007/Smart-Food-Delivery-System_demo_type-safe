import type { CustomerDeliveryContact } from '@/objects/user/CustomerDeliveryContact'
import type { CustomerProfile } from '@/objects/user/CustomerProfile'

/** 展示用：无列表或空列表时，用档案顶层字段合成一组 */
export function normalizedDeliveryContacts(profile: CustomerProfile): CustomerDeliveryContact[] {
  const raw = profile.deliveryContacts
  if (raw && raw.length > 0) {
    return withSingleDefault(raw)
  }
  return [
    {
      id: `${profile.id}-dc-legacy`,
      name: profile.name,
      phone: profile.phone,
      address: profile.defaultAddress,
      isDefault: true,
    },
  ]
}

export function withSingleDefault(contacts: CustomerDeliveryContact[]): CustomerDeliveryContact[] {
  if (contacts.length === 0) return contacts
  const preferred = contacts.find((c) => c.isDefault)?.id ?? contacts[0].id
  return contacts.map((c) => ({ ...c, isDefault: c.id === preferred }))
}

export function validateDeliveryContacts(contacts: CustomerDeliveryContact[]): string | null {
  if (contacts.length === 0) {
    return '至少保留一组收货信息。'
  }
  for (const c of contacts) {
    if (!c.id.trim()) {
      return '收货信息缺少编号。'
    }
    if (!c.name.trim()) {
      return '联系人姓名不能为空。'
    }
    if (!c.phone.trim()) {
      return '联系电话不能为空。'
    }
    if (!c.address.trim()) {
      return '收货地址不能为空。'
    }
  }
  const defaults = contacts.filter((c) => c.isDefault)
  if (defaults.length !== 1) {
    return '必须且只能有一组默认收货信息。'
  }
  return null
}

export function setDefaultById(
  contacts: CustomerDeliveryContact[],
  id: string,
): CustomerDeliveryContact[] {
  return contacts.map((c) => ({ ...c, isDefault: c.id === id }))
}

export function removeContactById(
  contacts: CustomerDeliveryContact[],
  id: string,
): CustomerDeliveryContact[] | { error: string } {
  if (contacts.length <= 1) {
    return { error: '至少保留一组收货信息。' }
  }
  const next = contacts.filter((c) => c.id !== id)
  return withSingleDefault(next)
}

export function appendContact(
  contacts: CustomerDeliveryContact[],
  payload: { name: string; phone: string; address: string; asDefault: boolean },
  newId: string,
): CustomerDeliveryContact[] {
  if (payload.asDefault) {
    return [
      ...contacts.map((c) => ({ ...c, isDefault: false })),
      {
        id: newId,
        name: payload.name,
        phone: payload.phone,
        address: payload.address,
        isDefault: true,
      },
    ]
  }
  return withSingleDefault([
    ...contacts,
    {
      id: newId,
      name: payload.name,
      phone: payload.phone,
      address: payload.address,
      isDefault: false,
    },
  ])
}
