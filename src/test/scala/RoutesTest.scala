
import scala.concurrent.Future
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.http.scaladsl.model.{ContentTypes, FormData, HttpMethods, HttpRequest, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.Matchers
import org.scalatest.funsuite.AnyFunSuite
import org.scalamock.scalatest.MockFactory
import poca.{Categories, MyDatabase, Product, Products, Routes, User, UserAlreadyExistsException, Users}


class RoutesTest extends AnyFunSuite with Matchers with MockFactory with ScalatestRouteTest {

    // the Akka HTTP route testkit does not yet support a typed actor system (https://github.com/akka/akka-http/issues/2036)
    // so we have to adapt for now
    lazy val testKit = ActorTestKit()
    implicit def typedSystem = testKit.system
    override def createActorSystem(): akka.actor.ActorSystem =
        testKit.system.classicSystem

    /*test("Route GET /hello should say hello") {
        var mockUsers = mock[Users]
        val routesUnderTest = new Routes(mockUsers).routes

        val request = HttpRequest(uri = "/hello")
        request ~> routesUnderTest ~> check {
            status should ===(StatusCodes.OK)

            contentType should ===(ContentTypes.`text/html(UTF-8)`)

            entityAs[String] should ===("<h1>Say hello to akka-http</h1>")
        }
    }*/

    test("Route GET /signup should returns the signup page") {
        var mockUsers = mock[Users]
        var mockProduct = mock[Products]
        var mockCategories = mock[Categories]
        val routesUnderTest = new Routes(mockUsers,mockProduct,mockCategories).routes

        val request = HttpRequest(uri = "/signup")
        request ~> routesUnderTest ~> check {
            status should ===(StatusCodes.OK)

            contentType should ===(ContentTypes.`text/html(UTF-8)`)
        }
    }

    test("Route POST /register should create a new user") {
        var mockUsers = mock[Users]
        (mockUsers.createUser _).expects("toto","ptoto","toto@mail.com").returning(Future(())).once()
        var mockProduct = mock[Products]
        var mockCategories = mock[Categories]

        val routesUnderTest = new Routes(mockUsers,mockProduct,mockCategories).routes

        val request = HttpRequest(
            method = HttpMethods.POST,
            uri = "/register",
            entity = FormData(("username", "toto"),("password","ptoto"),("email","toto@mail.com")).toEntity
        )
        request ~> routesUnderTest ~> check {
            status should ===(StatusCodes.OK)

            contentType should ===(ContentTypes.`text/plain(UTF-8)`)

            entityAs[String] should ===("Welcome 'toto'! You've just been registered to our great marketplace.")
        }
    }

    test("Route POST /register should warn the user when username is already taken") {
        var mockUsers = mock[Users]
        (mockUsers.createUser _).expects("toto","ptoto","toto@mail.com").returns(Future({
            throw new UserAlreadyExistsException("")
        })).once()
        var mockProduct = mock[Products]
        var mockCategories = mock[Categories]

        val routesUnderTest = new Routes(mockUsers,mockProduct, mockCategories).routes

        val request = HttpRequest(
            method = HttpMethods.POST,
            uri = "/register",
            entity = FormData(("username", "toto"),("password","ptoto"),("email","toto@mail.com")).toEntity
        )
        request ~> routesUnderTest ~> check {
            status should ===(StatusCodes.OK)

            contentType should ===(ContentTypes.`text/plain(UTF-8)`)

            entityAs[String] should ===("The username 'toto' is already taken. Please choose another username.")
        }
    }

    test("Route GET /users should display the list of users") {
        var mockUsers = mock[Users]
        val userList = List(
            User(username="riri", userId="id1",password = "priri", mail = "toto@mail.com", cart = ""),
            User(username="fifi", userId="id2",password = "pfifi",mail = "fifi@maiil.com", cart = ""),
            User(username="lulu", userId="id2",password="",mail="", cart = "")
        )
        (mockUsers.getAllUsers _).expects().returns(Future(userList)).once()
        var mockProduct = mock[Products]
        var mockCategories = mock[Categories]

        val routesUnderTest = new Routes(mockUsers,mockProduct,mockCategories).routes

        val request = HttpRequest(uri = "/users")
        request ~> routesUnderTest ~> check {
            status should ===(StatusCodes.OK)

            contentType should ===(ContentTypes.`text/html(UTF-8)`)
        }
    }
}
