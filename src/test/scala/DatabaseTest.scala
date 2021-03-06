
import scala.util.{Failure, Success}
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.meta._
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers}
import org.scalatest.funsuite.AnyFunSuite
import com.typesafe.scalalogging.LazyLogging
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import ch.qos.logback.classic.{Level, Logger}
import org.mindrot.jbcrypt.BCrypt
import org.slf4j.LoggerFactory
import poca.{Categories, Category, CategoryAlreadyExistsException, MyDatabase, Product, ProductAlreadyExistsException, Products, Routes, RunMigrations, User, UserAlreadyExistsException, Users}


class DatabaseTest extends AnyFunSuite with Matchers with BeforeAndAfterAll with BeforeAndAfterEach with LazyLogging {
    val rootLogger: Logger = LoggerFactory.getLogger("com").asInstanceOf[Logger]
    rootLogger.setLevel(Level.INFO)
    val slickLogger: Logger = LoggerFactory.getLogger("slick").asInstanceOf[Logger]
    slickLogger.setLevel(Level.INFO)

    // In principle, mutable objets should not be shared between tests, because tests should be independent from each other. However for performance the connection to the database should not be recreated for each test. Here we prefer to share the database.
    override def beforeAll() {
        val isRunningOnCI = sys.env.getOrElse("CI", "") != ""
        val configName = if (isRunningOnCI) "myTestDBforCI" else "myTestDB"
        val config = ConfigFactory.load().getConfig(configName)
        MyDatabase.initialize(config)
    }
    override def afterAll() {
        MyDatabase.db.close
    }

    override def beforeEach() {
        val resetSchema = sqlu"drop schema public cascade; create schema public;"
        val resetFuture: Future[Int] = MyDatabase.db.run(resetSchema)
        Await.result(resetFuture, Duration.Inf)
        new RunMigrations(MyDatabase.db)()
    }

    test("Users.createUser should create a new user") {
        val users: Users = new Users()

        val createUserFuture: Future[Unit] = users.createUser("toto","ptoto","toto@mail.com")
        Await.ready(createUserFuture, Duration.Inf)

        // Check that the future succeeds
        createUserFuture.value should be(Some(Success(())))

        val getUsersFuture: Future[Seq[User]] = users.getAllUsers()
        val allUsers: Seq[User] = Await.result(getUsersFuture, Duration.Inf)

        allUsers.length should be(1)
        allUsers.head.username should be("toto")
    }

    test("Users.createUser returned future should fail if the user already exists") {
        val users: Users = new Users()

        val createUserFuture: Future[Unit] = users.createUser("toto","ptoto","toto@mail.com")
        Await.ready(createUserFuture, Duration.Inf)

        val createDuplicateUserFuture: Future[Unit] = users.createUser("toto","ptoto","toto@mail.com")
        Await.ready(createDuplicateUserFuture, Duration.Inf)

        createDuplicateUserFuture.value match {
            case Some(Failure(exc: UserAlreadyExistsException)) => exc.getMessage should equal ("A user with username 'toto' already exists.")
            case _ => fail("The future should fail.")
        }
    }

    test("Users.getUserByUsername should return no user if it does not exist") {
        val users: Users = new Users()

        val createUserFuture: Future[Unit] = users.createUser("toto","ptoto","toto@mail.com")
        Await.ready(createUserFuture, Duration.Inf)

        val returnedUserFuture: Future[Option[User]] = users.getUserByUsername("somebody-else")
        val returnedUser: Option[User] = Await.result(returnedUserFuture, Duration.Inf)

        returnedUser should be(None)
    }

    test("Users.getUserByUsername should return a user") {
        val users: Users = new Users()

        val createUserFuture: Future[Unit] = users.createUser("toto","ptoto","toto@mail.com")
        Await.ready(createUserFuture, Duration.Inf)

        val returnedUserFuture: Future[Option[User]] = users.getUserByUsername("toto")
        val returnedUser: Option[User] = Await.result(returnedUserFuture, Duration.Inf)

        returnedUser match {
            case Some(user) => user.username should be("toto")
            case None => fail("Should return a user.")
        }
    }

