package poca

import slick.jdbc.PostgresProfile.api._
import java.util.UUID
import scala.util.{Success, Failure}
import scala.util.control.Breaks._
import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }

case class Product(productId: String, productname: String, productdescription : String, productprice : BigDecimal, productcategory : String)


//TODO remove this exeception : it doesn't matter if two products have the same name.
final case class ProductAlreadyExistsException(private val message: String="", private val cause: Throwable=None.orNull)
  extends Exception(message, cause)


class Products {

  class ProductsTable(tag: Tag) extends Table[(String, String,String,BigDecimal,String)](tag, "products") {
    def productId = column[String]("productId", O.PrimaryKey)

    def productname = column[String]("productname")

    def productdescription = column[String]("productdescription")

    def productprice = column[BigDecimal]("productprice")

    def productcategory = column[String]("productcategory")

    def * = (productId, productname,productdescription,productprice,productcategory)
  }

    implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global
    val db = MyDatabase.db
    val products = TableQuery[ProductsTable]

    def createProduct(productname: String,productdescription :String, productprice :BigDecimal,productcategory : String): Future[Unit] = {
        val productId = UUID.randomUUID.toString()
        val existingProductsFuture = getProductByProductId(productId)
        existingProductsFuture .flatMap(existingProducts => {
            if (existingProducts.isEmpty) {

                val newProduct = Product(productId,productname,productdescription,productprice,productcategory)
                val newProductAsTuple: (String, String,String,BigDecimal,String) = Product.unapply(newProduct).get

                val dbio: DBIO[Int] = products += newProductAsTuple
                var resultFuture: Future[Int] = db.run(dbio)

                // We do not care about the Int value
                resultFuture.map(_ => ())
            } else {
                throw new ProductAlreadyExistsException(s"A product with productname '$productId' already exists.")
            }
        })
    }

    def getProductByProductId(productId: String): Future[Option[Product]] = {
        val query = products.filter(_.productId === productId)

        val productListFuture = db.run(query.result)

        productListFuture.map((productList: Seq[(String, String,String,BigDecimal,String)]) => {
            productList.length match {
                case 0 => None
                case 1 => Some(Product tupled productList.head)
                case _ => throw new InconsistentStateException(s"Productname $productId is linked to several products in database!")
            }
        })
    }

    def getProductByProductname(productname: String): Future[Option[Product]] = {
        val query = products.filter(_.productname === productname)

        val productListFuture = db.run(query.result)

        productListFuture.map((productList: Seq[(String, String,String,BigDecimal,String)]) => {
            productList.length match {
                case 0 => None
                case 1 => Some(Product tupled productList.head)
                case _ => throw new InconsistentStateException(s"Productname $productname is linked to several products in database!")
            }
        })
    }

    def getProductByProductCategory(productcat: String): Future[Seq[Product]] = {
      val query = products.filter(_.productcategory === productcat)
      val productListFuture = db.run(query.result)
      productListFuture.map((productList: Seq[(String, String,String,BigDecimal,String)]) => {
          productList.map(Product tupled _)
      })
    }

    def getAllProducts(): Future[Seq[Product]] = {
        val productListFuture = db.run(products.result)

        productListFuture.map((productList: Seq[(String, String,String,BigDecimal,String)]) => {
            productList.map(Product tupled _)
        })
    }

    def getSuggestion(userInput : String) : Future[List[String]] = {
        getAllProducts().map({ products => {
                val MAX_SUGGESTIONS = 3
                var lst : List[String] = List()
                var sorted_products_name = products.map(x=>x.productname).sorted
                var j = 0
                breakable {
                    for(product <- sorted_products_name){
                        if(product.size >= userInput.size && product.substring(0,userInput.size) == userInput){
                            lst = product::lst
                            j+=1
                            if(j == MAX_SUGGESTIONS){
                                break                    
                            }
                        }
                    }
                }
                lst.reverse
            }
        })
    }
}


