package poca

import scala.concurrent.Future
import slick.jdbc.PostgresProfile.api._
import java.util.UUID

case class Product(productId: String, productName: String)

final case class ProductAlreadyExistsException(private val message: String="", private val cause: Throwable=None.orNull)
  extends Exception(message, cause)


class Products {

  class Productstable(tag: Tag) extends Table[(String, String)](tag, "products") {
    def productId = column[String]("productId", O.PrimaryKey)

    def productname = column[String]("productname")

    def * = (productId, productname)
  }

}


