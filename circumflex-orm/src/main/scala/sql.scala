package ru.circumflex
package orm

import java.sql.ResultSet

/*!# Schema objects

Following classes represent various database schema objects:

  * `Schema` corresponds to database schema (or catalog), if such objects
  are supported by database vendor;
  * `Constraint` corresponds to one of database constraint types:

    * `Unique`,
    * `ForeignKey`,
    * `CheckConstraint`;

  * `Index` corresponds to database index.

Circumflex ORM also uses some helpers to make DSL-style data definition.
*/
class Schema(val name: String) extends SchemaObject {
  def objectName = "SCHEMA " + name
  def sqlCreate()(implicit ormConf: ORMConfiguration) = ormConf.dialect.createSchema(this)
  def sqlDrop()(implicit ormConf: ORMConfiguration) = ormConf.dialect.dropSchema(this)
}

abstract class Constraint(val constraintName: String,
                          val relation: Relation[_, _])
    extends SchemaObject with SQLable {

  def objectName = "CONSTRAINT " + constraintName
  def sqlCreate()(implicit ormConf: ORMConfiguration) = ormConf.dialect.alterTableAddConstraint(this)
  def sqlDrop()(implicit ormConf: ORMConfiguration) = ormConf.dialect.alterTableDropConstraint(this)
  def toSql()(implicit ormConf: ORMConfiguration) = ormConf.dialect.constraintDefinition(this)

  def sqlDefinition: String

  //TODO override def toString = toSql
  override def toString: String = throw new Exception();
}

class UniqueKey(name: String,
                relation: Relation[_, _],
                val columns: Seq[ValueHolder[_, _]])
    extends Constraint(name, relation) {
  def sqlDefinition = ormConf.dialect.uniqueKeyDefinition(this)
}

class ForeignKey(name: String,
                 childRelation: Relation[_, _],
                 val childColumns: Seq[ValueHolder[_, _]],
                 val parentRelation: Relation[_, _],
                 val parentColumns: Seq[ValueHolder[_, _]])
    extends Constraint(name, childRelation) {

  protected var _onDelete: ForeignKeyAction = NO_ACTION
  def onDelete = _onDelete
  def ON_DELETE(action: ForeignKeyAction): this.type = {
    _onDelete = action
    this
  }

  protected var _onUpdate: ForeignKeyAction = NO_ACTION
  def onUpdate = _onUpdate
  def ON_UPDATE(action: ForeignKeyAction): this.type = {
    _onUpdate = action
    this
  }

  def sqlDefinition = ormConf.dialect.foreignKeyDefinition(this)
}

class CheckConstraint(name: String,
                      relation: Relation[_, _],
                      val expression: String)
    extends Constraint(name, relation) {
  def sqlDefinition = ormConf.dialect.checkConstraintDefinition(this)
}

class Index(val name: String,
            val relation: Relation[_, _],
            val expression: String)
    extends SchemaObject {

  protected var _unique: Boolean = false
  def isUnique = _unique
  def UNIQUE: this.type = {
    this._unique = true
    this
  }

  private var _method: String = "btree"
  def usingClause = _method
  def USING(method: String): this.type = {
    this._method = method
    this
  }

  private var _where: Predicate = EmptyPredicate
  def whereClause = _where
  def WHERE(predicate: Predicate): this.type = {
    this._where = predicate
    this
  }

  def objectName = "INDEX " + name
  def sqlCreate()(implicit ormConf: ORMConfiguration) = ormConf.dialect.createIndex(this)
  def sqlDrop()(implicit ormConf: ORMConfiguration) = ormConf.dialect.dropIndex(this)
}

class ConstraintHelper(name: String, relation: Relation[_, _]) {
  def UNIQUE(columns: ValueHolder[_, _]*): UniqueKey =
    new UniqueKey(name, relation, columns.toList)

  def CHECK(expression: String): CheckConstraint =
    new CheckConstraint(name, relation, expression)

  def FOREIGN_KEY(parentRelation: Relation[_, _],
                  childColumns: Seq[ValueHolder[_, _]],
                  parentColumns: Seq[ValueHolder[_, _]]): ForeignKey =
    new ForeignKey(name, relation, childColumns, parentRelation, parentColumns)

  def FOREIGN_KEY(parentRelation: Relation[_, _],
                  columns: (ValueHolder[_, _], ValueHolder[_, _])*): ForeignKey = {
    val childColumns = columns.map(_._1)
    val parentColumns = columns.map(_._2)
    FOREIGN_KEY(parentRelation, childColumns, parentColumns)
  }

  def FOREIGN_KEY(localColumns: ValueHolder[_, _]*): ForeignKeyHelper =
    new ForeignKeyHelper(name, relation, localColumns)
}

class ForeignKeyHelper(name: String, childRelation: Relation[_, _], childColumns: Seq[ValueHolder[_, _]]) {
  def REFERENCES(parentRelation: Relation[_, _],
                 parentColumns: ValueHolder[_, _]*): ForeignKey =
    new ForeignKey(name, childRelation, childColumns, parentRelation, parentColumns)
}

class DefinitionHelper[R <: Record[_, R]](name: String, record: R) {
  def INTEGER = new IntField(name, record)
  def BIGINT = new LongField(name, record)
  def DOUBLE(precision: Int = -1, scale: Int = 0) =
    new DoubleField(name, record, precision, scale)
  def NUMERIC(precision: Int = -1,
              scale: Int = 0,
              roundingMode: BigDecimal.RoundingMode.RoundingMode = BigDecimal.RoundingMode.HALF_EVEN) =
    new NumericField(name, record, precision, scale, roundingMode)
  def TEXT = new TextField(name, record, ormConf.dialect.textType)
  def VARCHAR(length: Int = -1) = new TextField(name, record, length)
  def BOOLEAN = new BooleanField(name, record)
  def DATE = new DateField(name, record)
  def TIME = new TimeField(name, record)
  def TIMESTAMP = new TimestampField(name, record)
  def XML(root: String = name) = new XmlField(name, record, root)
  def BINARY = new BinaryField(name, record)

  def INDEX(expression: String) = new Index(name, record.relation, expression)
}

case class ForeignKeyAction(_toSql: String) extends SQLable {
 //TODO override def toString = toSql
  override def toSql()(implicit ormConf: ORMConfiguration) = _toSql;
  override def toString: String = throw new Exception
}

case class JoinType(_toSql: String) extends SQLable {
   override def toSql()(implicit ormConf: ORMConfiguration) = _toSql;
 //TODO override def toString = toSql
   override def toString: String = throw new Exception
}

case class SetOperation(_toSql: String) extends SQLable {
   override def toSql()(implicit ormConf: ORMConfiguration) = _toSql;
 //TODO  override def toString = toSql
   override def toString: String = throw new Exception
}

class Order(val expression: String, val _parameters: Seq[Any])
    extends Expression {
  protected var _specificator = ormConf.dialect.asc
  def parameters()(implicit ormConf: ORMConfiguration) = _parameters;
  def ASC()(implicit ormConf: ORMConfiguration): this.type = {
    this._specificator = ormConf.dialect.asc
    this
  }
  def DESC()(implicit ormConf: ORMConfiguration): this.type = {
    this._specificator = ormConf.dialect.desc
    this
  }
  def toSql()(implicit ormConf: ORMConfiguration) = expression + " " + _specificator
}
