
package poca

import scala.concurrent.Future
import slick.jdbc.PostgresProfile.api._
import java.util.UUID
import org.mindrot.jbcrypt.BCrypt
import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration

case class User(userId: String, username: String, password : String, mail: String, cart: String)

final case class UserAlreadyExistsException(private val message: String="", private val cause: Throwable=None.orNull)
    extends Exception(message, cause) 
final case class InconsistentStateException(private val message: String="", private val cause: Throwable=None.orNull)
    extends Exception(message, cause) 

class Users {
    class UsersTable(tag: Tag) extends Table[(String, String, String, String, String)](tag, "users") {
        def userId = column[String]("userId", O.PrimaryKey)
        def username = column[String]("username")
        def password = column[String]("password")
        def mail = column[String]("mail")
        def cart = column[String]("cart")

        def * = (userId, username,password, mail, cart)
    }

    implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global
    val db = MyDatabase.db
    val users = TableQuery[UsersTable]

    def createUser(username: String,password : String, mail : String): Future[Unit] = {
        val existingUsersFuture = getUserByUsername(username)

        existingUsersFuture.flatMap(existingUsers => {
            if (existingUsers.isEmpty) {
                val userId = UUID.randomUUID.toString()
                val protectpassword = BCrypt.hashpw(password,BCrypt.gensalt(12))
                val newUser = User(userId=userId, username=username, password = protectpassword,mail = mail, cart = "")
                val newUserAsTuple: (String, String, String, String, String) = User.unapply(newUser).get

                val dbio: DBIO[Int] = users += newUserAsTuple
                var resultFuture: Future[Int] = db.run(dbio)

                // We do not care about the Int value
                resultFuture.map(_ => ())
            } else {
                throw new UserAlreadyExistsException(s"A user with username '$username' already exists.")
            }
        })
    }

    def getUserByUsername(username: String): Future[Option[User]] = {
        val query = users.filter(_.username === username)

        val userListFuture = db.run(query.result)

        userListFuture.map((userList: Seq[(String, String, String, String, String)]) => {
            userList.length match {
                case 0 => None
                case 1 => Some(User tupled userList.head)
                case _ => throw new InconsistentStateException(s"Username $username is linked to several users in database!")
            }
        })
    }

    def changeUsername(uname : String, newusername : String) : Future[Unit] = {
        val userFuture = getUserByUsername(uname)
        userFuture.map{
            case Some(user) => {
                val query = users.update(user.userId,newusername,user.password,user.mail,user.cart)
                db.run(query)
            }
            case _ =>
        }
    }

    def changePassword(username : String, newpassword : String ) : Future[Unit] = {
        val userFuture = getUserByUsername(username)
        val protectpassword = BCrypt.hashpw(newpassword,BCrypt.gensalt(12))

        userFuture.map{
            case Some(user) => {
                val query = users.update(user.userId,user.username,protectpassword,user.mail,user.cart)
                db.run(query)
            }
            case _ =>
        }
    }

    def changeMail(uname : String, newMail : String) : Future[Unit] = {
        val userFuture = getUserByUsername(uname)
        userFuture.map{
            case Some(user) => {
                val query = users.update(user.userId,user.username,user.password,newMail,user.cart)
                db.run(query)
            }
            case _ =>
        }
    }

    def getAllUsers(): Future[Seq[User]] = {
        val userListFuture = db.run(users.result)

        userListFuture.map((userList: Seq[(String, String, String, String, String)]) => {
            userList.map(User tupled _)
        })
    }


    
    def getCartByUsername(username: String ): Future[String] = {
        val userFuture = getUserByUsername(username)
        userFuture.map{
            case Some(user) => {
                user.cart
            }
        }
        
    }
    

    def addProductCart(username: String , productID: String): Future[Unit] = {
        val userFuture = getUserByUsername(username)
        userFuture.map{
            case Some(user) => {
                val newCart = user.cart + productID +","
                val query = users.update(user.userId,user.username,user.password,user.mail,newCart)
                db.run(query)
            }
            case _ =>
        }
    }

    def removeProductCart(username: String , productID: String): Future[Unit] = {
        val userFuture = getUserByUsername(username)
        userFuture.map{
            case Some(user) => {
                val newCart = user.cart.replace(productID+",", "") 
                val query = users.update(user.userId,user.username,user.password,user.mail,newCart)
                db.run(query)
            }
            case _ =>
        }
    }

}
