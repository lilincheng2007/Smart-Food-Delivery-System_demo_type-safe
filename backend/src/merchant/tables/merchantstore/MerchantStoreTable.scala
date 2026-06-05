package delivery.merchant.tables.merchantstore

import cats.effect.IO
import delivery.merchant.objects.Merchant
import delivery.shared.json.ApiJsonCodecs.given
import delivery.shared.objects.MerchantCategory
import io.circe.parser.decode
import io.circe.syntax.*
import org.postgresql.util.PGobject

import java.sql.{Connection, PreparedStatement, ResultSet}

object MerchantStoreTable:

  private val upsertSql: String =
    """
      |INSERT INTO merchant_stores (
      |  id, owner_username, store_name, category, address, phone, rating,
      |  tags, featured_product_ids, image_url, description, announcement, updated_at
      |)
      |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())
      |ON CONFLICT (id) DO UPDATE SET
      |  owner_username = EXCLUDED.owner_username,
      |  store_name = EXCLUDED.store_name,
      |  category = EXCLUDED.category,
      |  address = EXCLUDED.address,
      |  phone = EXCLUDED.phone,
      |  rating = EXCLUDED.rating,
      |  tags = EXCLUDED.tags,
      |  featured_product_ids = EXCLUDED.featured_product_ids,
      |  image_url = EXCLUDED.image_url,
      |  description = EXCLUDED.description,
      |  announcement = EXCLUDED.announcement,
      |  updated_at = now()
      |""".stripMargin

  private val upsertCatalogSql: String =
    """
      |INSERT INTO catalog_merchants (
      |  id, store_name, category, address, phone, rating, tags, featured_product_ids, image_url, description, announcement, updated_at
      |)
      |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())
      |ON CONFLICT (id) DO UPDATE SET
      |  store_name = EXCLUDED.store_name,
      |  category = EXCLUDED.category,
      |  address = EXCLUDED.address,
      |  phone = EXCLUDED.phone,
      |  rating = EXCLUDED.rating,
      |  tags = EXCLUDED.tags,
      |  featured_product_ids = EXCLUDED.featured_product_ids,
      |  image_url = EXCLUDED.image_url,
      |  description = EXCLUDED.description,
      |  announcement = EXCLUDED.announcement,
      |  updated_at = now()
      |""".stripMargin

  def upsert(connection: Connection, ownerUsername: String, merchant: Merchant): IO[Merchant] =
    IO.blocking {
      val statement = connection.prepareStatement(upsertSql)
      try
        bindStore(statement, merchant, Some(ownerUsername))
        val _ = statement.executeUpdate()
      finally statement.close()

      val catalogStatement = connection.prepareStatement(upsertCatalogSql)
      try
        bindStore(catalogStatement, merchant, None)
        val _ = catalogStatement.executeUpdate()
        merchant
      finally catalogStatement.close()
    }

  private val listCatalogSql: String =
    """
      |SELECT id, store_name, category, address, phone, rating, tags, featured_product_ids, image_url, description, announcement
      |FROM catalog_merchants
      |ORDER BY updated_at DESC
      |""".stripMargin

  private[merchant] def listCatalog(connection: Connection): IO[List[Merchant]] =
    IO.blocking {
      val statement = connection.prepareStatement(listCatalogSql)
      try
        val resultSet = statement.executeQuery()
        try
          val builder = List.newBuilder[Merchant]
          while resultSet.next() do builder += readMerchant(resultSet)
          builder.result()
        finally resultSet.close()
      finally statement.close()
    }

  private val listByOwnerSql: String =
    """
      |SELECT id, store_name, category, address, phone, rating, tags, featured_product_ids, image_url, description, announcement
      |FROM merchant_stores
      |WHERE owner_username = ?
      |ORDER BY updated_at DESC
      |""".stripMargin

  private[merchant] def listByOwner(connection: Connection, ownerUsername: String): IO[List[Merchant]] =
    IO.blocking {
      val statement = connection.prepareStatement(listByOwnerSql)
      try
        statement.setString(1, ownerUsername)
        val resultSet = statement.executeQuery()
        try
          val builder = List.newBuilder[Merchant]
          while resultSet.next() do builder += readMerchant(resultSet)
          builder.result()
        finally resultSet.close()
      finally statement.close()
    }

  private def bindStore(statement: PreparedStatement, merchant: Merchant, ownerUsername: Option[String]): Unit =
    statement.setString(1, merchant.id)
    ownerUsername match
      case Some(username) =>
        statement.setString(2, username)
        statement.setString(3, merchant.storeName)
        statement.setString(4, merchant.category.toString)
        statement.setString(5, merchant.address)
        statement.setString(6, merchant.phone)
        statement.setDouble(7, merchant.rating)
        statement.setObject(8, jsonb(merchant.tags.asJson.noSpaces))
        statement.setObject(9, jsonb(merchant.featuredProductIds.asJson.noSpaces))
        merchant.imageUrl match
          case Some(value) => statement.setString(10, value)
          case None        => statement.setNull(10, java.sql.Types.VARCHAR)
        statement.setString(11, merchant.description)
        statement.setString(12, merchant.announcement)
      case None =>
        statement.setString(2, merchant.storeName)
        statement.setString(3, merchant.category.toString)
        statement.setString(4, merchant.address)
        statement.setString(5, merchant.phone)
        statement.setDouble(6, merchant.rating)
        statement.setObject(7, jsonb(merchant.tags.asJson.noSpaces))
        statement.setObject(8, jsonb(merchant.featuredProductIds.asJson.noSpaces))
        merchant.imageUrl match
          case Some(value) => statement.setString(9, value)
          case None        => statement.setNull(9, java.sql.Types.VARCHAR)
        statement.setString(10, merchant.description)
        statement.setString(11, merchant.announcement)

  private def readMerchant(resultSet: ResultSet): Merchant =
    Merchant(
      id = resultSet.getString("id"),
      storeName = resultSet.getString("store_name"),
      category = MerchantCategory.fromString(resultSet.getString("category")).getOrElse(MerchantCategory.中餐),
      address = resultSet.getString("address"),
      phone = resultSet.getString("phone"),
      rating = resultSet.getBigDecimal("rating").doubleValue(),
      tags = decode[List[String]](resultSet.getString("tags")).getOrElse(Nil),
      featuredProductIds = decode[List[String]](resultSet.getString("featured_product_ids")).getOrElse(Nil),
      imageUrl = Option(resultSet.getString("image_url")),
      description = Option(resultSet.getString("description")).getOrElse(""),
      announcement = Option(resultSet.getString("announcement")).getOrElse("")
    )

  private def jsonb(value: String): PGobject =
    val pg = PGobject()
    pg.setType("jsonb")
    pg.setValue(value)
    pg

end MerchantStoreTable
