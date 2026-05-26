import { createBrowserRouter, Navigate } from 'react-router-dom'

import { GuestRouteGuard, RoleRouteGuard } from '@/components/RoleRouteGuards'
import CustomerPortal from '@/pages/CustomerPortal'
import CustomerCheckoutPage from '@/pages/CustomerPortal/CustomerCheckoutPage'
import CustomerMerchantOrderPage from '@/pages/CustomerPortal/CustomerMerchantOrderPage'
import Login from '@/pages/Login'
import MerchantConsole from '@/pages/MerchantConsole'
import Register from '@/pages/Register'
import RiderApp from '@/pages/RiderApp'

const routes = [
  {
    path: '/',
    element: <Navigate replace to="/auth/login" />,
  },
  {
    path: '/auth/login',
    element: (
      <GuestRouteGuard>
        <Login />
      </GuestRouteGuard>
    ),
  },
  {
    path: '/auth/register',
    element: (
      <GuestRouteGuard>
        <Register />
      </GuestRouteGuard>
    ),
  },
  {
    path: '/delivery/customer/m/:merchantId',
    element: (
      <RoleRouteGuard allowedRoles={['customer']}>
        <CustomerMerchantOrderPage />
      </RoleRouteGuard>
    ),
  },
  {
    path: '/delivery/customer/checkout',
    element: (
      <RoleRouteGuard allowedRoles={['customer']}>
        <CustomerCheckoutPage />
      </RoleRouteGuard>
    ),
  },
  {
    path: '/delivery/customer',
    element: (
      <RoleRouteGuard allowedRoles={['customer']}>
        <CustomerPortal />
      </RoleRouteGuard>
    ),
  },
  {
    path: '/delivery/merchant',
    element: (
      <RoleRouteGuard allowedRoles={['merchant']}>
        <MerchantConsole />
      </RoleRouteGuard>
    ),
  },
  {
    path: '/delivery/rider',
    element: (
      <RoleRouteGuard allowedRoles={['rider']}>
        <RiderApp />
      </RoleRouteGuard>
    ),
  },
  {
    path: '*',
    element: <Navigate replace to="/auth/login" />,
  },
]

export const router = createBrowserRouter(routes)
