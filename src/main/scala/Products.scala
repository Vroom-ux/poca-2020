package poca

import scala.concurrent.Future
import slick.jdbc.PostgresProfile.api._
import java.util.UUID

case class Product(productId: String, productname: String, productdescription : String, productprice : BigDecimal, productcategory : String)

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
        val existingProductsFuture = getProductByProductname(productname)

        existingProductsFuture .flatMap(existingProducts => {
            if (existingProducts.isEmpty) {
                val productId = UUID.randomUUID.toString()
                val newProduct = Product(productId,productname,productdescription,productprice,productcategory)
                val newProductAsTuple: (String, String,String,BigDecimal,String) = Product.unapply(newProduct).get

                val dbio: DBIO[Int] = products += newProductAsTuple
                var resultFuture: Future[Int] = db.run(dbio)

                // We do not care about the Int value
                resultFuture.map(_ => ())
            } else {
                throw new ProductAlreadyExistsException(s"A product with productname '$productname' already exists.")
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

    def getAllProducts(): Future[Seq[Product]] = {
        val productListFuture = db.run(products.result)

        productListFuture.map((productList: Seq[(String, String,String,BigDecimal,String)]) => {
            productList.map(Product tupled _)
        })
    }
}


