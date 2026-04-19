import { createBrowserRouter, Navigate } from 'react-router-dom'

import { GuestRouteGuard, RoleRouteGuard } from '@/components/RoleRouteGuards'
import CustomerPortal from '@/pages/CustomerPortal'
import DeliveryDashboard from '@/pages/DeliveryDashboard'
import Login from '@/pages/Login'
import MerchantConsole from '@/pages/MerchantConsole'
import OrderCenter from '@/pages/OrderCenter'
import PlatformAdmin from '@/pages/PlatformAdmin'
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
    path: '/delivery/dashboard',
    element: (
      <RoleRouteGuard allowedRoles={['admin']}>
        <DeliveryDashboard />
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
    path: '/delivery/orders',
    element: (
      <RoleRouteGuard allowedRoles={['admin']}>
        <OrderCenter />
      </RoleRouteGuard>
    ),
  },
  {
    path: '/delivery/admin',
    element: (
      <RoleRouteGuard allowedRoles={['admin']}>
        <PlatformAdmin />
      </RoleRouteGuard>
    ),
  },
  {
    path: '*',
    element: <Navigate replace to="/auth/login" />,
  },
]

export const router = createBrowserRouter(routes)
