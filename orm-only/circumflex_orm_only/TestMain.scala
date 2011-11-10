package circumflex_orm_only


import ru.circumflex.orm._



class Country extends Record[String, Country] {
  val code = "code".VARCHAR(2).NOT_NULL.DEFAULT("'ch'")
  val name = "name".TEXT.NOT_NULL

  def cities = inverseMany(City.country)
  def relation = Country
  def PRIMARY_KEY = code
}

object Country extends Country with Table[String, Country]

class City extends Record[Long, City] with SequenceGenerator[Long, City] {
  val id = "id".BIGINT.NOT_NULL.AUTO_INCREMENT
  val name = "name".TEXT
  val country = "country_code".TEXT.NOT_NULL
          .REFERENCES(Country)
          .ON_DELETE(CASCADE)
          .ON_UPDATE(CASCADE)

  def relation = City
  def PRIMARY_KEY = id
}

object City extends City with Table[Long, City]



object Creation {

  def main(args: Array[String]) {
    
    import ru.circumflex.core._

    // create my-configuration, put to context
    implicit val ormConf = new MyORMConfiguration
    ormConf.url = "jdbc:h2:sample";
    ormConf.username = "sa"
    ormConf.password = ""
    ormConf.dialect = new H2Dialect
    
//    ctx.put("orm.conf", conf)
    /*
    
    val cx = Circumflex
    cx("orm.connection.driver") = "org.h2.Driver"
    cx("orm.connection.url") = "jdbc:h2:sample"
    cx("orm.connection.username") = "sa"
    cx("orm.connection.password") = ""
    */
    
    val unit = new DDLUnit(Country, City)    
    //unit.createSqls.foreach(println)    
    unit.CREATE()
    
    val country = new Country()
    country.code := "jp"
    country.name := "japan"
    country.save
    
    val country2 = new Country()
    country2.code := "us"
    country2.name := "united states"
    country2.save
    
    COMMIT
    
    val co = Country AS "co"
    val result = (SELECT(co.*) FROM co).list()
    result.foreach(println)
//    result.foreach(r => println(r.code()))
  }
}