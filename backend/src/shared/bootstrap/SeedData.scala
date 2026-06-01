package delivery.shared.bootstrap

import delivery.merchant.objects.*
import delivery.order.objects.*
import delivery.rider.objects.Rider
import delivery.shared.objects.{InventoryStatus, ListingStatus, MerchantCategory, RiderStatus, Voucher}
import delivery.user.objects.Customer

object SeedData:

  val seedCustomers: List[Customer] = List(
    Customer(
      id = "u-1001",
      name = "李然",
      phone = "13800001234",
      defaultAddress = "浦东新区世纪大道 100 号 1203 室",
      walletBalance = 188.5,
      orderHistoryIds = Nil,
      vouchers = List(Voucher("v-1", "满30减10", 10, 30, "2026-12-31", 1)),
      foodiePoints = 0,
      foodieLevel = 1
    )
  )

  val seedMerchants: List[Merchant] = List(
    Merchant(
      "m-2001",
      "阿强小炒",
      MerchantCategory.中餐,
      "浦东新区张杨路 88 号",
      "021-60001234",
      4.8,
      List("五星店铺", "出餐快"),
      List("p-3001", "p-3002"),
      Some("https://picsum.photos/seed/m2001-store/480/280"),
      "锅气十足的家常小炒店，主打热乎下饭的经典中餐。"
    ),
    Merchant(
      "m-2002",
      "甜点星球",
      MerchantCategory.饮品甜点,
      "徐汇区天钥桥路 77 号",
      "021-60005678",
      4.7,
      List("下午茶首选"),
      List("p-3003"),
      Some("https://picsum.photos/seed/m2002-store/480/280"),
      "把水果、奶香与轻甜灵感装进每一杯的下午茶小星球。"
    )
  )

  val seedProducts: List[Product] = List(
    Product("p-3001", "m-2001", "宫保鸡丁饭", 28, "经典川味，微辣。", "https://picsum.photos/200/120?food-1", 768, 88, ListingStatus.上架, InventoryStatus.充足, Some("会员价 25.5")),
    Product("p-3002", "m-2001", "番茄牛腩饭", 32, "慢炖牛腩，配时蔬。", "https://picsum.photos/200/120?food-2", 514, 12, ListingStatus.上架, InventoryStatus.紧张, None),
    Product("p-3003", "m-2002", "杨枝甘露大杯", 22, "芒果+西米+西柚，冰爽口感。", "https://picsum.photos/200/120?drink-1", 903, 66, ListingStatus.上架, InventoryStatus.充足, Some("第二杯半价"))
  )

  val seedRiders: List[Rider] = List(
    Rider("r-5001", "王涛", "13900006543", "浦东新区商城路地铁站", RiderStatus.空闲, 1280, 4.9, "陆家嘴站", 13880),
    Rider("r-5002", "赵鹏", "13700007654", "徐汇区肇嘉浜路", RiderStatus.配送中, 1024, 4.8, "徐家汇站", 12200)
  )

  val seedOrders: List[Order] = Nil

end SeedData