    test("Users.getAllUsers should return a list of users") {
        val users: Users = new Users()

        val createUserFuture: Future[Unit] = users.createUser("riri","priri","riri@mail.com")
        Await.ready(createUserFuture, Duration.Inf)

        val createAnotherUserFuture: Future[Unit] = users.createUser("fifi","pfifi","fifi@mail.com")
        Await.ready(createAnotherUserFuture, Duration.Inf)

        val returnedUserSeqFuture: Future[Seq[User]] = users.getAllUsers()
        val returnedUserSeq: Seq[User] = Await.result(returnedUserSeqFuture, Duration.Inf)

        returnedUserSeq.length should be(2)
    }
    
    // Cart
    test("Users.getCartByUsername should return the non parsed cart"){
        val users: Users = new Users()

        val createUserFuture: Future[Unit] = users.createUser("toto","ptoto","toto@mail.com")
        Await.ready(createUserFuture, Duration.Inf)

        val FutureCart: Future[String] = users.getCartByUsername("toto")
        val Cart: String = Await.result(FutureCart, Duration.Inf)

        Cart should be("")
    }

    test("Users.addProductCart should add product to cart"){
        val users: Users = new Users()

        val createUserFuture: Future[Unit] = users.createUser("toto","ptoto","toto@mail.com")
        Await.ready(createUserFuture, Duration.Inf)


        val returnedFuture: Future[Unit] = users.addProductCart("toto", "01")
        Await.ready(returnedFuture, Duration.Inf)

        val returnedUserFuture: Future[Option[User]] = users.getUserByUsername("toto")
        val returnedUser: Option[User] = Await.result(returnedUserFuture, Duration.Inf)

        returnedUser match {
            case Some(user) => user.cart should be("01,")
        }
    }

    test("Users.removeProductCart should remove product to cart"){
        val users: Users = new Users()

        val createUserFuture: Future[Unit] = users.createUser("toto","ptoto","toto@mail.com")
        Await.ready(createUserFuture, Duration.Inf)


        val returnedFuture1: Future[Unit] = users.addProductCart("toto", "01")
        Await.ready(returnedFuture1, Duration.Inf)

        val returnedFuture2: Future[Unit] = users.addProductCart("toto", "02")
        Await.ready(returnedFuture2, Duration.Inf)

        val returnedFuture3: Future[Unit] = users.addProductCart("toto", "03")
        Await.ready(returnedFuture3, Duration.Inf)

        val returnedFuture: Future[Unit] = users.removeProductCart("toto", "03")
        Await.ready(returnedFuture, Duration.Inf)

        val returnedUserFuture: Future[Option[User]] = users.getUserByUsername("toto")
        val returnedUser: Option[User] = Await.result(returnedUserFuture, Duration.Inf)

        returnedUser match {
            case Some(user) => user.cart should be("01,02,")
        }
    }

    test("Users.removeProductCart should do nothing if the product isn't in the cart"){
        val users: Users = new Users()

        val createUserFuture: Future[Unit] = users.createUser("toto","ptoto","toto@mail.com")
        Await.ready(createUserFuture, Duration.Inf)


        val returnedFuture1: Future[Unit] = users.addProductCart("toto", "01")
        Await.ready(returnedFuture1, Duration.Inf)

        val returnedFuture: Future[Unit] = users.removeProductCart("toto", "03")
        Await.ready(returnedFuture, Duration.Inf)

        val returnedUserFuture: Future[Option[User]] = users.getUserByUsername("toto")
        val returnedUser: Option[User] = Await.result(returnedUserFuture, Duration.Inf)

        returnedUser match {
            case Some(user) => user.cart should be("01,")
        }
    }

