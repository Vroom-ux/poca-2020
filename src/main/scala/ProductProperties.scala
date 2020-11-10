package poca

import scala.concurrent.Future
import slick.jdbc.PostgresProfile.api._
import java.util.UUID

import slick.lifted.ProvenShape

case class ProductPropertie(price: Float, matter: String, dimension:String)


final case class ProductPropertiesAlreadyExistsException(private val message: String="", private val cause: Throwable=None.orNull)
    extends Exception(message, cause) 



class ProductProperties {

  class ProductPropertiestable(tag: Tag) extends Table[(Float, String, String)](tag, "ProductProperties") {

    def price = column[Float]("price")

    def matter = column[String]("matter")

    def dimension = column[String]("dimension")

    def * = (price, matter, dimension)
  }

}

