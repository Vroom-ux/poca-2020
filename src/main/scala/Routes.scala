package poca

import scala.concurrent.Future
import akka.http.scaladsl.server.Directives.{complete, concat, formFieldMap, get, getFromResource, parameters, path, post}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import com.typesafe.scalalogging.LazyLogging
import TwirlMarshaller._
import org.mindrot.jbcrypt.BCrypt
import play.twirl.api.HtmlFormat

class Routes(users: Users, products : Products, categories : Categories) extends LazyLogging {
    implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global

    def getHello : HtmlFormat.Appendable = {
        logger.info("I got a request to greet.")
        //HttpEntity(
        //    ContentTypes.`text/html(UTF-8)`,
        //    "<h1>Say hello to akka-http</h1>"
        //)
        html.hello()
    }

    def getSignup : HtmlFormat.Appendable = {
        logger.info("I got a request for signup.")
        html.signup()
    }

    def getaddProductTest : Future[HtmlFormat.Appendable] = {
        logger.info("I got a request for addProductTest.")

        val categorySeqFuture: Future[Seq[Category]] = categories.getAllCategories()

        categorySeqFuture.map(categorySeq => html.addProductTest(categorySeq))
    }

    def getSignin : HtmlFormat.Appendable = {
        logger.info("I got a request for signin.")
        html.signin()
    }

    def isEmpty(x: String) : Boolean = { x == null || x.trim.isEmpty}

    def login(fields: Map[String, String]): Future[HttpResponse] = {
        logger.info("I got a request for login.")
        
        (fields.get("username"),fields.get("password")) match {
            case (Some(username),Some(password)) => {
                if(isEmpty(username)){
                    Future(
                        HttpResponse(
                            StatusCodes.OK,
                            entity=s"Field 'username' not found.",
                        )
                    )
                }else if(isEmpty(password)){
                    Future(
                        HttpResponse(
                            StatusCodes.OK,
                            entity=s"Field 'password' not found.",
                        )
                    )
                } else {
                    val userSigninFuture: Future[Option[User]] = users.getUserByUsername(username=username)
                    userSigninFuture.flatMap(userSignin => {
                        if(userSignin.isEmpty){
                            Future(
                                HttpResponse(
                                    StatusCodes.OK,
                                    entity=s"Wrong 'username' or password",
                                )
                            )
                        } else {
                            val User(uid,uname,upass,umail,"")=userSignin.getOrElse(Nil)
                            if(BCrypt.checkpw(password,upass)){
                                Future(
                                    HttpResponse(
                                        StatusCodes.OK,
                                        entity=s"Welcome back '$username' your password is '$password', your id is '$uid' and your mail is '$umail'",
                                    )
                                )
                            } else {
                                Future(
                                    HttpResponse(
                                        StatusCodes.OK,
                                        entity=s"Wrong username or 'password'",
                                    )
                                )
                            }
                        }
                    })
                }
            }
            case _ => {
                Future(
                    HttpResponse(
                        StatusCodes.BadRequest,
                        entity="Field 'username' or 'password' not found."
                    )
                )
            }
        }
    }    

    def register(fields: Map[String, String]): Future[HttpResponse] = {
        logger.info("I got a request to register.")

        (fields.get("username"),fields.get("password"),fields.get("email")) match {
            case (Some(username),Some(password),Some(mail)) => {
                if(isEmpty(username)){
                    Future(
                        HttpResponse(
                            StatusCodes.OK,
                            entity=s"Field 'username' not found.",
                        )
                    )
                }else if(isEmpty(password)){
                    Future(
                        HttpResponse(
                            StatusCodes.OK,
                            entity=s"Field 'password' not found.",
                        )
                    )
                }else if(isEmpty(mail)){
                    Future(
                        HttpResponse(
                            StatusCodes.OK,
                            entity=s"Field 'mail' not found.",
                        )
                    )
                }else{
                    val userCreation: Future[Unit] = users.createUser(username=username,password = password,mail = mail)

                    userCreation.map(_ => {
                        HttpResponse(
                            StatusCodes.OK,
                            entity=s"Welcome '$username'! You've just been registered to our great marketplace.",
                        )
                    }).recover({
                        case exc: UserAlreadyExistsException => {
                            HttpResponse(
                                StatusCodes.OK,
                                entity=s"The username '$username' is already taken. Please choose another username.",
                            )
                        }
                    })
                }
            }
            case _ => {
                Future(
                    HttpResponse(
                        StatusCodes.BadRequest,
                        entity="Field 'username', 'password' or 'mail' not found."
                    )
                )
            }
        }
    }