    test("Users.changeUsername should change user's username"){
        val users: Users = new Users()

        val createUserFuture: Future[Unit] = users.createUser("toto","ptoto","toto@mail.com")
        Await.ready(createUserFuture, Duration.Inf)
        createUserFuture.value should be(Some(Success(())))

        val returnedFuture: Future[Unit] = users.changeUsername("toto","tata")
        Await.ready(returnedFuture, Duration.Inf)
        returnedFuture.value should be(Some(Success(())))

        val getUsersFuture: Future[Seq[User]] = users.getAllUsers()
        val allUsers: Seq[User] = Await.result(getUsersFuture, Duration.Inf)

        allUsers.length should be(1)
        allUsers.head.username should be("tata")

    }

    /*test("Users.changePassword should change user's Password"){
        val users: Users = new Users()

        val createUserFuture: Future[Unit] = users.createUser("toto","ptoto","toto@mail.com")
        Await.ready(createUserFuture, Duration.Inf)
        createUserFuture.value should be(Some(Success(())))

        val password = "ptata"
        val returnedFuture: Future[Unit] = users.changePassword("toto",password)
        val protectpassword = BCrypt.hashpw(password,BCrypt.gensalt(12))

        Await.ready(returnedFuture, Duration.Inf)
        returnedFuture.value should be(Some(Success(())))

        val getUsersFuture: Future[Seq[User]] = users.getAllUsers()
        val allUsers: Seq[User] = Await.result(getUsersFuture, Duration.Inf)

        allUsers.length should be(1)
        allUsers.head.password should be(protectpassword)

    }*/

    test("Users.changeMail should change user's Mail"){
        val users: Users = new Users()

        val createUserFuture: Future[Unit] = users.createUser("toto","ptoto","toto@mail.com")
        Await.ready(createUserFuture, Duration.Inf)
        createUserFuture.value should be(Some(Success(())))

        val returnedFuture: Future[Unit] = users.changeMail("toto","tata@mail.com")
        Await.ready(returnedFuture, Duration.Inf)
        returnedFuture.value should be(Some(Success(())))

        val getUsersFuture: Future[Seq[User]] = users.getAllUsers()
        val allUsers: Seq[User] = Await.result(getUsersFuture, Duration.Inf)

        allUsers.length should be(1)
        allUsers.head.mail should be("tata@mail.com")

    }


    // Products

    test("Products.createProduct should create a new product") {
        val products: Products = new Products()

        val createProductFuture: Future[Unit] = products.createProduct("bestProduct","bestproduct",BigDecimal("0.00"),"test")
        Await.ready(createProductFuture, Duration.Inf)

        // Check that the future succeeds
        createProductFuture.value should be(Some(Success(())))

        val getProductsFuture: Future[Seq[Product]] = products.getAllProducts()
        val allProducts: Seq[Product] = Await.result(getProductsFuture, Duration.Inf)

        allProducts.length should be(1)
        allProducts.head.productname should be("bestProduct")
    }


    test("Products.getProductByProductname should return no product if it does not exist") {
        val products: Products = new Products()

        val createProductFuture: Future[Unit] = products.createProduct("bestProduct","bestproduct",BigDecimal("0.00"),"test")
        Await.ready(createProductFuture, Duration.Inf)

        val returnedProductFuture: Future[Option[Product]] = products.getProductByProductname("other-product")
        val returnedProduct: Option[Product] = Await.result(returnedProductFuture, Duration.Inf)

        returnedProduct should be(None)
    }

    test("Products.getProductByProductname should return a product") {
        val products: Products = new Products()

        val createProductFuture: Future[Unit] = products.createProduct("bestProduct","bestproduct",BigDecimal("0.00"),"test")
        Await.ready(createProductFuture, Duration.Inf)

        val returnedProductFuture: Future[Option[Product]] = products.getProductByProductname("bestProduct")
        val returnedProduct: Option[Product] = Await.result(returnedProductFuture, Duration.Inf)

        returnedProduct match {
            case Some(product) => product.productname should be("bestProduct")
            case None => fail("Should return a product.")
        }
    }

