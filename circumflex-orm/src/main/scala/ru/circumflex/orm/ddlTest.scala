package ru.circumflex
package orm

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

import org.junit._
import Assert._

class ddlTest {
  
  @Test
  def h2CreationTest() {
    implicit val ormConf = new MyORMConfiguration
    /*
    ormConf.url = "jdbc:h2:sample";
    ormConf.username = "sa"
    ormConf.password = ""
    */
    ormConf.dialect = new H2Dialect
    val unit = new DDLUnit(Country, City)    
    assertTrue(unit.createSqls == List(
"CREATE SCHEMA public",
"CREATE SEQUENCE public.city_id_seq",
"CREATE TABLE public.country (code VARCHAR(2) NOT NULL DEFAULT 'ch', name VARCHAR NOT NULL, PRIMARY KEY (code))",
"CREATE TABLE public.city (id BIGINT NOT NULL DEFAULT NEXTVAL('public.city_id_seq'), name VARCHAR, country_code VARCHAR NOT NULL, PRIMARY KEY (id))",
"ALTER TABLE public.city ADD CONSTRAINT city_country_code_fkey FOREIGN KEY (country_code) REFERENCES public.country (code) ON DELETE CASCADE ON UPDATE CASCADE"
    ))
  }
  
  @Test
  def mysqlCreationTest() {
    implicit val ormConf = new MyORMConfiguration
    /*
    ormConf.url = "jdbc:h2:sample";
    ormConf.username = "sa"
    ormConf.password = ""
    */
    ormConf.dialect = new MySQLDialect
    val unit = new DDLUnit(Country, City)  
    assertTrue(unit.createSqls == List(
"CREATE SEQUENCE public.city_id_seq",
"CREATE TABLE country (code VARCHAR(2) NOT NULL DEFAULT 'ch', name TEXT NOT NULL, PRIMARY KEY (code))",
"CREATE TABLE city (id BIGINT NOT NULL AUTO_INCREMENT, name TEXT, country_code TEXT NOT NULL, PRIMARY KEY (id))",
"ALTER TABLE city ADD CONSTRAINT city_country_code_fkey FOREIGN KEY (country_code) REFERENCES country (code) ON DELETE CASCADE ON UPDATE CASCADE"
    ))
  }
    
  
}