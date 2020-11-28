
package poca

import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration
import com.typesafe.scalalogging.LazyLogging
import slick.jdbc.PostgresProfile.api._


class Migration03CreateCategoryTable(db: Database) extends Migration with LazyLogging {
  class CurrentCategoryTable(tag: Tag) extends Table[(Int, String)](tag, "categories") {
    def categoryId = column[Int]("categoryId", O.PrimaryKey)
    def categoryName = column[String]("categoryname")
    def * = (categoryId, categoryName)
  }

  override def apply(): Unit = {
    implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global
    val categories = TableQuery[CurrentCategoryTable]
    val dbio: DBIO[Unit] = categories.schema.create
    val creationFuture: Future[Unit] = db.run(dbio)
    Await.result(creationFuture, Duration.Inf)
    logger.info("Done creating table Category")
    val adddefaultcategoriesFuture: Future[Unit] = db.run(
      DBIO.seq(
        categories ++= Seq(
          (1,"Animalerie"),
          (2,"Bijoux"),
          (3,"Bricolage"),
          (4,"Informatique"),
          (5,"Mode"),
          (6,"Sport")
        )
      )
    )
    Await.result(adddefaultcategoriesFuture, Duration.Inf)
    logger.info("Done creating categories")
  }
}
