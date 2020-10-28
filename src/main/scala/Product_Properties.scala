package poca

import scala.concurrent.Future
import slick.jdbc.PostgresProfile.api._
import java.util.UUID

import slick.lifted.ProvenShape

case class Product_Propertie(price: Float, matter: String, dimension:String,color:String,mark:String)


final case class Product_PropertiesAlreadyExistsException(private val message: String="", private val cause: Throwable=None.orNull)
    extends Exception(message, cause) 



class Product_Properties {

  class Product_Propertiestable(tag: Tag) extends Table[(Float, String, String,String,String)](tag, "Product_Properties") {

    def price = column[Float]("price")

    def matter = column[String]("matter")

    def dimension = column[String]("dimension")

    def color = column[String]("color")

    def mark = column[String]("mark")

    def * = (price, matter, dimension,color,mark)
  }

}

