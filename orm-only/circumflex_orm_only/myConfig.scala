package circumflex_orm_only

import ru.circumflex._
import ru.circumflex.orm._
import ru.circumflex.core._
import java.sql.{Connection, _}


class MyORMConfiguration extends ORMConfiguration {

  override val name: String = ""
  //def prefix(sym: String) = if (name == "") "" else name + sym

  // Database connectivity parameters
  private var _url: Option[String] = None
  def url_=(value: String) { require(value != null); _url = Some(value) }
  override def url = _url.getOrElse(throw new ORMException(
    "Missing mandatory configuration parameter 'url'."))
    
  private var _username: Option[String] = None
  def username_=(value: String) { require(value != null); _username = Some(value); }
  override def username = _username.getOrElse(throw new ORMException(
    "Missing mandatory configuration parameter 'username'."))
    
  private var _password: Option[String] = None
  def password_=(value: String) { require(value != null); _password = Some(value) }
  override def password = _password.getOrElse(throw new ORMException(
    "Missing mandatory configuration parameter 'password'."))

  private var _dialect: Option[Dialect] = None
  def dialect_=(value: Dialect) { require(value != null); _dialect = Some(value) }
  override def dialect = _dialect.getOrElse(throw new ORMException(
      "Missing mandatory configuration parameter 'dialect'."))

  private var _driver: Option[String] = None
  def driver_=(value: String) { require(value != null); _driver = Some(value) }
  override def driver = _driver.getOrElse(dialect.driverClass)
  
  private var _isolation: Int = Connection.TRANSACTION_SERIALIZABLE
  def isolation_=(value: Int) { _isolation = value }
  override def isolation = _isolation
  

  // Configuration objects

  override def typeConverter: TypeConverter = { throw new Exception }
  
  private var _transactionManager: TransactionManager = new DefaultTransactionManager(this);
  def transactionManager_=(value: TransactionManager) { require(value != null); _transactionManager = value }
  override def transactionManager: TransactionManager = { _transactionManager }
  
  private var _defaultSchema = new Schema("public")
  def defaultSchema_=(value: Schema) { require(value != null); _defaultSchema = value }
  override def defaultSchema: Schema = { _defaultSchema }
  
  private var _statisticsManager = new StatisticsManager;
  def statisticsManager_=(value: StatisticsManager) { require(value != null); _statisticsManager = value }
  override def statisticsManager: StatisticsManager = { _statisticsManager }
  
  private var _connectionProvider: ConnectionProvider = null
  def connectionProvider_=(value: ConnectionProvider) { require(value != null); _connectionProvider = value }
  override def connectionProvider: ConnectionProvider = this.synchronized { 
    if (_connectionProvider != null) return _connectionProvider;
    _connectionProvider = new SimpleConnectionProvider(driver, url, username, password, isolation)
    _connectionProvider
  }
  
  /*
  lazy val typeConverter: TypeConverter = cx.instantiate[TypeConverter](
    "orm.typeConverter", new TypeConverter)
    
  lazy val transactionManager: TransactionManager = cx.instantiate[TransactionManager](
    "orm.transactionManager", new DefaultTransactionManager)
    
  lazy val defaultSchema: Schema = new Schema(
    cx.get("orm.defaultSchema").map(_.toString).getOrElse("public"))
  
  lazy val statisticsManager: StatisticsManager = cx.instantiate[StatisticsManager](
    "orm.statisticsManager", new StatisticsManager)
    
  lazy val connectionProvider: ConnectionProvider = cx.instantiate[ConnectionProvider](
    "orm.connectionProvider", new SimpleConnectionProvider(driver, url, username, password, isolation))

   */
  
}



