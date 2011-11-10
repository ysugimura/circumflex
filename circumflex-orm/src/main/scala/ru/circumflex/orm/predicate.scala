package ru.circumflex
package orm

/*!# Predicates

`Predicate` is essentially a parameterized expression which yields boolean
value when executed by database.

Predicates are designed to participate in `WHERE` clauses of SQL queries.
*/
trait Predicate extends Expression {    
  def isEmptyPredicate(implicit ormConf: ORMConfiguration) = 
    parameters.isEmpty && ormConf.dialect.emptyPredicate == toSql
}

object Predicate {
  implicit def toAggregateHelper(predicate: Predicate) =
    new AggregatePredicateHelper(predicate)
}

object EmptyPredicate extends Predicate {
  def parameters()(implicit ormConf: ORMConfiguration): scala.Seq[Any] = Nil
  def toSql()(implicit ormConf: ORMConfiguration): String = ormConf.dialect.emptyPredicate
}

class SimpleExpression(val expression: String, val _parameters: Seq[Any])(implicit ormConf: ORMConfiguration)
    extends Predicate {
  def parameters()(implicit ormConf: ORMConfiguration) = _parameters
  def toSql()(implicit ormConf: ORMConfiguration) = expression
}

class AggregatePredicate(operatorGet: (ORMConfiguration) => String,
                         protected var _predicates: Seq[Predicate])
    extends Predicate {
  
  def operator()(implicit ormConf: ORMConfiguration): String = operatorGet(ormConf)
  def parameters()(implicit ormConf: ORMConfiguration) = predicates.flatMap(_.parameters)
  def add(predicate: Predicate*): this.type = {
    _predicates ++= predicate.toList
    this
  }
  def predicates: Seq[Predicate] = _predicates.flatMap {
    case EmptyPredicate => None
    case p: AggregatePredicate if (p.predicates.size == 0) => None
    case p: AggregatePredicate if (p.predicates.size == 1) =>
      Some(p.predicates(0))
    case p => Some(p)
  }
  def toSql()(implicit ormConf: ORMConfiguration): String = {
    val p = predicates
    if (p.size == 0) EmptyPredicate.toSql
    else "(" + p.map(_.toSql).mkString(" " + operator + " ") + ")"
  }
}

class SubqueryExpression[T](expression: String,
                            val subquery: SQLQuery[T])(implicit ormConf: ORMConfiguration)
    extends SimpleExpression(
      ormConf.dialect.subquery(expression, subquery), subquery.parameters)

/*! `SimpleExpressionHelper` is used to compose predicates in a DSL-style.
`String` expressions are converted to `SimpleExpressionHelper` implicitly.
 */
class SimpleExpressionHelper(val expr: String) {

  // Simple expressions

  def EQ(value: Any)(implicit ormConf: ORMConfiguration) = new SimpleExpression(ormConf.dialect.EQ(expr), List(value))
  def NE(value: Any)(implicit ormConf: ORMConfiguration) = new SimpleExpression(ormConf.dialect.NE(expr), List(value))
  def GT(value: Any)(implicit ormConf: ORMConfiguration) = new SimpleExpression(ormConf.dialect.GT(expr), List(value))
  def GE(value: Any)(implicit ormConf: ORMConfiguration) = new SimpleExpression(ormConf.dialect.GE(expr), List(value))
  def LT(value: Any)(implicit ormConf: ORMConfiguration) = new SimpleExpression(ormConf.dialect.LT(expr), List(value))
  def LE(value: Any)(implicit ormConf: ORMConfiguration) = new SimpleExpression(ormConf.dialect.LE(expr), List(value))
  def IS_NULL(implicit ormConf: ORMConfiguration) = new SimpleExpression(ormConf.dialect.IS_NULL(expr), Nil)
  def IS_NOT_NULL(implicit ormConf: ORMConfiguration) = new SimpleExpression(ormConf.dialect.IS_NOT_NULL(expr), Nil)
  def LIKE(value: Any)(implicit ormConf: ORMConfiguration) = new SimpleExpression(ormConf.dialect.LIKE(expr), List(value))
  def ILIKE(value: Any)(implicit ormConf: ORMConfiguration) = new SimpleExpression(ormConf.dialect.ILIKE(expr), List(value))
  def IN(params: Any*)(implicit ormConf: ORMConfiguration) = new SimpleExpression(
    ormConf.dialect.parameterizedIn(expr, params.map(p => "?")), params.toList)
  def BETWEEN(lowerValue: Any, upperValue: Any)(implicit ormConf: ORMConfiguration) = new SimpleExpression(
    ormConf.dialect.BETWEEN(expr), List(lowerValue, upperValue))

