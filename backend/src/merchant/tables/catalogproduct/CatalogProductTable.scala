package delivery.merchant.tables.catalogproduct

import cats.effect.IO
import delivery.merchant.objects.{Product, ProductBundleGroup}
import delivery.platform.json.ApiJsonCodecs.given
import delivery.domain.{InventoryStatus, ListingStatus, ProductId}
import io.circe.parser.decode
import io.circe.syntax.*
import org.postgresql.util.PGobject

import java.sql.{Connection, PreparedStatement, ResultSet}

object CatalogProductTable:

  private val upsertSql: String =
    """
      |INSERT INTO catalog_products (
      |  id, merchant_id, name, price, description, image_url, monthly_sales,
      |  remaining_stock, listing_status, inventory_status, inventory_mode, max_per_order, discount_text, category_name, bundle_config, updated_at
      |)
      |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())
      |ON CONFLICT (id) DO UPDATE SET
      |  merchant_id = EXCLUDED.merchant_id,
      |  name = EXCLUDED.name,
      |  price = EXCLUDED.price,
      |  description = EXCLUDED.description,
      |  image_url = EXCLUDED.image_url,
      |  monthly_sales = EXCLUDED.monthly_sales,
      |  remaining_stock = EXCLUDED.remaining_stock,
      |  listing_status = EXCLUDED.listing_status,
      |  inventory_status = EXCLUDED.inventory_status,
      |  inventory_mode = EXCLUDED.inventory_mode,
      |  max_per_order = EXCLUDED.max_per_order,
      |  discount_text = EXCLUDED.discount_text,
      |  category_name = EXCLUDED.category_name,
      |  bundle_config = EXCLUDED.bundle_config,
      |  updated_at = now()
      |""".stripMargin

  def upsert(connection: Connection, product: Product): IO[Product] =
    IO.blocking {
      val statement = connection.prepareStatement(upsertSql)
      try
        bindProduct(statement, product)
        val _ = statement.executeUpdate()
        product
      finally statement.close()
    }

  private val listSql: String =
    """
      |SELECT id, merchant_id, name, price, description, image_url, monthly_sales,
      |       remaining_stock, listing_status, inventory_status, inventory_mode, max_per_order, discount_text, category_name, bundle_config
      |FROM catalog_products
      |ORDER BY category_name ASC, updated_at DESC
      |""".stripMargin

  private val listForUpdateSql: String =
    """
      |SELECT id, merchant_id, name, price, description, image_url, monthly_sales,
      |       remaining_stock, listing_status, inventory_status, inventory_mode, max_per_order, discount_text, category_name, bundle_config
      |FROM catalog_products
      |ORDER BY category_name ASC, updated_at DESC
      |FOR UPDATE
      |""".stripMargin

  def list(connection: Connection): IO[List[Product]] =
    readProductList(connection, listSql)

  def listForUpdate(connection: Connection): IO[List[Product]] =
    readProductList(connection, listForUpdateSql)

  private def readProductList(connection: Connection, sql: String): IO[List[Product]] =
    IO.blocking {
      val statement = connection.prepareStatement(sql)
      try
        val resultSet = statement.executeQuery()
        try
          val builder = List.newBuilder[Product]
          while resultSet.next() do builder += readProduct(resultSet)
          builder.result()
        finally resultSet.close()
      finally statement.close()
    }

  private val findByIdSql: String =
    """
      |SELECT id, merchant_id, name, price, description, image_url, monthly_sales,
      |       remaining_stock, listing_status, inventory_status, inventory_mode, max_per_order, discount_text, category_name, bundle_config
      |FROM catalog_products
      |WHERE id = ?
      |""".stripMargin

  private[merchant] def findById(connection: Connection, id: ProductId): IO[Option[Product]] =
    IO.blocking {
      val statement = connection.prepareStatement(findByIdSql)
      try
        statement.setString(1, id)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then Some(readProduct(resultSet))
          else None
        finally resultSet.close()
      finally statement.close()
    }

  private def bindProduct(statement: PreparedStatement, product: Product): Unit =
    statement.setString(1, product.id)
    statement.setString(2, product.merchantId)
    statement.setString(3, product.name)
    statement.setDouble(4, product.price)
    statement.setString(5, product.description)
    statement.setString(6, product.imageUrl)
    statement.setInt(7, product.monthlySales)
    statement.setInt(8, product.remainingStock)
    statement.setString(9, product.listingStatus.toString)
    statement.setString(10, product.inventoryStatus.toString)
    statement.setString(11, normalizedInventoryMode(product.inventoryMode))
    product.maxPerOrder match
      case Some(value) => statement.setInt(12, value)
      case None        => statement.setNull(12, java.sql.Types.INTEGER)
    product.discountText match
      case Some(value) => statement.setString(13, value)
      case None        => statement.setNull(13, java.sql.Types.VARCHAR)
    statement.setString(14, normalizedCategoryName(product.categoryName))
    statement.setObject(15, jsonb(product.bundleGroups.asJson.noSpaces))

  private def readProduct(resultSet: ResultSet): Product =
    Product(
      id = resultSet.getString("id"),
      merchantId = resultSet.getString("merchant_id"),
      name = resultSet.getString("name"),
      price = resultSet.getBigDecimal("price").doubleValue(),
      description = resultSet.getString("description"),
      imageUrl = resultSet.getString("image_url"),
      monthlySales = resultSet.getInt("monthly_sales"),
      remainingStock = resultSet.getInt("remaining_stock"),
      listingStatus = ListingStatus.fromString(resultSet.getString("listing_status")).getOrElse(ListingStatus.下架),
      inventoryStatus = InventoryStatus.fromString(resultSet.getString("inventory_status")).getOrElse(InventoryStatus.售罄),
      inventoryMode = normalizedInventoryMode(Option(resultSet.getString("inventory_mode")).getOrElse("finite")),
      maxPerOrder = Option(resultSet.getInt("max_per_order")).filter(_ => !resultSet.wasNull()),
      discountText = Option(resultSet.getString("discount_text")),
      categoryName = normalizedCategoryName(Option(resultSet.getString("category_name")).getOrElse("")),
      bundleGroups = Option(resultSet.getString("bundle_config")).flatMap(raw => decode[List[ProductBundleGroup]](raw).toOption).getOrElse(Nil)
    )

  private def normalizedCategoryName(raw: String): String =
    val trimmed = raw.trim
    if trimmed.isEmpty then "默认分类" else trimmed

  private def normalizedInventoryMode(raw: String): String =
    val trimmed = raw.trim
    if Set("unlimited", "finite", "soldOut").contains(trimmed) then trimmed else "finite"

  private def jsonb(value: String): PGobject =
    val pg = PGobject()
    pg.setType("jsonb")
    pg.setValue(value)
    pg

end CatalogProductTable
