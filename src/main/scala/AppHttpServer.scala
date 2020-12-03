
package poca

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration
import com.typesafe.scalalogging.LazyLogging
import ch.qos.logback.classic.{Level, Logger}
import org.slf4j.LoggerFactory
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import scala.util.{Success, Failure}

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import com.softwaremill.session.CsrfDirectives._
import com.softwaremill.session.CsrfOptions._
import com.softwaremill.session.SessionDirectives._
import com.softwaremill.session.SessionOptions._
import com.softwaremill.session._

object AppHttpServer extends LazyLogging {
    val rootLogger: Logger = LoggerFactory.getLogger("com").asInstanceOf[Logger]
    rootLogger.setLevel(Level.INFO)
    val slickLogger: Logger = LoggerFactory.getLogger("slick").asInstanceOf[Logger]
    slickLogger.setLevel(Level.INFO)

    def initDatabase() = {
        val isRunningOnCloud = sys.env.getOrElse("DB_HOST", "") != ""
        var rootConfig = ConfigFactory.load()
        val dbConfig = if (isRunningOnCloud) {
            val dbHost = sys.env.getOrElse("DB_HOST", "")
            val dbPassword = sys.env.getOrElse("DB_PASSWORD", "")

            val originalConfig = rootConfig.getConfig("cloudDB")
            originalConfig.
                withValue("properties.serverName", ConfigValueFactory.fromAnyRef(dbHost)).
                withValue("properties.password", ConfigValueFactory.fromAnyRef(dbPassword))
        } else {
            rootConfig.getConfig("localDB")
        }
        MyDatabase.initialize(dbConfig)
    }

def main(args: Array[String]): Unit = {
        implicit val actorsSystem = ActorSystem(guardianBehavior=Behaviors.empty, name="my-system")
        implicit val actorsExecutionContext = actorsSystem.executionContext

        import actorsSystem.dispatchers
        val sessionConfig = SessionConfig.default(    "c05ll3lesrinf39t7mc5h6un6r0c69lgfno69dsak3vabeqamouq4328cuaekros401ajdpkh60rrtpd8ro24rbuqmgtnd1ebag6ljnb65i8a55d482ok7o0nch0bfbe")
        implicit val sessionManager = new SessionManager[MyScalaSession](sessionConfig)
        implicit val refreshTokenStorage = new InMemoryRefreshTokenStorage[MyScalaSession] {
            def log(msg: String) = logger.info(msg)
        }
        initDatabase
        val db = MyDatabase.db
        new RunMigrations(db)()

        def mySetSession(v: MyScalaSession) = setSession(refreshable, usingCookies, v)

        val myRequiredSession = requiredSession(refreshable, usingCookies)
        val myInvalidateSession = invalidateSession(refreshable, usingCookies)
        
        var users = new Users()
        var products = new Products()
        var categories = new Categories()
        val routes = new Routes(users, products,categories,myRequiredSession,myInvalidateSession,mySetSession)
        
        val bindingFuture = Http().newServerAt("0.0.0.0", 8080).bind(routes.routes)

        val serverStartedFuture = bindingFuture.map(binding => {
            val address = binding.localAddress
            logger.info(s"Server online at http://${address.getHostString}:${address.getPort}/")
        })

        val waitOnFuture = serverStartedFuture.flatMap(unit => Future.never)
        
        scala.sys.addShutdownHook { 
            actorsSystem.terminate
            db.close
        }

        Await.ready(waitOnFuture, Duration.Inf)
    }
}