    test("Products.getAllProducts should return a list of products") {
        val products: Products = new Products()

        val createProductFuture: Future[Unit] = products.createProduct("bestProduct","bestproduct",BigDecimal("0.00"),"test")
        Await.ready(createProductFuture, Duration.Inf)

        val createAnotherProductFuture: Future[Unit] = products.createProduct("bestProduct2","bestproduct2",BigDecimal("0.00"),"test")
        Await.ready(createAnotherProductFuture, Duration.Inf)

        val returnedProductSeqFuture: Future[Seq[Product]] = products.getAllProducts()
        val returnedProductSeq: Seq[Product] = Await.result(returnedProductSeqFuture, Duration.Inf)

        returnedProductSeq.length should be(2)
    }

    test("Products.getProductByProductCategory should return a list of products by category") {
        val products: Products = new Products()

        val createProductFuture1: Future[Unit] = products.createProduct("bestProduct","bestproduct",BigDecimal("0.00"),"test1")
        Await.ready(createProductFuture1, Duration.Inf)
        val createProductFuture2: Future[Unit] = products.createProduct("bestProduct","bestproduct",BigDecimal("0.00"),"test1")
        Await.ready(createProductFuture2, Duration.Inf)
        val createProductFuture3: Future[Unit] = products.createProduct("bestProduct","bestproduct",BigDecimal("0.00"),"test2")
        Await.ready(createProductFuture3, Duration.Inf)

        val returnedProductSeqFuture: Future[Seq[Product]] = products.getProductByProductCategory("test1")
        val returnedProductSeq: Seq[Product] = Await.result(returnedProductSeqFuture, Duration.Inf)

        returnedProductSeq.length should be(2)
    }


    test("Categories.addCategory should add new category"){
        val categories: Categories = new Categories()

        val createCategoryFuture: Future[Unit] = categories.addCategory("toto")
        Await.ready(createCategoryFuture, Duration.Inf)

        // Check that the future succeeds
        createCategoryFuture.value should be(Some(Success(())))

        val getCategoriesFuture: Future[Seq[Category]] = categories.getAllCategories()
        val allCategories: Seq[Category] = Await.result(getCategoriesFuture, Duration.Inf)

        allCategories.length should be(7)
        //TODO : have to change this test if add by alphabetical order is implemented
        allCategories.last.categoryName should be ("toto")
    }

    test("Categories.addCategory returned future should fail if the category already exists") {
        val categories: Categories = new Categories()

        val createCategoryFuture: Future[Unit] = categories.addCategory("toto")
        Await.ready(createCategoryFuture, Duration.Inf)

        val createDupCategoryFuture: Future[Unit] = categories.addCategory("toto")
        Await.ready(createDupCategoryFuture, Duration.Inf)

        createDupCategoryFuture.value match {
            case Some(Failure(exc: CategoryAlreadyExistsException)) => exc.getMessage should equal ("A Category with Categoryname 'toto' already exists.")
            case _ => fail("The future should fail.")
        }
    }

    test("Categories.getCategorybyCategoryName should return a category") {
        val categories: Categories = new Categories()

        val returnedCategoryFuture: Future[Option[Category]] = categories.getCategorybyCategoryName("Sport")
        val returnedCategory: Option[Category] = Await.result(returnedCategoryFuture, Duration.Inf)

        returnedCategory match {
            case Some(category) => category.categoryName should be("Sport")
            case None => fail("Should return a category.")
        }
    }

    test("Categories.getAllCategories should return a list of categories") {
        val categories: Categories = new Categories()

        val returnedCategoryFutureSeq: Future[Seq[Category]] = categories.getAllCategories()
        val returnedCategorySeq: Seq[Category] = Await.result(returnedCategoryFutureSeq, Duration.Inf)

        returnedCategorySeq.length should be(6)
    }
    
}
