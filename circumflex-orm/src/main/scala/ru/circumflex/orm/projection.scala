package ru.circumflex
package orm

import java.sql.ResultSet

/*!# Projections

In relational algebra a _projection_ is a function which describes a subset of
columns returned from an SQL query. In Circumflex ORM instances of the `Projection`
trait are used to process `ResultSet` and determine the result type of SQL queries.

We distinguish between _atomic_ and _composite_ projections: the former ones
span across only one column of `ResultSet`, the latter ones contain a list of internal
projections and therefore span across multiple columns.

Like with relation nodes, special alias `this` is expanded into query-unique alias
to prevent collisions when aliases are not assigned explicitly.

Circumflex ORM supports querying arbitrary expressions which your database understands,
you only need to explicitly specify an expected type.
*/
trait Projection[T] extends SQLable {

  def read(rs: ResultSet)(implicit ormConf: ORMConfiguration): Option[T]

  def sqlAliases()(implicit ormConf: ORMConfiguration): Seq[String]

  protected var _alias: String = "this"
  def alias = _alias
  def AS(alias: String): this.type = {
    this._alias = alias
    this
  }

  // TODOoverride def toString = toSql
  override def toString: String = throw new Exception
}

object Projection {
  implicit def toOrder(p: Projection[_]): Order =
    new Order(p.alias, Nil)
}

trait AtomicProjection[T] extends Projection[T] {
  def expression()(implicit ormConf: ORMConfiguration): String
  def sqlAliases()(implicit ormConf: ORMConfiguration) = List(alias)
}

trait CompositeProjection[T] extends Projection[T] {
  def subProjections(): Seq[Projection[_]]
  def sqlAliases()(implicit ormConf: ORMConfiguration) = subProjections.flatMap(_.sqlAliases)

  /* TODO
  override def equals(obj: Any) = obj match {
    case p: CompositeProjection[_] =>
      this.subProjections.toList == p.subProjections.toList
    case _ => false
  }
  */
  override def equals(obj: Any): Boolean = throw new Exception
  

  private var _hash = 0;
  /* TODO
  override def hashCode: Int = {
    if (_hash == 0)
      for (p <- subProjections)
        _hash = 31 * _hash + p.hashCode
    _hash
  }
  */
  override def hashCode: Int = throw new Exception

  def toSql()(implicit ormConf: ORMConfiguration) = subProjections.map(_.toSql).mkString(", ")
}

class ExpressionProjection[T](val _expression: String)
    extends AtomicProjection[T] {

  def expression()(implicit ormConf: ORMConfiguration) = _expression
  def toSql()(implicit ormConf: ORMConfiguration) = ormConf.dialect.alias(expression, alias)

  def read(rs: ResultSet)(implicit ormConf: ORMConfiguration): Option[T] = {
    val o = rs.getObject(alias)
    if (rs.wasNull) None
    else Some(o.asInstanceOf[T])
  }

  /* TODO
  override def equals(obj: Any) = obj match {
    case p: ExpressionProjection[_] =>
      p.expression == this.expression
    case _ => false
  }
  */
  override def equals(obj: Any): Boolean = throw new Exception

  // TODO override def hashCode = expression.hashCode
  override def hashCode: Int = throw new Exception
}

class FieldProjection[T, R <: Record[_, R]](
        val node: RelationNode[_, R],
        val field: Field[T, R])
    extends AtomicProjection[T] {

  def expression()(implicit ormConf: ORMConfiguration) = ormConf.dialect.qualifyColumn(field, node.alias)

  def toSql()(implicit ormConf: ORMConfiguration): String = ormConf.dialect.alias(expression, alias)

  def read(rs: ResultSet)(implicit ormConf: ORMConfiguration) = field.read(rs, alias)

  /* TODO
  override def equals(obj: Any) = obj match {
    case p: FieldProjection[_, _] =>
      p.node == this.node && p.field.name == this.field.name
    case _ => false
  }
  */
  override def equals(obj: Any) = throw new Exception
  //TODO override def hashCode = node.hashCode * 31 + field.name.hashCode
  override def hashCode: Int = throw new Exception
}

class RecordProjection[PK, R <: Record[PK, R]](val node: RelationNode[PK, R])
    extends CompositeProjection[R] {

  protected def _fieldProjections(): Seq[FieldProjection[_, R]] = node
      .relation.fields.map(f => new FieldProjection(node, f))

  def subProjections() = _fieldProjections

  protected def _readCell[T](rs: ResultSet, vh: ValueHolder[T, R])(implicit ormConf: ORMConfiguration): Option[T] = vh match {
    case f: Field[T, R] => _fieldProjections.find(_.field == f)
        .flatMap(_.asInstanceOf[Projection[T]].read(rs))
    case a: Association[T, R, _] => _readCell(rs, a.field)
    case p: FieldComposition2[Any, Any, R] => (_readCell(rs, p._1), _readCell(rs, p._2)) match {
      case (Some(v1), Some(v2)) => Some((v1, v2).asInstanceOf[T])
      case _ => None
    }
  }

  def read(rs: ResultSet)(implicit ormConf: ORMConfiguration): Option[R] = { 
     val tx: Transaction = ormConf.transactionManager.get
    _readCell(rs, node.relation.PRIMARY_KEY).flatMap(id =>  
    tx.cache.cacheRecord(id, node.relation, Some(readRecord(rs))))
  }
  
  protected def readRecord(rs: ResultSet)(implicit ormConf: ORMConfiguration): R = {
    val record: R = node.relation.recordClass.newInstance
    _fieldProjections.foreach { p =>
      node.relation.getField(record, p.field.asInstanceOf[Field[Any, R]]).set(p.read(rs))
    }
    record
  }

  override def equals(obj: Any) = obj match {
    case p: RecordProjection[_, _] => this.node == p.node
    case _ => false
  }

  override def hashCode = node.hashCode
}

class UntypedTupleProjection(val _subProjections: Projection[_]*)
    extends CompositeProjection[Array[Option[Any]]] {
   def subProjections() = _subProjections
  def read(rs: ResultSet)(implicit ormConf: ORMConfiguration): Option[Array[Option[Any]]] = Some(subProjections.map(_.read(rs)).toArray)
}

class PairProjection[T1, T2] (_1: Projection[T1], _2: Projection[T2])
    extends CompositeProjection[(Option[T1], Option[T2])] {
  def subProjections() = List[Projection[_]](_1, _2)
  def read(rs: ResultSet)(implicit ormConf: ORMConfiguration): Option[(Option[T1], Option[T2])] =
    Some((_1.read(rs), _2.read(rs)))
}

class AliasMapProjection(val _subProjections: Seq[Projection[_]])
    extends CompositeProjection[Map[String, Any]] {
  def subProjections() = _subProjections
  def read(rs: ResultSet)(implicit ormConf: ORMConfiguration): Option[Map[String, Any]] = {
    val pairs = subProjections.flatMap { p =>
      p.read(rs).map(v => p.alias -> v).asInstanceOf[Option[(String, Any)]]
    }
    Some(Map[String, Any](pairs: _*))
  }
}
