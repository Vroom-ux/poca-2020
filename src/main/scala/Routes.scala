
package poca

import scala.concurrent.Future
import akka.http.scaladsl.server.Directives.{path, get, post, formFieldMap, complete, concat}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model.{HttpEntity, HttpResponse, ContentTypes, StatusCodes}
import com.typesafe.scalalogging.LazyLogging
import TwirlMarshaller._


class Routes(users: Users) extends LazyLogging {
    implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global

    def getHello() = {
        logger.info("I got a request to greet.")
        //HttpEntity(
        //    ContentTypes.`text/html(UTF-8)`,
        //    "<h1>Say hello to akka-http</h1>"
        //)
        html.hello()
    }

    def getSignup() = {
        logger.info("I got a request for signup.")
        html.signup()
    }

    def getSignin() = {
        logger.info("I got a request for signin.")
        html.signin()
    }

    def isEmpty(x: String) = { x == null || x.trim.isEmpty}

    def login(fields: Map[String, String]): Future[HttpResponse] = {
        logger.info("I got a request for login.")
        
        fields.get("username") match {
            case Some(username) => {
                if(isEmpty(username)){
                    Future(
                        HttpResponse(
                            StatusCodes.OK,
                            entity=s"Field 'username' not found.",
                        )
                    )
                } else {
                    val userSigninFuture: Future[Option[User]] = users.getUserByUsername(username=username)
                    userSigninFuture.flatMap(userSignin => {
                        if(userSignin.isEmpty){
                            Future(
                                HttpResponse(
                                    StatusCodes.OK,
                                    entity=s"The user '$username' doesn't exist",
                                )
                            )
                        } else {
                            Future(
                                HttpResponse(
                                    StatusCodes.OK,
                                    entity=s"Welcome back '$username'",
                                )
                            )
                        }
                    })
                }
            }
            case None => {
                Future(
                    HttpResponse(
                        StatusCodes.BadRequest,
                        entity="Field 'username' not found."
                    )
                )
            }
        }
    }    

    def register(fields: Map[String, String]): Future[HttpResponse] = {
        logger.info("I got a request to register.")

        (fields.get("username"),fields.get("password"),fields.get("email")) match {
            case (Some(username),Some(password),Some(mail)) => {
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
            case _ => {
                Future(
                    HttpResponse(
                        StatusCodes.BadRequest,
                        entity="Field 'username' not found."
                    )
                )
            }
        }
    }

    def getUsers() = {
        logger.info("I got a request to get user list.")

        val userSeqFuture: Future[Seq[User]] = users.getAllUsers()

        userSeqFuture.map(userSeq => html.users(userSeq))
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
            }
        )

}
