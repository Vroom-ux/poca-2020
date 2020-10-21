package poca

import scala.concurrent.Future
import slick.jdbc.PostgresProfile.api._
import java.util.UUID

case class Product(productId: String, productName: String)
case class ProductProperties(prix: Float, matiere: String, dimension:Float)

final case class ProductAlreadyExistsException(private val message: String="", private val cause: Throwable=None.orNull)
  extends Exception(message, cause)
final case class ProductPropertiesAlreadyExistsException(private val message: String="", private val cause: Throwable=None.orNull)
    extends Exception(message, cause) 


class Products {

  class Productstable(tag: Tag) extends Table[(String, String)](tag, "products") {
    def productId = column[String]("productId", O.PrimaryKey)

    def productname = column[String]("productname")

    def * = (productId, productname)
  }

}

class ProductProperties {

  class Productstable(tag: Tag) extends Table[(Float, String,Float)](tag, "ProductProperties") {

    def productname = column[Float]("prix")
    def productname = column[String]("matiere")
    def productname = column[Float]("dimension")

    def * = (prix, matiere,dimension)
  }

}
