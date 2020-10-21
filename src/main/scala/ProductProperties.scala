package poca

import scala.concurrent.Future
import slick.jdbc.PostgresProfile.api._
import java.util.UUID

case class ProductProperties(price: Float, matter: String, dimension:String)


final case class ProductPropertiesAlreadyExistsException(private val message: String="", private val cause: Throwable=None.orNull)
    extends Exception(message, cause) 



class ProductProperties {

  class ProductPropertiestable(tag: Tag) extends Table[(Float, String,String)](tag, "ProductProperties") {

    def productname = column[Float]("price")
    def productname = column[String]("matter")
    def productname = column[String]("dimension")

    def * = (prix, matiere,dimension)
  }

