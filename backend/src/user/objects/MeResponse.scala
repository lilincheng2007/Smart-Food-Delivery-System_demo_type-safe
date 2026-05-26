package delivery.user.objects

type MeResponse =
  CustomerMeResponse | delivery.merchant.objects.MerchantMeResponse | delivery.rider.objects.RiderMeResponse
