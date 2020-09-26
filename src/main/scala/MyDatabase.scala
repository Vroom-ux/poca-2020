/*
    Initialization code for the database, although simple, is extracted to its own file in order to avoid mixing up the execution context used for the database with the other execution contexts of the application.

    The access to the database is made available as a singleton.
*/

import slick.jdbc.PostgresProfile.api._

object MyDatabase {
    implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global
    var db: Database = null
    
    // Run this method at the entrypoint. This is a kind of manual dependency injection to make it possible to test against a test database.
    // After, you can get the member `db` and use it directy.
    // At the end, call `db.close`.
    def initialize(configName: String): Unit = {
        db = Database.forConfig(configName)
    }
}