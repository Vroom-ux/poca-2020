
package poca

import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration
import com.typesafe.scalalogging.LazyLogging
import slick.jdbc.PostgresProfile.api._


class Migration02CreateProductsTable(db: Database) extends Migration with LazyLogging {
    class CurrentProductsTable(tag: Tag) extends Table[(String, String)](tag, "products") {
        def productId = column[String]("productId", O.PrimaryKey)
        def productname = column[String]("productname")
        def * = (productId, productname)
    }

    override def apply(): Unit = {
        implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global
        val products = TableQuery[CurrentProductsTable]
        val dbio: DBIO[Unit] = products.schema.create
        val creationFuture: Future[Unit] = db.run(dbio)
        Await.result(creationFuture, Duration.Inf)
        logger.info("Done creating table Products")
    }
}