    def addProduct(fields: Map[String, String]): Future[HttpResponse] = {
        logger.info("I got a request to add product.")

        (fields.get("productname"),fields.get("productdescription"),fields.get("productprice"),fields.get("productcategory")) match {
            case (Some(productname),Some(productdescript),Some(productprice),Some(productcat)) => {
                if(isEmpty(productname)){
                    Future(
                        HttpResponse(
                            StatusCodes.OK,
                            entity=s"Field 'Product name' not found.",
                        )
                    )
                }else{
                    val price = BigDecimal(productprice)
                    val productCreation: Future[Unit] = products.createProduct(productname,productdescript,price,productcat)

                    productCreation.map(_ => {
                        HttpResponse(
                            StatusCodes.OK,
                            entity=s"added product '$productname'!",
                        )
                    }).recover({
                        case exc: ProductAlreadyExistsException => {
                            HttpResponse(
                                StatusCodes.OK,
                                entity=s"The product '$productname' already exists",
                            )
                        }
                    })
                }
            }
            case _ => {
                Future(
                    HttpResponse(
                        StatusCodes.BadRequest,
                        entity="Field 'Product name' not found."
                    )
                )
            }
        }
    }

    def getUsers : Future[HtmlFormat.Appendable] = {
        logger.info("I got a request to get user list.")

        val userSeqFuture: Future[Seq[User]] = users.getAllUsers()

        userSeqFuture.map(userSeq => html.users(userSeq))
    }

    def getMarket(fields: Map[String, String]) : Future[HtmlFormat.Appendable] = {
        logger.info("I got a request to get product list.")
        val categoriesSeqFuture : Future[Seq[Category]] = categories.getAllCategories()
        (fields.get("SelectCategory"),fields.get("sortByPrice")) match {
            case (Some(category),Some(price)) => {
                val productSeqfuture: Future[Seq[Product]] = if(category != "All") products.getProductByProductCategory(category) else products.getAllProducts()
                if(price == "Lowest first"){
                    productSeqfuture.flatMap(productSeq => categoriesSeqFuture.map(cat => html.market(productSeq.sortWith(_.productprice<_.productprice),cat,category)))
                }else if(price == "Highest first"){
                    productSeqfuture.flatMap(productSeq => categoriesSeqFuture.map(cat => html.market(productSeq.sortWith(_.productprice>_.productprice),cat,category)))
                }else {
                    productSeqfuture.flatMap(productSeq => categoriesSeqFuture.map(cat => html.market(productSeq, cat, category)))
                }
            }
            case _ => {
                val productSeqfuture: Future[Seq[Product]] = products.getAllProducts()
                productSeqfuture.flatMap(productSeq => categoriesSeqFuture.map(cat => html.market(productSeq,cat,"")))
            }
        }}

    def getproductdetails(productId: String) : Future[HtmlFormat.Appendable] = {
        logger.info("I got a request to get details of product.")
        val productFuture = products.getProductByProductId(productId)
        productFuture.map(productOPT => html.productdetails(productOPT))
    }

    def getProfile() :HtmlFormat.Appendable  = {
        logger.info("I got a request to get user's profile")

        html.profile()
    }


    val routes: Route = 
        concat(
            path("hello") {
                get {
                    complete(getHello)
                }
            },
            path("signup") {
                get {
                    complete(getSignup)
                }
            },
            path("register") {
                (post & formFieldMap) { fields =>
                    complete(register(fields))
                }
            },
            path("users") {
                get {
                    complete(getUsers)
                }
            },
            path("login") {
                (post & formFieldMap) { fields =>
                    complete(login(fields))
                }
            },
            path("signin") {
                get {
                    complete(getSignin)
                }
            },
            path("market"){
                formFieldMap { fields =>
                    complete(getMarket(fields))
                }
            },
            path("addProduct"){
                (post & formFieldMap) { fields =>
                    complete(addProduct(fields))
                }
            },
            path("addProductTest") {
                get {
                    complete(getaddProductTest)
                }
            },path("style.css"){
               logger.info("I got a request for css ressource.")
               getFromResource("format/style.css")
            },path("logo"){
               logger.info("I got a request for the logo.")
               getFromResource("img/logo.png")
            },
            path("productdetails") {
                get {
                    parameters("pId") { pId =>
                        complete(getproductdetails(pId))
                    }
                }
            },
            path("profile") {
                get {
                    complete(getProfile)
                }
            }
        )

}
