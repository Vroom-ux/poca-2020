
package poca

import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration
import com.typesafe.scalalogging.LazyLogging
import slick.jdbc.PostgresProfile.api._


class Migration01CreateUsersTable(db: Database) extends Migration with LazyLogging {
    class CurrentUsersTable(tag: Tag) extends Table[(String, String, String, String, String)](tag, "users") {
        def userId = column[String]("userId", O.PrimaryKey)
        def username = column[String]("username")
        def password = column[String]("password")
        def mail = column[String]("mail")
        def cart = column[String]("cart")
        def * = (userId, username, password, mail, cart)
    }

    override def apply(): Unit = {
        implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global
        val users = TableQuery[CurrentUsersTable]
        val dbio: DBIO[Unit] = users.schema.create
        val creationFuture: Future[Unit] = db.run(dbio)
        Await.result(creationFuture, Duration.Inf)
        logger.info("Done creating table Users")
    }
}
