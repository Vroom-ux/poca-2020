package poca

import scala.concurrent.Future
import slick.jdbc.PostgresProfile.api._
import java.util.UUID

import slick.lifted.ProvenShape

case class Category_Descriptions(Id_Categorie: Float,name_Categorie: String)


final case class Category_DescriptionAlreadyExistsException(private val message: String="", private val cause: Throwable=None.orNull)
    extends Exception(message, cause) 



class Category_Description {

  class Category_Descriptiontable(tag: Tag) extends Table[(Float,String)](tag, "Category_Description") {


    def Id_Categorie = column[Float]("Id_Categorie")

    def name_Categorie = column[String]("name_Categorie")


    def * = (Id_Categorie,name_Categorie)
  }

}