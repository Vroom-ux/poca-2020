package poca

import scala.concurrent.Future
import slick.jdbc.PostgresProfile.api._
import java.util.UUID

import slick.lifted.ProvenShape

case class Category_Products(Id_Produit: Float, Id_Categorie: Float)


final case class Category_ProductAlreadyExistsException(private val message: String="", private val cause: Throwable=None.orNull)
    extends Exception(message, cause) 



class Category_Product {

  class Category_Producttable(tag: Tag) extends Table[(Float, Float)](tag, "Category_Product") {

    def Id_Produit = column[Float]("Id_Produit")

    def Id_Categorie = column[Float]("Id_Categorie")

    def * = (Id_Produit,Id_Categorie)
  }

}