  // Simple subqueries

  def IN(query: SQLQuery[_])(implicit ormConf: ORMConfiguration) =
    new SubqueryExpression(ormConf.dialect.IN(expr), query)
  def NOT_IN(query: SQLQuery[_])(implicit ormConf: ORMConfiguration) =
    new SubqueryExpression(ormConf.dialect.NOT_IN(expr), query)

  def EQ_ALL(query: SQLQuery[_])(implicit ormConf: ORMConfiguration) =
    new SubqueryExpression(ormConf.dialect.EQ(expr, ormConf.dialect.ALL), query)
  def NE_ALL(query: SQLQuery[_])(implicit ormConf: ORMConfiguration) =
    new SubqueryExpression(ormConf.dialect.NE(expr, ormConf.dialect.ALL), query)
  def GT_ALL(query: SQLQuery[_])(implicit ormConf: ORMConfiguration) =
    new SubqueryExpression(ormConf.dialect.GT(expr, ormConf.dialect.ALL), query)
  def GE_ALL(query: SQLQuery[_])(implicit ormConf: ORMConfiguration) =
    new SubqueryExpression(ormConf.dialect.GE(expr, ormConf.dialect.ALL), query)
  def LT_ALL(query: SQLQuery[_])(implicit ormConf: ORMConfiguration) =
    new SubqueryExpression(ormConf.dialect.LT(expr, ormConf.dialect.ALL), query)
  def LE_ALL(query: SQLQuery[_])(implicit ormConf: ORMConfiguration) =
    new SubqueryExpression(ormConf.dialect.LE(expr, ormConf.dialect.ALL), query)

  def EQ_SOME(query: SQLQuery[_])(implicit ormConf: ORMConfiguration) =
    new SubqueryExpression(ormConf.dialect.EQ(expr, ormConf.dialect.SOME), query)
  def NE_SOME(query: SQLQuery[_])(implicit ormConf: ORMConfiguration) =
    new SubqueryExpression(ormConf.dialect.NE(expr, ormConf.dialect.SOME), query)
  def GT_SOME(query: SQLQuery[_])(implicit ormConf: ORMConfiguration) =
    new SubqueryExpression(ormConf.dialect.GT(expr, ormConf.dialect.SOME), query)
  def GE_SOME(query: SQLQuery[_])(implicit ormConf: ORMConfiguration) =
    new SubqueryExpression(ormConf.dialect.GE(expr, ormConf.dialect.SOME), query)
  def LT_SOME(query: SQLQuery[_])(implicit ormConf: ORMConfiguration) =
    new SubqueryExpression(ormConf.dialect.LT(expr, ormConf.dialect.SOME), query)
  def LE_SOME(query: SQLQuery[_])(implicit ormConf: ORMConfiguration) =
    new SubqueryExpression(ormConf.dialect.LE(expr, ormConf.dialect.SOME), query)
}

/*! `AggregatePredicateHelper` is used to compose predicates using infix notation. */
class AggregatePredicateHelper(predicate: Predicate) {
  def AND(predicates: Predicate*) =
    new AggregatePredicate(_.dialect.AND, predicate::predicates.toList)
  def OR(predicates: Predicate*) =
    new AggregatePredicate(_.dialect.OR, predicate::predicates.toList)
}