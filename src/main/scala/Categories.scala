package poca

import scala.concurrent.{Await, Future}
import slick.jdbc.PostgresProfile.api._
import java.util.UUID

import scala.concurrent.duration.Duration

case class Category(categoryId : String, categoryName : String)

final case class CategoryAlreadyExistsException(private val message: String="", private val cause: Throwable=None.orNull)
  extends Exception(message, cause)


class Categories {

  class CategoriesTable(tag: Tag) extends Table[(String, String)](tag, "categories") {
    def categoryId = column[String]("categoryId", O.PrimaryKey)
    def categoryName = column[String]("categoryname")
    def * = (categoryId, categoryName)
  }

  implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global
  val db = MyDatabase.db
  val categories = TableQuery[CategoriesTable]

  def addCategory(categoryName : String): Future[Unit] ={
    //TODO : insère la catégorie par ordre alphabétique
    val existingCategoriesFuture = getCategorybyCategoryName(categoryName)

    existingCategoriesFuture.flatMap(existingCategories => {
      if (existingCategories.isEmpty) {
        val categoryId = UUID.randomUUID.toString()
        val newCategory = Category(categoryId,categoryName)
        val newCategoryAsTuple: (String, String) = Category.unapply(newCategory).get

        val dbio: DBIO[Int] = categories += newCategoryAsTuple
        var resultFuture: Future[Int] = db.run(dbio)

        // We do not care about the Int value
        resultFuture.map(_ => ())
      } else {
        throw new CategoryAlreadyExistsException(s"A Category with Categoryname '$categoryName' already exists.")
      }
    })
  }

  def getCategorybyCategoryName(categoryName: String): Future[Option[Category]] = {
    val query = categories.filter(_.categoryName === categoryName)

    val categoryListFuture = db.run(query.result)

    categoryListFuture.map((categoryList: Seq[(String, String)]) => {
      categoryList.length match {
        case 0 => None
        case 1 => Some(Category tupled categoryList.head)
        case _ => throw new InconsistentStateException(s"Categoryname $categoryName is linked to several categoriess in database!")
      }
    })
  }

  def getAllCategories(): Future[Seq[Category]] = {
    val categoryListFuture = db.run(categories.result)

    categoryListFuture.map((categoryList: Seq[(String,String)]) => {
      categoryList.map(Category tupled _)
    })
  }
}
