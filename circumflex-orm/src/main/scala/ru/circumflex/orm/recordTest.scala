package ru.circumflex.orm

import org.junit._
import Assert._

import com.borachio._

object recordTest {
  class Country extends Record[String, Country] {
    val code = "code".VARCHAR(2).NOT_NULL.DEFAULT("'ch'")
    val name = "name".TEXT.NOT_NULL

    def relation = Country
    def PRIMARY_KEY = code
  }
  object Country extends Country with Table[String, Country]
}
class recordTest extends AbstractMockFactory {

  import recordTest._
  
  @Before def before = resetExpectations
  @After def after = verifyExpectations

  @Test
  def sample() {
    val transaction = mock[Transaction]
    val transactionManager = mock[TransactionManager]
    transactionManager expects 'get returning transaction
    
    implicit val ormConf = mock[ORMConfiguration]   
    ormConf expects 'transactionManager returning transactionManager
    
    val dialect = new H2Dialect
    ormConf expects 'dialect returning dialect
    
    val country = new Country()
    country.code := "jp"
    country.name := "japan"
    country.INSERT_!()
  }
}
