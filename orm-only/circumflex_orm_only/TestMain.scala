package circumflex_orm_only


import ru.circumflex.orm._



class Country extends Record[String, Country] {
  val code = "code".VARCHAR(2).NOT_NULL.DEFAULT("'ch'")
  def name()(implicit ormConf: ORMConfiguration) = "name".TEXT.NOT_NULL

  def cities = inverseMany(City.country)
  def relation = Country
  def PRIMARY_KEY = code
}

object Country extends Country with Table[String, Country]

class City extends Record[Long, City] with SequenceGenerator[Long, City] {
  val id = "id".BIGINT.NOT_NULL.AUTO_INCREMENT
  def name()(implicit ormConf: ORMConfiguration) = "name".TEXT
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
    /*
    import ru.circumflex.core._

    // create my-configuration, put to context
    val conf = new MyORMConfiguration
    conf.url = "jdbc:h2:sample";
    conf.username = "sa"
    conf.password = ""
    conf.dialect = new H2Dialect
    
    ctx.put("orm.conf", conf)
    /*
    
    val cx = Circumflex
    cx("orm.connection.driver") = "org.h2.Driver"
    cx("orm.connection.url") = "jdbc:h2:sample"
    cx("orm.connection.username") = "sa"
    cx("orm.connection.password") = ""
    */
    
    val unit = new DDLUnit(Country, City)
    unit.CREATE()
    */
  }
}