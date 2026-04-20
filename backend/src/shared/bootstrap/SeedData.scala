package delivery.shared.bootstrap

import delivery.admin.objects.*
import delivery.merchant.objects.*
import delivery.order.objects.*
import delivery.rider.objects.Rider
import delivery.shared.objects.Voucher
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
      vouchers = List(Voucher("v-1", "满30减8", 8, 30, "2026-04-30", 2))
    )
  )

  val seedMerchants: List[Merchant] = List(
    Merchant(
      "m-2001",
      "阿强小炒",
      "中餐",
      "浦东新区张杨路 88 号",
      "021-60001234",
      4.8,
      List("五星店铺", "出餐快"),
      List("p-3001", "p-3002"),
      Some("https://picsum.photos/seed/m2001-store/480/280")
    ),
    Merchant(
      "m-2002",
      "甜点星球",
      "饮品甜点",
      "徐汇区天钥桥路 77 号",
      "021-60005678",
      4.7,
      List("下午茶首选"),
      List("p-3003"),
      Some("https://picsum.photos/seed/m2002-store/480/280")
    )
  )

  val seedProducts: List[Product] = List(
    Product("p-3001", "m-2001", "宫保鸡丁饭", 28, "经典川味，微辣。", "https://picsum.photos/200/120?food-1", 768, 88, "上架", "充足", Some("会员价 25.5")),
    Product("p-3002", "m-2001", "番茄牛腩饭", 32, "慢炖牛腩，配时蔬。", "https://picsum.photos/200/120?food-2", 514, 12, "上架", "紧张", None),
    Product("p-3003", "m-2002", "杨枝甘露大杯", 22, "芒果+西米+西柚，冰爽口感。", "https://picsum.photos/200/120?drink-1", 903, 66, "上架", "充足", Some("第二杯半价"))
  )

  val seedRiders: List[Rider] = List(
    Rider("r-5001", "王涛", "13900006543", "浦东新区商城路地铁站", "空闲", 1280, 4.9, "陆家嘴站", 13880),
    Rider("r-5002", "赵鹏", "13700007654", "徐汇区肇嘉浜路", "配送中", 1024, 4.8, "徐家汇站", 12200)
  )

  val seedOrders: List[Order] = Nil

  val seedServiceAgents: List[CustomerServiceAgent] = List(
    CustomerServiceAgent("cs-01", "陈琳", "售后服务部", "在线", List("t-1", "t-2"))
  )

  val seedOperationsManagers: List[OperationsManager] = List(
    OperationsManager("op-01", "吴越", "浦东新区", List("m-2001", "m-2002"), List("周末满减冲刺", "新商家冷启动流量扶持"))
  )

  val seedMerchantApplications: List[MerchantApplication] = List(
    MerchantApplication("ma-001", "张晨", "夜猫子烧烤", "夜宵", "杨浦区", "待审核")
  )

  val seedComplaintTickets: List[ComplaintTicket] = Nil

  val seedCampaigns: List[PromotionCampaign] = List(
    PromotionCampaign("c-01", "新客立减 12 元", "新客", "进行中"),
    PromotionCampaign("c-02", "周末全城满 39 减 6", "全体用户", "草稿")
  )

end SeedData
